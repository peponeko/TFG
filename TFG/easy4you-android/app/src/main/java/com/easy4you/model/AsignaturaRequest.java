package com.easy4you.model;

public class AsignaturaRequest {
  private Long usuarioId;
  private String nombre;
  private String descripcion;
  private String colorHex;

  public AsignaturaRequest(Long usuarioId, String nombre, String descripcion, String colorHex) {
    this.usuarioId = usuarioId;
    this.nombre = nombre;
    this.descripcion = descripcion;
    this.colorHex = colorHex;
  }

  public Long getUsuarioId() {
    return usuarioId;
  }

  public void setUsuarioId(Long usuarioId) {
    this.usuarioId = usuarioId;
  }

  public String getNombre() {
    return nombre;
  }

  public void setNombre(String nombre) {
    this.nombre = nombre;
  }

  public String getDescripcion() {
    return descripcion;
  }

  public void setDescripcion(String descripcion) {
    this.descripcion = descripcion;
  }

  public String getColorHex() {
    return colorHex;
  }

  public void setColorHex(String colorHex) {
    this.colorHex = colorHex;
  }
}

