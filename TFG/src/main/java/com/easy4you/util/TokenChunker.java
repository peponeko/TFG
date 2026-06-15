package com.easy4you.util;

import java.util.ArrayList;
import java.util.List;

public final class TokenChunker {

  private TokenChunker() {}

  public static List<Chunk> chunk(String text, int chunkTokens, int overlapTokens) {
    if (text == null) {
      return List.of();
    }
    String normalized = text.replaceAll("\\s+", " ").trim();
    if (normalized.isBlank()) {
      return List.of();
    }

    if (chunkTokens <= 0) {
      throw new IllegalArgumentException("chunkTokens debe ser > 0");
    }
    if (overlapTokens < 0) {
      throw new IllegalArgumentException("overlapTokens debe ser >= 0");
    }
    if (overlapTokens >= chunkTokens) {
      throw new IllegalArgumentException("overlapTokens debe ser < chunkTokens");
    }

    String[] tokens = normalized.split(" ");
    List<Chunk> out = new ArrayList<>();

    int start = 0;
    while (start < tokens.length) {
      int end = Math.min(tokens.length, start + chunkTokens);
      StringBuilder sb = new StringBuilder();
      for (int i = start; i < end; i++) {
        if (i > start) {
          sb.append(' ');
        }
        sb.append(tokens[i]);
      }
      int tokenCount = end - start;
      out.add(new Chunk(sb.toString(), tokenCount));

      if (end >= tokens.length) {
        break;
      }
      start = Math.max(0, end - overlapTokens);
    }

    return out;
  }

  public record Chunk(String text, int tokenCount) {}
}

