package com.example.cogcdat_2.api;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.example.cogcdat_2.LoginActivity;
import com.example.cogcdat_2.api.models.TokenResponse;
import com.example.cogcdat_2.sync.SyncManager;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

import okhttp3.Cache;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class ApiClient {
    private static final String TAG = "ApiClient";
    private static final String BASE_URL = "http://192.168.3.15:8000/api/v1/";
    private static final long CACHE_SIZE = 10 * 1024 * 1024;

    private static ApiClient instance;
    private final ApiService apiService;
    private final OkHttpClient okHttpClient;
    private String authToken;
    private Context appContext;

    // Флаг для предотвращения бесконечного цикла обновления
    private boolean isRefreshing = false;

    private ApiClient(Context context) {
        this.appContext = context.getApplicationContext();

        File cacheDir = new File(appContext.getCacheDir(), "http-cache");
        Cache cache = new Cache(cacheDir, CACHE_SIZE);

        HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor();
        loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY);

        // Интерцептор для добавления токена и обработки 401
        Interceptor authInterceptor = chain -> {
            Request original = chain.request();
            Request.Builder builder = original.newBuilder();

            if (authToken != null) {
                builder.header("Authorization", "Bearer " + authToken);
            }

            Response response = chain.proceed(builder.build());

            // Если получили 401, пробуем обновить токен
            if (response.code() == 401 && !isRefreshing) {
                response.close();

                Log.d(TAG, "Token expired, trying to refresh...");
                isRefreshing = true;

                String newToken = refreshToken();
                isRefreshing = false;

                if (newToken != null) {
                    Log.d(TAG, "Token refreshed successfully");
                    // Повторяем запрос с новым токеном
                    Request newRequest = original.newBuilder()
                            .header("Authorization", "Bearer " + newToken)
                            .build();
                    return chain.proceed(newRequest);
                } else {
                    Log.e(TAG, "Failed to refresh token, logging out");
                    // Не удалось обновить токен - выходим
                    SyncManager syncManager = SyncManager.getInstance(appContext);
                    syncManager.logout();

                    Intent intent = new Intent(appContext, LoginActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    appContext.startActivity(intent);

                    throw new IOException("Session expired. Please login again.");
                }
            }

            return response;
        };

        // Интерцептор для кэширования офлайн-режима
        Interceptor offlineCacheInterceptor = chain -> {
            Request request = chain.request();

            if (!isNetworkAvailable(appContext)) {
                request = request.newBuilder()
                        .header("Cache-Control", "only-if-cached, max-stale=" + 7 * 24 * 60 * 60)
                        .build();
            }

            return chain.proceed(request);
        };

        okHttpClient = new OkHttpClient.Builder()
                .cache(cache)
                .addInterceptor(authInterceptor)
                .addInterceptor(offlineCacheInterceptor)
                .addInterceptor(loggingInterceptor)
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build();

        Gson gson = new GsonBuilder()
                .setDateFormat("yyyy-MM-dd'T'HH:mm:ss")
                .create();

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(okHttpClient)
                .addConverterFactory(GsonConverterFactory.create(gson))
                .build();

        apiService = retrofit.create(ApiService.class);
    }

    private String refreshToken() {
        try {
            if (authToken == null) return null;

            retrofit2.Response<TokenResponse> response = apiService.refreshToken("Bearer " + authToken).execute();

            if (response.isSuccessful() && response.body() != null) {
                String newToken = response.body().getAccessToken();
                setAuthToken(newToken);

                // Сохраняем новый токен в SyncManager
                SyncManager syncManager = SyncManager.getInstance(appContext);
                syncManager.saveToken(newToken);

                return newToken;
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to refresh token", e);
        }
        return null;
    }

    public static synchronized ApiClient getInstance(Context context) {
        if (instance == null) {
            instance = new ApiClient(context);
        }
        return instance;
    }

    public ApiService getApiService() {
        return apiService;
    }

    public void setAuthToken(String token) {
        this.authToken = token;
    }

    public String getAuthToken() {
        return authToken;
    }

    public OkHttpClient getOkHttpClient() {
        return okHttpClient;
    }

    private boolean isNetworkAvailable(Context context) {
        android.net.ConnectivityManager cm = (android.net.ConnectivityManager)
                context.getSystemService(Context.CONNECTIVITY_SERVICE);
        android.net.NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        return activeNetwork != null && activeNetwork.isConnectedOrConnecting();
    }
}