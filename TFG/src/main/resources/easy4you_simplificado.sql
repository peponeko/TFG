-- =============================================
-- Easy4You (TFG DAM) - ESQUEMA SIMPLIFICADO
-- Objetivo: defendible y sin sobreingeniería (sin chunks/embeddings/artefactos).
-- Filosofía para el tribunal: la IA "lee" el texto extraído del documento.
-- =============================================

CREATE DATABASE IF NOT EXISTS easy4you_db
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

USE easy4you_db;

-- -----------------------------
-- Seguridad y usuarios
-- -----------------------------

CREATE TABLE IF NOT EXISTS rol (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  nombre VARCHAR(50) NOT NULL,
  descripcion VARCHAR(255),
  creado_en DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  actualizado_en DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_rol_nombre (nombre)
);

CREATE TABLE IF NOT EXISTS usuario (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  nombre VARCHAR(100) NOT NULL,
  apellidos VARCHAR(150),
  email VARCHAR(190) NOT NULL,
  password_hash VARCHAR(255) NOT NULL,
  imagen_url VARCHAR(500),
  activo TINYINT(1) NOT NULL DEFAULT 1,
  verificado TINYINT(1) NOT NULL DEFAULT 0,
  ultimo_login DATETIME,
  creado_en DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  actualizado_en DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_usuario_email (email)
);

CREATE TABLE IF NOT EXISTS usuario_rol (
  usuario_id BIGINT NOT NULL,
  rol_id BIGINT NOT NULL,
  PRIMARY KEY (usuario_id, rol_id),
  CONSTRAINT fk_ur_usuario FOREIGN KEY (usuario_id) REFERENCES usuario (id) ON DELETE CASCADE,
  CONSTRAINT fk_ur_rol FOREIGN KEY (rol_id) REFERENCES rol (id) ON DELETE CASCADE
);

-- -----------------------------
-- Notebooks (por asignatura) y "temas" simplificados
-- -----------------------------

CREATE TABLE IF NOT EXISTS asignatura (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  usuario_id BIGINT NOT NULL,
  nombre VARCHAR(120) NOT NULL,
  descripcion LONGTEXT,
  color_hex VARCHAR(7),
  trimestre TINYINT DEFAULT NULL COMMENT '1=Primer, 2=Segundo, 3=Tercero, NULL=Sin asignar',
  creado_en DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  actualizado_en DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  KEY idx_asignatura_usuario (usuario_id),
  CONSTRAINT fk_asig_usuario FOREIGN KEY (usuario_id) REFERENCES usuario (id) ON DELETE CASCADE
);

-- Fusión Unidad + Tema: mantenemos una sola tabla "tema" (organización simple).
CREATE TABLE IF NOT EXISTS tema (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  asignatura_id BIGINT NOT NULL,
  titulo VARCHAR(200) NOT NULL,
  descripcion LONGTEXT,
  orden INT NOT NULL DEFAULT 0,
  creado_en DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  actualizado_en DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  KEY idx_tema_asignatura (asignatura_id),
  CONSTRAINT fk_tema_asignatura FOREIGN KEY (asignatura_id) REFERENCES asignatura (id) ON DELETE CASCADE
);

-- -----------------------------
-- Documentos (con texto extraído "en crudo")
-- -----------------------------

