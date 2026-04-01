package com.example.cogcdat_2;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;

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
                if (!savedLang.equals(currentLang)) {
                    // Если язык в настройках не совпадает с текущим, применяем его
                    LocaleHelper.setLocale(this, savedLang);
                    recreate(); // пересоздаём активность, чтобы применить новый язык
                }
            }
        }
    }
}