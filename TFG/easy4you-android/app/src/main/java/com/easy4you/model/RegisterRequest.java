package com.easy4you.model;

public class RegisterRequest {
  private String nombre;
  private String apellidos;
  private String email;
  private String password;

  public RegisterRequest(String nombre, String apellidos, String email, String password) {
    this.nombre = nombre;
    this.apellidos = apellidos;
    this.email = email;
    this.password = password;
  }

  public String getNombre() {
    return nombre;
  }

  public void setNombre(String nombre) {
    this.nombre = nombre;
  }

  public String getApellidos() {
    return apellidos;
  }

  public void setApellidos(String apellidos) {
    this.apellidos = apellidos;
  }

  public String getEmail() {
    return email;
  }

  public void setEmail(String email) {
    this.email = email;
  }

  public String getPassword() {
    return password;
  }

  public void setPassword(String password) {
    this.password = password;
  }
}

