package com.easy4you.ui;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.easy4you.R;
import com.easy4you.adapter.AsignaturaAdapter;
import com.easy4you.api.ApiClient;
import com.easy4you.api.ApiService;
import com.easy4you.model.Asignatura;
import com.easy4you.model.AsignaturaRequest;
import com.easy4you.utils.SessionManager;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import java.util.List;
import java.util.regex.Pattern;
import androidx.appcompat.app.AlertDialog;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AsignaturasActivity extends AppCompatActivity {

  private RecyclerView rv;
  private ProgressBar progress;
  private TextView tvEmpty;
  private FloatingActionButton fabAdd;

  private SessionManager sessionManager;
  private ApiService apiService;
  private AsignaturaAdapter adapter;

  private static final Pattern HEX_COLOR = Pattern.compile("^#[0-9a-fA-F]{6}$");

  @Override
  protected void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_asignaturas);

    sessionManager = new SessionManager(this);
    apiService = ApiClient.getService(this);

    MaterialToolbar toolbar = findViewById(R.id.toolbarAsignaturas);
    setSupportActionBar(toolbar);
    if (getSupportActionBar() != null) {
      getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }
    toolbar.setNavigationOnClickListener(v -> finish());

    rv = findViewById(R.id.rvAsignaturas);
    progress = findViewById(R.id.progressAsignaturas);
    tvEmpty = findViewById(R.id.tvEmptyAsignaturas);
    fabAdd = findViewById(R.id.fabAddAsignatura);

    adapter =
        new AsignaturaAdapter(
            asignatura -> {
              Intent intent = new Intent(this, DetalleAsignaturaActivity.class);
              intent.putExtra(DetalleAsignaturaActivity.EXTRA_ASIGNATURA_ID, asignatura.getId());
              startActivity(intent);
            });

    rv.setLayoutManager(new LinearLayoutManager(this));
    rv.setAdapter(adapter);

    fabAdd.setOnClickListener(v -> mostrarDialogNuevaAsignatura());
  }

  @Override
  protected void onResume() {
    super.onResume();
    if (!sessionManager.isLoggedIn()) {
      goLogin();
      return;
    }
    cargarAsignaturas();
  }

  private void cargarAsignaturas() {
    setLoading(true);
    long usuarioId = sessionManager.getUsuarioId();
    Long filtroUsuario = usuarioId > 0 ? usuarioId : null;

    apiService
        .getAsignaturas(filtroUsuario)
        .enqueue(
            new Callback<List<Asignatura>>() {
              @Override
              public void onResponse(Call<List<Asignatura>> call, Response<List<Asignatura>> response) {
                setLoading(false);
                if (response.code() == 401 || response.code() == 403) {
                  sessionManager.clear();
                  goLogin();
                  return;
                }
                if (!response.isSuccessful()) {
                  Toast.makeText(AsignaturasActivity.this, "No se pudieron cargar las asignaturas", Toast.LENGTH_SHORT)
                      .show();
                  return;
                }

                List<Asignatura> data = response.body();
                adapter.submitList(data);
                tvEmpty.setVisibility(data == null || data.isEmpty() ? View.VISIBLE : View.GONE);
              }

              @Override
              public void onFailure(Call<List<Asignatura>> call, Throwable t) {
                setLoading(false);
                Toast.makeText(AsignaturasActivity.this, "Error de red: " + t.getMessage(), Toast.LENGTH_SHORT).show();
              }
            });
  }

  private void setLoading(boolean loading) {
    progress.setVisibility(loading ? View.VISIBLE : View.GONE);
    rv.setVisibility(loading ? View.GONE : View.VISIBLE);
    if (loading) {
      tvEmpty.setVisibility(View.GONE);
    }
  }

  private void goLogin() {
    startActivity(new Intent(this, LoginActivity.class));
    finish();
  }

  private void mostrarDialogNuevaAsignatura() {
    View view = getLayoutInflater().inflate(R.layout.dialog_add_asignatura, null);
    TextInputLayout tilNombre = view.findViewById(R.id.tilDialogNombre);
    TextInputLayout tilDescripcion = view.findViewById(R.id.tilDialogDescripcion);
    TextInputLayout tilColor = view.findViewById(R.id.tilDialogColor);
    TextInputEditText etNombre = view.findViewById(R.id.etDialogNombre);
    TextInputEditText etDescripcion = view.findViewById(R.id.etDialogDescripcion);
    TextInputEditText etColor = view.findViewById(R.id.etDialogColor);

    AlertDialog dialog =
        new MaterialAlertDialogBuilder(this)
            .setTitle("Nueva asignatura")
            .setView(view)
            .setNegativeButton("Cancelar", null)
            .setPositiveButton("Crear", null)
            .create();

    dialog.setOnShowListener(
        d -> {
          dialog
              .getButton(AlertDialog.BUTTON_POSITIVE)
              .setOnClickListener(
                  v -> {
                    tilNombre.setError(null);
                    tilDescripcion.setError(null);
                    tilColor.setError(null);

                    String nombre = etNombre.getText() != null ? etNombre.getText().toString().trim() : "";
                    String descripcion =
                        etDescripcion.getText() != null ? etDescripcion.getText().toString().trim() : "";
                    String color = etColor.getText() != null ? etColor.getText().toString().trim() : "";

                    boolean ok = true;
                    if (TextUtils.isEmpty(nombre)) {
                      tilNombre.setError("El nombre es obligatorio");
                      ok = false;
                    }
                    if (!TextUtils.isEmpty(color) && !HEX_COLOR.matcher(color).matches()) {
                      tilColor.setError("Formato inválido. Ejemplo: #2F80ED");
                      ok = false;
                    }
                    if (!ok) {
                      return;
                    }

                    crearAsignatura(nombre, descripcion, TextUtils.isEmpty(color) ? null : color, dialog);
                  });
        });

    dialog.show();
  }

  private void crearAsignatura(String nombre, String descripcion, String colorHex, AlertDialog dialog) {
    long usuarioId = sessionManager.getUsuarioId();
    if (usuarioId <= 0) {
      Toast.makeText(this, "Sesión inválida", Toast.LENGTH_SHORT).show();
      sessionManager.clear();
      goLogin();
      return;
    }

    setLoading(true);
    apiService
        .createAsignatura(new AsignaturaRequest(usuarioId, nombre, descripcion, colorHex))
        .enqueue(
            new Callback<Asignatura>() {
              @Override
              public void onResponse(Call<Asignatura> call, Response<Asignatura> response) {
                setLoading(false);
                if (response.code() == 401 || response.code() == 403) {
                  sessionManager.clear();
                  goLogin();
                  return;
                }
                if (!response.isSuccessful()) {
                  Toast.makeText(AsignaturasActivity.this, "No se pudo crear la asignatura", Toast.LENGTH_SHORT).show();
                  return;
                }
                dialog.dismiss();
                cargarAsignaturas();
              }

              @Override
              public void onFailure(Call<Asignatura> call, Throwable t) {
                setLoading(false);
                Toast.makeText(AsignaturasActivity.this, "Error de red: " + t.getMessage(), Toast.LENGTH_SHORT).show();
              }
            });
  }
}
