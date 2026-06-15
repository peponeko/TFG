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

  /**
   * Extrae un array JSON de respuestas del modelo (a veces envuelven el JSON en bloques markdown).
   */
  public static String extractJsonArray(String raw) {
    if (raw == null || raw.isBlank()) {
      return "";
    }
    String t = raw.trim();
    if (t.startsWith("```")) {
      int firstLine = t.indexOf('\n');
      int fenceEnd = t.lastIndexOf("```");
      if (firstLine > 0 && fenceEnd > firstLine) {
        t = t.substring(firstLine + 1, fenceEnd).trim();
      }
    }
    int start = t.indexOf('[');
    int end = t.lastIndexOf(']');
    if (start >= 0 && end > start) {
      return t.substring(start, end + 1).trim();
    }
    return t;
  }
}
