package com.example.cogcdat_2;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import androidx.appcompat.app.AppCompatDelegate;

import com.example.cogcdat_2.sync.SyncManager;
import com.example.cogcdat_2.sync.SyncScheduler;

public class App extends Application {

    private SyncScheduler syncScheduler;

    @Override
    public void onCreate() {
        super.onCreate();

        // Загружаем сохраненную тему и применяем её глобально
        SharedPreferences prefs = getSharedPreferences("app_prefs", MODE_PRIVATE);
        String themeValue = prefs.getString("theme", Theme.SYSTEM.getValue());
        applyTheme(Theme.fromValue(themeValue));

        // Запускаем периодическую синхронизацию
        SyncManager syncManager = SyncManager.getInstance(this);
        if (syncManager.getSavedToken() != null) {
            syncScheduler = SyncScheduler.getInstance(syncManager);
            syncScheduler.startPeriodicSync();
        }
    }

    @Override
    public void onTerminate() {
        super.onTerminate();
        // Синхронизация при закрытии приложения
        if (syncScheduler != null) {
            syncScheduler.syncOnAppExit();
            syncScheduler.stopPeriodicSync();
        }
    }

    public static void applyTheme(Theme theme) {
        switch (theme) {
            case LIGHT:
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                break;
            case DARK:
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                break;
            case SYSTEM:
            default:
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
                break;
        }
    }

    public static void saveAndApplyTheme(Context context, Theme theme) {
        context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
                .edit()
                .putString("theme", theme.getValue())
                .apply();
        applyTheme(theme);
    }
}