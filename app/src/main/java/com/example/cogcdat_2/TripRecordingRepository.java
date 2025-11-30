package com.example.cogcdat_2;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import android.content.Context;
import android.util.Log;

/**
 * Репозиторий, реализующий шаблон Singleton, для обмена данными (TripRecordingData)
 * между GpsRecordingService и GpsRecordingActivity.
 * Инициализация должна происходить в классе Application.
 */
public class TripRecordingRepository {

    private static final String TAG = "TripRecordingRepository";

    // Единственный экземпляр репозитория
    private static TripRecordingRepository instance;
    private static Context applicationContext;

    // LiveData для хранения и передачи обновлений о поездке
    private final MutableLiveData<TripRecordingData> tripUpdates = new MutableLiveData<>();

    /**
     * Приватный конструктор для реализации Singleton.
     * @param context Контекст приложения.
     */
    private TripRecordingRepository(Context context) {
        // Сохраняем контекст приложения для безопасного использования в будущем
        applicationContext = context.getApplicationContext();

        // Устанавливаем начальное пустое значение
        // Предполагается, что класс TripRecordingData существует.
        // tripUpdates.setValue(new TripRecordingData(0.0, 0, 0.0, false));
        Log.d(TAG, "TripRecordingRepository: Новый экземпляр успешно создан.");
    }

    /**
     * Статический метод для обязательной инициализации синглтона
     * в классе Application. Должен быть вызван только один раз.
     * @param context Контекст, обычно из Application.onCreate().
     */
    public static synchronized void initialize(Context context) {
        if (instance == null) {
            instance = new TripRecordingRepository(context.getApplicationContext());
            Log.i(TAG, "TripRecordingRepository инициализирован.");
        } else {
            Log.w(TAG, "Попытка повторной инициализации TripRecordingRepository. Игнорируется.");
        }
    }

    /**
     * Предоставляет единственный экземпляр TripRecordingRepository.
     * @return Инициализированный экземпляр репозитория.
     * @throws IllegalStateException Если initialize() не был вызван в CarManagerApp.
     */
    public static TripRecordingRepository getInstance() {
        if (instance == null) {
            // Выброс исключения, чтобы предотвратить NullPointerException в Activity
            throw new IllegalStateException("TripRecordingRepository не был инициализирован. Вызовите initialize(Context) в CarManagerApp.");
        }
        return instance;
    }

    /**
     * Возвращает LiveData, на которую подписывается Activity.
     * @return LiveData с текущими данными о поездке.
     */
    public LiveData<TripRecordingData> getTripUpdates() {
        return tripUpdates;
    }

    /**
     * Обновляет данные о поездке, вызывается из GpsRecordingService.
     * Использует postValue для безопасного обновления из любого потока.
     * @param data Обновленные данные о поездке.
     */
    public void postUpdate(TripRecordingData data) {
        tripUpdates.postValue(data);
    }

    // Вспомогательный метод для сброса данных, например, после завершения поездки
    public void resetTripData() {
        // Предполагается, что класс TripRecordingData существует.
        // tripUpdates.postValue(new TripRecordingData(0.0, 0, 0.0, false));
    }


    /**
     * Сбрасывает данные поездки.
     * Используется сервисом как сигнал к Activity о завершении записи.
     */
    public void postReset() {
        // Отправка данных, которые Activity (GpsRecordingActivity) воспримет как сигнал к закрытию
        tripUpdates.postValue(new TripRecordingData(0.0, 0L, 0.0, false));
        Log.d(TAG, "LiveData reset posted. Signal to close Activity.");
    }
}