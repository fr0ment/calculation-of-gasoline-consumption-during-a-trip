package com.example.cogcdat_2;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import androidx.appcompat.app.AppCompatActivity;

public class WelcomeActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_welcome);

        Button btnContinue = findViewById(R.id.btn_continue);
        btnContinue.setOnClickListener(v -> {
            // Отмечаем, что первый запуск завершен
            SharedPreferences prefs = getSharedPreferences("app_prefs", MODE_PRIVATE);
            prefs.edit().putBoolean("is_first_launch", false).apply();

            // Переходим к добавлению автомобиля
            Intent intent = new Intent(WelcomeActivity.this, AddCarActivity.class);
            startActivity(intent);
            // Не закрываем WelcomeActivity, чтобы можно было вернуться назад
        });
    }
}
