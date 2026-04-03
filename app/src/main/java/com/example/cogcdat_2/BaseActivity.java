package com.example.cogcdat_2;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;

import java.util.Locale;

public abstract class BaseActivity extends AppCompatActivity {
    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(LocaleHelper.updateBaseContextLocale(newBase));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // здесь можно добавить общие настройки
    }
    @Override
    protected void onResume() {
        super.onResume();
        // Получаем ID текущего пользователя
        SharedPreferences prefs = getSharedPreferences("user", MODE_PRIVATE);
        String userId = prefs.getString("user_id", null);
        if (userId != null) {
            DatabaseHelper dbHelper = new DatabaseHelper(this);
            UserSettings settings = dbHelper.getUserSettings(userId);
            if (settings != null) {
                String savedLang = settings.getLanguage().getValue();
                String currentLang = LocaleHelper.getLanguage(this);
                if (!savedLang.equalsIgnoreCase(currentLang)) {
                    // Если язык в настройках не совпадает с текущим, применяем его
                    LocaleHelper.setLocale(this, savedLang);
                    recreate(); // пересоздаём активность, чтобы применить новый язык
                }
            }
        }
    }

    public static String getLocalizedFuelUnit(Context context, String unit) {
        if (unit == null) return context.getString(R.string.fuel_unit_liter);

        String normalized = unit.toLowerCase();

        // Литр / литры
        if (normalized.equals("л") || normalized.equals("l") ||
                normalized.contains("liter") || normalized.contains("litre")) {
            return context.getString(R.string.fuel_unit_liter);
        }
        // Галлон
        else if (normalized.equals("гал") || normalized.equals("gal") ||
                normalized.contains("gallon")) {
            return context.getString(R.string.unit_gallon);
        }
        // кВтч
        else if (normalized.equals("квтч") || normalized.equals("kwh")) {
            return context.getString(R.string.unit_kwh);
        }
        // м³
        else if (normalized.equals("м³") || normalized.equals("m³")) {
            return context.getString(R.string.unit_m3);
        }

        return unit;
    }

}