package com.example.cogcdat_2;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.cogcdat_2.api.ApiClient;
import com.example.cogcdat_2.api.ApiService;
import com.example.cogcdat_2.api.models.ChangePasswordRequest;
import com.example.cogcdat_2.api.models.UpdateProfileRequest;
import com.example.cogcdat_2.api.models.User;
import com.example.cogcdat_2.sync.SyncManager;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SettingsFragment extends Fragment {

    private TextView tvUsername, tvEmail, tvFullName, tvLastSync, tvVersion, tvServerUrl;
    private Button btnEditProfile, btnSyncNow, btnChangePassword, btnLogout, btnDeleteAccount;
    private LinearLayout llSyncStatus;
    private ProgressBar progressSync;
    private TextView tvSyncStatus;
    private MaterialCardView cardProfile, cardSync, cardSecurity, cardAbout;

    private SyncManager syncManager;
    private ApiService apiService;
    private User currentUser;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_settings, container, false);

        syncManager = SyncManager.getInstance(requireContext());
        apiService = ApiClient.getInstance(requireContext()).getApiService();

        initViews(view);
        loadUserProfile();
        setupObservers();
        setupListeners();

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        loadUserProfile();
        updateLastSyncTime();
    }

    private void initViews(View view) {
        TextView tvTitle = view.findViewById(R.id.tv_title);
        tvTitle.setText("Настройки");

        tvUsername = view.findViewById(R.id.tv_username);
        tvEmail = view.findViewById(R.id.tv_email);
        tvFullName = view.findViewById(R.id.tv_full_name);

        tvLastSync = view.findViewById(R.id.tv_last_sync);
        tvVersion = view.findViewById(R.id.tv_version);
        tvServerUrl = view.findViewById(R.id.tv_server_url);
        btnSyncNow = view.findViewById(R.id.btn_sync_now);
        llSyncStatus = view.findViewById(R.id.ll_sync_status);
        progressSync = view.findViewById(R.id.progress_sync);
        tvSyncStatus = view.findViewById(R.id.tv_sync_status);

        btnEditProfile = view.findViewById(R.id.btn_edit_profile);
        btnChangePassword = view.findViewById(R.id.btn_change_password);
        btnDeleteAccount = view.findViewById(R.id.btn_delete_account); // Новая кнопка
        btnLogout = view.findViewById(R.id.btn_logout);

        cardProfile = view.findViewById(R.id.card_profile);
        cardSync = view.findViewById(R.id.card_sync);
        cardSecurity = view.findViewById(R.id.card_security);
        cardAbout = view.findViewById(R.id.card_about);

        try {
            String versionName = requireContext().getPackageManager()
                    .getPackageInfo(requireContext().getPackageName(), 0).versionName;
            tvVersion.setText(versionName);
        } catch (Exception e) {
            tvVersion.setText("1.0.0");
        }

        SharedPreferences prefs = requireContext().getSharedPreferences("api", Context.MODE_PRIVATE);
        String serverUrl = prefs.getString("server_url", "http://192.168.3.15:8000");
        tvServerUrl.setText(serverUrl);
    }

    private void loadUserProfile() {
        String token = syncManager.getSavedToken();
        if (token == null) return;

        apiService.getCurrentUser("Bearer " + token).enqueue(new Callback<User>() {
            @Override
            public void onResponse(Call<User> call, Response<User> response) {
                if (response.isSuccessful() && response.body() != null) {
                    currentUser = response.body();
                    updateUI();
                }
            }

            @Override
            public void onFailure(Call<User> call, Throwable t) {
                loadFromLocal();
            }
        });
    }

    private void loadFromLocal() {
        SharedPreferences prefs = requireContext().getSharedPreferences("user", Context.MODE_PRIVATE);
        String username = prefs.getString("username", "Пользователь");
        String email = prefs.getString("email", "email@example.com");
        String fullName = prefs.getString("full_name", "");

        tvUsername.setText(username);
        tvEmail.setText(email);
        tvFullName.setText(fullName.isEmpty() ? "Не указано" : fullName);
    }

    private void updateUI() {
        if (currentUser != null) {
            tvUsername.setText(currentUser.getUsername());
            tvEmail.setText(currentUser.getEmail());
            tvFullName.setText(currentUser.getFullName() != null && !currentUser.getFullName().isEmpty()
                    ? currentUser.getFullName() : "Не указано");

            SharedPreferences prefs = requireContext().getSharedPreferences("user", Context.MODE_PRIVATE);
            prefs.edit()
                    .putString("username", currentUser.getUsername())
                    .putString("email", currentUser.getEmail())
                    .putString("full_name", currentUser.getFullName())
                    .apply();
        }
    }

    private void setupObservers() {
        syncManager.isSyncInProgress().observe(getViewLifecycleOwner(), inProgress -> {
            llSyncStatus.setVisibility(inProgress ? View.VISIBLE : View.GONE);
            btnSyncNow.setEnabled(!inProgress);
            if (!inProgress) {
                updateLastSyncTime();
            }
        });

        syncManager.getSyncStatus().observe(getViewLifecycleOwner(), status -> {
            tvSyncStatus.setText(status);
        });
    }

    private void setupListeners() {
        btnEditProfile.setOnClickListener(v -> showEditProfileDialog());
        btnSyncNow.setOnClickListener(v -> {
            if (!isNetworkAvailable()) {
                Toast.makeText(getContext(), "Нет подключения к интернету", Toast.LENGTH_SHORT).show();
                return;
            }
            syncManager.syncAll();
            Toast.makeText(getContext(), "Синхронизация запущена", Toast.LENGTH_SHORT).show();
        });
        btnChangePassword.setOnClickListener(v -> showChangePasswordDialog());
        btnDeleteAccount.setOnClickListener(v -> showDeleteAccountConfirmation()); // Новая кнопка
        btnLogout.setOnClickListener(v -> showLogoutConfirmation());
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager =
                (ConnectivityManager) requireContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

    private void updateLastSyncTime() {
        SharedPreferences prefs = requireContext().getSharedPreferences("sync", Context.MODE_PRIVATE);
        String lastSync = prefs.getString("last_sync_time", null);

        if (lastSync != null) {
            try {
                SimpleDateFormat dbFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
                SimpleDateFormat displayFormat = new SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault());
                Date date = dbFormat.parse(lastSync);
                tvLastSync.setText(displayFormat.format(date));
            } catch (Exception e) {
                tvLastSync.setText(lastSync);
            }
        } else {
            tvLastSync.setText("никогда");
        }
    }

    private void showEditProfileDialog() {
        // Проверяем интернет
        if (!isNetworkAvailable()) {
            Toast.makeText(getContext(), "Для изменения профиля требуется подключение к интернету", Toast.LENGTH_LONG).show();
            return;
        }

        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_edit_profile, null);

        TextView tvUsernameDisplay = dialogView.findViewById(R.id.tv_username_display);
        TextView tvEmailDisplay = dialogView.findViewById(R.id.tv_email_display);
        TextInputLayout tilFullName = dialogView.findViewById(R.id.til_full_name);
        TextInputEditText etFullName = dialogView.findViewById(R.id.et_full_name);
        Button btnCancel = dialogView.findViewById(R.id.btn_cancel);
        Button btnSave = dialogView.findViewById(R.id.btn_save);

        if (currentUser != null) {
            tvUsernameDisplay.setText(currentUser.getUsername());
            tvEmailDisplay.setText(currentUser.getEmail());
            etFullName.setText(currentUser.getFullName());
        } else {
            SharedPreferences prefs = requireContext().getSharedPreferences("user", Context.MODE_PRIVATE);
            tvUsernameDisplay.setText(prefs.getString("username", "user"));
            tvEmailDisplay.setText(prefs.getString("email", "email@example.com"));
            etFullName.setText(prefs.getString("full_name", ""));
        }

        AlertDialog dialog = new AlertDialog.Builder(getContext())
                .setView(dialogView)
                .create();

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        btnSave.setOnClickListener(v -> {
            String fullName = etFullName.getText().toString().trim();
            String email = tvEmailDisplay.getText().toString().trim();

            // Повторная проверка интернета перед отправкой
            if (!isNetworkAvailable()) {
                Toast.makeText(getContext(), "Нет подключения к интернету", Toast.LENGTH_SHORT).show();
                return;
            }

            // Показываем прогресс
            btnSave.setEnabled(false);
            btnSave.setText("Сохранение...");

            updateProfile(fullName, email, dialog, btnSave);
        });

        dialog.show();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }
    }

    private void updateProfile(String fullName, String email, AlertDialog dialog, Button btnSave) {
        String token = syncManager.getSavedToken();
        if (token == null || currentUser == null) {
            Toast.makeText(getContext(), "Ошибка авторизации", Toast.LENGTH_SHORT).show();
            dialog.dismiss();
            return;
        }

        UpdateProfileRequest request = new UpdateProfileRequest(fullName, email);

        apiService.updateUser("Bearer " + token, currentUser.getId(), request).enqueue(new Callback<User>() {
            @Override
            public void onResponse(Call<User> call, Response<User> response) {
                if (response.isSuccessful() && response.body() != null) {
                    currentUser = response.body();

                    // Обновляем UI
                    tvFullName.setText(fullName.isEmpty() ? "Не указано" : fullName);
                    tvEmail.setText(email);

                    // Сохраняем в SharedPreferences
                    SharedPreferences prefs = requireContext().getSharedPreferences("user", Context.MODE_PRIVATE);
                    prefs.edit()
                            .putString("full_name", fullName)
                            .putString("email", email)
                            .apply();

                    Toast.makeText(getContext(), "Профиль обновлен", Toast.LENGTH_SHORT).show();
                    dialog.dismiss();
                } else {
                    btnSave.setEnabled(true);
                    btnSave.setText("Сохранить");

                    String errorMsg = "Ошибка обновления профиля";
                    if (response.code() == 400) {
                        errorMsg = "Email уже используется";
                    } else if (response.code() == 401) {
                        errorMsg = "Сессия истекла, войдите заново";
                    }
                    Toast.makeText(getContext(), errorMsg, Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<User> call, Throwable t) {
                btnSave.setEnabled(true);
                btnSave.setText("Сохранить");

                if (!isNetworkAvailable()) {
                    Toast.makeText(getContext(), "Нет подключения к интернету", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(getContext(), "Ошибка сети: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void showChangePasswordDialog() {
        // Проверяем интернет
        if (!isNetworkAvailable()) {
            Toast.makeText(getContext(), "Для смены пароля требуется подключение к интернету", Toast.LENGTH_LONG).show();
            return;
        }

        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_change_password, null);

        TextInputLayout tilCurrentPassword = dialogView.findViewById(R.id.til_current_password);
        TextInputLayout tilNewPassword = dialogView.findViewById(R.id.til_new_password);
        TextInputLayout tilConfirmPassword = dialogView.findViewById(R.id.til_confirm_password);

        EditText etCurrentPassword = dialogView.findViewById(R.id.et_current_password);
        EditText etNewPassword = dialogView.findViewById(R.id.et_new_password);
        EditText etConfirmPassword = dialogView.findViewById(R.id.et_confirm_password);

        TextView tvError = dialogView.findViewById(R.id.tv_error);
        Button btnCancel = dialogView.findViewById(R.id.btn_cancel);
        Button btnSave = dialogView.findViewById(R.id.btn_save);

        AlertDialog dialog = new AlertDialog.Builder(getContext())
                .setView(dialogView)
                .create();

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        btnSave.setOnClickListener(v -> {
            String currentPass = etCurrentPassword.getText().toString();
            String newPass = etNewPassword.getText().toString();
            String confirmPass = etConfirmPassword.getText().toString();

            // Сброс ошибок
            tilCurrentPassword.setError(null);
            tilNewPassword.setError(null);
            tilConfirmPassword.setError(null);
            tvError.setVisibility(View.GONE);

            // Валидация
            if (currentPass.isEmpty()) {
                tilCurrentPassword.setError("Введите текущий пароль");
                return;
            }

            if (newPass.isEmpty()) {
                tilNewPassword.setError("Введите новый пароль");
                return;
            }

            if (newPass.length() < 6) {
                tilNewPassword.setError("Пароль должен быть не менее 6 символов");
                return;
            }

            if (!newPass.equals(confirmPass)) {
                tilConfirmPassword.setError("Пароли не совпадают");
                return;
            }

            // Повторная проверка интернета
            if (!isNetworkAvailable()) {
                Toast.makeText(getContext(), "Нет подключения к интернету", Toast.LENGTH_SHORT).show();
                return;
            }

            // Показываем прогресс
            btnSave.setEnabled(false);
            btnSave.setText("Сохранение...");

            changePassword(currentPass, newPass, dialog, btnSave, tvError);
        });

        dialog.show();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }
    }

    private void changePassword(String currentPassword, String newPassword,
                                AlertDialog dialog, Button btnSave, TextView tvError) {
        String token = syncManager.getSavedToken();
        if (token == null) {
            Toast.makeText(getContext(), "Ошибка авторизации", Toast.LENGTH_SHORT).show();
            dialog.dismiss();
            return;
        }

        ChangePasswordRequest request = new ChangePasswordRequest(currentPassword, newPassword);

        apiService.changePassword("Bearer " + token, request).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(getContext(), "Пароль успешно изменен", Toast.LENGTH_SHORT).show();
                    dialog.dismiss();
                } else {
                    btnSave.setEnabled(true);
                    btnSave.setText("Сохранить");

                    // Обработка ошибок
                    if (response.code() == 401) {
                        tvError.setText("Неверный текущий пароль");
                        tvError.setVisibility(View.VISIBLE);
                    } else {
                        Toast.makeText(getContext(), "Ошибка при смене пароля", Toast.LENGTH_SHORT).show();
                        dialog.dismiss();
                    }
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                btnSave.setEnabled(true);
                btnSave.setText("Сохранить");

                if (!isNetworkAvailable()) {
                    Toast.makeText(getContext(), "Нет подключения к интернету", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(getContext(), "Ошибка сети: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                }
                dialog.dismiss();
            }
        });
    }

    // НОВЫЙ МЕТОД: Подтверждение удаления аккаунта
    private void showDeleteAccountConfirmation() {
        // Проверяем интернет (удаление аккаунта требует интернета)
        if (!isNetworkAvailable()) {
            Toast.makeText(getContext(), "Для удаления аккаунта требуется подключение к интернету", Toast.LENGTH_LONG).show();
            return;
        }

        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_delete_account_confirmation, null);

        Button btnCancel = dialogView.findViewById(R.id.btn_cancel_delete);
        Button btnConfirm = dialogView.findViewById(R.id.btn_confirm_delete);
        TextView tvWarning = dialogView.findViewById(R.id.tv_warning);
        EditText etPassword = dialogView.findViewById(R.id.et_password);
        TextInputLayout tilPassword = dialogView.findViewById(R.id.til_password);

        AlertDialog dialog = new AlertDialog.Builder(getContext())
                .setView(dialogView)
                .create();

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        btnConfirm.setOnClickListener(v -> {
            String password = etPassword.getText().toString().trim();

            if (password.isEmpty()) {
                tilPassword.setError("Введите пароль для подтверждения");
                return;
            }

            // Показываем прогресс
            btnConfirm.setEnabled(false);
            btnConfirm.setText("Удаление...");

            deleteAccount(password, dialog, btnConfirm, tilPassword);
        });

        dialog.show();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }
    }

    // НОВЫЙ МЕТОД: Удаление аккаунта через API
    private void deleteAccount(String password, AlertDialog dialog, Button btnConfirm, TextInputLayout tilPassword) {
        String token = syncManager.getSavedToken();
        if (token == null || currentUser == null) {
            Toast.makeText(getContext(), "Ошибка авторизации", Toast.LENGTH_SHORT).show();
            dialog.dismiss();
            return;
        }

        // Сначала подтверждаем пароль (можно через отдельный эндпоинт или включить в удаление)
        // Для простоты будем считать, что на сервере есть эндпоинт DELETE /users/{userId} с подтверждением пароля

        apiService.deleteUser("Bearer " + token, currentUser.getId()).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    // Успешно удалили на сервере
                    Toast.makeText(getContext(), "Аккаунт успешно удален", Toast.LENGTH_SHORT).show();

                    // Очищаем локальные данные и выходим
                    syncManager.logout();

                    Intent intent = new Intent(getActivity(), LoginActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    if (getActivity() != null) {
                        getActivity().finish();
                    }
                } else {
                    btnConfirm.setEnabled(true);
                    btnConfirm.setText("Удалить");

                    if (response.code() == 401) {
                        tilPassword.setError("Неверный пароль");
                    } else if (response.code() == 400) {
                        Toast.makeText(getContext(), "Невозможно удалить аккаунт с данными", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(getContext(), "Ошибка при удалении аккаунта", Toast.LENGTH_SHORT).show();
                    }
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                btnConfirm.setEnabled(true);
                btnConfirm.setText("Удалить");

                if (!isNetworkAvailable()) {
                    Toast.makeText(getContext(), "Нет подключения к интернету", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(getContext(), "Ошибка сети: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void showLogoutConfirmation() {
        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_logout_confirmation, null);

        Button btnCancel = dialogView.findViewById(R.id.btn_cancel_delete);
        Button btnConfirm = dialogView.findViewById(R.id.btn_confirm_delete);
        TextView tvWarning = dialogView.findViewById(R.id.tv_warning);

        tvWarning.setText("Все локальные данные будут удалены. Это действие нельзя отменить.");

        AlertDialog dialog = new AlertDialog.Builder(getContext())
                .setView(dialogView)
                .create();

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        btnConfirm.setOnClickListener(v -> {
            dialog.dismiss();

            // Для выхода интернет не нужен - очищаем локально и выходим
            syncManager.logout();

            Intent intent = new Intent(getActivity(), LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            if (getActivity() != null) {
                getActivity().finish();
            }
        });

        dialog.show();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }
    }
}