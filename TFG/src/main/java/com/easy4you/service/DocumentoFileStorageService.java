package com.easy4you.service;

import java.io.IOException;
import java.nio.file.Path;

public interface DocumentoFileStorageService {

  Path createTempFile(String extension) throws IOException;

  String buildRelativePath(Long usuarioId, Long asignaturaId, Long temaId, Long documentoId, String storedFilename);

  Path resolveAbsolutePath(String relativePath);

  void moveToRelativePath(Path tempFile, String relativePath) throws IOException;

  void deleteIfExists(Path path);
}

