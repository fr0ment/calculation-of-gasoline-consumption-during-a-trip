package com.example.cogcdat_2;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.cogcdat_2.api.ApiClient;
import com.example.cogcdat_2.api.ApiService;
import com.example.cogcdat_2.api.models.User;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class RegisterActivity extends BaseActivity {

    private TextInputLayout tilUsername, tilEmail, tilFullName, tilPassword, tilConfirmPassword;
    private TextInputEditText etUsername, etEmail, etFullName, etPassword, etConfirmPassword;
    private Button btnRegister;
    private TextView tvLoginLink, tvError;
    private ProgressBar progressBar;
    private ApiService apiService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        apiService = ApiClient.getInstance(this).getApiService();

        initViews();
        setupListeners();
    }

    private void initViews() {
        tilUsername = findViewById(R.id.til_username);
        tilEmail = findViewById(R.id.til_email);
        tilFullName = findViewById(R.id.til_full_name);
        tilPassword = findViewById(R.id.til_password);
        tilConfirmPassword = findViewById(R.id.til_confirm_password);

        etUsername = findViewById(R.id.et_username);
        etEmail = findViewById(R.id.et_email);
        etFullName = findViewById(R.id.et_full_name);
        etPassword = findViewById(R.id.et_password);
        etConfirmPassword = findViewById(R.id.et_confirm_password);

        btnRegister = findViewById(R.id.btn_register);
        tvLoginLink = findViewById(R.id.tv_login_link);
        tvError = findViewById(R.id.tv_error);
        progressBar = findViewById(R.id.progress_bar);
    }

    private void setupListeners() {
        btnRegister.setOnClickListener(v -> attemptRegister());
        tvLoginLink.setOnClickListener(v -> finish());
    }

    private void attemptRegister() {
        // Сброс ошибок
        tilUsername.setError(null);
        tilEmail.setError(null);
        tilPassword.setError(null);
        tilConfirmPassword.setError(null);
        tvError.setVisibility(View.GONE);

        String username = etUsername.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String fullName = etFullName.getText().toString().trim();
        String password = etPassword.getText().toString().trim();
        String confirmPassword = etConfirmPassword.getText().toString().trim();

        boolean cancel = false;
        View focusView = null;

        // Проверка подтверждения пароля
        if (!TextUtils.isEmpty(password) && !password.equals(confirmPassword)) {
            tilConfirmPassword.setError(getString(R.string.passwords_do_not_match));
            focusView = tilConfirmPassword;
            cancel = true;
        }

        // Проверка пароля
        if (TextUtils.isEmpty(password)) {
            tilPassword.setError(getString(R.string.enter_password));
            focusView = tilPassword;
            cancel = true;
        } else if (password.length() < 6) {
            tilPassword.setError(getString(R.string.password_min_length));
            focusView = tilPassword;
            cancel = true;
        }

        // Проверка email
        if (TextUtils.isEmpty(email)) {
            tilEmail.setError(getString(R.string.enter_email));
            focusView = tilEmail;
            cancel = true;
        } else if (!isEmailValid(email)) {
            tilEmail.setError(getString(R.string.invalid_email));
            focusView = tilEmail;
            cancel = true;
        }

        // Проверка имени пользователя
        if (TextUtils.isEmpty(username)) {
            tilUsername.setError(getString(R.string.enter_username));
            focusView = tilUsername;
            cancel = true;
        } else if (username.length() < 3) {
            tilUsername.setError(getString(R.string.username_min_length));
            focusView = tilUsername;
            cancel = true;
        }

        if (cancel) {
            focusView.requestFocus();
        } else {
            showProgress(true);
            performRegister(username, email, password, fullName);
        }
    }

    private boolean isEmailValid(String email) {
        return android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches();
    }

    private void performRegister(String username, String email, String password, String fullName) {
        User user = new User(username, email, password, fullName);

        apiService.register(user).enqueue(new Callback<User>() {
            @Override
            public void onResponse(Call<User> call, Response<User> response) {
                showProgress(false);

                if (response.isSuccessful()) {
                    Toast.makeText(RegisterActivity.this,
                            getString(R.string.registration_success),
                            Toast.LENGTH_LONG).show();
                    finish();
                } else {
                    String error = getString(R.string.registration_error);
                    try {
                        if (response.errorBody() != null) {
                            error = response.errorBody().string();
                            // Парсим ошибку с сервера
                            if (error.contains("already registered")) {
                                error = getString(R.string.user_already_exists);
                            }
                        }
                    } catch (Exception e) {
                        error = getString(R.string.server_error) + response.code();
                    }
                    showError(error);
                }
            }

            @Override
            public void onFailure(Call<User> call, Throwable t) {
                showProgress(false);
                showError(getString(R.string.network_error) + t.getMessage());
            }
        });
    }

    private void showProgress(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        btnRegister.setEnabled(!show);
        tvLoginLink.setEnabled(!show);

        tilUsername.setEnabled(!show);
        tilEmail.setEnabled(!show);
        tilFullName.setEnabled(!show);
        tilPassword.setEnabled(!show);
        tilConfirmPassword.setEnabled(!show);
    }

    private void showError(String message) {
        tvError.setText(message);
        tvError.setVisibility(View.VISIBLE);
    }
}