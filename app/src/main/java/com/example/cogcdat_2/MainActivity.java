package com.example.cogcdat_2;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.widget.Button;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class MainActivity extends AppCompatActivity {
    private BottomNavigationView bottomNavigationView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Проверяем, есть ли машины
        DatabaseHelper dbHelper = new DatabaseHelper(this);
        boolean hasCars = !dbHelper.getAllCars().isEmpty();

        if (hasCars) {
            // Если машины есть, показываем основной интерфейс
            initializeMainUI(savedInstanceState);
        } else {
            // Если машин нет, показываем приветственный экран в этой же активности
            showWelcomeScreen();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Проверяем, есть ли машины при возвращении в активность
        DatabaseHelper dbHelper = new DatabaseHelper(this);
        boolean hasCars = !dbHelper.getAllCars().isEmpty();

        if (hasCars) {
            // Если машины появились, переключаемся на основной интерфейс
            initializeMainUI(null);
        }
        // Если машин нет, остаемся на приветственном экране
    }

    private void showWelcomeScreen() {
        setContentView(R.layout.activity_welcome);

        Button btnContinue = findViewById(R.id.btn_continue);
        btnContinue.setOnClickListener(v -> {
            // Отмечаем, что первый запуск завершен
            SharedPreferences prefs = getSharedPreferences("app_prefs", MODE_PRIVATE);
            prefs.edit().putBoolean("is_first_launch", false).apply();

            // Переходим к добавлению автомобиля
            Intent intent = new Intent(MainActivity.this, AddCarActivity.class);
            startActivity(intent);
        });
    }

    private void initializeMainUI(Bundle savedInstanceState) {
        setContentView(R.layout.activity_main);

        bottomNavigationView = findViewById(R.id.bottom_navigation);

        // 💡 Оптимизированное назначение слушателя с использованием лямбда-выражения
        bottomNavigationView.setOnItemSelectedListener(navListener);

        // Загружаем фрагмент по умолчанию при первом запуске (на всякий случай, если ID изменится)
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, new TripsFragment())
                    .commit();
        }

        requestNotificationPermission();
    }

    private void requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {  // Android 13+
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                // Разрешение не дано — запрашиваем
                ActivityCompat.requestPermissions(this,
                        new String[]{android.Manifest.permission.POST_NOTIFICATIONS},
                        101);
            } else {
                // Разрешение уже дано — ничего не делаем
            }
        }
    }

    // 💡 Используем новый интерфейс OnItemSelectedListener
    private BottomNavigationView.OnItemSelectedListener navListener =
        new BottomNavigationView.OnItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                Fragment selectedFragment = null;

                // 💡 Используем switch с item.getItemId()
                int itemId = item.getItemId();

                // 🛑 ВНИМАНИЕ: Убедитесь, что ID (R.id.nav_trips, R.id.nav_cars, R.id.nav_analytics)
                // ТОЧНО СОВПАДАЮТ с ID в вашем файле menu/bottom_nav_menu.xml

                if (itemId == R.id.nav_trips) {
                    selectedFragment = new TripsFragment();
                } else if (itemId == R.id.nav_cars) {
                    selectedFragment = new CarsFragment();
                } else if (itemId == R.id.nav_analytics) {
                    selectedFragment = new AnalyticsFragment();
                }

                if (selectedFragment != null) {
                    getSupportFragmentManager().beginTransaction()
                            .replace(R.id.fragment_container, selectedFragment)
                            // Добавление .setReorderingAllowed(true) может улучшить производительность
                            .setReorderingAllowed(true)
                            .commit();
                }
                // Возвращаем true, чтобы элемент был помечен как выбранный
                return true;
            }
        };
}