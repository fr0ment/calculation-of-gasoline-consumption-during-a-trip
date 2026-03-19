package com.example.cogcdat_2.api;

import com.example.cogcdat_2.api.models.ApiCar;
import com.example.cogcdat_2.api.models.ApiTrip;
import com.example.cogcdat_2.api.models.ApiUserSettings;
import com.example.cogcdat_2.api.models.ChangePasswordRequest;
import com.example.cogcdat_2.api.models.LoginRequest;
import com.example.cogcdat_2.api.models.SyncRequest;
import com.example.cogcdat_2.api.models.SyncResponse;
import com.example.cogcdat_2.api.models.TokenResponse;
import com.example.cogcdat_2.api.models.UpdateProfileRequest;
import com.example.cogcdat_2.api.models.User;

import okhttp3.MultipartBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.*;

public interface ApiService {
    // Аутентификация
    @POST("auth/login/json")
    Call<TokenResponse> login(@Body LoginRequest request);

    @POST("auth/register")
    Call<User> register(@Body User user);

    @GET("auth/me")
    Call<User> getCurrentUser(@Header("Authorization") String token);

    // Обновление профиля
    @PUT("users/{userId}")
    Call<User> updateUser(@Header("Authorization") String token,
                          @Path("userId") String userId,
                          @Body UpdateProfileRequest request);

    // Смена пароля
    @POST("auth/change-password")
    Call<Void> changePassword(@Header("Authorization") String token,
                              @Body ChangePasswordRequest request);

    // Удаление пользователя
    @DELETE("users/{userId}")
    Call<Void> deleteUser(@Header("Authorization") String token, @Path("userId") String userId);

    // Настройки пользователя
    @GET("settings")
    Call<ApiUserSettings> getSettings(@Header("Authorization") String token);

    @PUT("settings")
    Call<ApiUserSettings> updateSettings(@Header("Authorization") String token,
                                         @Body ApiUserSettings settings);

    // Автомобили
    @GET("cars")
    Call<java.util.List<ApiCar>> getCars(@Header("Authorization") String token);

    @POST("cars")
    Call<ApiCar> createCar(@Header("Authorization") String token, @Body ApiCar car);

    @PUT("cars/{carId}")
    Call<ApiCar> updateCar(@Header("Authorization") String token, @Path("carId") String carId, @Body ApiCar car);

    @DELETE("cars/{carId}")
    Call<Void> deleteCar(@Header("Authorization") String token, @Path("carId") String carId);

    @Multipart
    @POST("cars/{carId}/image")
    Call<ApiCar> uploadCarImage(@Header("Authorization") String token,
                                @Path("carId") String carId,
                                @Part MultipartBody.Part file);

    // Поездки
    @GET("trips")
    Call<java.util.List<ApiTrip>> getTrips(@Header("Authorization") String token,
                                           @Query("car_id") String carId);

    @POST("trips")
    Call<ApiTrip> createTrip(@Header("Authorization") String token, @Body ApiTrip trip);

    @PUT("trips/{tripId}")
    Call<ApiTrip> updateTrip(@Header("Authorization") String token,
                             @Path("tripId") String tripId,
                             @Body ApiTrip trip);

    @DELETE("trips/{tripId}")
    Call<Void> deleteTrip(@Header("Authorization") String token, @Path("tripId") String tripId);

    // Синхронизация
    @POST("sync")
    Call<SyncResponse> syncData(@Header("Authorization") String token, @Body SyncRequest request);

    // Загрузка изображений
    @GET
    Call<ResponseBody> downloadImage(@Url String imageUrl);
}