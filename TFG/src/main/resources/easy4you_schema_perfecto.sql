-- ==========================================================
-- Easy4You - Esquema "perfecto" alineado con las entidades JPA
-- ==========================================================
--
-- Objetivo:
-- - Evitar errores tipo: "Field 'unidad_id' doesn't have a default value"
-- - Dejar la BD consistente con el modelo actual (Tema -> Asignatura)
-- - Incluir también tablas que siguen existiendo en el código (legacy),
--   para que Hibernate no intente parchear el esquema con ddl-auto=update.
--
-- Recomendación de ejecución:
-- 1) Drop + create BD (o usa un schema nuevo)
-- 2) Ejecuta este SQL
-- 3) Arranca la app con spring.jpa.hibernate.ddl-auto=validate (ideal) o update
--
-- Nota: pensado para MySQL 5.5+ / InnoDB / utf8mb4

SET FOREIGN_KEY_CHECKS = 0;

-- CREATE DATABASE IF NOT EXISTS easy4you_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
-- USE easy4you_db;

-- -----------------------------
-- Seguridad / usuarios
-- -----------------------------

DROP TABLE IF EXISTS sesion_estudio_tema;
DROP TABLE IF EXISTS usuario_rol;

DROP TABLE IF EXISTS pregunta_test_opcion;
DROP TABLE IF EXISTS pregunta_test;
DROP TABLE IF EXISTS flashcard;
DROP TABLE IF EXISTS resumen;
DROP TABLE IF EXISTS nota;
DROP TABLE IF EXISTS chat_mensaje;
DROP TABLE IF EXISTS chat_conversacion;
DROP TABLE IF EXISTS artefacto_generado;
DROP TABLE IF EXISTS documento_chunk;
DROP TABLE IF EXISTS actividad_repaso;
DROP TABLE IF EXISTS progreso_tema;
DROP TABLE IF EXISTS progreso_asignatura;
DROP TABLE IF EXISTS recordatorio;
DROP TABLE IF EXISTS sesion_estudio;
DROP TABLE IF EXISTS documento;
DROP TABLE IF EXISTS tema;
DROP TABLE IF EXISTS unidad;
DROP TABLE IF EXISTS resultado_aprendizaje;
DROP TABLE IF EXISTS asignatura;
DROP TABLE IF EXISTS rol;
DROP TABLE IF EXISTS usuario;

