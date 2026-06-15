package com.easy4you.service.impl;

import com.easy4you.config.StorageProperties;
import com.easy4you.service.DocumentoFileStorageService;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DocumentoFileStorageServiceImpl implements DocumentoFileStorageService {

  private final StorageProperties storageProperties;

  @Override
  public Path createTempFile(String extension) throws IOException {
    Path base = resolveBasePath();
    Path tmpDir = base.resolve("tmp");
    Files.createDirectories(tmpDir);

    String suffix = extension == null || extension.isBlank() ? ".bin" : "." + extension;
    return Files.createTempFile(tmpDir, "upload-", suffix);
  }

  @Override
  public String buildRelativePath(
      Long usuarioId, Long asignaturaId, Long temaId, Long documentoId, String storedFilename) {
    if (usuarioId == null || asignaturaId == null || documentoId == null) {
      throw new IllegalArgumentException("Faltan datos para construir la ruta de almacenamiento");
    }
    if (storedFilename == null || storedFilename.isBlank()) {
      storedFilename = UUID.randomUUID() + ".bin";
    }

    Path relative;
    if (temaId != null) {
      relative =
          Paths.get(
              String.valueOf(usuarioId),
              String.valueOf(asignaturaId),
              String.valueOf(temaId),
              String.valueOf(documentoId),
              storedFilename);
    } else {
      relative =
          Paths.get(
              String.valueOf(usuarioId), String.valueOf(asignaturaId), String.valueOf(documentoId), storedFilename);
    }
    return relative.toString().replace('\\', '/');
  }

  @Override
  public Path resolveAbsolutePath(String relativePath) {
    Path base = resolveBasePath();
    if (relativePath == null || relativePath.isBlank()) {
      throw new IllegalArgumentException("relativePath es obligatorio");
    }
    Path normalized = Paths.get(relativePath.replace('\\', '/')).normalize();
    Path resolved = base.resolve(normalized).normalize();
    if (!resolved.startsWith(base)) {
      throw new IllegalArgumentException("Ruta no válida");
    }
    return resolved;
  }

  @Override
  public void moveToRelativePath(Path tempFile, String relativePath) throws IOException {
    if (tempFile == null) {
      throw new IllegalArgumentException("tempFile es obligatorio");
    }
    Path destination = resolveAbsolutePath(relativePath);
    Files.createDirectories(destination.getParent());
    Files.move(tempFile, destination, StandardCopyOption.REPLACE_EXISTING);
  }

  @Override
  public void deleteIfExists(Path path) {
    if (path == null) {
      return;
    }
    try {
      Files.deleteIfExists(path);
    } catch (Exception ignored) {
      // best-effort
    }
  }

  private Path resolveBasePath() {
    String basePath = storageProperties.getBasePath();
    if (basePath == null || basePath.isBlank()) {
      basePath = "./uploads";
    }
    return Paths.get(basePath).toAbsolutePath().normalize();
  }
}

