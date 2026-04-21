package com.easy4you.ui;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import com.easy4you.R;
import com.easy4you.api.ApiClient;
import com.easy4you.api.ApiService;
import com.easy4you.model.AuthResponse;
import com.easy4you.model.LoginRequest;
import com.easy4you.utils.SessionManager;
import com.google.android.material.textfield.TextInputLayout;
import android.util.Patterns;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LoginActivity extends AppCompatActivity {

  private EditText etEmail;
  private EditText etPassword;
  private Button btnLogin;
  private ProgressBar progress;
  private TextView tvGoRegister;
  private TextInputLayout tilEmail;
  private TextInputLayout tilPassword;

  private SessionManager sessionManager;
  private ApiService apiService;

  @Override
  protected void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_login);

    sessionManager = new SessionManager(this);
    apiService = ApiClient.getService(this);

    if (sessionManager.isLoggedIn()) {
      goMain();
      return;
    }

    tilEmail = findViewById(R.id.tilEmail);
    tilPassword = findViewById(R.id.tilPassword);
    etEmail = findViewById(R.id.etEmail);
    etPassword = findViewById(R.id.etPassword);
    btnLogin = findViewById(R.id.btnLogin);
    progress = findViewById(R.id.progressLogin);
    tvGoRegister = findViewById(R.id.tvGoRegister);

    btnLogin.setOnClickListener(v -> doLogin());
    tvGoRegister.setOnClickListener(v -> startActivity(new Intent(this, RegisterActivity.class)));
  }

  private void doLogin() {
    String email = etEmail.getText().toString().trim();
    String password = etPassword.getText().toString();

    tilEmail.setError(null);
    tilPassword.setError(null);

    boolean ok = true;
    if (TextUtils.isEmpty(email) || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
      tilEmail.setError("Introduce un email válido");
      ok = false;
    }
    if (TextUtils.isEmpty(password) || password.length() < 6) {
      tilPassword.setError("La contraseña debe tener al menos 6 caracteres");
      ok = false;
    }
    if (!ok) {
      return;
    }

    setLoading(true);
    apiService
        .login(new LoginRequest(email, password))
        .enqueue(
            new Callback<AuthResponse>() {
              @Override
              public void onResponse(Call<AuthResponse> call, Response<AuthResponse> response) {
                setLoading(false);
                if (!response.isSuccessful() || response.body() == null || response.body().getToken() == null) {
                  Toast.makeText(LoginActivity.this, "Credenciales inválidas", Toast.LENGTH_SHORT).show();
                  return;
                }

                AuthResponse auth = response.body();
                sessionManager.saveSession(auth.getToken(), auth.getUsuarioId(), auth.getEmail());
                goMain();
              }

              @Override
              public void onFailure(Call<AuthResponse> call, Throwable t) {
                setLoading(false);
                Toast.makeText(LoginActivity.this, "Error de red: " + t.getMessage(), Toast.LENGTH_SHORT).show();
              }
            });
  }

  private void setLoading(boolean loading) {
    progress.setVisibility(loading ? View.VISIBLE : View.GONE);
    btnLogin.setEnabled(!loading);
  }

  private void goMain() {
    Intent intent = new Intent(this, MainActivity.class);
    startActivity(intent);
    finish();
  }
}
