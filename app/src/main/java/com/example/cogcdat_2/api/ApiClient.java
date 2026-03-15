package com.example.cogcdat_2.api;

import android.content.Context;
import android.content.SharedPreferences;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import okhttp3.Cache;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class ApiClient {
    private static final String BASE_URL = "http://192.168.3.15:8000/api/v1/"; // Замените на ваш IP
    private static final long CACHE_SIZE = 10 * 1024 * 1024; // 10 MB

    private static ApiClient instance;
    private final ApiService apiService;
    private final OkHttpClient okHttpClient;
    private String authToken;

    private ApiClient(Context context) {
        // Настройка кэша
        File cacheDir = new File(context.getCacheDir(), "http-cache");
        Cache cache = new Cache(cacheDir, CACHE_SIZE);

        // Логирование запросов (только для отладки)
        HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor();
        loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY);

        // Интерцептор для добавления токена авторизации
        Interceptor authInterceptor = chain -> {
            okhttp3.Request original = chain.request();
            okhttp3.Request.Builder builder = original.newBuilder();

            if (authToken != null) {
                builder.header("Authorization", "Bearer " + authToken);
            }

            return chain.proceed(builder.build());
        };

        // Интерцептор для кэширования офлайн-режима
        Interceptor offlineCacheInterceptor = chain -> {
            okhttp3.Request request = chain.request();

            // Проверяем, есть ли интернет
            if (!isNetworkAvailable(context)) {
                // Если нет интернета, используем кэш
                request = request.newBuilder()
                        .header("Cache-Control", "only-if-cached, max-stale=" + 7 * 24 * 60 * 60) // 7 дней
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

    public static synchronized ApiClient getInstance(Context context) {
        if (instance == null) {
            instance = new ApiClient(context.getApplicationContext());
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