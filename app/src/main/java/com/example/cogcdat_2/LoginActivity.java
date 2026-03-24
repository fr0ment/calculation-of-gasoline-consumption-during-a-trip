package com.example.cogcdat_2;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.Observer;

import com.example.cogcdat_2.sync.SyncManager;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

public class LoginActivity extends AppCompatActivity {

    private TextInputLayout tilUsername, tilPassword;
    private TextInputEditText etUsername, etPassword;
    private Button btnLogin;
    private TextView tvRegisterLink, tvError;
    private ProgressBar progressBar;
    private SyncManager syncManager;

    // Флаг для отслеживания, были ли уже загружены данные
    private boolean isDataLoaded = false;
    private Handler handler = new Handler(Looper.getMainLooper());
    private Runnable timeoutRunnable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        syncManager = SyncManager.getInstance(this);

        // Проверяем, не авторизован ли уже пользователь
        if (syncManager.getSavedToken() != null) {
            // Если уже авторизован, проверяем статус синхронизации
            checkSyncStatusAndProceed();
            return;
        }

        initViews();
        setupListeners();
    }

    private void initViews() {
        tilUsername = findViewById(R.id.til_username);
        tilPassword = findViewById(R.id.til_password);
        etUsername = findViewById(R.id.et_username);
        etPassword = findViewById(R.id.et_password);
        btnLogin = findViewById(R.id.btn_login);
        tvRegisterLink = findViewById(R.id.tv_register_link);
        tvError = findViewById(R.id.tv_error);
        progressBar = findViewById(R.id.progress_bar);
    }

    private void setupListeners() {
        btnLogin.setOnClickListener(v -> attemptLogin());
        tvRegisterLink.setOnClickListener(v -> {
            Intent intent = new Intent(LoginActivity.this, RegisterActivity.class);
            startActivity(intent);
        });
    }

    private void attemptLogin() {
        // Сброс ошибок
        tilUsername.setError(null);
        tilPassword.setError(null);
        tvError.setVisibility(View.GONE);

        String username = etUsername.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        boolean cancel = false;
        View focusView = null;

        // Проверка пароля
        if (TextUtils.isEmpty(password)) {
            tilPassword.setError("Введите пароль");
            focusView = tilPassword;
            cancel = true;
        }

        // Проверка имени пользователя
        if (TextUtils.isEmpty(username)) {
            tilUsername.setError("Введите имя пользователя");
            focusView = tilUsername;
            cancel = true;
        } else if (username.length() < 3) {
            tilUsername.setError("Имя должно быть не менее 3 символов");
            focusView = tilUsername;
            cancel = true;
        }

        if (cancel) {
            focusView.requestFocus();
        } else {
            showProgress(true, "Вход в систему...");
            performLogin(username, password);
        }
    }

    private void performLogin(String username, String password) {
        syncManager.login(username, password, new SyncManager.AuthCallback() {
            @Override
            public void onSuccess(String token) {
                runOnUiThread(() -> {
                    showProgress(true, "Загрузка данных с сервера...");

                    // Устанавливаем таймаут на случай, если синхронизация зависнет
                    setupTimeout();

                    // Наблюдаем за синхронизацией
                    syncManager.isSyncInProgress().observe(LoginActivity.this, inProgress -> {
                        if (!inProgress && !isDataLoaded) {
                            // Синхронизация завершена - применяем тему и переходим
                            applyThemeAfterSync();
                            checkAllDataLoaded();
                        }
                    });

                    // Также наблюдаем за статусом, чтобы видеть прогресс
                    syncManager.getSyncStatus().observe(LoginActivity.this, status -> {
                        runOnUiThread(() -> {
                            tvError.setText(status);
                            tvError.setVisibility(View.VISIBLE);
                            tvError.setTextColor(getResources().getColor(R.color.text_primary));
                        });
                    });
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    showProgress(false, null);
                    tvError.setText(error);
                    tvError.setVisibility(View.VISIBLE);
                    tvError.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
                });
            }
        });
    }

    // Добавьте этот метод для применения темы после синхронизации
    private void applyThemeAfterSync() {
        SharedPreferences prefs = getSharedPreferences("user", MODE_PRIVATE);
        String userId = prefs.getString("user_id", null);

        if (userId != null) {
            DatabaseHelper dbHelper = new DatabaseHelper(this);
            UserSettings settings = dbHelper.getUserSettings(userId);

            // Применяем тему
            App.applyTheme(settings.getTheme());

            // Сохраняем тему в глобальные настройки
            App.saveAndApplyTheme(this, settings.getTheme());

            Log.d("LoginActivity", "Theme applied after sync: " + settings.getTheme().getDisplayName());
        }
    }

    // Обновите checkAllDataLoaded, чтобы он действительно запускал MainActivity
    private void checkAllDataLoaded() {
        // Проверяем, загружены ли все данные
        // Для простоты даем небольшую задержку, чтобы точно убедиться, что все операции завершены
        handler.postDelayed(() -> {
            if (!isDataLoaded) {
                isDataLoaded = true;
                if (timeoutRunnable != null) {
                    handler.removeCallbacks(timeoutRunnable);
                }

                // Убираем наблюдателей, чтобы они не мешали
                syncManager.isSyncInProgress().removeObservers(LoginActivity.this);
                syncManager.getSyncStatus().removeObservers(LoginActivity.this);

                startMainActivity();
            }
        }, 1000); // 1 секунда
    }

    private void setupTimeout() {
        // Таймаут 30 секунд на случай, если синхронизация зависнет
        timeoutRunnable = () -> {
            if (!isDataLoaded) {
                runOnUiThread(() -> {
                    tvError.setText("Превышено время ожидания. Проверьте соединение.");
                    tvError.setVisibility(View.VISIBLE);
                    tvError.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
                    showProgress(false, null);
                });
            }
        };
        handler.postDelayed(timeoutRunnable, 30000);
    }

    private void checkSyncStatusAndProceed() {
        // Проверяем, идет ли синхронизация
        if (syncManager.isSyncInProgress().getValue() != null &&
                syncManager.isSyncInProgress().getValue()) {

            // Синхронизация уже идет - показываем прогресс и ждем
            setContentView(R.layout.activity_login);
            initViews();
            showProgress(true, "Загрузка данных...");

            syncManager.isSyncInProgress().observe(this, inProgress -> {
                if (!inProgress && !isDataLoaded) {
                    checkAllDataLoaded();
                }
            });

            syncManager.getSyncStatus().observe(this, status -> {
                runOnUiThread(() -> {
                    tvError.setText(status);
                    tvError.setVisibility(View.VISIBLE);
                    tvError.setTextColor(getResources().getColor(android.R.color.white));
                });
            });
        } else {
            // Синхронизация не идет - просто переходим в MainActivity
            startMainActivity();
        }
    }

    private void showProgress(boolean show, String message) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        btnLogin.setEnabled(!show);
        tvRegisterLink.setEnabled(!show);
        etUsername.setEnabled(!show);
        etPassword.setEnabled(!show);

        if (show && message != null) {
            tvError.setText(message);
            tvError.setVisibility(View.VISIBLE);
            tvError.setTextColor(getResources().getColor(android.R.color.white));
        } else if (!show) {
            tvError.setVisibility(View.GONE);
        }
    }

    private void startMainActivity() {
        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (timeoutRunnable != null) {
            handler.removeCallbacks(timeoutRunnable);
        }
    }
}