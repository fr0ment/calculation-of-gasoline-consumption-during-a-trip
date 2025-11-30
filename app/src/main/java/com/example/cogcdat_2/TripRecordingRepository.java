package com.example.cogcdat_2;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import android.content.Context;
import android.util.Log;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

/**
 * Репозиторий, реализующий шаблон Singleton, для обмена данными (TripRecordingData)
 * между GpsRecordingService и GpsRecordingActivity.
 */
public class TripRecordingRepository {

    private static final String TAG = "TripRecordingRepository";
    private static final SimpleDateFormat DB_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());

    // Единственный экземпляр репозитория
    private static TripRecordingRepository instance;
    private static Context applicationContext;

    // LiveData для хранения и передачи обновлений о поездке
    private final MutableLiveData<TripRecordingData> tripUpdates = new MutableLiveData<>();

    // Состояние поездки (НОВЫЕ ПОЛЯ)
    private double currentDistanceKm = 0.0;
    private long currentDurationMs = 0;
    private double totalFuelRecharged = 0.0;
    private boolean isPaused = false;
    private boolean isRecording = false; // Флаг активной записи
    private double initialFuelLevel = 0.0;

    // Хранение времени начала и текущего времени
    private String startDateTime = null;
    private String currentDateTime = null;


    private TripRecordingRepository(Context context) {

    }

    /**
     * Инициализация Singleton. Должна быть вызвана при запуске приложения.
     */
    public static synchronized void initialize(Context context) {
        if (instance == null) {
            applicationContext = context.getApplicationContext();
            instance = new TripRecordingRepository(applicationContext);
        }
    }

    /**
     * Возвращает единственный экземпляр репозитория.
     */
    public static synchronized TripRecordingRepository getInstance() {
        if (instance == null) {
            throw new IllegalStateException("TripRecordingRepository must be initialized!");
        }
        return instance;
    }

    // --- Методы доступа к LiveData ---
    public LiveData<TripRecordingData> getTripUpdates() {
        return tripUpdates;
    }

    // --- Методы управления состоянием ---

    /**
     * Возвращает true, если запись активна. (ИСПРАВЛЕНО: Раньше метод отсутствовал)
     */
    public boolean isRecording() {
        return isRecording;
    }

    /**
     * Запускает запись поездки. (ИСПРАВЛЕНО: Раньше метод отсутствовал/был с другими параметрами)
     */
    public void startRecording() {
        if (isRecording) return;
        isRecording = true;
        isPaused = false;
        currentDistanceKm = 0.0;
        currentDurationMs = 0;
        totalFuelRecharged = 0.0;
        // Установка времени начала
        // FIX: Устанавливаем часовой пояс в UTC, если не указано, чтобы сохранить согласованность с ISO8601,
        // но здесь используется Locale.getDefault(), поэтому оставляем без изменения формата.
        startDateTime = DB_DATE_FORMAT.format(new Date());

        // Публикация начальных данных
        tripUpdates.postValue(new TripRecordingData(
                currentDistanceKm,
                currentDurationMs,
                totalFuelRecharged,
                isPaused
        ));
    }

    /**
     * Обновляет данные о поездке и публикует их.
     * ИСПРАВЛЕНО: Теперь принимает только 2 аргумента.
     * @param distanceKm Общая дистанция.
     * @param durationMs Общая длительность.
     */
    public void updateTripUpdates(double distanceKm, long durationMs) {
        if (!isRecording || isPaused) return;

        currentDistanceKm = distanceKm;
        currentDurationMs = durationMs;

        // Публикация обновленных данных
        tripUpdates.postValue(new TripRecordingData(
                currentDistanceKm,
                currentDurationMs,
                totalFuelRecharged,
                isPaused
        ));
        // Обновление текущего времени (для времени окончания)
        currentDateTime = DB_DATE_FORMAT.format(new Date());
    }

    /**
     * Устанавливает/снимает паузу.
     */
    public void setPaused(boolean paused) {
        this.isPaused = paused;
        // Публикация обновления состояния
        tripUpdates.postValue(new TripRecordingData(
                currentDistanceKm,
                currentDurationMs,
                totalFuelRecharged,
                isPaused
        ));
    }
    /**
     * Возвращает общее количество заправленного топлива. (НОВЫЙ ГЕТТЕР)
     */
    public double getTotalFuelRecharged() {
        return totalFuelRecharged;
    }

    /**
     * Возвращает пройденное расстояние
     */
    public double getCurrentDistanceKm() {
        return currentDistanceKm;
    }

    /**
     * Добавляет заправленное топливо.
     */
    public void addRefuel(double amount) {
        totalFuelRecharged += amount;
        // Публикация обновления
        tripUpdates.postValue(new TripRecordingData(
                currentDistanceKm,
                currentDurationMs,
                totalFuelRecharged,
                isPaused
        ));
    }

    /**
     * Сбрасывает состояние и сигнализирует об остановке.
     * ИСПРАВЛЕНО: Название метода - reset().
     */
    public void reset() {
        isRecording = false;

        // Отправка специального значения, которое Activity воспримет как сигнал к закрытию
        tripUpdates.postValue(new TripRecordingData(-1.0, 0, 0.0, false));

        // Сброс данных репозитория
        currentDistanceKm = 0.0;
        currentDurationMs = 0;
        totalFuelRecharged = 0.0;
        isPaused = false;
        startDateTime = null;
        currentDateTime = null;
    }

    /**
     * Возвращает время начала поездки.
     */
    public String getStartDateTime() {
        // В идеале, startDateTime не должен быть null, если вызывается при сохранении
        return startDateTime;
    }

    /**
     * Возвращает текущее время поездки (которое будет использоваться как EndDateTime).
     */
    public String getCurrentDateTime() {
        // В идеале, currentDateTime не должен быть null, если вызывается при сохранении
        return currentDateTime;
    }

    public long getCurrentDurationMs() {
        return currentDurationMs;
    }

    /**
     * Устанавливает начальный уровень топлива.
     * Вызывается из GpsRecordingActivity перед стартом записи.
     */
    public void setInitialFuelLevel(double initialFuelLevel) { // <--- ДОБАВИТЬ ЭТОТ МЕТОД
        this.initialFuelLevel = initialFuelLevel;
    }

    /**
     * Возвращает начальный уровень топлива. (Может понадобиться для расчетов при сохранении поездки)
     */
    public double getInitialFuelLevel() { // <--- РЕКОМЕНДУЕТСЯ ДОБАВИТЬ
        return initialFuelLevel;
    }
}