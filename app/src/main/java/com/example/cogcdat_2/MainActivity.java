package com.example.cogcdat_2;

import android.os.Bundle;
import android.view.MenuItem;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class MainActivity extends AppCompatActivity {
    private BottomNavigationView bottomNavigationView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
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