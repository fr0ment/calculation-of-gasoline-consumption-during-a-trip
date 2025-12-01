package com.example.cogcdat_2;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.SystemClock;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;

import java.util.Locale;

// Сервис для записи GPS-данных в фоновом режиме
public class GpsRecordingService extends Service implements LocationListener {

    private static final String TAG = "GpsRecordingService";
    private static final int NOTIFICATION_ID = 1;
    private static final String CHANNEL_ID = "GpsRecordingChannel";

    // Константы для BroadcastReceiver
    public static final String ACTION_PAUSE_RESUME = "com.example.cogcdat_2.ACTION_PAUSE_RESUME";

    private LocationManager locationManager;
    private Location lastLocation;
    private double totalDistanceKm = 0.0;

    // Состояние времени
    private long timeStarted = 0;
    private long timePaused = 0;
    private long totalTimePaused = 0;
    private boolean isPaused = false;

    // ID автомобиля для восстановления Activity из уведомления
    private int carId = -1;

    // Таймер для обновления LiveData и уведомления каждую секунду
    private final Handler handler = new Handler();
    private final long UPDATE_INTERVAL = 1000; // 1 секунда

    private final Runnable timerRunnable = new Runnable() {
        @Override
        public void run() {
            // FIX: Добавлено условие проверки, что запись активна
            if (!isPaused && TripRecordingRepository.getInstance().isRecording()) {
                // Обновляем LiveData в репозитории
                long currentDuration = calculateCurrentDurationMs();
                TripRecordingRepository.getInstance().updateTripUpdates(totalDistanceKm, currentDuration);

                // Обновляем уведомление
                updateNotification(totalDistanceKm, currentDuration);
            }
            // Планируем следующее выполнение
            handler.postDelayed(this, UPDATE_INTERVAL);
        }
    };

    // Ресивер для действий из уведомления и Activity
    private final BroadcastReceiver notificationActionReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (ACTION_PAUSE_RESUME.equals(intent.getAction())) {
                togglePauseResume();
            }
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        // Регистрируем ресивер для паузы/возобновления
        // Используем константу для флага экспорта (для совместимости)
        int flags = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU ?
                Context.RECEIVER_EXPORTED : 0;

        // FIX: Использовать registerReceiver с IntentFilter, который создается всегда,
        // а не только для O и выше, хотя флаг экспорта актуален для более новых версий.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            registerReceiver(notificationActionReceiver, new IntentFilter(ACTION_PAUSE_RESUME), flags);
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        // Сохраняем ID автомобиля при старте сервиса
        if (intent != null) {
            carId = intent.getIntExtra("car_id", -1);
        }

        // Инициализация при старте (если не была уже начата)
        if (!TripRecordingRepository.getInstance().isRecording()) {
            // Инициализация репозитория (установка времени начала, сброс топлива)
            // InitialFuelLevel уже должен быть установлен из Activity
            TripRecordingRepository.getInstance().startRecording();

            timeStarted = SystemClock.elapsedRealtime();
            totalDistanceKm = 0.0;
            totalTimePaused = 0;
            isPaused = false;

            // Начинаем работу GPS и таймера
            startLocationUpdates();
            handler.post(timerRunnable);
        }

