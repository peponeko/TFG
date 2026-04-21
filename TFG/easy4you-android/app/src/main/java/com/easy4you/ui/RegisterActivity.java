package com.easy4you.ui;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
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
import com.easy4you.model.RegisterRequest;
import com.easy4you.utils.SessionManager;
import com.google.android.material.textfield.TextInputLayout;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class RegisterActivity extends AppCompatActivity {

  private EditText etNombre;
  private EditText etApellidos;
  private EditText etEmail;
  private EditText etPassword;
  private Button btnRegister;
  private ProgressBar progress;
  private TextView tvGoLogin;

  private TextInputLayout tilNombre;
  private TextInputLayout tilEmail;
  private TextInputLayout tilPassword;

  private SessionManager sessionManager;
  private ApiService apiService;

  @Override
  protected void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_register);

    sessionManager = new SessionManager(this);
    apiService = ApiClient.getService(this);

    tilNombre = findViewById(R.id.tilNombre);
    tilEmail = findViewById(R.id.tilEmail);
    tilPassword = findViewById(R.id.tilPassword);
    etNombre = findViewById(R.id.etNombre);
    etApellidos = findViewById(R.id.etApellidos);
    etEmail = findViewById(R.id.etEmail);
    etPassword = findViewById(R.id.etPassword);
    btnRegister = findViewById(R.id.btnRegister);
    progress = findViewById(R.id.progressRegister);
    tvGoLogin = findViewById(R.id.tvGoLogin);

    btnRegister.setOnClickListener(v -> doRegister());
    tvGoLogin.setOnClickListener(v -> finish());
  }

  private void doRegister() {
    String nombre = etNombre.getText().toString().trim();
    String apellidos = etApellidos.getText().toString().trim();
    String email = etEmail.getText().toString().trim();
    String password = etPassword.getText().toString();

    tilNombre.setError(null);
    tilEmail.setError(null);
    tilPassword.setError(null);

    boolean ok = true;
    if (TextUtils.isEmpty(nombre)) {
      tilNombre.setError("El nombre es obligatorio");
      ok = false;
    }
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
        .register(new RegisterRequest(nombre, apellidos, email, password))
        .enqueue(
            new Callback<AuthResponse>() {
              @Override
              public void onResponse(Call<AuthResponse> call, Response<AuthResponse> response) {
                setLoading(false);
                if (!response.isSuccessful() || response.body() == null || response.body().getToken() == null) {
                  Toast.makeText(RegisterActivity.this, "No se pudo registrar", Toast.LENGTH_SHORT).show();
                  return;
                }

                AuthResponse auth = response.body();
                sessionManager.saveSession(auth.getToken(), auth.getUsuarioId(), auth.getEmail());

                Intent intent = new Intent(RegisterActivity.this, MainActivity.class);
                startActivity(intent);
                finishAffinity();
              }

              @Override
              public void onFailure(Call<AuthResponse> call, Throwable t) {
                setLoading(false);
                Toast.makeText(RegisterActivity.this, "Error de red: " + t.getMessage(), Toast.LENGTH_SHORT).show();
              }
            });
  }

  private void setLoading(boolean loading) {
    progress.setVisibility(loading ? View.VISIBLE : View.GONE);
    btnRegister.setEnabled(!loading);
  }
}
