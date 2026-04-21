package com.easy4you.utils;

import android.content.Context;
import android.content.SharedPreferences;

public class SessionManager {

  private static final String PREFS_NAME = "easy4you_session";
  private static final String KEY_TOKEN = "token";
  private static final String KEY_USER_ID = "usuarioId";
  private static final String KEY_EMAIL = "email";

  private final SharedPreferences prefs;

  public SessionManager(Context context) {
    this.prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
  }

  public void saveSession(String token, long usuarioId, String email) {
    prefs
        .edit()
        .putString(KEY_TOKEN, token)
        .putLong(KEY_USER_ID, usuarioId)
        .putString(KEY_EMAIL, email)
        .apply();
  }

  public String getToken() {
    return prefs.getString(KEY_TOKEN, null);
  }

  public long getUsuarioId() {
    return prefs.getLong(KEY_USER_ID, -1L);
  }

  public String getEmail() {
    return prefs.getString(KEY_EMAIL, null);
  }

  public boolean isLoggedIn() {
    String token = getToken();
    return token != null && !token.trim().isEmpty();
  }

  public void clear() {
    prefs.edit().clear().apply();
  }
}

