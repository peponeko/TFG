package com.easy4you.api;

import com.easy4you.utils.SessionManager;
import java.io.IOException;
import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

public class AuthInterceptor implements Interceptor {

  private final SessionManager sessionManager;

  public AuthInterceptor(SessionManager sessionManager) {
    this.sessionManager = sessionManager;
  }

  @Override
  public Response intercept(Chain chain) throws IOException {
    Request original = chain.request();
    String path = original.url().encodedPath();

    // No añadir token en endpoints públicos
    if (path != null && path.startsWith("/api/auth/")) {
      return chain.proceed(original);
    }

    String token = sessionManager.getToken();
    if (token == null || token.trim().isEmpty()) {
      return chain.proceed(original);
    }

    if (original.header("Authorization") != null) {
      return chain.proceed(original);
    }

    Request requestWithAuth =
        original.newBuilder()
            .header("Authorization", "Bearer " + token)
            .header("Accept", "application/json")
            .build();

    return chain.proceed(requestWithAuth);
  }
}

