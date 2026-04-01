package com.example.cogcdat_2;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Build;
import android.os.LocaleList;

import java.util.Locale;

public class LocaleHelper {
    private static final String SELECTED_LANGUAGE = "Locale.Helper.Selected.Language";

    public static Context setLocale(Context context, String languageCode) {
        persistLanguage(context, languageCode);
        return updateResources(context, languageCode);
    }

    private static void persistLanguage(Context context, String languageCode) {
        SharedPreferences prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE);
        prefs.edit().putString(SELECTED_LANGUAGE, languageCode).apply();
    }

    public static String getLanguage(Context context) {
        SharedPreferences prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE);
        String savedLang = prefs.getString(SELECTED_LANGUAGE, null);
        if (savedLang != null) return savedLang;
        // При первом запуске определяем системный язык
        return getSystemLanguage();
    }

    private static String getSystemLanguage() {
        Locale locale;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            locale = LocaleList.getDefault().get(0);
        } else {
            locale = Locale.getDefault();
        }
        String lang = locale.getLanguage();
        // Поддерживаем только русский и английский
        if (lang.equalsIgnoreCase("ru")) return "ru";
        else return "en";
    }

    private static Context updateResources(Context context, String languageCode) {
        Locale locale = new Locale(languageCode);
        Locale.setDefault(locale);

        Resources res = context.getResources();
        Configuration config = new Configuration(res.getConfiguration());
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            config.setLocale(locale);
            config.setLocales(new LocaleList(locale));
            return context.createConfigurationContext(config);
        } else {
            config.locale = locale;
            res.updateConfiguration(config, res.getDisplayMetrics());
            return context;
        }
    }

    public static Context updateBaseContextLocale(Context context) {
        String language = getLanguage(context);
        return updateResources(context, language);
    }

    public static Context wrap(Context base) {
        return updateBaseContextLocale(base);
    }
}