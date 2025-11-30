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
import android.os.IBinder;
import android.util.Log;
import androidx.core.app.NotificationCompat;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

// Сервис для записи GPS-данных в фоновом режиме
public class GpsRecordingService extends Service implements LocationListener {

    private static final String TAG = "GpsRecordingService";
    private static final int NOTIFICATION_ID = 1;
    private static final String CHANNEL_ID = "GpsRecordingChannel";
    private static final SimpleDateFormat ISO_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US);

    // Константы для BroadcastReceiver
    public static final String ACTION_PAUSE_RESUME = "ACTION_PAUSE_RESUME";
    public static final String ACTION_REFUEL = "ACTION_REFUEL";

    public static boolean isServiceRunning = false;

    private LocationManager locationManager;
    private DatabaseHelper dbHelper;
    private TripRecordingRepository repository;

    // Данные поездки
    private int carId;
    private String tripName;
    private double initialFuel;
    private double totalDistanceKm = 0.0;
    private double fuelRecharged = 0.0;
    private long startTimeMs = 0;
    private long pauseTimeOffsetMs = 0;
    private long lastPauseStartMs = 0;
    private Location lastLocation = null;
    private boolean isPaused = false;
    private double finalFuel = -1.0;

    // BroadcastReceiver для команд управления (Пауза/Продолжить/Заправка)
    private final BroadcastReceiver controlReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent == null || repository == null) return;

            String action = intent.getAction();

            if (ACTION_PAUSE_RESUME.equals(action)) {
                togglePauseResume();
            } else if (ACTION_REFUEL.equals(action)) {
                // Логика заправки
                double amount = intent.getDoubleExtra("AMOUNT", 0.0);
                if (amount > 0) {
                    fuelRecharged += amount;
                    updateRepository(); // Обновление LiveData
                    Log.d(TAG, String.format(Locale.getDefault(), "Заправлено: %.2f L. Общее заправленное: %.2f L", amount, fuelRecharged));
                }
            }
        }
    };

    /**
     * Подавляем предупреждение линтера, которое требует флаг RECEIVER_EXPORTED/NOT_EXPORTED
     * для вызова registerReceiver на старых API (24-32).
     */
    @SuppressLint({"MissingPermission", "UnspecifiedRegisterReceiverFlag"})
    @Override
    public void onCreate() {
        super.onCreate();
        ISO_DATE_FORMAT.setTimeZone(TimeZone.getTimeZone("UTC"));

        createNotificationChannel();
        startForeground(NOTIFICATION_ID, buildNotification(0.0, 0));

        dbHelper = new DatabaseHelper(this);

        // ИСПРАВЛЕНО: Прямое получение экземпляра Repository.
        // Singleton-паттерн гарантирует, что он будет создан при первом вызове.
        repository = TripRecordingRepository.getInstance();
        if (repository == null) {
            Log.e(TAG, "Критическая ошибка: не удалось получить TripRecordingRepository.");
            stopSelf();
            return;
        }

        isServiceRunning = true;

        // --- РЕГИСТРАЦИЯ КОНТРОЛЬНОГО BROADCASTRECEIVER ---
        IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_PAUSE_RESUME);
        filter.addAction(ACTION_REFUEL);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(controlReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
        } else {
            registerReceiver(controlReceiver, filter);
        }
        // ------------------------------------------------------------------------------------

        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null) {
            stopSelf();
            return START_NOT_STICKY;
        }

        if (startTimeMs == 0) {
            carId = intent.getIntExtra("CAR_ID", -1);
            tripName = intent.getStringExtra("TRIP_NAME");
            initialFuel = intent.getDoubleExtra("INITIAL_FUEL", 0.0);
            startTimeMs = System.currentTimeMillis();
            Log.d(TAG, "Поездка начата. Начальное топливо: " + initialFuel);
        }

        // Запрос обновлений GPS
        try {
            locationManager.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER,
                    10000,
                    10,
                    this);
            Log.d(TAG, "GPS updates requested.");
        } catch (SecurityException e) {
            Log.e(TAG, "Нет разрешения на GPS. Невозможно запустить сервис.", e);
            stopSelf();
        }

        startDurationUpdateThread();

        // Если пришла команда остановки
        double receivedFinalFuel = intent.getDoubleExtra("FINAL_FUEL", -1.0);
        if (receivedFinalFuel != -1.0) {
            this.finalFuel = receivedFinalFuel;
            Log.d(TAG, "Получен сигнал остановки с остатком топлива: " + finalFuel);
            handleStopCommand();
        }

        return START_STICKY;
    }

    private void handleStopCommand() {
        if (finalFuel != -1.0) {
            saveTripToDatabase();
        } else {
            Log.w(TAG, "Команда остановки без finalFuel.");
        }

        // ОТПРАВКА СИГНАЛА Activity, ЧТО СЕРВИС ОСТАНАВЛИВАЕТСЯ И ДАННЫЕ СБРОШЕНЫ
        if (repository != null) {
            repository.postReset();
        }

        stopSelf();
    }

    // --- ПОТОК ДЛЯ ОБНОВЛЕНИЯ ВРЕМЕНИ В LIVE DATA ---
    private Thread durationUpdateThread;

    private void startDurationUpdateThread() {
        if (durationUpdateThread == null || !durationUpdateThread.isAlive()) {
            durationUpdateThread = new Thread(new Runnable() {
                @Override
                public void run() {
                    while (isServiceRunning) {
                        try {
                            Thread.sleep(1000);
                            if (!isPaused) {
                                // ОБЯЗАТЕЛЬНОЕ обновление LiveData для обновления времени в UI
                                updateRepository();
                            }
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            Log.d(TAG, "Поток обновления времени прерван.");
                        }
                    }
                }
            });
            durationUpdateThread.start();
            Log.d(TAG, "Поток обновления длительности запущен.");
        }
    }

    // --- ЛОГИКА GPS ОБНОВЛЕНИЯ И РАСЧЕТА ДИСТАНЦИИ/ВРЕМЕНИ ---

    @Override
    public void onLocationChanged(Location location) {
        if (isPaused || repository == null || location == null) return;

        if (lastLocation != null) {
            float distanceMeters = lastLocation.distanceTo(location);
            totalDistanceKm += distanceMeters / 1000.0;
        }
        lastLocation = location;

        updateRepository();
    }

    private void updateRepository() {
        if (repository == null || startTimeMs == 0) return;

        long totalDurationMs = calculateDurationMs();

        TripRecordingData data = new TripRecordingData(
                totalDistanceKm,
                totalDurationMs,
                fuelRecharged,
                isPaused
        );
        repository.postUpdate(data);

        updateNotification(totalDistanceKm, totalDurationMs);
    }

    /**
     * Корректный расчет длительности с учетом всех пауз.
     */
    private long calculateDurationMs() {
        if (startTimeMs == 0) return 0;

        long currentTotalElapsed = System.currentTimeMillis() - startTimeMs;
        long timePaused = pauseTimeOffsetMs;

        if (isPaused) {
            // Если сейчас на паузе, добавляем время текущей паузы к общему смещению
            timePaused += (System.currentTimeMillis() - lastPauseStartMs);
        }

        return currentTotalElapsed - timePaused;
    }

    // --- ЛОГИКА ПАУЗЫ/ПРОДОЛЖЕНИЯ ---

    private void togglePauseResume() {
        isPaused = !isPaused;

        if (isPaused) {
            lastPauseStartMs = System.currentTimeMillis();
            Log.d(TAG, "Запись приостановлена.");
        } else {
            if (lastPauseStartMs > 0) {
                // Добавляем время текущей паузы к общему смещению
                pauseTimeOffsetMs += (System.currentTimeMillis() - lastPauseStartMs);
                lastPauseStartMs = 0;
            }
            Log.d(TAG, "Запись возобновлена.");
        }
        updateRepository(); // Обновление LiveData для кнопки/статуса
    }

    // --- ОСТАНОВКА СЕРВИСА И СОХРАНЕНИЕ ---

    @Override
    public void onDestroy() {
        super.onDestroy();
        isServiceRunning = false;

        if (durationUpdateThread != null) {
            durationUpdateThread.interrupt();
        }

        if (locationManager != null) {
            locationManager.removeUpdates(this);
        }

        // Отмена регистрации Receiver
        unregisterReceiver(controlReceiver);

        Log.d(TAG, "GpsRecordingService остановлен.");
    }

    // ... (saveTripToDatabase, formatMsToISO8601, Notification methods без изменений) ...

    private void saveTripToDatabase() {
        if (carId == -1 || dbHelper == null) {
            Log.e(TAG, "Невозможно сохранить поездку: ID автомобиля или DatabaseHelper недействительны.");
            return;
        }

        long totalDurationMs = calculateDurationMs();
        double distanceKm = totalDistanceKm;

        double fuelUsed = (initialFuel + fuelRecharged) - finalFuel;
        if (fuelUsed < 0) {
            fuelUsed = 0;
        }

        double fuelConsumption = 0.0;
        if (distanceKm > 0.1) { // Проверяем, чтобы избежать деления на ноль
            fuelConsumption = (fuelUsed / distanceKm) * 100.0;
        }

        String startDateTime = formatMsToISO8601(startTimeMs);
        String endDateTime = formatMsToISO8601(System.currentTimeMillis());

        Trip trip = new Trip(
                carId,
                tripName,
                startDateTime,
                endDateTime,
                distanceKm,
                fuelUsed,
                fuelConsumption
        );

        long result = dbHelper.addTrip(trip);
        if (result > 0) {
            Log.d(TAG, "Поездка успешно сохранена в БД, ID: " + result);
        } else {
            Log.e(TAG, "Ошибка при сохранении поездки в БД.");
        }
    }

    private String formatMsToISO8601(long timestampMs) {
        return ISO_DATE_FORMAT.format(new Date(timestampMs));
    }

    private Notification buildNotification(double distanceKm, long durationMs) {
        String durationStr = formatDuration(durationMs);
        String contentText = String.format(Locale.getDefault(),
                "Пробег: %.2f км | Время: %s", distanceKm, durationStr);

        Intent notificationIntent = new Intent(this, GpsRecordingActivity.class);
        notificationIntent.putExtra("CAR_ID", carId);

        PendingIntent pendingIntent = PendingIntent.getActivity(
                this,
                0,
                notificationIntent,
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT
        );

        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle(tripName != null ? tripName : "Запись поездки...")
                .setContentText(contentText)
                .setSmallIcon(R.drawable.ic_car)
                .setContentIntent(pendingIntent)
                .setOngoing(true)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .build();
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