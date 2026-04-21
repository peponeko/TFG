package com.easy4you.service;

import org.springframework.web.multipart.MultipartFile;

public interface DocumentoIngestionService {
  DocumentoUploadResult upload(MultipartFile file, Long asignaturaId, Long temaId);
}

