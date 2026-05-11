package com.easy4you.service.impl;

import com.easy4you.config.StorageProperties;
import com.easy4you.exception.BadRequestException;
import com.easy4you.exception.NotFoundException;
import com.easy4you.model.entity.Asignatura;
import com.easy4you.model.entity.Documento;
import com.easy4you.model.entity.Tema;
import com.easy4you.model.entity.Usuario;
import com.easy4you.model.enums.EstadoProcesadoDocumento;
import com.easy4you.repository.AsignaturaRepository;
import com.easy4you.repository.DocumentoRepository;
import com.easy4you.security.AuthenticatedUserService;
import com.easy4you.service.DocumentoFileStorageService;
import com.easy4you.service.DocumentoIngestionService;
import com.easy4you.service.DocumentoProcessingService;
import com.easy4you.service.DocumentoUploadResult;
import com.easy4you.service.TemaService;
import com.easy4you.util.DetectedDocument;
import com.easy4you.util.DocumentFileDetector;
import com.easy4you.util.SupportedDocumentType;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class DocumentoIngestionServiceImpl implements DocumentoIngestionService {

  private static final Logger log = LoggerFactory.getLogger(DocumentoIngestionServiceImpl.class);

  private static final int BUFFER_SIZE = 8192;

  private final StorageProperties storageProperties;
  private final DocumentoRepository documentoRepository;
  private final AsignaturaRepository asignaturaRepository;
  private final TemaService temaService;
  private final DocumentoFileStorageService documentoFileStorageService;
  private final DocumentoProcessingService documentoProcessingService;
  private final AuthenticatedUserService authenticatedUserService;

  @Override
  @Transactional
  public DocumentoUploadResult upload(MultipartFile file, Long asignaturaId, Long temaId) {
    if (file == null || file.isEmpty()) {
      throw new BadRequestException("El fichero es obligatorio");
    }
    if (asignaturaId == null) {
      throw new BadRequestException("asignaturaId es obligatorio");
    }

    Usuario usuario = authenticatedUserService.requireUsuarioActual();

    Asignatura asignatura =
        asignaturaRepository
            .findByIdAndUsuarioId(asignaturaId, usuario.getId())
            .orElseThrow(() -> new NotFoundException("Asignatura no encontrada: " + asignaturaId));

    Tema tema = resolveTemaOrThrow(temaId, asignaturaId, usuario.getId());

    String originalFilename = file.getOriginalFilename();
    String extension = DocumentFileDetector.extensionFromFilename(originalFilename);

    long maxBytes =
        "zip".equals(extension)
            ? storageProperties.getMaxZipSize().toBytes()
            : storageProperties.getMaxFileSize().toBytes();

    Path tempFile = null;
    try {
      tempFile = documentoFileStorageService.createTempFile(extension != null ? extension : "bin");
      CopyResult copyResult = copyWithSha256(file.getInputStream(), tempFile, maxBytes);

      DetectedDocument detected;
      try {
        detected = DocumentFileDetector.detect(tempFile, originalFilename);
      } catch (IllegalArgumentException ex) {
        throw new BadRequestException(ex.getMessage());
      }

      if (detected.type() == SupportedDocumentType.ZIP) {
        DocumentoUploadResult result = processZip(tempFile, usuario, asignatura, tema);
        log.info(
            "ZIP subido: usuarioId={}, asignaturaId={}, temaId={}, documentos={}",
            usuario.getId(),
            asignaturaId,
            temaId,
            result.documentos().size());
        return result;
      }

      DocumentoUploadResult result =
          upsertDocumentoFromTempFile(
              tempFile,
              copyResult.sha256(),
              copyResult.sizeBytes(),
              detected,
              originalFilename,
              usuario,
              asignatura,
              tema);

      log.info(
          "Documento subido: usuarioId={}, asignaturaId={}, temaId={}, documentos={}",
          usuario.getId(),
          asignaturaId,
          temaId,
          result.documentos().size());

      return result;
    } catch (IOException ex) {
      throw new BadRequestException("No se ha podido leer el fichero");
    } finally {
      documentoFileStorageService.deleteIfExists(tempFile);
    }
  }

  private DocumentoUploadResult processZip(Path zipTempFile, Usuario usuario, Asignatura asignatura, Tema tema) {
    long maxEntryBytes = storageProperties.getMaxFileSize().toBytes();

    List<Documento> documentos = new ArrayList<>();
    List<String> warnings = new ArrayList<>();

    try (InputStream in = java.nio.file.Files.newInputStream(zipTempFile);
        ZipInputStream zis = new ZipInputStream(in)) {
      ZipEntry entry;
      while ((entry = zis.getNextEntry()) != null) {
        if (entry.isDirectory()) {
          continue;
        }

        String entryFilename = safeZipEntryFilename(entry.getName());
        String entryExtension = DocumentFileDetector.extensionFromFilename(entryFilename);
        if (entryExtension == null) {
          warnings.add("Entrada sin extensión ignorada: " + entryFilename);
          continue;
        }

        if (!isSupportedEntryExtension(entryExtension)) {
          warnings.add("Entrada no soportada ignorada: " + entryFilename);
          continue;
        }

        Path entryTemp = null;
        try {
          entryTemp = documentoFileStorageService.createTempFile(entryExtension);
          CopyResult copy = copyWithSha256(zis, entryTemp, maxEntryBytes);

          DetectedDocument detected;
          try {
            detected = DocumentFileDetector.detect(entryTemp, entryFilename);
          } catch (IllegalArgumentException ex) {
            warnings.add("Entrada inválida ignorada: " + entryFilename + " (" + ex.getMessage() + ")");
            continue;
          }

          if (detected.type() == SupportedDocumentType.ZIP) {
            warnings.add("Entrada ZIP anidada ignorada: " + entryFilename);
            continue;
          }

          DocumentoUploadResult upsert =
              upsertDocumentoFromTempFile(
                  entryTemp,
                  copy.sha256(),
                  copy.sizeBytes(),
                  detected,
                  entryFilename,
                  usuario,
                  asignatura,
                  tema);
          documentos.addAll(upsert.documentos());
          warnings.addAll(upsert.warnings());
        } catch (BadRequestException ex) {
          warnings.add("Entrada ignorada: " + entryFilename + " (" + ex.getMessage() + ")");
        } finally {
          documentoFileStorageService.deleteIfExists(entryTemp);
        }
      }
    } catch (IOException ex) {
      throw new BadRequestException("No se ha podido procesar el ZIP");
    }

    if (documentos.isEmpty()) {
      throw new BadRequestException("El ZIP no contiene ficheros soportados");
    }

    return new DocumentoUploadResult(documentos, warnings);
  }

  private DocumentoUploadResult upsertDocumentoFromTempFile(
      Path tempFile,
      String sha256,
      long sizeBytes,
      DetectedDocument detected,
      String originalFilename,
      Usuario usuario,
      Asignatura asignatura,
      Tema tema) {

    Optional<Documento> existing =
        documentoRepository.findTopByUsuarioIdAndChecksumSha256(usuario.getId(), sha256);
    if (existing.isPresent()) {
      return new DocumentoUploadResult(
          List.of(existing.get()),
          List.of("Documento duplicado (SHA-256). Reutilizando id=" + existing.get().getId()));
    }

    Documento documento = new Documento();
    documento.setUsuario(usuario);
    documento.setAsignatura(asignatura);
    documento.setTema(tema);
    documento.setNombreOriginal(originalFilename != null ? originalFilename : "documento");
    documento.setRutaArchivo("PENDIENTE");
    documento.setMimeType(detected.mimeType());
    documento.setExtension(detected.extension());
    documento.setTamanoBytes(sizeBytes);
    documento.setChecksumSha256(sha256);
    documento.setEstadoProcesado(EstadoProcesadoDocumento.PENDIENTE);

    Documento saved = documentoRepository.saveAndFlush(documento);

    String storedFilename = UUID.randomUUID() + "." + detected.extension();
    String relativePath =
        documentoFileStorageService.buildRelativePath(
            usuario.getId(), asignatura.getId(), tema != null ? tema.getId() : null, saved.getId(), storedFilename);

    try {
      documentoFileStorageService.moveToRelativePath(tempFile, relativePath);
    } catch (IOException ex) {
      documentoRepository.deleteById(saved.getId());
      throw new BadRequestException("No se ha podido guardar el fichero");
    }

    saved.setRutaArchivo(relativePath);
    documentoRepository.save(saved);

    runAfterCommit(() -> documentoProcessingService.procesarAsync(saved.getId()));

    return new DocumentoUploadResult(List.of(saved), List.of());
  }

  private Tema resolveTemaOrThrow(Long temaId, Long asignaturaId, Long usuarioId) {
    if (temaId == null) {
      return null;
    }

    Tema tema = temaService.obtenerPorId(temaId);

    Long temaAsignaturaId = tema.getAsignatura() != null ? tema.getAsignatura().getId() : null;
    Long temaUsuarioId =
        tema.getAsignatura() != null && tema.getAsignatura().getUsuario() != null
            ? tema.getAsignatura().getUsuario().getId()
            : null;

    if (temaAsignaturaId == null || temaUsuarioId == null) {
      throw new BadRequestException("Tema inválido: " + temaId);
    }

    if (!asignaturaId.equals(temaAsignaturaId)) {
      throw new BadRequestException("El tema no pertenece a la asignatura indicada");
    }

    if (!usuarioId.equals(temaUsuarioId)) {
      throw new BadRequestException("El tema no pertenece al usuario autenticado");
    }

    return tema;
  }

  private CopyResult copyWithSha256(InputStream inputStream, Path targetFile, long maxBytes) throws IOException {
    MessageDigest digest;
    try {
      digest = MessageDigest.getInstance("SHA-256");
    } catch (Exception ex) {
      throw new IllegalStateException("SHA-256 no disponible", ex);
    }

    long total = 0;
    try (InputStream in = inputStream; OutputStream out = java.nio.file.Files.newOutputStream(targetFile)) {
      byte[] buf = new byte[BUFFER_SIZE];
      int read;
      while ((read = in.read(buf)) != -1) {
        total += read;
        if (total > maxBytes) {
          throw new BadRequestException("Tamaño máximo excedido");
        }
        digest.update(buf, 0, read);
        out.write(buf, 0, read);
      }
    }

    String sha256 = HexFormat.of().formatHex(digest.digest());
    return new CopyResult(total, sha256);
  }

  private void runAfterCommit(Runnable runnable) {
    if (runnable == null) {
      return;
    }

    if (!TransactionSynchronizationManager.isSynchronizationActive()) {
      runnable.run();
      return;
    }

    TransactionSynchronizationManager.registerSynchronization(
        new TransactionSynchronization() {
          @Override
          public void afterCommit() {
            runnable.run();
          }
        });
  }

  private boolean isSupportedEntryExtension(String ext) {
    return "pdf".equals(ext) || "docx".equals(ext) || "txt".equals(ext) || "md".equals(ext);
  }

  private String safeZipEntryFilename(String entryName) {
    if (entryName == null || entryName.isBlank()) {
      return "documento";
    }
    String normalized = entryName.replace('\\', '/');
    int lastSlash = normalized.lastIndexOf('/');
    if (lastSlash >= 0 && lastSlash < normalized.length() - 1) {
      return normalized.substring(lastSlash + 1);
    }
    return normalized;
  }

  private record CopyResult(long sizeBytes, String sha256) {}
}

