    package com.example.cogcdat_2;

    import android.app.AlertDialog;
    import android.content.Context;
    import android.content.Intent;
    import android.content.SharedPreferences;
    import android.net.ConnectivityManager;
    import android.net.NetworkInfo;
    import android.os.Bundle;
    import android.util.Log;
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
    import androidx.appcompat.app.AppCompatDelegate;
    import androidx.fragment.app.Fragment;
    import androidx.recyclerview.widget.LinearLayoutManager;
    import androidx.recyclerview.widget.RecyclerView;

    import com.example.cogcdat_2.api.ApiClient;
    import com.example.cogcdat_2.api.ApiService;
    import com.example.cogcdat_2.api.models.ApiUserSettings;
    import com.example.cogcdat_2.api.models.ChangePasswordRequest;
    import com.example.cogcdat_2.api.models.UpdateProfileRequest;
    import com.example.cogcdat_2.api.models.User;
    import com.example.cogcdat_2.sync.SyncManager;
    import com.google.android.material.card.MaterialCardView;
    import com.google.android.material.textfield.TextInputEditText;
    import com.google.android.material.textfield.TextInputLayout;

    import java.text.SimpleDateFormat;
    import java.util.ArrayList;
    import java.util.Arrays;
    import java.util.Date;
    import java.util.List;
    import java.util.Locale;

    import retrofit2.Call;
    import retrofit2.Callback;
    import retrofit2.Response;

    public class SettingsFragment extends Fragment {

        private TextView tvUsername, tvEmail, tvFullName, tvLastSync, tvVersion, tvServerUrl;
        private TextView tvDistanceUnitValue, tvThemeValue, tvLanguageValue;
        private Button btnEditProfile, btnSyncNow, btnChangePassword, btnLogout, btnDeleteAccount;
        private Button btnChangeDistanceUnit, btnChangeTheme, btnChangeLanguage;
        private LinearLayout llSyncStatus;
        private ProgressBar progressSync;
        private TextView tvSyncStatus;
        private MaterialCardView cardProfile, cardSync, cardSecurity, cardAbout, cardPreferences;

        private SyncManager syncManager;
        private ApiService apiService;
        private DatabaseHelper dbHelper;
        private UserSettings userSettings;
        private User currentUser;
        private String currentUserId;

        @Override
        public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
            View view = inflater.inflate(R.layout.fragment_settings, container, false);

            syncManager = SyncManager.getInstance(requireContext());
            apiService = ApiClient.getInstance(requireContext()).getApiService();
            dbHelper = new DatabaseHelper(requireContext());

            initViews(view);
            loadCurrentUserId();
            loadUserProfile();
            loadUserSettings();
            setupObservers();
            setupListeners();

            return view;
        }

        private boolean isFragmentAttached = true;

        @Override
        public void onAttach(@NonNull Context context) {
            super.onAttach(context);
            isFragmentAttached = true;
        }

        @Override
        public void onDetach() {
            super.onDetach();
            isFragmentAttached = false;
        }

        @Override
        public void onDestroyView() {
            super.onDestroyView();
            isFragmentAttached = false;
        }



        @Override
        public void onResume() {
            super.onResume();
            loadCurrentUserId();
            loadUserProfile();
            loadUserSettings();
            updateLastSyncTime();
        }

        private void initViews(View view) {
            TextView tvTitle = view.findViewById(R.id.tv_title);
            tvTitle.setText(getString(R.string.settings_fragment_title));

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

            // Настройки предпочтений
            tvDistanceUnitValue = view.findViewById(R.id.tv_distance_unit_value);
            tvThemeValue = view.findViewById(R.id.tv_theme_value);
            tvLanguageValue = view.findViewById(R.id.tv_language_value);
            btnChangeDistanceUnit = view.findViewById(R.id.btn_change_distance_unit);
            btnChangeTheme = view.findViewById(R.id.btn_change_theme);
            btnChangeLanguage = view.findViewById(R.id.btn_change_language);

            btnEditProfile = view.findViewById(R.id.btn_edit_profile);
            btnChangePassword = view.findViewById(R.id.btn_change_password);
            btnDeleteAccount = view.findViewById(R.id.btn_delete_account);
            btnLogout = view.findViewById(R.id.btn_logout);

            cardProfile = view.findViewById(R.id.card_profile);
            cardSync = view.findViewById(R.id.card_sync);
            cardSecurity = view.findViewById(R.id.card_security);
            cardAbout = view.findViewById(R.id.card_about);
            cardPreferences = view.findViewById(R.id.card_preferences);

            try {
                String versionName = requireContext().getPackageManager()
                        .getPackageInfo(requireContext().getPackageName(), 0).versionName;
                tvVersion.setText(versionName);
            } catch (Exception e) {
                tvVersion.setText(getString(R.string.default_version));
            }

            SharedPreferences prefs = requireContext().getSharedPreferences("api", Context.MODE_PRIVATE);
            String serverUrl = prefs.getString("server_url", "http://192.168.3.15:8000");
            tvServerUrl.setText(serverUrl);
        }

        private void loadCurrentUserId() {
            SharedPreferences prefs = requireContext().getSharedPreferences("user", Context.MODE_PRIVATE);
            currentUserId = prefs.getString("user_id", null);
        }

        private void loadUserProfile() {
            String token = syncManager.getSavedToken();
            if (token == null) return;

            apiService.getCurrentUser("Bearer " + token).enqueue(new Callback<User>() {
                @Override
                public void onResponse(Call<User> call, Response<User> response) {
                    if (!isFragmentAttached || !isAdded() || getActivity() == null) {
                        return; // Фрагмент уже откреплен - выходим
                    }

                    if (response.isSuccessful() && response.body() != null) {
                        currentUser = response.body();
                        updateUI();

                        if (currentUserId == null) {
                            SharedPreferences prefs = requireContext().getSharedPreferences("user", Context.MODE_PRIVATE);
                            prefs.edit().putString("user_id", currentUser.getId()).apply();
                            currentUserId = currentUser.getId();
                        }
                    }
                }

                @Override
                public void onFailure(Call<User> call, Throwable t) {
                    if (!isFragmentAttached || !isAdded() || getActivity() == null) {
                        return;
                    }
                    loadFromLocal();
                }
            });
        }

        private void loadUserSettings() {
            if (currentUserId == null) return;

            userSettings = dbHelper.getUserSettings(currentUserId);
            updateSettingsUI();

            if (isNetworkAvailable() && syncManager.getSavedToken() != null) {
                loadSettingsFromServer();
            }
        }

        private void loadSettingsFromServer() {
            String token = syncManager.getSavedToken();
            if (token == null) return;

            apiService.getSettings("Bearer " + token).enqueue(new Callback<ApiUserSettings>() {
                @Override
                public void onResponse(Call<ApiUserSettings> call, Response<ApiUserSettings> response) {
                    if (!isFragmentAttached || !isAdded() || getActivity() == null) {
                        return;
                    }

                    if (response.isSuccessful() && response.body() != null) {
                        ApiUserSettings serverSettings = response.body();
                        UserSettings serverLocalSettings = serverSettings.toLocalSettings();

                        UserSettings localSettings = dbHelper.getUserSettings(currentUserId);

                        if (localSettings == null ||
                                isServerNewer(serverLocalSettings.getUpdatedAt(), localSettings.getUpdatedAt())) {

                            dbHelper.saveUserSettings(serverLocalSettings);
                            userSettings = serverLocalSettings;
                            updateSettingsUI();

                            // Применяем тему
                            if (isFragmentAttached && getActivity() != null) {
                                App.saveAndApplyTheme(requireContext(), serverLocalSettings.getTheme());
                            }

                            Log.d("Settings", "Settings updated from server");
                        }
                    }
                }

                @Override
                public void onFailure(Call<ApiUserSettings> call, Throwable t) {
                    if (!isFragmentAttached || !isAdded() || getActivity() == null) {
                        return;
                    }
                    Log.e("Settings", "Failed to load settings from server", t);
                }
            });
        }

        private boolean isServerNewer(String serverTime, String localTime) {
            if (localTime == null) return true;
            if (serverTime == null) return false;
            return serverTime.compareTo(localTime) > 0;
        }

        private void loadFromLocal() {
            SharedPreferences prefs = requireContext().getSharedPreferences("user", Context.MODE_PRIVATE);
            String username = prefs.getString("username", getString(R.string.default_username));
            String email = prefs.getString("email", "email@example.com");
            String fullName = prefs.getString("full_name", "");

            tvUsername.setText(username);
            tvEmail.setText(email);
            tvFullName.setText(fullName.isEmpty() ? getString(R.string.not_specified) : fullName);
        }

        private void updateUI() {
            if (!isFragmentAttached || !isAdded() || getActivity() == null || getView() == null) {
                return;
            }

            if (currentUser != null) {
                tvUsername.setText(currentUser.getUsername());
                tvEmail.setText(currentUser.getEmail());
                tvFullName.setText(currentUser.getFullName() != null && !currentUser.getFullName().isEmpty()
                        ? currentUser.getFullName() : getString(R.string.not_specified));

                SharedPreferences prefs = requireContext().getSharedPreferences("user", Context.MODE_PRIVATE);
                prefs.edit()
                        .putString("username", currentUser.getUsername())
                        .putString("email", currentUser.getEmail())
                        .putString("full_name", currentUser.getFullName())
                        .putString("user_id", currentUser.getId())
                        .apply();

                currentUserId = currentUser.getId();
            }
        }

        private void updateSettingsUI() {
            if (!isFragmentAttached || !isAdded() || getActivity() == null || getView() == null) {
                return;
            }

            if (userSettings != null) {
                // Передаём контекст requireContext()
                tvDistanceUnitValue.setText(userSettings.getDistanceUnit().getDisplayName(requireContext()));
                tvThemeValue.setText(userSettings.getTheme().getDisplayName(requireContext()));
                tvLanguageValue.setText(userSettings.getLanguage().getDisplayName(requireContext()));
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
                    Toast.makeText(getContext(), getString(R.string.no_internet_connection), Toast.LENGTH_SHORT).show();
                    return;
                }
                syncManager.syncAll();
                Toast.makeText(getContext(), R.string.sync_started, Toast.LENGTH_SHORT).show();
            });
            btnChangePassword.setOnClickListener(v -> showChangePasswordDialog());
            btnDeleteAccount.setOnClickListener(v -> showDeleteAccountConfirmation());
            btnLogout.setOnClickListener(v -> showLogoutConfirmation());

            btnChangeDistanceUnit.setOnClickListener(v -> showDistanceUnitDialog());
            btnChangeTheme.setOnClickListener(v -> showThemeDialog());
            btnChangeLanguage.setOnClickListener(v -> showLanguageDialog());
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
                tvLastSync.setText(R.string.never);
            }
        }

        /**
         * Красивый диалог выбора единиц расстояния
         */
        private void showDistanceUnitDialog() {
            View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_unit_selector, null);

            TextView tvTitle = dialogView.findViewById(R.id.tv_dialog_title);
            RecyclerView rvUnits = dialogView.findViewById(R.id.rv_units);
            Button btnCancel = dialogView.findViewById(R.id.btn_cancel_unit);
            Button btnConfirm = dialogView.findViewById(R.id.btn_confirm_unit);

            tvTitle.setText(R.string.distance_units_dialog_title);

            // Список единиц расстояния
            List<DistanceUnit> units = Arrays.asList(DistanceUnit.KM, DistanceUnit.MI);
            List<String> unitNames = new ArrayList<>();
            for (DistanceUnit unit : units) {
                unitNames.add(unit.getDisplayName(requireContext()));
            }

            // Создаем адаптер
            UnitSelectionAdapter adapter = new UnitSelectionAdapter(unitNames, null);

            // Находим текущую позицию
            int currentPosition = units.indexOf(userSettings.getDistanceUnit());
            adapter.setSelectedPosition(currentPosition);

            rvUnits.setLayoutManager(new LinearLayoutManager(requireContext()));
            rvUnits.setAdapter(adapter);

            AlertDialog dialog = new AlertDialog.Builder(requireContext())
                    .setView(dialogView)
                    .create();

            btnCancel.setOnClickListener(v -> dialog.dismiss());

            btnConfirm.setOnClickListener(v -> {
                int selectedPos = adapter.getSelectedPosition();
                if (selectedPos >= 0 && selectedPos < units.size()) {
                    DistanceUnit selectedUnit = units.get(selectedPos);

                    if (selectedUnit != userSettings.getDistanceUnit()) {
                        userSettings.setDistanceUnit(selectedUnit);
                        saveSettings();
                    }
                }
                dialog.dismiss();
            });

            dialog.show();

            if (dialog.getWindow() != null) {
                dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
            }
        }

        /**
         * Красивый диалог выбора темы
         */
        private void showThemeDialog() {
            View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_unit_selector, null);

            TextView tvTitle = dialogView.findViewById(R.id.tv_dialog_title);
            RecyclerView rvUnits = dialogView.findViewById(R.id.rv_units);
            Button btnCancel = dialogView.findViewById(R.id.btn_cancel_unit);
            Button btnConfirm = dialogView.findViewById(R.id.btn_confirm_unit);

            tvTitle.setText(R.string.theme_dialog_title);

            List<Theme> themes = Arrays.asList(Theme.SYSTEM, Theme.LIGHT, Theme.DARK);
            List<String> themeNames = new ArrayList<>();
            for (Theme theme : themes) {
                themeNames.add(theme.getDisplayName(requireContext()));
            }

            UnitSelectionAdapter adapter = new UnitSelectionAdapter(themeNames, null);

            int currentPosition = themes.indexOf(userSettings.getTheme());
            adapter.setSelectedPosition(currentPosition);

            rvUnits.setLayoutManager(new LinearLayoutManager(requireContext()));
            rvUnits.setAdapter(adapter);

            AlertDialog dialog = new AlertDialog.Builder(requireContext())
                    .setView(dialogView)
                    .create();

            // Отмечаем, что диалог открыт
            dialog.setOnDismissListener(dialogInterface -> {
                // Ничего не делаем
            });

            btnCancel.setOnClickListener(v -> dialog.dismiss());

            btnConfirm.setOnClickListener(v -> {
                int selectedPos = adapter.getSelectedPosition();
                if (selectedPos >= 0 && selectedPos < themes.size()) {
                    Theme selectedTheme = themes.get(selectedPos);

                    if (selectedTheme != userSettings.getTheme()) {
                        userSettings.setTheme(selectedTheme);

                        // Сначала сохраняем настройки
                        saveSettings();

                        // Закрываем диалог
                        dialog.dismiss();

                    } else {
                        dialog.dismiss();
                    }
                }
            });

            dialog.show();

            if (dialog.getWindow() != null) {
                dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
            }
        }

        /**
         * Красивый диалог выбора языка
         */
        private void showLanguageDialog() {
            String[] languages = {
                    getString(R.string.lang_russian),
                    getString(R.string.lang_english)
            };
            int checkedItem = getSavedLanguageIndex();

            new AlertDialog.Builder(requireContext())
                    .setTitle(R.string.language_label)
                    .setSingleChoiceItems(languages, checkedItem, (dialog, which) -> {
                        String newLang = (which == 0) ? "ru" : "en";
                        String currentLang = LocaleHelper.getLanguage(requireContext());

                        if (!newLang.equals(currentLang)) {
                            // Сохраняем язык в UserSettings (БД)
                            if (userSettings != null) {
                                userSettings.setLanguage(newLang);
                                dbHelper.saveUserSettings(userSettings);
                            }
                            // Применяем локаль через LocaleHelper (SharedPreferences)
                            LocaleHelper.setLocale(requireContext(), newLang);
                            // Пересоздаём текущую активность для обновления интерфейса
                            requireActivity().recreate();
                        }
                        dialog.dismiss();
                    })
                    .setNegativeButton(R.string.cancel, null)
                    .show();
        }

        private int getSavedLanguageIndex() {
            String lang = LocaleHelper.getLanguage(requireContext());
            return lang.equals("ru") ? 0 : 1;
        }

        private void restartApp() {
            if (getActivity() == null) return;

            Intent intent = new Intent(requireContext(), MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            getActivity().finishAffinity(); // Корректное завершение вместо exit(0)
        }


        private void saveSettings() {
            if (currentUserId == null) {
                Toast.makeText(getContext(), R.string.error_user_not_identified, Toast.LENGTH_SHORT).show();
                return;
            }

            dbHelper.saveUserSettings(userSettings);
            updateSettingsUI();

            // Применяем тему НО НЕ ПЕРЕЗАГРУЖАЕМ ФРАГМЕНТ
            if (isFragmentAttached && getActivity() != null) {
                App.saveAndApplyTheme(requireContext(), userSettings.getTheme());
            }

            if (isNetworkAvailable() && syncManager.getSavedToken() != null) {
                uploadSettingsToServer();
            }
        }

        private void uploadSettingsToServer() {
            String token = syncManager.getSavedToken();
            ApiUserSettings apiSettings = new ApiUserSettings(userSettings);

            apiService.updateSettings("Bearer " + token, apiSettings).enqueue(new Callback<ApiUserSettings>() {
                @Override
                public void onResponse(Call<ApiUserSettings> call, Response<ApiUserSettings> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        Log.d("Settings", "Settings uploaded successfully");
                    } else {
                        Log.e("Settings", "Failed to upload settings");
                    }
                }

                @Override
                public void onFailure(Call<ApiUserSettings> call, Throwable t) {
                    Log.e("Settings", "Network error uploading settings", t);
                }
            });
        }


        // --- Адаптер для выбора единиц ---
        private class UnitSelectionAdapter extends RecyclerView.Adapter<UnitSelectionAdapter.UnitViewHolder> {
            private List<String> items;
            private int selectedPosition = -1;

            public UnitSelectionAdapter(List<String> items, Integer currentSelection) {
                this.items = items;
            }

            public void setSelectedPosition(int position) {
                int oldPosition = selectedPosition;
                selectedPosition = position;
                if (oldPosition != -1) {
                    notifyItemChanged(oldPosition);
                }
                if (selectedPosition != -1) {
                    notifyItemChanged(selectedPosition);
                }
            }

            public int getSelectedPosition() {
                return selectedPosition;
            }

            @NonNull
            @Override
            public UnitViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                View view = LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.item_unit_option, parent, false);
                return new UnitViewHolder(view);
            }

            @Override
            public void onBindViewHolder(@NonNull UnitViewHolder holder, int position) {
                String item = items.get(position);
                holder.tvUnitName.setText(item);

                boolean isSelected = position == selectedPosition;
                if (isSelected) {
                    holder.cardView.setStrokeWidth(5);
                    holder.cardView.setStrokeColor(requireContext().getColor(R.color.primary));
                } else {
                    holder.cardView.setStrokeWidth(0);
                }

                holder.itemView.setOnClickListener(v -> {
                    setSelectedPosition(position);
                });
            }

            @Override
            public int getItemCount() {
                return items.size();
            }

            class UnitViewHolder extends RecyclerView.ViewHolder {
                TextView tvUnitName;
                MaterialCardView cardView;

                UnitViewHolder(@NonNull View itemView) {
                    super(itemView);
                    cardView = (MaterialCardView) itemView;
                    tvUnitName = itemView.findViewById(R.id.tv_unit_name);
                }
            }
        }
        private void showEditProfileDialog() {
            if (!isNetworkAvailable()) {
                Toast.makeText(getContext(), R.string.profile_edit_requires_internet, Toast.LENGTH_LONG).show();
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

                if (!isNetworkAvailable()) {
                    Toast.makeText(getContext(), R.string.no_internet_connection, Toast.LENGTH_SHORT).show();
                    return;
                }

                btnSave.setEnabled(false);
                btnSave.setText(getString(R.string.saving));

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
                Toast.makeText(getContext(), getString(R.string.authorization_error), Toast.LENGTH_SHORT).show();
                dialog.dismiss();
                return;
            }

            UpdateProfileRequest request = new UpdateProfileRequest(fullName, email);

            apiService.updateUser("Bearer " + token, currentUser.getId(), request).enqueue(new Callback<User>() {
                @Override
                public void onResponse(Call<User> call, Response<User> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        currentUser = response.body();

                        tvFullName.setText(fullName.isEmpty() ? getString(R.string.not_specified) : fullName);
                        tvEmail.setText(email);

                        SharedPreferences prefs = requireContext().getSharedPreferences("user", Context.MODE_PRIVATE);
                        prefs.edit()
                                .putString("full_name", fullName)
                                .putString("email", email)
                                .apply();

                        Toast.makeText(getContext(), R.string.profile_updated, Toast.LENGTH_SHORT).show();
                        dialog.dismiss();
                    } else {
                        btnSave.setEnabled(true);
                        btnSave.setText(R.string.save_button);

                        String errorMsg = getString(R.string.profile_update_error);
                        if (response.code() == 400) {
                            errorMsg = getString(R.string.email_already_in_use);
                        } else if (response.code() == 401) {
                            errorMsg = getString(R.string.session_expired);
                        }
                        Toast.makeText(getContext(), errorMsg, Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onFailure(Call<User> call, Throwable t) {
                    btnSave.setEnabled(true);
                    btnSave.setText(R.string.save_button);

                    if (!isNetworkAvailable()) {
                        Toast.makeText(getContext(), R.string.no_internet_connection, Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(getContext(), R.string.network_error + t.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                }
            });
        }

        private void showChangePasswordDialog() {
            if (!isNetworkAvailable()) {
                Toast.makeText(getContext(), R.string.password_change_requires_internet, Toast.LENGTH_LONG).show();
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

                tilCurrentPassword.setError(null);
                tilNewPassword.setError(null);
                tilConfirmPassword.setError(null);
                tvError.setVisibility(View.GONE);

                if (currentPass.isEmpty()) {
                    tilCurrentPassword.setError(getString(R.string.enter_current_password));
                    return;
                }

                if (newPass.isEmpty()) {
                    tilNewPassword.setError(getString(R.string.enter_new_password));
                    return;
                }

                if (newPass.length() < 6) {
                    tilNewPassword.setError(getString(R.string.password_min_length));
                    return;
                }

                if (!newPass.equals(confirmPass)) {
                    tilConfirmPassword.setError(getString(R.string.passwords_do_not_match));
                    return;
                }

                if (!isNetworkAvailable()) {
                    Toast.makeText(getContext(), R.string.no_internet_connection, Toast.LENGTH_SHORT).show();
                    return;
                }

                btnSave.setEnabled(false);
                btnSave.setText(R.string.saving);

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
                Toast.makeText(getContext(), getString(R.string.authorization_error), Toast.LENGTH_SHORT).show();
                dialog.dismiss();
                return;
            }

            ChangePasswordRequest request = new ChangePasswordRequest(currentPassword, newPassword);

            apiService.changePassword("Bearer " + token, request).enqueue(new Callback<Void>() {
                @Override
                public void onResponse(Call<Void> call, Response<Void> response) {
                    if (response.isSuccessful()) {
                        Toast.makeText(getContext(), R.string.password_changed_successfully, Toast.LENGTH_SHORT).show();
                        dialog.dismiss();
                    } else {
                        btnSave.setEnabled(true);
                        btnSave.setText(R.string.save_button);

                        if (response.code() == 401) {
                            tvError.setText(R.string.invalid_current_password);
                            tvError.setVisibility(View.VISIBLE);
                        } else {
                            Toast.makeText(getContext(), R.string.password_change_error, Toast.LENGTH_SHORT).show();
                            dialog.dismiss();
                        }
                    }
                }

                @Override
                public void onFailure(Call<Void> call, Throwable t) {
                    btnSave.setEnabled(true);
                    btnSave.setText(R.string.save_button);

                    if (!isNetworkAvailable()) {
                        Toast.makeText(getContext(), R.string.no_internet_connection, Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(getContext(), R.string.network_error + t.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                    dialog.dismiss();
                }
            });
        }

        private void showDeleteAccountConfirmation() {
            if (!isNetworkAvailable()) {
                Toast.makeText(getContext(), R.string.account_delete_requires_internet, Toast.LENGTH_LONG).show();
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
                    tilPassword.setError(getString(R.string.enter_password_to_confirm));
                    return;
                }

                btnConfirm.setEnabled(false);
                btnConfirm.setText(R.string.deleting);

                deleteAccount(password, dialog, btnConfirm, tilPassword);
            });

            dialog.show();
            if (dialog.getWindow() != null) {
                dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
            }
        }

        private void deleteAccount(String password, AlertDialog dialog, Button btnConfirm, TextInputLayout tilPassword) {
            String token = syncManager.getSavedToken();
            if (token == null || currentUser == null) {
                Toast.makeText(getContext(), "Ошибка авторизации", Toast.LENGTH_SHORT).show();
                dialog.dismiss();
                return;
            }

            apiService.deleteUser("Bearer " + token, currentUser.getId()).enqueue(new Callback<Void>() {
                @Override
                public void onResponse(Call<Void> call, Response<Void> response) {
                    if (response.isSuccessful()) {
                        Toast.makeText(getContext(), R.string.account_deleted_successfully, Toast.LENGTH_SHORT).show();

                        syncManager.logout();

                        Intent intent = new Intent(getActivity(), LoginActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                        if (getActivity() != null) {
                            getActivity().finish();
                        }
                    } else {
                        btnConfirm.setEnabled(true);
                        btnConfirm.setText(R.string.delete_button);

                        if (response.code() == 401) {
                            tilPassword.setError(getString(R.string.invalid_password));
                        } else {
                            Toast.makeText(getContext(), R.string.account_delete_error, Toast.LENGTH_SHORT).show();
                        }
                    }
                }

                @Override
                public void onFailure(Call<Void> call, Throwable t) {
                    btnConfirm.setEnabled(true);
                    btnConfirm.setText(R.string.delete_button);

                    if (!isNetworkAvailable()) {
                        Toast.makeText(getContext(), R.string.no_internet_connection, Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(getContext(), R.string.network_error + t.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                }
            });
        }

        private void showLogoutConfirmation() {
            View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_logout_confirmation, null);

            Button btnCancel = dialogView.findViewById(R.id.btn_cancel_delete);
            Button btnConfirm = dialogView.findViewById(R.id.btn_confirm_delete);
            TextView tvWarning = dialogView.findViewById(R.id.tv_warning);

            tvWarning.setText(R.string.logout_warning_message);

            AlertDialog dialog = new AlertDialog.Builder(getContext())
                    .setView(dialogView)
                    .create();

            btnCancel.setOnClickListener(v -> dialog.dismiss());

            btnConfirm.setOnClickListener(v -> {
                dialog.dismiss();
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