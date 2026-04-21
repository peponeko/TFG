package com.easy4you.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import com.easy4you.R;
import com.easy4you.api.ApiClient;
import com.easy4you.api.ApiService;
import com.easy4you.model.Asignatura;
import com.easy4you.utils.SessionManager;
import com.google.android.material.appbar.MaterialToolbar;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class DetalleAsignaturaActivity extends AppCompatActivity {

  public static final String EXTRA_ASIGNATURA_ID = "extra_asignatura_id";

  private TextView tvNombre;
  private TextView tvDescripcion;
  private TextView tvColor;
  private ProgressBar progress;

  private ApiService apiService;
  private SessionManager sessionManager;

  @Override
  protected void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_asignatura_detalle);

    apiService = ApiClient.getService(this);
    sessionManager = new SessionManager(this);

    MaterialToolbar toolbar = findViewById(R.id.toolbarDetalle);
    setSupportActionBar(toolbar);
    if (getSupportActionBar() != null) {
      getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }
    toolbar.setNavigationOnClickListener(v -> finish());

    tvNombre = findViewById(R.id.tvDetalleNombre);
    tvDescripcion = findViewById(R.id.tvDetalleDescripcion);
    tvColor = findViewById(R.id.tvDetalleColor);
    progress = findViewById(R.id.progressDetalleAsignatura);
  }

  @Override
  protected void onResume() {
    super.onResume();
    if (!sessionManager.isLoggedIn()) {
      startActivity(new Intent(this, LoginActivity.class));
      finish();
      return;
    }

    long id = getIntent().getLongExtra(EXTRA_ASIGNATURA_ID, -1L);
    if (id <= 0) {
      Toast.makeText(this, "Asignatura no válida", Toast.LENGTH_SHORT).show();
      finish();
      return;
    }

    cargarDetalle(id);
  }

  private void cargarDetalle(long id) {
    setLoading(true);
    apiService
        .getAsignatura(id)
        .enqueue(
            new Callback<Asignatura>() {
              @Override
              public void onResponse(Call<Asignatura> call, Response<Asignatura> response) {
                setLoading(false);
                if (response.code() == 401 || response.code() == 403) {
                  sessionManager.clear();
                  startActivity(new Intent(DetalleAsignaturaActivity.this, LoginActivity.class));
                  finish();
                  return;
                }
                if (!response.isSuccessful() || response.body() == null) {
                  Toast.makeText(DetalleAsignaturaActivity.this, "No se pudo cargar el detalle", Toast.LENGTH_SHORT)
                      .show();
                  return;
                }

                Asignatura a = response.body();
                tvNombre.setText(a.getNombre());
                tvDescripcion.setText(a.getDescripcion() != null ? a.getDescripcion() : "");
                tvColor.setText(a.getColorHex() != null ? a.getColorHex() : "");
                if (getSupportActionBar() != null) {
                  getSupportActionBar().setTitle(a.getNombre());
                }
              }

              @Override
              public void onFailure(Call<Asignatura> call, Throwable t) {
                setLoading(false);
                Toast.makeText(DetalleAsignaturaActivity.this, "Error de red: " + t.getMessage(), Toast.LENGTH_SHORT)
                    .show();
              }
            });
  }

  private void setLoading(boolean loading) {
    progress.setVisibility(loading ? View.VISIBLE : View.GONE);
  }
}
