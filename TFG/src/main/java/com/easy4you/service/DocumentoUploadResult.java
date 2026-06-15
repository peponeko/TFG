package com.easy4you.service;

import com.easy4you.model.entity.Documento;
import java.util.List;

public record DocumentoUploadResult(List<Documento> documentos, List<String> warnings) {}