CREATE TABLE usuario (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  nombre VARCHAR(100) NOT NULL,
  apellidos VARCHAR(150),
  email VARCHAR(190) NOT NULL,
  password_hash VARCHAR(255) NOT NULL,
  imagen_url VARCHAR(500),
  activo TINYINT(1) NOT NULL DEFAULT 1,
  verificado TINYINT(1) NOT NULL DEFAULT 0,
  ultimo_login DATETIME NULL,
  creado_en DATETIME NOT NULL,
  actualizado_en DATETIME NOT NULL,
  UNIQUE KEY uk_usuario_email (email)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE rol (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  nombre VARCHAR(50) NOT NULL,
  descripcion VARCHAR(255),
  creado_en DATETIME NOT NULL,
  actualizado_en DATETIME NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE usuario_rol (
  usuario_id BIGINT NOT NULL,
  rol_id BIGINT NOT NULL,
  PRIMARY KEY (usuario_id, rol_id),
  KEY idx_usuario_rol_rol (rol_id),
  CONSTRAINT fk_usuario_rol_usuario FOREIGN KEY (usuario_id) REFERENCES usuario (id) ON DELETE CASCADE,
  CONSTRAINT fk_usuario_rol_rol FOREIGN KEY (rol_id) REFERENCES rol (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- -----------------------------
-- Asignaturas + (legacy) RA/Unidad
-- -----------------------------

CREATE TABLE asignatura (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  usuario_id BIGINT NOT NULL,
  nombre VARCHAR(120) NOT NULL,
  descripcion LONGTEXT,
  color_hex VARCHAR(7),
  trimestre INT NULL,
  creado_en DATETIME NOT NULL,
  actualizado_en DATETIME NOT NULL,
  KEY idx_asignatura_usuario (usuario_id),
  CONSTRAINT fk_asignatura_usuario FOREIGN KEY (usuario_id) REFERENCES usuario (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE resultado_aprendizaje (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  asignatura_id BIGINT NOT NULL,
  codigo VARCHAR(20),
  descripcion LONGTEXT NOT NULL,
  orden INT NOT NULL DEFAULT 0,
  creado_en DATETIME NOT NULL,
  actualizado_en DATETIME NOT NULL,
  KEY idx_ra_asignatura (asignatura_id),
  CONSTRAINT fk_ra_asignatura FOREIGN KEY (asignatura_id) REFERENCES asignatura (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE unidad (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  resultado_aprendizaje_id BIGINT NOT NULL,
  titulo VARCHAR(160) NOT NULL,
  descripcion LONGTEXT,
  orden INT NOT NULL DEFAULT 0,
  creado_en DATETIME NOT NULL,
  actualizado_en DATETIME NOT NULL,
  KEY idx_unidad_ra (resultado_aprendizaje_id),
  CONSTRAINT fk_unidad_ra FOREIGN KEY (resultado_aprendizaje_id) REFERENCES resultado_aprendizaje (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- -----------------------------
-- Tema (nuevo modelo: asignatura_id)
-- -----------------------------

CREATE TABLE tema (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  asignatura_id BIGINT NOT NULL,
  trimestre INT NULL,
  titulo VARCHAR(200) NOT NULL,
  descripcion LONGTEXT,
  orden INT NOT NULL DEFAULT 0,
  palabras_clave VARCHAR(500),
  creado_en DATETIME NOT NULL,
  actualizado_en DATETIME NOT NULL,
  KEY idx_tema_asignatura (asignatura_id),
  CONSTRAINT fk_tema_asignatura FOREIGN KEY (asignatura_id) REFERENCES asignatura (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- -----------------------------
-- Documentos + (legacy) chunks
-- -----------------------------

CREATE TABLE documento (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  usuario_id BIGINT NOT NULL,
  asignatura_id BIGINT NOT NULL,
  tema_id BIGINT NULL,
  nombre_original VARCHAR(255) NOT NULL,
  ruta_archivo VARCHAR(600) NOT NULL,
  mime_type VARCHAR(100) NOT NULL,
  extension VARCHAR(10) NOT NULL,
  tamano_bytes BIGINT NOT NULL,
  checksum_sha256 VARCHAR(64),
  extraido_texto LONGTEXT,
  paginas INT,
  estado_procesado VARCHAR(20) NOT NULL,
  error_procesado LONGTEXT,
  creado_en DATETIME NOT NULL,
  actualizado_en DATETIME NOT NULL,
  KEY idx_documento_usuario (usuario_id),
  KEY idx_documento_asignatura (asignatura_id),
  KEY idx_documento_tema (tema_id),
  KEY idx_documento_checksum (checksum_sha256),
  CONSTRAINT fk_documento_usuario FOREIGN KEY (usuario_id) REFERENCES usuario (id) ON DELETE CASCADE,
  CONSTRAINT fk_documento_asignatura FOREIGN KEY (asignatura_id) REFERENCES asignatura (id) ON DELETE CASCADE,
  CONSTRAINT fk_documento_tema FOREIGN KEY (tema_id) REFERENCES tema (id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE documento_chunk (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  documento_id BIGINT NOT NULL,
  indice_chunk INT NOT NULL,
  texto LONGTEXT NOT NULL,
  pagina_origen INT NULL,
  token_count INT NOT NULL,
  embedding LONGTEXT,
  creado_en DATETIME NOT NULL,
  actualizado_en DATETIME NOT NULL,
  UNIQUE KEY uk_documento_chunk_documento_indice (documento_id, indice_chunk),
  KEY idx_documento_chunk_documento (documento_id),
  KEY idx_documento_chunk_documento_indice (documento_id, indice_chunk),
  CONSTRAINT fk_documento_chunk_documento FOREIGN KEY (documento_id) REFERENCES documento (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- -----------------------------
-- Chat
-- -----------------------------

CREATE TABLE chat_conversacion (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  usuario_id BIGINT NOT NULL,
  asignatura_id BIGINT NULL,
  tema_id BIGINT NULL,
  titulo VARCHAR(200) NOT NULL,
  fuentes_activas LONGTEXT,
  creado_en DATETIME NOT NULL,
  actualizado_en DATETIME NOT NULL,
  KEY idx_chat_conversacion_usuario (usuario_id),
  KEY idx_chat_conversacion_asignatura (asignatura_id),
  KEY idx_chat_conversacion_tema (tema_id),
  CONSTRAINT fk_chat_conversacion_usuario FOREIGN KEY (usuario_id) REFERENCES usuario (id) ON DELETE CASCADE,
  CONSTRAINT fk_chat_conversacion_asignatura FOREIGN KEY (asignatura_id) REFERENCES asignatura (id) ON DELETE SET NULL,
  CONSTRAINT fk_chat_conversacion_tema FOREIGN KEY (tema_id) REFERENCES tema (id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE chat_mensaje (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  conversacion_id BIGINT NOT NULL,
  rol VARCHAR(20) NOT NULL,
  contenido LONGTEXT NOT NULL,
  fuentes_usadas LONGTEXT,
  creado_en DATETIME NOT NULL,
  actualizado_en DATETIME NOT NULL,
  KEY idx_chat_mensaje_conversacion (conversacion_id),
  CONSTRAINT fk_chat_mensaje_conversacion FOREIGN KEY (conversacion_id) REFERENCES chat_conversacion (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- -----------------------------
-- Notas
-- -----------------------------

CREATE TABLE nota (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  usuario_id BIGINT NOT NULL,
  documento_id BIGINT NULL,
  chunk_id BIGINT NULL,
  tema_id BIGINT NULL,
  titulo VARCHAR(200) NOT NULL,
  contenido LONGTEXT NOT NULL,
  color_hex VARCHAR(7),
  creado_en DATETIME NOT NULL,
  actualizado_en DATETIME NOT NULL,
  KEY idx_nota_usuario (usuario_id),
  KEY idx_nota_documento (documento_id),
  KEY idx_nota_chunk (chunk_id),
  KEY idx_nota_tema (tema_id),
  CONSTRAINT fk_nota_usuario FOREIGN KEY (usuario_id) REFERENCES usuario (id) ON DELETE CASCADE,
  CONSTRAINT fk_nota_documento FOREIGN KEY (documento_id) REFERENCES documento (id) ON DELETE SET NULL,
  CONSTRAINT fk_nota_chunk FOREIGN KEY (chunk_id) REFERENCES documento_chunk (id) ON DELETE SET NULL,
  CONSTRAINT fk_nota_tema FOREIGN KEY (tema_id) REFERENCES tema (id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- -----------------------------
-- Resúmenes
-- -----------------------------

CREATE TABLE resumen (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  usuario_id BIGINT NOT NULL,
  tema_id BIGINT NOT NULL,
  documento_id BIGINT NULL,
  titulo VARCHAR(200) NOT NULL,
  contenido LONGTEXT NOT NULL,
  puntos_clave LONGTEXT,
  origen VARCHAR(10) NOT NULL,
  creado_en DATETIME NOT NULL,
  actualizado_en DATETIME NOT NULL,
  KEY idx_resumen_usuario (usuario_id),
  KEY idx_resumen_tema (tema_id),
  KEY idx_resumen_documento (documento_id),
  CONSTRAINT fk_resumen_usuario FOREIGN KEY (usuario_id) REFERENCES usuario (id) ON DELETE CASCADE,
  CONSTRAINT fk_resumen_tema FOREIGN KEY (tema_id) REFERENCES tema (id) ON DELETE CASCADE,
  CONSTRAINT fk_resumen_documento FOREIGN KEY (documento_id) REFERENCES documento (id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- -----------------------------
-- Flashcards / Tests
-- -----------------------------

CREATE TABLE flashcard (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  usuario_id BIGINT NOT NULL,
  tema_id BIGINT NOT NULL,
  documento_id BIGINT NULL,
  chunk_origen_id BIGINT NULL,
  pregunta LONGTEXT NOT NULL,
  respuesta LONGTEXT NOT NULL,
  dificultad INT NOT NULL DEFAULT 3,
  creado_en DATETIME NOT NULL,
  actualizado_en DATETIME NOT NULL,
  KEY idx_flashcard_usuario (usuario_id),
  KEY idx_flashcard_tema (tema_id),
  KEY idx_flashcard_documento (documento_id),
  KEY idx_flashcard_chunk (chunk_origen_id),
  CONSTRAINT fk_flashcard_usuario FOREIGN KEY (usuario_id) REFERENCES usuario (id) ON DELETE CASCADE,
  CONSTRAINT fk_flashcard_tema FOREIGN KEY (tema_id) REFERENCES tema (id) ON DELETE CASCADE,
  CONSTRAINT fk_flashcard_documento FOREIGN KEY (documento_id) REFERENCES documento (id) ON DELETE SET NULL,
  CONSTRAINT fk_flashcard_chunk FOREIGN KEY (chunk_origen_id) REFERENCES documento_chunk (id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE pregunta_test (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  usuario_id BIGINT NOT NULL,
  tema_id BIGINT NOT NULL,
  documento_id BIGINT NULL,
  chunk_origen_id BIGINT NULL,
  enunciado LONGTEXT NOT NULL,
  explicacion LONGTEXT,
  dificultad INT NOT NULL DEFAULT 3,
  creado_en DATETIME NOT NULL,
  actualizado_en DATETIME NOT NULL,
  KEY idx_pregunta_test_usuario (usuario_id),
  KEY idx_pregunta_test_tema (tema_id),
  KEY idx_pregunta_test_documento (documento_id),
  KEY idx_pregunta_test_chunk (chunk_origen_id),
  CONSTRAINT fk_pregunta_test_usuario FOREIGN KEY (usuario_id) REFERENCES usuario (id) ON DELETE CASCADE,
  CONSTRAINT fk_pregunta_test_tema FOREIGN KEY (tema_id) REFERENCES tema (id) ON DELETE CASCADE,
  CONSTRAINT fk_pregunta_test_documento FOREIGN KEY (documento_id) REFERENCES documento (id) ON DELETE SET NULL,
  CONSTRAINT fk_pregunta_test_chunk FOREIGN KEY (chunk_origen_id) REFERENCES documento_chunk (id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE pregunta_test_opcion (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  pregunta_test_id BIGINT NOT NULL,
  texto VARCHAR(400) NOT NULL,
  es_correcta TINYINT(1) NOT NULL DEFAULT 0,
  orden INT NOT NULL DEFAULT 0,
  KEY idx_pregunta_test_opcion_pregunta (pregunta_test_id),
  CONSTRAINT fk_pregunta_test_opcion_pregunta FOREIGN KEY (pregunta_test_id) REFERENCES pregunta_test (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- -----------------------------
-- Progreso + Repaso
-- -----------------------------

CREATE TABLE progreso_asignatura (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  usuario_id BIGINT NOT NULL,
  asignatura_id BIGINT NOT NULL,
  porcentaje DECIMAL(5,2) NOT NULL DEFAULT 0.00,
  sesiones_completadas INT NOT NULL DEFAULT 0,
  minutos_estudiados INT NOT NULL DEFAULT 0,
  ultima_sesion DATETIME NULL,
  creado_en DATETIME NOT NULL,
  actualizado_en DATETIME NOT NULL,
  KEY idx_progreso_asig_usuario (usuario_id),
  KEY idx_progreso_asig_asignatura (asignatura_id),
  CONSTRAINT fk_progreso_asig_usuario FOREIGN KEY (usuario_id) REFERENCES usuario (id) ON DELETE CASCADE,
  CONSTRAINT fk_progreso_asig_asignatura FOREIGN KEY (asignatura_id) REFERENCES asignatura (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE progreso_tema (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  usuario_id BIGINT NOT NULL,
  tema_id BIGINT NOT NULL,
  porcentaje DECIMAL(5,2) NOT NULL DEFAULT 0.00,
  sesiones_completadas INT NOT NULL DEFAULT 0,
  minutos_estudiados INT NOT NULL DEFAULT 0,
  ultima_sesion DATETIME NULL,
  creado_en DATETIME NOT NULL,
  actualizado_en DATETIME NOT NULL,
  KEY idx_progreso_tema_usuario (usuario_id),
  KEY idx_progreso_tema_tema (tema_id),
  CONSTRAINT fk_progreso_tema_usuario FOREIGN KEY (usuario_id) REFERENCES usuario (id) ON DELETE CASCADE,
  CONSTRAINT fk_progreso_tema_tema FOREIGN KEY (tema_id) REFERENCES tema (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE actividad_repaso (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  usuario_id BIGINT NOT NULL,
  tema_id BIGINT NOT NULL,
  documento_id BIGINT NULL,
  tipo VARCHAR(20) NOT NULL,
  enunciado LONGTEXT NOT NULL,
  solucion LONGTEXT,
  creado_en DATETIME NOT NULL,
  actualizado_en DATETIME NOT NULL,
  KEY idx_actividad_repaso_usuario (usuario_id),
  KEY idx_actividad_repaso_tema (tema_id),
  KEY idx_actividad_repaso_documento (documento_id),
  CONSTRAINT fk_actividad_repaso_usuario FOREIGN KEY (usuario_id) REFERENCES usuario (id) ON DELETE CASCADE,
  CONSTRAINT fk_actividad_repaso_tema FOREIGN KEY (tema_id) REFERENCES tema (id) ON DELETE CASCADE,
  CONSTRAINT fk_actividad_repaso_documento FOREIGN KEY (documento_id) REFERENCES documento (id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- -----------------------------
-- Sesiones + recordatorios
-- -----------------------------

CREATE TABLE sesion_estudio (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  usuario_id BIGINT NOT NULL,
  titulo VARCHAR(200) NOT NULL,
  descripcion LONGTEXT,
  fecha_inicio DATETIME NOT NULL,
  fecha_fin DATETIME NOT NULL,
  estado VARCHAR(15) NOT NULL,
  minutos_objetivo INT NULL,
  minutos_real INT NULL,
  creado_en DATETIME NOT NULL,
  actualizado_en DATETIME NOT NULL,
  KEY idx_sesion_estudio_usuario (usuario_id),
  CONSTRAINT fk_sesion_estudio_usuario FOREIGN KEY (usuario_id) REFERENCES usuario (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE sesion_estudio_tema (
  sesion_estudio_id BIGINT NOT NULL,
  tema_id BIGINT NOT NULL,
  PRIMARY KEY (sesion_estudio_id, tema_id),
  KEY idx_sesion_estudio_tema_tema (tema_id),
  CONSTRAINT fk_sesion_estudio_tema_sesion FOREIGN KEY (sesion_estudio_id) REFERENCES sesion_estudio (id) ON DELETE CASCADE,
  CONSTRAINT fk_sesion_estudio_tema_tema FOREIGN KEY (tema_id) REFERENCES tema (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE recordatorio (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  usuario_id BIGINT NOT NULL,
  sesion_estudio_id BIGINT NOT NULL,
  titulo VARCHAR(200) NOT NULL,
  mensaje VARCHAR(500),
  fecha_hora DATETIME NOT NULL,
  estado VARCHAR(10) NOT NULL,
  creado_en DATETIME NOT NULL,
  actualizado_en DATETIME NOT NULL,
  KEY idx_recordatorio_usuario (usuario_id),
  KEY idx_recordatorio_sesion (sesion_estudio_id),
  CONSTRAINT fk_recordatorio_usuario FOREIGN KEY (usuario_id) REFERENCES usuario (id) ON DELETE CASCADE,
  CONSTRAINT fk_recordatorio_sesion FOREIGN KEY (sesion_estudio_id) REFERENCES sesion_estudio (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- -----------------------------
-- Artefactos (aunque sea legacy, sigue en código)
-- -----------------------------

CREATE TABLE artefacto_generado (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  asignatura_id BIGINT NOT NULL,
  tipo VARCHAR(20) NOT NULL,
  estado VARCHAR(20) NOT NULL,
  ruta_archivo VARCHAR(600),
  metadatos LONGTEXT,
  creado_en DATETIME NOT NULL,
  actualizado_en DATETIME NOT NULL,
  KEY idx_artefacto_generado_asignatura (asignatura_id),
  KEY idx_artefacto_generado_tipo (tipo),
  KEY idx_artefacto_generado_estado (estado),
  CONSTRAINT fk_artefacto_generado_asignatura FOREIGN KEY (asignatura_id) REFERENCES asignatura (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

SET FOREIGN_KEY_CHECKS = 1;

