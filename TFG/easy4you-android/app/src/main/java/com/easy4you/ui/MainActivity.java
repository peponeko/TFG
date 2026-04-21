package com.easy4you.ui;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import com.easy4you.R;
import com.easy4you.utils.SessionManager;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.card.MaterialCardView;

public class MainActivity extends AppCompatActivity {

  private SessionManager sessionManager;

  @Override
  protected void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    sessionManager = new SessionManager(this);
    if (!sessionManager.isLoggedIn()) {
      goLogin();
      return;
    }

    setContentView(R.layout.activity_main);

    MaterialToolbar toolbar = findViewById(R.id.toolbarMain);
    setSupportActionBar(toolbar);

    TextView tvSubtitulo = findViewById(R.id.tvMainSubtitle);
    tvSubtitulo.setText(sessionManager.getEmail() != null ? sessionManager.getEmail() : "");

    MaterialCardView cardAsignaturas = findViewById(R.id.cardAsignaturas);
    MaterialCardView cardLogout = findViewById(R.id.cardLogout);

    cardAsignaturas.setOnClickListener(v -> startActivity(new Intent(this, AsignaturasActivity.class)));
    cardLogout.setOnClickListener(
        v -> {
          sessionManager.clear();
          goLogin();
        });
  }

  @Override
  protected void onResume() {
    super.onResume();
    if (!sessionManager.isLoggedIn()) {
      goLogin();
    }
  }

  private void goLogin() {
    Intent intent = new Intent(this, LoginActivity.class);
    startActivity(intent);
    finish();
  }
}

