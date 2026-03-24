package com.example.cogcdat_2;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.Button;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.example.cogcdat_2.sync.SyncManager;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class MainActivity extends AppCompatActivity {
    private BottomNavigationView bottomNavigationView;
    private static final String KEY_SELECTED_ITEM = "selected_nav_item";
    private boolean isUISetup = false; // Флаг для отслеживания инициализации UI

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        SharedPreferences prefs = getSharedPreferences("app_prefs", MODE_PRIVATE);
        String themeValue = prefs.getString("theme", Theme.SYSTEM.getValue());
        App.applyTheme(Theme.fromValue(themeValue));

        SyncManager syncManager = SyncManager.getInstance(this);

        if (syncManager.getSavedToken() == null) {
            // Если нет токена, но есть данные в БД - очищаем (остались от предыдущего пользователя)
            DatabaseHelper dbHelper = new DatabaseHelper(this);
            if (!dbHelper.getAllCars().isEmpty()) {
                // Очищаем данные, так как пользователь не авторизован
                syncManager.clearLocalData();
            }

            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        // Проверяем, есть ли машины
        DatabaseHelper dbHelper = new DatabaseHelper(this);
        boolean hasCars = !dbHelper.getAllCars().isEmpty();

        if (hasCars) {
            // Если машины есть, показываем основной интерфейс
            initializeMainUI(savedInstanceState);
            isUISetup = true;
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
            // Если UI еще не настроен или мы на приветственном экране
            if (!isUISetup || findViewById(R.id.fragment_container) == null) {
                initializeMainUI(null);
                isUISetup = true;
            }
            // Иначе ничего не делаем - сохраняем текущее состояние
        } else {
            // Если машин нет, показываем приветственный экран
            if (isUISetup) {
                showWelcomeScreen();
                isUISetup = false;
            }
        }
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        if (bottomNavigationView != null) {
            outState.putInt(KEY_SELECTED_ITEM, bottomNavigationView.getSelectedItemId());
        }
    }

    private void showWelcomeScreen() {
        setContentView(R.layout.activity_welcome);
        isUISetup = false;

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
        bottomNavigationView.setOnItemSelectedListener(navListener);

        // Загружаем фрагмент только если нет сохраненного состояния
        if (savedInstanceState == null) {
            // Проверяем, есть ли уже фрагмент
            Fragment currentFragment = getSupportFragmentManager()
                    .findFragmentById(R.id.fragment_container);

            if (currentFragment == null) {
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.fragment_container, new TripsFragment())
                        .commit();
            }
        } else {
            // Восстанавливаем выбранный пункт меню
            int selectedItemId = savedInstanceState.getInt(KEY_SELECTED_ITEM, R.id.nav_trips);
            bottomNavigationView.setSelectedItemId(selectedItemId);
        }

        requestNotificationPermission();
        isUISetup = true;
    }

    private void requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{android.Manifest.permission.POST_NOTIFICATIONS},
                        101);
            }
        }
    }

    private BottomNavigationView.OnItemSelectedListener navListener =
            new BottomNavigationView.OnItemSelectedListener() {
                @Override
                public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                    Fragment selectedFragment = null;
                    int itemId = item.getItemId();

                    if (itemId == R.id.nav_trips) {
                        selectedFragment = new TripsFragment();
                    } else if (itemId == R.id.nav_cars) {
                        selectedFragment = new CarsFragment();
                    } else if (itemId == R.id.nav_analytics) {
                        selectedFragment = new AnalyticsFragment();
                    } else if (itemId == R.id.nav_settings) {
                        selectedFragment = new SettingsFragment();
                    }

                    if (selectedFragment != null) {
                        getSupportFragmentManager().beginTransaction()
                                .replace(R.id.fragment_container, selectedFragment)
                                .setReorderingAllowed(true)
                                .commit();
                    }
                    return true;
                }
            };
}