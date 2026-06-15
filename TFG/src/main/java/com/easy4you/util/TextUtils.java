package com.easy4you.util;

/** Utilidades de texto reutilizables (recortar, normalizar, etc.). */
public final class TextUtils {

  private TextUtils() {}

  /** Devuelve la cadena recortada y, si excede {@code maxChars}, añade puntos suspensivos. */
  public static String truncate(String text, int maxChars) {
    if (text == null) {
      return "";
    }
    String t = text.trim();
    if (t.length() <= maxChars) {
      return t;
    }
    return t.substring(0, Math.max(0, maxChars - 1)).trim() + "…";
  }

  /** Devuelve la cadena recortada con trim, o vacía si es null. */
  public static String safeTrim(String s) {
    return s == null ? "" : s.trim();
  }
}
