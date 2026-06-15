package com.easy4you.service.impl;

import com.easy4you.model.entity.Documento;
import com.easy4you.model.entity.DocumentoChunk;
import com.easy4you.model.enums.EstadoProcesadoDocumento;
import com.easy4you.repository.DocumentoChunkRepository;
import com.easy4you.repository.DocumentoRepository;
import com.easy4you.service.DocumentoFileStorageService;
import com.easy4you.service.DocumentoProcessingService;
import com.easy4you.util.TokenChunker;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DocumentoProcessingServiceImpl implements DocumentoProcessingService {

  private static final Logger log = LoggerFactory.getLogger(DocumentoProcessingServiceImpl.class);

  private static final int CHUNK_TOKENS = 500;
  private static final int CHUNK_OVERLAP_TOKENS = 50;

  private final DocumentoRepository documentoRepository;
  private final DocumentoChunkRepository documentoChunkRepository;
  private final DocumentoFileStorageService documentoFileStorageService;

  @Override
  @Async
  @Transactional
  public void procesarAsync(Long documentoId) {
    if (documentoId == null) {
      return;
    }

    Documento documento = documentoRepository.findById(documentoId).orElse(null);
    if (documento == null) {
      log.warn("Documento no encontrado para procesado async: documentoId={}", documentoId);
      return;
    }

    if (documento.getEstadoProcesado() == EstadoProcesadoDocumento.PROCESANDO
        || documento.getEstadoProcesado() == EstadoProcesadoDocumento.PROCESADO
        || documento.getEstadoProcesado() == EstadoProcesadoDocumento.LISTO) {
      return;
    }

    documento.setEstadoProcesado(EstadoProcesadoDocumento.PROCESANDO);
    documento.setErrorExtraccion(null);
    documentoRepository.save(documento);

    documentoChunkRepository.deleteByDocumentoId(documentoId);

    try {
      if (documento.getRutaArchivo() == null || documento.getRutaArchivo().isBlank()) {
        throw new IllegalStateException("rutaArchivo no está definida");
      }

      Path absolutePath = documentoFileStorageService.resolveAbsolutePath(documento.getRutaArchivo());
      if (!Files.exists(absolutePath)) {
        throw new IllegalStateException("El fichero no existe: " + absolutePath);
      }

      String ext = documento.getExtension() == null ? "" : documento.getExtension().trim().toLowerCase();

      ExtractionResult extraction =
          switch (ext) {
            case "pdf" -> extractPdf(absolutePath);
            case "docx" -> extractDocx(absolutePath);
            case "txt", "md" -> extractText(absolutePath);
            default -> throw new IllegalArgumentException("Extensión no soportada para procesado: " + ext);
          };

      documento.setTextoExtraido(extraction.fullText());
      documento.setPaginas(extraction.pages());
      documentoRepository.save(documento);

      List<DocumentoChunk> chunks = new ArrayList<>();
      int idx = 0;

      if (extraction.pagesText() != null && !extraction.pagesText().isEmpty()) {
        for (PageText page : extraction.pagesText()) {
          for (TokenChunker.Chunk c : TokenChunker.chunk(page.text(), CHUNK_TOKENS, CHUNK_OVERLAP_TOKENS)) {
            DocumentoChunk chunk = new DocumentoChunk();
            chunk.setDocumento(documento);
            chunk.setIndiceChunk(idx++);
            chunk.setTexto(c.text());
            chunk.setPaginaOrigen(page.pageNumber());
            chunk.setTokenCount(c.tokenCount());
            chunks.add(chunk);
          }
        }
      } else {
        for (TokenChunker.Chunk c : TokenChunker.chunk(extraction.fullText(), CHUNK_TOKENS, CHUNK_OVERLAP_TOKENS)) {
          DocumentoChunk chunk = new DocumentoChunk();
          chunk.setDocumento(documento);
          chunk.setIndiceChunk(idx++);
          chunk.setTexto(c.text());
          chunk.setPaginaOrigen(null);
          chunk.setTokenCount(c.tokenCount());
          chunks.add(chunk);
        }
      }

      documentoChunkRepository.saveAll(chunks);

      documento.setEstadoProcesado(EstadoProcesadoDocumento.PROCESADO);
      documento.setErrorExtraccion(null);
      documentoRepository.save(documento);

      log.info(
          "Procesado completado: documentoId={}, chunks={}, paginas={}",
          documento.getId(),
          chunks.size(),
          extraction.pages());
    } catch (Exception ex) {
      log.error("Error procesando documentoId={}: {}", documentoId, ex.getMessage(), ex);
      documentoChunkRepository.deleteByDocumentoId(documentoId);
      documento.setEstadoProcesado(EstadoProcesadoDocumento.ERROR);
      documento.setErrorExtraccion(buildErrorMessage(ex));
      documentoRepository.save(documento);
    }
  }

  private ExtractionResult extractPdf(Path filePath) throws Exception {
    try (PDDocument pdf = Loader.loadPDF(filePath.toFile())) {
      int pages = pdf.getNumberOfPages();
      PDFTextStripper stripper = new PDFTextStripper();
      stripper.setSortByPosition(true);

      StringBuilder full = new StringBuilder();
      List<PageText> pagesText = new ArrayList<>();

      for (int page = 1; page <= pages; page++) {
        stripper.setStartPage(page);
        stripper.setEndPage(page);
        String pageText = stripper.getText(pdf);
        if (pageText != null && !pageText.isBlank()) {
          pagesText.add(new PageText(page, pageText));
          full.append(pageText).append('\n');
        }
      }

      return new ExtractionResult(full.toString().trim(), pages, pagesText);
    }
  }

  private ExtractionResult extractDocx(Path filePath) throws Exception {
    try (XWPFDocument doc = new XWPFDocument(Files.newInputStream(filePath));
        XWPFWordExtractor extractor = new XWPFWordExtractor(doc)) {
      String text = extractor.getText();
      return new ExtractionResult(text != null ? text.trim() : "", null, List.of());
    }
  }

  private ExtractionResult extractText(Path filePath) throws Exception {
    String text = Files.readString(filePath, StandardCharsets.UTF_8);
    return new ExtractionResult(text != null ? text.trim() : "", null, List.of());
  }

  private String buildErrorMessage(Exception ex) {
    String msg = ex.getMessage();
    if (msg == null || msg.isBlank()) {
      msg = ex.getClass().getSimpleName();
    }
    String out = ex.getClass().getSimpleName() + ": " + msg;
    if (out.length() > 4000) {
      return out.substring(0, 3990) + "…";
    }
    return out;
  }

  private record ExtractionResult(String fullText, Integer pages, List<PageText> pagesText) {}

  private record PageText(int pageNumber, String text) {}
}