CREATE TABLE IF NOT EXISTS documento (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  usuario_id BIGINT NOT NULL,
  asignatura_id BIGINT NOT NULL,
  tema_id BIGINT,
  nombre_original VARCHAR(255) NOT NULL,
  ruta_archivo VARCHAR(600) NOT NULL,
  mime_type VARCHAR(100) NOT NULL,
  extension VARCHAR(10) NOT NULL,
  tamano_bytes BIGINT NOT NULL,
  checksum_sha256 VARCHAR(64),
  extraido_texto LONGTEXT,
  paginas INT,
  estado_procesado VARCHAR(20) NOT NULL DEFAULT 'PENDIENTE',
  error_procesado LONGTEXT,
  creado_en DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  actualizado_en DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  KEY idx_doc_usuario (usuario_id),
  KEY idx_doc_asignatura (asignatura_id),
  KEY idx_doc_tema (tema_id),
  CONSTRAINT fk_doc_usuario FOREIGN KEY (usuario_id) REFERENCES usuario (id) ON DELETE CASCADE,
  CONSTRAINT fk_doc_asignatura FOREIGN KEY (asignatura_id) REFERENCES asignatura (id) ON DELETE CASCADE,
  CONSTRAINT fk_doc_tema FOREIGN KEY (tema_id) REFERENCES tema (id) ON DELETE SET NULL
);

-- -----------------------------
-- Resúmenes y estudio (sin tablas "artefacto")
-- -----------------------------

CREATE TABLE IF NOT EXISTS resumen (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  usuario_id BIGINT NOT NULL,
  documento_id BIGINT,
  tema_id BIGINT,
  titulo VARCHAR(200) NOT NULL,
  contenido LONGTEXT NOT NULL,
  puntos_clave LONGTEXT,
  origen VARCHAR(10) NOT NULL DEFAULT 'GENERADO',
  creado_en DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  actualizado_en DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  KEY idx_res_usuario (usuario_id),
  KEY idx_res_doc (documento_id),
  KEY idx_res_tema (tema_id),
  CONSTRAINT fk_res_usuario FOREIGN KEY (usuario_id) REFERENCES usuario (id) ON DELETE CASCADE,
  CONSTRAINT fk_res_doc FOREIGN KEY (documento_id) REFERENCES documento (id) ON DELETE SET NULL,
  CONSTRAINT fk_res_tema FOREIGN KEY (tema_id) REFERENCES tema (id) ON DELETE SET NULL
);

CREATE TABLE IF NOT EXISTS flashcard (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  usuario_id BIGINT NOT NULL,
  documento_id BIGINT,
  tema_id BIGINT,
  pregunta LONGTEXT NOT NULL,
  respuesta LONGTEXT NOT NULL,
  dificultad INT NOT NULL DEFAULT 3,
  creado_en DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  actualizado_en DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  KEY idx_fc_usuario (usuario_id),
  KEY idx_fc_doc (documento_id),
  KEY idx_fc_tema (tema_id),
  CONSTRAINT fk_fc_usuario FOREIGN KEY (usuario_id) REFERENCES usuario (id) ON DELETE CASCADE,
  CONSTRAINT fk_fc_doc FOREIGN KEY (documento_id) REFERENCES documento (id) ON DELETE SET NULL,
  CONSTRAINT fk_fc_tema FOREIGN KEY (tema_id) REFERENCES tema (id) ON DELETE SET NULL
);

CREATE TABLE IF NOT EXISTS pregunta_test (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  usuario_id BIGINT NOT NULL,
  documento_id BIGINT,
  tema_id BIGINT,
  enunciado LONGTEXT NOT NULL,
  explicacion LONGTEXT,
  dificultad INT NOT NULL DEFAULT 3,
  creado_en DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  actualizado_en DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  KEY idx_pt_usuario (usuario_id),
  KEY idx_pt_doc (documento_id),
  KEY idx_pt_tema (tema_id),
  CONSTRAINT fk_pt_usuario FOREIGN KEY (usuario_id) REFERENCES usuario (id) ON DELETE CASCADE,
  CONSTRAINT fk_pt_doc FOREIGN KEY (documento_id) REFERENCES documento (id) ON DELETE SET NULL,
  CONSTRAINT fk_pt_tema FOREIGN KEY (tema_id) REFERENCES tema (id) ON DELETE SET NULL
);

