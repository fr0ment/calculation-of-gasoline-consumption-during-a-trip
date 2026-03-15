package com.example.cogcdat_2.sync;

import android.content.Context;
import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.cogcdat_2.Car;
import com.example.cogcdat_2.DatabaseHelper;
import com.example.cogcdat_2.SelectedCarManager;
import com.example.cogcdat_2.Trip;
import com.example.cogcdat_2.api.ApiClient;
import com.example.cogcdat_2.api.ApiService;
import com.example.cogcdat_2.api.models.ApiCar;
import com.example.cogcdat_2.api.models.ApiTrip;
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

    private SyncManager(Context context) {
        this.context = context.getApplicationContext();
        this.dbHelper = new DatabaseHelper(this.context);
        this.apiService = ApiClient.getInstance(this.context).getApiService();

        String token = getSavedToken();
        if (token != null) {
            ApiClient.getInstance(this.context).setAuthToken(token);
            isAuthenticated.setValue(true);
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

    private void saveToken(String token) {
        context.getSharedPreferences("auth", Context.MODE_PRIVATE)
                .edit()
                .putString(PREF_AUTH_TOKEN, token)
                .apply();
        ApiClient.getInstance(context).setAuthToken(token);
        isAuthenticated.postValue(true);
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

        SelectedCarManager.clear(context);

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
                    if (oldToken != null && !oldToken.equals(token)) {
                        clearLocalData();
                    }

                    saveToken(token);
                    callback.onSuccess(token);
                    forceSyncFromServer();
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

    // ========== Синхронизация ==========

    public LiveData<Boolean> isSyncInProgress() {
        return syncInProgress;
    }

    public LiveData<String> getSyncStatus() {
        return syncStatus;
    }

    private String getLastSyncTime() {
        return context.getSharedPreferences("sync", Context.MODE_PRIVATE)
                .getString(PREF_LAST_SYNC, null);
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
        syncStatus.postValue("Синхронизация...");

        List<ApiCar> apiCars = new ArrayList<>();
        for (Car car : dbHelper.getAllCars(true)) {
            apiCars.add(new ApiCar(car));
        }

        List<ApiTrip> apiTrips = new ArrayList<>();
        for (Trip trip : getAllTrips(true)) {
            apiTrips.add(new ApiTrip(trip));
        }

        String lastSync = getLastSyncTime();
        SyncRequest request = new SyncRequest(lastSync, apiCars, apiTrips);

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

    private void processSyncResponse(SyncResponse response) {
        Log.d(TAG, "Processing sync response. Cars received: " +
                (response.getCars() != null ? response.getCars().size() : 0));

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
                    // НОВЫЙ автомобиль с сервера
                    if (!serverCar.isDeleted()) {
                        // Убираем ЭТУ строку - она затирает то, что уже установлено в toLocalCar()!
                        // serverCar.setServerImageUrl(serverCar.getImagePath()); // <-- УДАЛИТЬ!

                        // Очищаем локальный путь (еще нет изображения)
                        serverCar.setImagePath(null);

                        dbHelper.addCar(serverCar);
                        Log.d(TAG, "Added new car from server: " + serverCar.getName() +
                                " with image version: " + serverCar.getImageVersion() +
                                " and URL: " + serverCar.getServerImageUrl()); // Теперь URL должен быть!

                        // Скачиваем изображение, если есть версия > 0
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

                            // Обновляем все поля, включая serverImageUrl
                            localCar.setName(serverCar.getName());
                            localCar.setDescription(serverCar.getDescription());
                            localCar.setDistanceUnit(serverCar.getDistanceUnit());
                            localCar.setFuelUnit(serverCar.getFuelUnit());
                            localCar.setFuelConsumptionUnit(serverCar.getFuelConsumptionUnit());
                            localCar.setFuelType(serverCar.getFuelType());
                            localCar.setTankVolume(serverCar.getTankVolume());
                            localCar.setUpdatedAt(serverCar.getUpdatedAt());
                            localCar.setDeleted(serverCar.isDeleted());
                            localCar.setDeletedAt(serverCar.getDeletedAt());

                            // Сохраняем URL с сервера
                            localCar.setServerImageUrl(serverCar.getImagePath());
                            // Восстанавливаем локальный путь
                            localCar.setImagePath(currentLocalPath);

                            // Проверяем версию изображения
                            if (serverCar.getImageVersion() > currentImageVersion) {
                                // На сервере новая версия - скачиваем
                                localCar.setImageVersion(serverCar.getImageVersion());
                                dbHelper.updateCar(localCar);

                                deleteLocalImage(currentLocalPath);
                                Log.d(TAG, "Server has newer image version (server: " + serverCar.getImageVersion() +
                                        ", local: " + currentImageVersion + "), downloading for car: " + serverCar.getName() +
                                        " from URL: " + serverCar.getImagePath());
                                downloadCarImage(serverCar.getId(), serverCar.getImagePath());
                            } else {
                                // Версии одинаковые или клиент новее
                                dbHelper.updateCar(localCar);
                                Log.d(TAG, "Updated car from server: " + serverCar.getName() +
                                        " with same or older image version (server: " + serverCar.getImageVersion() +
                                        ", local: " + currentImageVersion + ")");
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

        // Если уже полный URL
        if (imagePath.startsWith("http")) {
            return imagePath;
        }

        // Если путь начинается с /static (правильный URL с сервера)
        if (imagePath.startsWith("/static")) {
            // Базовая проверка BASE_URL
            String baseUrl = BASE_URL;
            if (baseUrl.endsWith("/")) {
                baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
            }
            String fullUrl = baseUrl + imagePath;
            Log.d(TAG, "Built static URL: " + fullUrl);
            return fullUrl;
        }

        // Если это локальный путь с телефона - не скачиваем
        if (imagePath.startsWith("/data")) {
            Log.d(TAG, "Skipping download for local path: " + imagePath);
            return null;
        }

        // Для любых других путей (на случай если сервер вернет что-то еще)
        Log.d(TAG, "Unknown path format: " + imagePath);
        return null;
    }

    public void forceSyncFromServer() {
        if (getSavedToken() == null) {
            Log.e(TAG, "No token, cannot force sync");
            return;
        }

        Log.d(TAG, "Starting force sync from server");

        SyncRequest request = new SyncRequest(null, new ArrayList<>(), new ArrayList<>());

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

        // Получаем текущую версию (уже установленную в Activity)
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
                        // Сохраняем URL, который вернул сервер
                        currentCar.setServerImageUrl(apiCar.getImagePath());
                        // Локальный путь уже правильный
                        currentCar.setImagePath(imageFile.getAbsolutePath());
                        // Версия НЕ МЕНЯЕТСЯ - она уже была увеличена в Activity
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
                car.setServerImageUrl(serverImageUrl); // СОХРАНЯЕМ URL В БД!
                // НЕ меняем версию при скачивании!
                dbHelper.updateCar(car);
                Log.d(TAG, "Car updated with image path: " + file.getAbsolutePath() +
                        " and server URL: " + serverImageUrl);
            }

        } catch (IOException e) {
            Log.e(TAG, "Error saving image", e);
        }
    }
}