        // Запускаем как Foreground Service
        startForeground(NOTIFICATION_ID, buildNotification(totalDistanceKm, calculateCurrentDurationMs()));

        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        stopLocationUpdates();
        handler.removeCallbacks(timerRunnable);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            unregisterReceiver(notificationActionReceiver);
        }

        // FIX: Убран вызов reset(), т.к. он уже вызывается из GpsRecordingActivity
        // при сохранении/остановке. Если stopService() вызван не из Activity,
        // Activity все равно не ждет сигнала, поэтому сброс здесь не нужен/дублирует.
        // TripRecordingRepository.getInstance().reset();
    }

    // --- Локация и Расстояние ---

    @SuppressLint("MissingPermission")
    private void startLocationUpdates() {
        try {
            // Запрашиваем обновления каждую секунду или при смещении на 5 метров
            // FIX: Проверка на isPaused
            if (!isPaused) {
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 5, this);
            }
        } catch (SecurityException e) {
            Log.e(TAG, "Location permission missing: " + e.getMessage());
        }
    }

    private void stopLocationUpdates() {
        locationManager.removeUpdates(this);
    }

    @Override
    public void onLocationChanged(@NonNull Location location) {
        if (!isPaused && TripRecordingRepository.getInstance().isRecording()) { // FIX: Проверка isRecording
            if (lastLocation != null) {
                // Расстояние в метрах
                float distanceMeters = lastLocation.distanceTo(location);
                double distanceKm = distanceMeters / 1000.0;
                totalDistanceKm += distanceKm;
            }

            lastLocation = location;

            // Обновляем LiveData при получении новой локации (если таймер работает, это дополнительно)
            long currentDuration = calculateCurrentDurationMs();
            TripRecordingRepository.getInstance().updateTripUpdates(totalDistanceKm, currentDuration);
            updateNotification(totalDistanceKm, currentDuration);
        }
    }

    // --- Логика Паузы ---

    private void togglePauseResume() {
        // FIX: Проверка, что запись активна, прежде чем менять состояние
        if (!TripRecordingRepository.getInstance().isRecording()) return;

        isPaused = !isPaused;

        if (isPaused) {
            timePaused = SystemClock.elapsedRealtime();
            stopLocationUpdates(); // Останавливаем GPS для экономии батареи
        } else {
            if (timePaused != 0) {
                totalTimePaused += (SystemClock.elapsedRealtime() - timePaused);
                timePaused = 0;
            }
            startLocationUpdates(); // Возобновляем GPS
        }

        // Обновляем LiveData в репозитории, чтобы Activity обновило UI (статус и кнопки)
        TripRecordingRepository.getInstance().setPaused(isPaused); // Используем метод репозитория
        // Обновление уведомления
        updateNotification(totalDistanceKm, calculateCurrentDurationMs());
    }

    // --- Логика Времени ---

    /**
     * Рассчитывает общую продолжительность поездки (в мс) за вычетом времени паузы.
     */
    private long calculateCurrentDurationMs() {
        if (timeStarted == 0) return 0;

        long totalElapsedTime = SystemClock.elapsedRealtime() - timeStarted;
        long currentTotalTimePaused = totalTimePaused;

        // Если сейчас на паузе, добавляем время текущей паузы
        if (isPaused && timePaused != 0) {
            currentTotalTimePaused += (SystemClock.elapsedRealtime() - timePaused);
        }

        return totalElapsedTime - currentTotalTimePaused;
    }

    // --- Уведомление ---

    private Notification buildNotification(double distanceKm, long durationMs) {
        // Создаем Intent для открытия Activity при клике на уведомление
        Intent notificationIntent = new Intent(this, GpsRecordingActivity.class);

        // ВАЖНО: Добавляем ID автомобиля, чтобы Activity могла корректно восстановиться
        notificationIntent.putExtra("car_id", carId);

        // NEW: Флаги для singleTask: Новый task, но singleTop (bring to front если уже запущена)
        notificationIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);

        PendingIntent pendingIntent = PendingIntent.getActivity(this,
                0, notificationIntent, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);

        // Создаем Intent для действия Пауза/Возобновление
        Intent pauseResumeIntent = new Intent(ACTION_PAUSE_RESUME);
        PendingIntent pauseResumePendingIntent = PendingIntent.getBroadcast(this,
                1, pauseResumeIntent, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);

        String buttonText = isPaused ? "ПРОДОЛЖИТЬ" : "ПАУЗА";
        // Предполагается, что ic_car существует и используется как плейсхолдер
        int buttonIcon = R.drawable.ic_car;

        String title = isPaused ? "Пауза записи поездки" : "Идет запись поездки";

        // Включаем заправленное топливо в уведомление
        double fuelRecharged = TripRecordingRepository.getInstance().getTotalFuelRecharged();
        String content = String.format(Locale.getDefault(),
                "Пробег: %.1f км | Время: %s | Заправка: %.2f л",
                distanceKm, formatDuration(durationMs), fuelRecharged);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle(title)
                .setContentText(content)
                .setSmallIcon(R.drawable.ic_car) // Предполагается, что ic_car существует
                .setContentIntent(pendingIntent)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                // ДОБАВЛЕНО: делает уведомление "неубираемым"
                .setOngoing(true)
                .setLocalOnly(true)
                .addAction(buttonIcon, buttonText, pauseResumePendingIntent);

        return builder.build();
    }

    private void updateNotification(double distanceKm, long durationMs) {
        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager != null) {
            manager.notify(NOTIFICATION_ID, buildNotification(distanceKm, durationMs));
        }
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel serviceChannel = new NotificationChannel(
                    CHANNEL_ID,
                    "GPS Recording Service Channel",
                    NotificationManager.IMPORTANCE_LOW
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(serviceChannel);
            }
        }
    }

    private String formatDuration(long durationMs) {
        long seconds = durationMs / 1000;
        long minutes = (seconds % 3600) / 60;
        long hours = seconds / 3600;
        // Формат ЧЧ:ММ:СС
        return String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, seconds % 60);
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {}
    @Override
    public void onProviderEnabled(String provider) {}
    @Override
    public void onProviderDisabled(String provider) {}

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}