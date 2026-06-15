package com.easy4you.service;

public record ChatSource(
    Long chunkId,
    Long documentoId,
    String documentoNombre,
    Integer indiceChunk,
    Integer paginaOrigen) {}