CREATE TABLE IF NOT EXISTS pregunta_test_opcion (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  pregunta_test_id BIGINT NOT NULL,
  texto VARCHAR(400) NOT NULL,
  es_correcta TINYINT(1) NOT NULL DEFAULT 0,
  orden INT NOT NULL DEFAULT 0,
  KEY idx_pto_pt (pregunta_test_id),
  CONSTRAINT fk_pto_pregunta FOREIGN KEY (pregunta_test_id) REFERENCES pregunta_test (id) ON DELETE CASCADE
);

-- -----------------------------
-- Chat (sin “fuentes por chunk”)
-- -----------------------------

CREATE TABLE IF NOT EXISTS chat_conversacion (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  usuario_id BIGINT NOT NULL,
  asignatura_id BIGINT,
  tema_id BIGINT,
  titulo VARCHAR(200) NOT NULL,
  fuentes_activas LONGTEXT,
  creado_en DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  actualizado_en DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  KEY idx_cc_usuario (usuario_id),
  CONSTRAINT fk_cc_usuario FOREIGN KEY (usuario_id) REFERENCES usuario (id) ON DELETE CASCADE,
  CONSTRAINT fk_cc_asignatura FOREIGN KEY (asignatura_id) REFERENCES asignatura (id) ON DELETE SET NULL,
  CONSTRAINT fk_cc_tema FOREIGN KEY (tema_id) REFERENCES tema (id) ON DELETE SET NULL
);

CREATE TABLE IF NOT EXISTS chat_mensaje (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  conversacion_id BIGINT NOT NULL,
  rol VARCHAR(20) NOT NULL,
  contenido LONGTEXT NOT NULL,
  creado_en DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  actualizado_en DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  KEY idx_cm_conversacion (conversacion_id),
  CONSTRAINT fk_cm_conversacion FOREIGN KEY (conversacion_id) REFERENCES chat_conversacion (id) ON DELETE CASCADE
);

-- -----------------------------
-- Notas (sin referencia a chunk)
-- -----------------------------

CREATE TABLE IF NOT EXISTS nota (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  usuario_id BIGINT NOT NULL,
  documento_id BIGINT,
  tema_id BIGINT,
  titulo VARCHAR(200) NOT NULL,
  contenido LONGTEXT NOT NULL,
  color_hex VARCHAR(7),
  creado_en DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  actualizado_en DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  KEY idx_nota_usuario (usuario_id),
  KEY idx_nota_documento (documento_id),
  KEY idx_nota_tema (tema_id),
  CONSTRAINT fk_nota_usuario FOREIGN KEY (usuario_id) REFERENCES usuario (id) ON DELETE CASCADE,
  CONSTRAINT fk_nota_documento FOREIGN KEY (documento_id) REFERENCES documento (id) ON DELETE SET NULL,
  CONSTRAINT fk_nota_tema FOREIGN KEY (tema_id) REFERENCES tema (id) ON DELETE SET NULL
);

-- -----------------------------
-- Planificador de estudio (exámenes por documento)
-- -----------------------------

CREATE TABLE IF NOT EXISTS examen (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  usuario_id BIGINT NOT NULL,
  asignatura_id BIGINT NOT NULL,
  documento_id BIGINT,
  titulo VARCHAR(200) NOT NULL,
  fecha DATE NOT NULL,
  notas VARCHAR(500),
  creado_en DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  actualizado_en DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  KEY idx_ex_usuario (usuario_id),
  KEY idx_ex_asignatura (asignatura_id),
  KEY idx_ex_documento (documento_id),
  CONSTRAINT fk_ex_usuario FOREIGN KEY (usuario_id) REFERENCES usuario (id) ON DELETE CASCADE,
  CONSTRAINT fk_ex_asignatura FOREIGN KEY (asignatura_id) REFERENCES asignatura (id) ON DELETE CASCADE,
  CONSTRAINT fk_ex_documento FOREIGN KEY (documento_id) REFERENCES documento (id) ON DELETE SET NULL
);

