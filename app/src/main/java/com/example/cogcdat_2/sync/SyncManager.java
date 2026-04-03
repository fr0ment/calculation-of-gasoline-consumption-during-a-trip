package com.example.cogcdat_2.sync;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Handler;
import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.cogcdat_2.Car;
import com.example.cogcdat_2.DatabaseHelper;
import com.example.cogcdat_2.DistanceUnit;
import com.example.cogcdat_2.Language;
import com.example.cogcdat_2.SelectedCarManager;
import com.example.cogcdat_2.Theme;
import com.example.cogcdat_2.Trip;
import com.example.cogcdat_2.UserSettings;
import com.example.cogcdat_2.api.ApiClient;
import com.example.cogcdat_2.api.ApiService;
import com.example.cogcdat_2.api.models.ApiCar;
import com.example.cogcdat_2.api.models.ApiTrip;
import com.example.cogcdat_2.api.models.ApiUserSettings;
import com.example.cogcdat_2.api.models.LoginRequest;
import com.example.cogcdat_2.api.models.SyncRequest;
import com.example.cogcdat_2.api.models.SyncResponse;
import com.example.cogcdat_2.api.models.TokenResponse;
import com.example.cogcdat_2.api.models.User;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SyncManager {
    private static final String TAG = "SyncManager";
    private static final SimpleDateFormat DB_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
    private static final String PREF_LAST_SYNC = "last_sync_time";
    private static final String PREF_AUTH_TOKEN = "auth_token";
    private static final String PREF_USER_ID = "user_id";

    // Базовый URL сервера для изображений
    private static final String BASE_URL = "http://192.168.3.15:8000";

    private static SyncManager instance;
    private final Context context;
    private final DatabaseHelper dbHelper;
    private final ApiService apiService;
    private final MutableLiveData<Boolean> syncInProgress = new MutableLiveData<>(false);
    private final MutableLiveData<String> syncStatus = new MutableLiveData<>("");
    private final MutableLiveData<Boolean> isAuthenticated = new MutableLiveData<>(false);

    private boolean isFirstSyncAfterLogin = true;
    private String currentUserId = null;

    private SyncManager(Context context) {
        this.context = context.getApplicationContext();
        this.dbHelper = new DatabaseHelper(this.context);
        this.apiService = ApiClient.getInstance(this.context).getApiService();

        String token = getSavedToken();
        if (token != null) {
            ApiClient.getInstance(this.context).setAuthToken(token);
            isAuthenticated.setValue(true);

            // Загружаем userId из SharedPreferences
            currentUserId = context.getSharedPreferences("user", Context.MODE_PRIVATE)
                    .getString(PREF_USER_ID, null);
        }
    }

    public static synchronized SyncManager getInstance(Context context) {
        if (instance == null) {
            instance = new SyncManager(context);
        }
        return instance;
    }

    // ========== Аутентификация ==========

    public LiveData<Boolean> isAuthenticated() {
        return isAuthenticated;
    }

    public String getSavedToken() {
        return context.getSharedPreferences("auth", Context.MODE_PRIVATE)
                .getString(PREF_AUTH_TOKEN, null);
    }

    public String getCurrentUserId() {
        if (currentUserId == null) {
            currentUserId = context.getSharedPreferences("user", Context.MODE_PRIVATE)
                    .getString(PREF_USER_ID, null);
        }
        return currentUserId;
    }

    public void saveToken(String token) {
        context.getSharedPreferences("auth", Context.MODE_PRIVATE)
                .edit()
                .putString(PREF_AUTH_TOKEN, token)
                .apply();
        ApiClient.getInstance(context).setAuthToken(token);
        isAuthenticated.postValue(true);
    }

    public void saveUserId(String userId) {
        this.currentUserId = userId;
        context.getSharedPreferences("user", Context.MODE_PRIVATE)
                .edit()
                .putString(PREF_USER_ID, userId)
                .apply();
    }

    public void clearLocalData() {
        List<Car> cars = dbHelper.getAllCars(true);

        for (Car car : cars) {
            if (car.getImagePath() != null && !car.getImagePath().isEmpty()) {
                File imageFile = new File(car.getImagePath());
                if (imageFile.exists()) {
                    imageFile.delete();
                    Log.d(TAG, "Deleted image: " + imageFile.getAbsolutePath());
                }
            }
        }

        for (Car car : cars) {
            List<Trip> trips = dbHelper.getTripsForCar(car.getId(), true);
            for (Trip trip : trips) {
                dbHelper.permanentDeleteTrip(trip.getId());
            }
            dbHelper.permanentDeleteCar(car.getId());
        }

        context.getSharedPreferences("sync", Context.MODE_PRIVATE)
                .edit()
                .remove(PREF_LAST_SYNC)
                .apply();

        context.getSharedPreferences("user", Context.MODE_PRIVATE)
                .edit()
                .remove(PREF_USER_ID)
                .apply();

        SelectedCarManager.clear(context);
        currentUserId = null;

        Log.d(TAG, "Локальные данные очищены");
    }

    public void logout() {
        clearLocalData();

        context.getSharedPreferences("auth", Context.MODE_PRIVATE)
                .edit()
                .remove(PREF_AUTH_TOKEN)
                .apply();

        ApiClient.getInstance(context).setAuthToken(null);
        isAuthenticated.postValue(false);
        isFirstSyncAfterLogin = true;

        Log.d(TAG, "Выход из аккаунта выполнен, данные очищены");
    }

    public interface AuthCallback {
        void onSuccess(String token);
        void onError(String error);
    }

    public void login(String username, String password, AuthCallback callback) {
        LoginRequest request = new LoginRequest(username, password);

        apiService.login(request).enqueue(new Callback<TokenResponse>() {
            @Override
            public void onResponse(Call<TokenResponse> call, Response<TokenResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    String token = response.body().getAccessToken();

                    String oldToken = getSavedToken();
                    if (oldToken != null && !oldToken.equalsIgnoreCase(token)) {
                        clearLocalData();
                    }

                    saveToken(token);

                    // Сразу после получения токена загружаем информацию о пользователе
                    loadUserInfo(token, callback);

                } else {
                    String error = "Ошибка входа: " + response.code();
                    try {
                        if (response.errorBody() != null) {
                            error = response.errorBody().string();
                        }
                    } catch (IOException e) {
                        // Игнорируем
                    }
                    callback.onError(error);
                }
            }

            @Override
            public void onFailure(Call<TokenResponse> call, Throwable t) {
                callback.onError("Ошибка сети: " + t.getMessage());
            }
        });
    }

    private void loadUserInfo(String token, AuthCallback callback) {
        apiService.getCurrentUser("Bearer " + token).enqueue(new Callback<User>() {
            @Override
            public void onResponse(Call<User> call, Response<User> response) {
                if (response.isSuccessful() && response.body() != null) {
                    User user = response.body();
                    saveUserId(user.getId());

                    // Сохраняем информацию о пользователе в SharedPreferences
                    context.getSharedPreferences("user", Context.MODE_PRIVATE)
                            .edit()
                            .putString("username", user.getUsername())
                            .putString("email", user.getEmail())
                            .putString("full_name", user.getFullName())
                            .apply();

                    callback.onSuccess(token);

                    // Загружаем все данные с сервера
                    forceSyncFromServer();
                } else {
                    callback.onError("Не удалось загрузить информацию о пользователе");
                }
            }

            @Override
            public void onFailure(Call<User> call, Throwable t) {
                callback.onError("Ошибка сети при загрузке информации о пользователе");
            }
        });
    }

    // ========== Синхронизация ==========

    public LiveData<Boolean> isSyncInProgress() {
        return syncInProgress;
    }

    public LiveData<String> getSyncStatus() {
        return syncStatus;
    }

    private void saveLastSyncTime() {
        String now = DB_DATE_FORMAT.format(new Date());
        context.getSharedPreferences("sync", Context.MODE_PRIVATE)
                .edit()
                .putString(PREF_LAST_SYNC, now)
                .apply();
    }

    private List<Trip> getAllTrips(boolean includeDeleted) {
        List<Trip> allTrips = new ArrayList<>();
        for (Car car : dbHelper.getAllCars(true)) {
            allTrips.addAll(dbHelper.getTripsForCar(car.getId(), includeDeleted));
        }
        return allTrips;
    }

    public void syncAll() {
        if (getSavedToken() == null) {
            syncStatus.postValue("Требуется авторизация");
            return;
        }

        syncInProgress.postValue(true);
        syncStatus.postValue("Начало синхронизации...");

        List<ApiCar> apiCars = new ArrayList<>();
        for (Car car : dbHelper.getAllCars(true)) {
            apiCars.add(new ApiCar(car));
        }

        List<ApiTrip> apiTrips = new ArrayList<>();
        for (Trip trip : getAllTrips(true)) {
            apiTrips.add(new ApiTrip(trip));
        }

        // Получаем настройки пользователя ТОЛЬКО если есть userId
        ApiUserSettings apiSettings = null;
        if (currentUserId != null) {
            // Используем прямой запрос к БД, но БЕЗ создания новых настроек
            UserSettings settings = getUserSettingsDirect(currentUserId);
            if (settings != null) {
                apiSettings = new ApiUserSettings(settings);
            }
        }



        String lastSync = getLastSyncTime();
        SyncRequest request = new SyncRequest(lastSync, apiCars, apiTrips, apiSettings);

        apiService.syncData("Bearer " + getSavedToken(), request).enqueue(new Callback<SyncResponse>() {
            @Override
            public void onResponse(Call<SyncResponse> call, Response<SyncResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    processSyncResponse(response.body());
                    saveLastSyncTime();
                    syncStatus.postValue("Синхронизация завершена");
                } else {
                    String error = "Ошибка синхронизации: " + response.code();
                    try {
                        if (response.errorBody() != null) {
                            error = response.errorBody().string();
                        }
                    } catch (IOException e) {
                        // Игнорируем
                    }
                    syncStatus.postValue(error);
                }
                syncInProgress.postValue(false);
            }

            @Override
            public void onFailure(Call<SyncResponse> call, Throwable t) {
                syncStatus.postValue("Ошибка сети: " + t.getMessage());
                syncInProgress.postValue(false);
            }
        });
    }
    private Handler syncHandler = new Handler();
    private Runnable syncRunnable;

    private UserSettings getUserSettingsDirect(String userId) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.query("user_settings", null,
                "user_id=?", new String[]{userId},
                null, null, null);

        UserSettings settings = null;
        if (cursor != null && cursor.moveToFirst()) {
            settings = new UserSettings();
            settings.setId(cursor.getString(cursor.getColumnIndexOrThrow("id")));
            settings.setUserId(cursor.getString(cursor.getColumnIndexOrThrow("user_id")));
            settings.setDistanceUnit(cursor.getString(cursor.getColumnIndexOrThrow("distance_unit")));
            settings.setTheme(cursor.getString(cursor.getColumnIndexOrThrow("theme")));
            settings.setLanguage(cursor.getString(cursor.getColumnIndexOrThrow("language")));
            settings.setCreatedAt(cursor.getString(cursor.getColumnIndexOrThrow("created_at")));
            settings.setUpdatedAt(cursor.getString(cursor.getColumnIndexOrThrow("updated_at")));
        }
        if (cursor != null) {
            cursor.close();
        }
        db.close();
        return settings;
    }


    private void processSyncResponse(SyncResponse response) {
        Log.d(TAG, "Processing sync response. Cars received: " +
                (response.getCars() != null ? response.getCars().size() : 0));

        // Обработка настроек пользователя - используйте прямой доступ
        if (response.getSettings() != null && currentUserId != null) {
            ApiUserSettings apiSettings = response.getSettings();
            UserSettings serverSettings = apiSettings.toLocalSettings();

            // Получаем локальные настройки без создания новых
            UserSettings localSettings = getUserSettingsDirect(currentUserId);

            if (localSettings == null ||
                    isServerNewer(serverSettings.getUpdatedAt(), localSettings.getUpdatedAt())) {
                // Сохраняем с флагом false, чтобы не вызывать синхронизацию
                dbHelper.saveUserSettings(serverSettings, false);
                Log.d(TAG, "Settings updated from server");
            }
        }

        // 1. Обрабатываем удаленные ID
        if (response.getDeletedCarIds() != null) {
            for (String carId : response.getDeletedCarIds()) {
                Car localCar = dbHelper.getCar(carId);
                if (localCar != null && !localCar.isDeleted()) {
                    deleteLocalImage(localCar.getImagePath());
                    dbHelper.deleteCar(carId);
                    Log.d(TAG, "Marked car as deleted: " + carId);
                }
            }
        }

        if (response.getDeletedTripIds() != null) {
            for (String tripId : response.getDeletedTripIds()) {
                Trip localTrip = dbHelper.getTrip(tripId);
                if (localTrip != null && !localTrip.isDeleted()) {
                    dbHelper.deleteTrip(tripId);
                    Log.d(TAG, "Marked trip as deleted: " + tripId);
                }
            }
        }

        // 2. Обрабатываем полученные автомобили
        if (response.getCars() != null) {
            for (ApiCar apiCar : response.getCars()) {
                Car serverCar = apiCar.toLocalCar();
                Car localCar = dbHelper.getCar(serverCar.getId());

                if (localCar == null) {
                    if (!serverCar.isDeleted()) {
                        // Логируем перед добавлением
                        Log.d(TAG, "About to add car: " + serverCar.getName() +
                                " with serverImageUrl: " + serverCar.getServerImageUrl());

                        serverCar.setImagePath(null);
                        dbHelper.addCar(serverCar);

                        // Проверим, сохранился ли URL
                        Car savedCar = dbHelper.getCar(serverCar.getId());
                        Log.d(TAG, "After saving, serverImageUrl: " +
                                (savedCar != null ? savedCar.getServerImageUrl() : "null"));

                        Log.d(TAG, "Added new car from server: " + serverCar.getName() +
                                " with image version: " + serverCar.getImageVersion() +
                                " and URL: " + serverCar.getServerImageUrl());

                        if (serverCar.getImageVersion() > 0) {
                            downloadCarImage(serverCar.getId(), serverCar.getServerImageUrl());
                        }
                    }
                } else {
                    // Обновление существующего
                    if (isServerNewer(serverCar.getUpdatedAt(), localCar.getUpdatedAt())) {
                        if (serverCar.isDeleted()) {
                            deleteLocalImage(localCar.getImagePath());
                            dbHelper.deleteCar(serverCar.getId());
                            Log.d(TAG, "Deleted car from server: " + serverCar.getName());
                        } else {
                            // Сохраняем текущий локальный путь
                            String currentLocalPath = localCar.getImagePath();
                            int currentImageVersion = localCar.getImageVersion();

                            // Обновляем все поля
                            localCar.setName(serverCar.getName());
                            localCar.setDescription(serverCar.getDescription());
                            localCar.setFuelUnit(serverCar.getFuelUnit());
                            localCar.setFuelType(serverCar.getFuelType());
                            localCar.setTankVolume(serverCar.getTankVolume());
                            localCar.setUpdatedAt(serverCar.getUpdatedAt());
                            localCar.setDeleted(serverCar.isDeleted());
                            localCar.setDeletedAt(serverCar.getDeletedAt());

                            // Сохраняем URL с сервера - ЭТО НУЖНО!
                            localCar.setServerImageUrl(serverCar.getImagePath());
                            // Восстанавливаем локальный путь
                            localCar.setImagePath(currentLocalPath);

                            // Проверяем версию изображения
                            if (serverCar.getImageVersion() > currentImageVersion) {
                                // На сервере новая версия - скачиваем
                                localCar.setImageVersion(serverCar.getImageVersion());
                                dbHelper.updateCar(localCar);

                                deleteLocalImage(currentLocalPath);
                                Log.d(TAG, "Server has newer image version, downloading for car: " + serverCar.getName() +
                                        " from URL: " + serverCar.getImagePath());
                                downloadCarImage(serverCar.getId(), serverCar.getImagePath());
                            } else {
                                dbHelper.updateCar(localCar);
                            }
                        }
                    }
                }
            }
        }

        // 3. Обрабатываем полученные поездки
        if (response.getTrips() != null) {
            for (ApiTrip apiTrip : response.getTrips()) {
                Trip serverTrip = apiTrip.toLocalTrip();
                Trip localTrip = dbHelper.getTrip(serverTrip.getId());

                if (localTrip == null) {
                    if (!serverTrip.isDeleted()) {
                        dbHelper.addTrip(serverTrip);
                        Log.d(TAG, "Added new trip from server: " + serverTrip.getName());
                    }
                } else {
                    if (isServerNewer(serverTrip.getUpdatedAt(), localTrip.getUpdatedAt())) {
                        if (serverTrip.isDeleted()) {
                            dbHelper.deleteTrip(serverTrip.getId());
                            Log.d(TAG, "Deleted trip from server");
                        } else {
                            dbHelper.updateTrip(serverTrip);
                            Log.d(TAG, "Updated trip from server");
                        }
                    }
                }
            }
        }

        if (isFirstSyncAfterLogin) {
            isFirstSyncAfterLogin = false;
            Log.d(TAG, "First sync after login completed");
        }
    }

    private String getFileNameFromUrl(String url) {
        if (url == null) return "";
        int lastSlash = url.lastIndexOf('/');
        if (lastSlash >= 0) {
            return url.substring(lastSlash + 1);
        }
        return url;
    }

    private String getFileNameFromPath(String path) {
        if (path == null) return "";
        int lastSlash = path.lastIndexOf('/');
        if (lastSlash >= 0) {
            return path.substring(lastSlash + 1);
        }
        return path;
    }

    private void deleteLocalImage(String imagePath) {
        if (imagePath != null && !imagePath.isEmpty()) {
            File imageFile = new File(imagePath);
            if (imageFile.exists()) {
                imageFile.delete();
                Log.d(TAG, "Deleted local image: " + imagePath);
            }
        }
    }

    private String buildImageUrl(String imagePath) {
        if (imagePath == null || imagePath.isEmpty()) {
            return null;
        }

        Log.d(TAG, "Building URL from path: " + imagePath);

        if (imagePath.startsWith("http")) {
            return imagePath;
        }

        if (imagePath.startsWith("/static")) {
            String baseUrl = BASE_URL;
            if (baseUrl.endsWith("/")) {
                baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
            }
            String fullUrl = baseUrl + imagePath;
            Log.d(TAG, "Built static URL: " + fullUrl);
            return fullUrl;
        }

        if (imagePath.startsWith("/data")) {
            Log.d(TAG, "Skipping download for local path: " + imagePath);
            return null;
        }

        Log.d(TAG, "Unknown path format: " + imagePath);
        return null;
    }

    public void forceSyncFromServer() {
        if (getSavedToken() == null) {
            Log.e(TAG, "No token, cannot force sync");
            return;
        }

        if (currentUserId == null) {
            currentUserId = getCurrentUserId();
            if (currentUserId == null) {
                Log.e(TAG, "No user ID, cannot force sync");
                return;
            }
        }

        Log.d(TAG, "Starting force sync from server");

        SyncRequest request = new SyncRequest(null, new ArrayList<>(), new ArrayList<>(), null);

        apiService.syncData("Bearer " + getSavedToken(), request).enqueue(new Callback<SyncResponse>() {
            @Override
            public void onResponse(Call<SyncResponse> call, Response<SyncResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Log.d(TAG, "Force sync response received successfully");
                    processSyncResponse(response.body());
                    saveLastSyncTime();

                    List<Car> cars = dbHelper.getAllCars();
                    Log.d(TAG, "Local cars after force sync: " + cars.size());
                } else {
                    String error = "Ошибка загрузки: " + response.code();
                    Log.e(TAG, error);
                }
            }

            @Override
            public void onFailure(Call<SyncResponse> call, Throwable t) {
                Log.e(TAG, "Force sync failed", t);
            }
        });
    }

    private boolean isServerNewer(String serverTime, String localTime) {
        if (localTime == null) return true;
        if (serverTime == null) return false;
        return serverTime.compareTo(localTime) > 0;
    }

    // ========== Работа с изображениями ==========

    public void uploadCarImage(String carId, File imageFile) {
        if (getSavedToken() == null) {
            Log.e(TAG, "No token, cannot upload image");
            return;
        }

        Log.d(TAG, "Uploading image for car: " + carId + ", file: " + imageFile.getName());

        Car currentCar = dbHelper.getCar(carId);
        int currentVersion = currentCar != null ? currentCar.getImageVersion() : 1;

        Log.d(TAG, "Uploading image with version: " + currentVersion);

        RequestBody requestFile = RequestBody.create(MediaType.parse("image/jpeg"), imageFile);
        MultipartBody.Part body = MultipartBody.Part.createFormData("file", imageFile.getName(), requestFile);

        apiService.uploadCarImage("Bearer " + getSavedToken(), carId, body).enqueue(new Callback<ApiCar>() {
            @Override
            public void onResponse(Call<ApiCar> call, Response<ApiCar> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Log.d(TAG, "Image uploaded successfully for car: " + carId);

                    ApiCar apiCar = response.body();
                    Car currentCar = dbHelper.getCar(carId);

                    if (currentCar != null) {
                        currentCar.setServerImageUrl(apiCar.getImagePath());
                        currentCar.setImagePath(imageFile.getAbsolutePath());
                        currentCar.setUpdatedAt(apiCar.getUpdatedAt());

                        dbHelper.updateCar(currentCar);
                        Log.d(TAG, "Local car updated. Server URL: " + apiCar.getImagePath() +
                                ", version remains: " + currentCar.getImageVersion());
                    }
                } else {
                    Log.e(TAG, "Image upload failed: " + response.code());
                }
            }

            @Override
            public void onFailure(Call<ApiCar> call, Throwable t) {
                Log.e(TAG, "Network error uploading image for car: " + carId, t);
            }
        });
    }

    private void downloadCarImage(String carId, String serverImageUrl) {
        Log.d(TAG, "Attempting to download image for car: " + carId);
        Log.d(TAG, "Server image URL from server: " + serverImageUrl);

        String imageUrl = buildImageUrl(serverImageUrl);
        Log.d(TAG, "Built image URL for download: " + imageUrl);

        if (imageUrl == null) {
            Log.d(TAG, "No valid URL to download for car: " + carId);
            return;
        }

        Log.d(TAG, "Downloading image for car: " + carId + " from: " + imageUrl);

        apiService.downloadImage(imageUrl).enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Log.d(TAG, "Download successful, saving image for car: " + carId);
                    saveImageToStorage(carId, response.body(), serverImageUrl);
                } else {
                    Log.e(TAG, "Image download failed: " + response.code() + " for URL: " + imageUrl);
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                Log.e(TAG, "Network error downloading image from: " + imageUrl, t);
            }
        });
    }

    private void saveImageToStorage(String carId, ResponseBody body, String serverImageUrl) {
        try {
            String fileName = "car_" + carId + ".jpg";
            File file = new File(context.getFilesDir(), fileName);

            InputStream inputStream = body.byteStream();
            FileOutputStream outputStream = new FileOutputStream(file);

            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }

            outputStream.close();
            inputStream.close();

            Log.d(TAG, "Image saved to: " + file.getAbsolutePath());

            Car car = dbHelper.getCar(carId);
            if (car != null) {
                car.setImagePath(file.getAbsolutePath());
                car.setServerImageUrl(serverImageUrl);
                dbHelper.updateCar(car);
                Log.d(TAG, "Car updated with image path: " + file.getAbsolutePath() +
                        " and server URL: " + serverImageUrl);
            }

        } catch (IOException e) {
            Log.e(TAG, "Error saving image", e);
        }
    }
    /**
     * Синхронная синхронизация (для выхода из приложения)
     */
    public void syncAllSync() {
        if (getSavedToken() == null) {
            Log.d(TAG, "No token, skipping sync");
            return;
        }

        try {
            List<ApiCar> apiCars = new ArrayList<>();
            for (Car car : dbHelper.getAllCars(true)) {
                apiCars.add(new ApiCar(car));
            }

            List<ApiTrip> apiTrips = new ArrayList<>();
            for (Trip trip : getAllTrips(true)) {
                apiTrips.add(new ApiTrip(trip));
            }

            ApiUserSettings apiSettings = null;
            if (currentUserId != null) {
                UserSettings settings = getUserSettingsDirect(currentUserId);
                if (settings != null) {
                    apiSettings = new ApiUserSettings(settings);
                }
            }

            String lastSync = getLastSyncTime();
            SyncRequest request = new SyncRequest(lastSync, apiCars, apiTrips, apiSettings);

            retrofit2.Response<SyncResponse> response = apiService
                    .syncData("Bearer " + getSavedToken(), request)
                    .execute(); // Синхронный вызов!

            if (response.isSuccessful() && response.body() != null) {
                processSyncResponse(response.body());
                saveLastSyncTime();
                Log.d(TAG, "Final sync completed successfully");
            } else {
                Log.e(TAG, "Final sync failed: " + response.code());
            }
        } catch (Exception e) {
            Log.e(TAG, "Final sync error", e);
        }
    }

    // Добавьте этот метод, если его нет (для получения lastSyncTime)
    private String getLastSyncTime() {
        return context.getSharedPreferences("sync", Context.MODE_PRIVATE)
                .getString(PREF_LAST_SYNC, null);
    }

}