package com.easy4you.util;

public final class JsonExtractor {

  private JsonExtractor() {}

  public static String extractFirstJsonArray(String raw) {
    if (raw == null) {
      return null;
    }

    String s = raw.trim();
    if (s.startsWith("```")) {
      s = stripCodeFences(s);
    }

    int start = s.indexOf('[');
    int end = s.lastIndexOf(']');
    if (start < 0 || end < 0 || end <= start) {
      return null;
    }
    return s.substring(start, end + 1).trim();
  }

  private static String stripCodeFences(String s) {
    String t = s.trim();
    if (!t.startsWith("```")) {
      return t;
    }
    int firstNewline = t.indexOf('\n');
    if (firstNewline >= 0) {
      t = t.substring(firstNewline + 1);
    } else {
      t = t.substring(3);
    }

    int lastFence = t.lastIndexOf("```");
    if (lastFence >= 0) {
      t = t.substring(0, lastFence);
    }
    return t.trim();
  }
}

