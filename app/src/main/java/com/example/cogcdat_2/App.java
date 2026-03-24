package com.example.cogcdat_2;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import androidx.appcompat.app.AppCompatDelegate;

public class App extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        // Загружаем сохраненную тему и применяем её глобально
        SharedPreferences prefs = getSharedPreferences("app_prefs", MODE_PRIVATE);
        String themeValue = prefs.getString("theme", Theme.SYSTEM.getValue());
        applyTheme(Theme.fromValue(themeValue));
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
        // Сохраняем в SharedPreferences
        context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
                .edit()
                .putString("theme", theme.getValue())
                .apply();

        // Применяем тему немедленно
        applyTheme(theme);
    }
}