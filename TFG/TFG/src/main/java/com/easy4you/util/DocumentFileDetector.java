package com.easy4you.util;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public final class DocumentFileDetector {

  private DocumentFileDetector() {}

  public static DetectedDocument detect(Path filePath, String originalFilename) throws IOException {
    String extension = extensionFromFilename(originalFilename);
    if (extension == null) {
      throw new IllegalArgumentException("El fichero debe tener extensión");
    }

    return switch (extension) {
      case "pdf" -> detectPdf(filePath);
      case "docx" -> detectDocx(filePath);
      case "zip" -> detectZip(filePath);
      case "txt" -> detectText(filePath, SupportedDocumentType.TXT, "text/plain");
      case "md" -> detectText(filePath, SupportedDocumentType.MD, "text/markdown");
      default -> throw new IllegalArgumentException("Extensión no soportada: " + extension);
    };
  }

  public static String extensionFromFilename(String filename) {
    if (filename == null) {
      return null;
    }
    String name = filename.trim();
    int lastDot = name.lastIndexOf('.');
    if (lastDot < 0 || lastDot == name.length() - 1) {
      return null;
    }
    return name.substring(lastDot + 1).toLowerCase(Locale.ROOT);
  }

  private static DetectedDocument detectPdf(Path filePath) throws IOException {
    byte[] header = readPrefix(filePath, 5);
    String s = new String(header);
    if (!s.startsWith("%PDF-")) {
      throw new IllegalArgumentException("El fichero no parece un PDF válido");
    }
    return new DetectedDocument(SupportedDocumentType.PDF, "pdf", "application/pdf");
  }

  private static DetectedDocument detectDocx(Path filePath) throws IOException {
    ensureZipSignature(filePath, "DOCX");

    boolean hasWordDocument = false;
    try (InputStream in = Files.newInputStream(filePath); ZipInputStream zis = new ZipInputStream(in)) {
      ZipEntry entry;
      while ((entry = zis.getNextEntry()) != null) {
        if ("word/document.xml".equals(entry.getName())) {
          hasWordDocument = true;
          break;
        }
      }
    }

    if (!hasWordDocument) {
      throw new IllegalArgumentException("El fichero no parece un DOCX válido");
    }

    return new DetectedDocument(
        SupportedDocumentType.DOCX,
        "docx",
        "application/vnd.openxmlformats-officedocument.wordprocessingml.document");
  }

  private static DetectedDocument detectZip(Path filePath) throws IOException {
    ensureZipSignature(filePath, "ZIP");
    return new DetectedDocument(SupportedDocumentType.ZIP, "zip", "application/zip");
  }

  private static DetectedDocument detectText(Path filePath, SupportedDocumentType type, String mimeType)
      throws IOException {
    if (looksLikeBinary(filePath)) {
      throw new IllegalArgumentException("El fichero parece binario (no texto)");
    }
    return new DetectedDocument(type, type == SupportedDocumentType.MD ? "md" : "txt", mimeType);
  }

  private static void ensureZipSignature(Path filePath, String label) throws IOException {
    byte[] header = readPrefix(filePath, 4);
    if (header.length < 2 || header[0] != 'P' || header[1] != 'K') {
      throw new IllegalArgumentException("El fichero no parece un " + label + " válido");
    }
  }

  private static boolean looksLikeBinary(Path filePath) throws IOException {
    byte[] sample = readPrefix(filePath, 2048);
    if (sample.length == 0) {
      return false;
    }
    for (byte b : sample) {
      if (b == 0) {
        return true;
      }
    }
    return false;
  }

  private static byte[] readPrefix(Path filePath, int maxBytes) throws IOException {
    try (InputStream in = Files.newInputStream(filePath)) {
      return in.readNBytes(maxBytes);
    }
  }
}

