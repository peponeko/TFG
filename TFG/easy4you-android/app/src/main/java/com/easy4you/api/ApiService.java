package com.easy4you.api;

import com.easy4you.model.Asignatura;
import com.easy4you.model.AsignaturaRequest;
import com.easy4you.model.AuthResponse;
import com.easy4you.model.LoginRequest;
import com.easy4you.model.RegisterRequest;
import java.util.List;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface ApiService {

  @POST("api/auth/login")
  Call<AuthResponse> login(@Body LoginRequest request);

  @POST("api/auth/register")
  Call<AuthResponse> register(@Body RegisterRequest request);

  @GET("api/asignaturas")
  Call<List<Asignatura>> getAsignaturas(@Query("usuarioId") Long usuarioId);

  @GET("api/asignaturas/{id}")
  Call<Asignatura> getAsignatura(@Path("id") long id);

  @POST("api/asignaturas")
  Call<Asignatura> createAsignatura(@Body AsignaturaRequest request);
}
