-- =============================================
--  Easy4You - Script de base de datos
--  IMPORTANTE: La app usa la BD "easy4you_db"
--  Ejecutar como root de MySQL:
--    mysql -u root -p < easy4you_db.sql
--  En PowerShell: Get-Content easy4you_db.sql | mysql -u root -p
-- =============================================

-- 1. Crear base de datos (mismo nombre que en application.properties)
CREATE DATABASE IF NOT EXISTS easy4you_db
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

CREATE USER IF NOT EXISTS 'easy4you_user'@'localhost' IDENTIFIED BY 'easy4you_pass';
GRANT ALL PRIVILEGES ON easy4you_db.* TO 'easy4you_user'@'localhost';
FLUSH PRIVILEGES;

USE easy4you_db;

-- =============================================
--  TABLAS (Hibernate las crea con ddl-auto=update,
--  este script sirve para clean install o documentación)
-- =============================================

-- Roles
CREATE TABLE IF NOT EXISTS rol (
    id             BIGINT AUTO_INCREMENT PRIMARY KEY,
    nombre         VARCHAR(50)  NOT NULL,
    descripcion    VARCHAR(255),
    creado_en      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    actualizado_en DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- Usuarios
CREATE TABLE IF NOT EXISTS usuario (
    id             BIGINT AUTO_INCREMENT PRIMARY KEY,
    nombre         VARCHAR(100) NOT NULL,
    apellidos      VARCHAR(150),
    email          VARCHAR(190) NOT NULL,
    password_hash  VARCHAR(255) NOT NULL,
    imagen_url     VARCHAR(500),
    activo         TINYINT(1)   NOT NULL DEFAULT 1,
    verificado     TINYINT(1)   NOT NULL DEFAULT 0,
    ultimo_login   DATETIME,
    creado_en      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    actualizado_en DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_usuario_email (email)
);

-- Tabla intermedia usuario-rol
CREATE TABLE IF NOT EXISTS usuario_rol (
    usuario_id BIGINT NOT NULL,
    rol_id     BIGINT NOT NULL,
    PRIMARY KEY (usuario_id, rol_id),
    CONSTRAINT fk_ur_usuario FOREIGN KEY (usuario_id) REFERENCES usuario (id) ON DELETE CASCADE,
    CONSTRAINT fk_ur_rol     FOREIGN KEY (rol_id)     REFERENCES rol     (id) ON DELETE CASCADE
);

-- Asignaturas
CREATE TABLE IF NOT EXISTS asignatura (
    id             BIGINT AUTO_INCREMENT PRIMARY KEY,
    usuario_id     BIGINT       NOT NULL,
    nombre         VARCHAR(120) NOT NULL,
    descripcion    LONGTEXT,
    color_hex      VARCHAR(7),
    creado_en      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    actualizado_en DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_asig_usuario FOREIGN KEY (usuario_id) REFERENCES usuario (id) ON DELETE CASCADE
);

-- Resultados de Aprendizaje
CREATE TABLE IF NOT EXISTS resultado_aprendizaje (
    id             BIGINT AUTO_INCREMENT PRIMARY KEY,
    asignatura_id  BIGINT   NOT NULL,
    codigo         VARCHAR(20),
    descripcion    LONGTEXT NOT NULL,
    orden          INT      NOT NULL DEFAULT 0,
    creado_en      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    actualizado_en DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_ra_asignatura FOREIGN KEY (asignatura_id) REFERENCES asignatura (id) ON DELETE CASCADE
);

-- Unidades
CREATE TABLE IF NOT EXISTS unidad (
    id                        BIGINT AUTO_INCREMENT PRIMARY KEY,
    resultado_aprendizaje_id  BIGINT       NOT NULL,
    titulo                    VARCHAR(160) NOT NULL,
    descripcion               LONGTEXT,
    orden                     INT          NOT NULL DEFAULT 0,
    creado_en                 DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    actualizado_en            DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_unidad_ra FOREIGN KEY (resultado_aprendizaje_id) REFERENCES resultado_aprendizaje (id) ON DELETE CASCADE
);

-- Temas
CREATE TABLE IF NOT EXISTS tema (
    id             BIGINT AUTO_INCREMENT PRIMARY KEY,
    unidad_id      BIGINT       NOT NULL,
    titulo         VARCHAR(200) NOT NULL,
    descripcion    LONGTEXT,
    orden          INT          NOT NULL DEFAULT 0,
    palabras_clave VARCHAR(500),
    creado_en      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    actualizado_en DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_tema_unidad FOREIGN KEY (unidad_id) REFERENCES unidad (id) ON DELETE CASCADE
);

-- Documentos
CREATE TABLE IF NOT EXISTS documento (
    id               BIGINT AUTO_INCREMENT PRIMARY KEY,
    usuario_id       BIGINT       NOT NULL,
    asignatura_id    BIGINT       NOT NULL,
    tema_id          BIGINT,
    nombre_original  VARCHAR(255) NOT NULL,
    ruta_archivo     VARCHAR(600) NOT NULL,
    mime_type        VARCHAR(100) NOT NULL,
    extension        VARCHAR(10)  NOT NULL,
    tamano_bytes     BIGINT       NOT NULL,
    checksum_sha256  VARCHAR(64),
    extraido_texto   LONGTEXT,
    paginas          INT,
    estado_procesado VARCHAR(20)  NOT NULL DEFAULT 'PENDIENTE',
    error_procesado  LONGTEXT,
    creado_en        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    actualizado_en   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_doc_usuario    FOREIGN KEY (usuario_id)    REFERENCES usuario    (id) ON DELETE CASCADE,
    CONSTRAINT fk_doc_asignatura FOREIGN KEY (asignatura_id) REFERENCES asignatura (id) ON DELETE CASCADE,
    CONSTRAINT fk_doc_tema       FOREIGN KEY (tema_id)       REFERENCES tema        (id) ON DELETE SET NULL
);

-- =============================================
--  EXTENSIÓN NOTEBOOKLM (nuevas tablas)
-- =============================================

-- Chunks de documento (texto extraído troceado)
CREATE TABLE IF NOT EXISTS documento_chunk (
    id             BIGINT AUTO_INCREMENT PRIMARY KEY,
    documento_id   BIGINT    NOT NULL,
    indice_chunk   INT       NOT NULL,
    texto          LONGTEXT  NOT NULL,
    pagina_origen  INT,
    token_count    INT       NOT NULL,
    embedding      LONGTEXT,
    creado_en      DATETIME  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    actualizado_en DATETIME  NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_documento_chunk_documento_indice (documento_id, indice_chunk),
    KEY idx_documento_chunk_documento (documento_id),
    FULLTEXT KEY idx_documento_chunk_fulltext (texto),
    CONSTRAINT fk_dc_documento FOREIGN KEY (documento_id) REFERENCES documento (id) ON DELETE CASCADE
);

-- Resúmenes generados
CREATE TABLE IF NOT EXISTS resumen (
    id             BIGINT AUTO_INCREMENT PRIMARY KEY,
    usuario_id     BIGINT       NOT NULL,
    tema_id        BIGINT       NOT NULL,
    documento_id   BIGINT,
    titulo         VARCHAR(200) NOT NULL,
    contenido      LONGTEXT     NOT NULL,
    puntos_clave   LONGTEXT,
    origen         VARCHAR(10)  NOT NULL DEFAULT 'GENERADO',
    creado_en      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    actualizado_en DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_res_usuario  FOREIGN KEY (usuario_id)   REFERENCES usuario   (id) ON DELETE CASCADE,
    CONSTRAINT fk_res_tema     FOREIGN KEY (tema_id)      REFERENCES tema      (id) ON DELETE CASCADE,
    CONSTRAINT fk_res_doc      FOREIGN KEY (documento_id) REFERENCES documento (id) ON DELETE SET NULL
);

-- Migración (si ya existe la tabla resumen sin puntos_clave):
-- ALTER TABLE resumen ADD COLUMN puntos_clave LONGTEXT;

-- Flashcards
CREATE TABLE IF NOT EXISTS flashcard (
    id             BIGINT AUTO_INCREMENT PRIMARY KEY,
    usuario_id     BIGINT   NOT NULL,
    tema_id        BIGINT   NOT NULL,
    documento_id   BIGINT,
    chunk_origen_id BIGINT,
    pregunta       LONGTEXT NOT NULL,
    respuesta      LONGTEXT NOT NULL,
    dificultad     INT      NOT NULL DEFAULT 3,
    creado_en      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    actualizado_en DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_fc_usuario  FOREIGN KEY (usuario_id)   REFERENCES usuario   (id) ON DELETE CASCADE,
    CONSTRAINT fk_fc_tema     FOREIGN KEY (tema_id)      REFERENCES tema      (id) ON DELETE CASCADE,
    CONSTRAINT fk_fc_doc      FOREIGN KEY (documento_id) REFERENCES documento (id) ON DELETE SET NULL,
    CONSTRAINT fk_fc_chunk    FOREIGN KEY (chunk_origen_id) REFERENCES documento_chunk (id) ON DELETE SET NULL
);

-- Migración (si ya existe la tabla flashcard sin chunk_origen_id):
-- ALTER TABLE flashcard ADD COLUMN chunk_origen_id BIGINT;
-- ALTER TABLE flashcard ADD CONSTRAINT fk_fc_chunk FOREIGN KEY (chunk_origen_id) REFERENCES documento_chunk (id) ON DELETE SET NULL;

-- Preguntas de test
CREATE TABLE IF NOT EXISTS pregunta_test (
    id             BIGINT AUTO_INCREMENT PRIMARY KEY,
    usuario_id     BIGINT   NOT NULL,
    tema_id        BIGINT   NOT NULL,
    documento_id   BIGINT,
    chunk_origen_id BIGINT,
    enunciado      LONGTEXT NOT NULL,
    explicacion    LONGTEXT,
    dificultad     INT      NOT NULL DEFAULT 3,
    creado_en      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    actualizado_en DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_pt_usuario  FOREIGN KEY (usuario_id)   REFERENCES usuario   (id) ON DELETE CASCADE,
    CONSTRAINT fk_pt_tema     FOREIGN KEY (tema_id)      REFERENCES tema      (id) ON DELETE CASCADE,
    CONSTRAINT fk_pt_doc      FOREIGN KEY (documento_id) REFERENCES documento (id) ON DELETE SET NULL,
    CONSTRAINT fk_pt_chunk    FOREIGN KEY (chunk_origen_id) REFERENCES documento_chunk (id) ON DELETE SET NULL
);

-- Migración (si ya existe la tabla pregunta_test sin chunk_origen_id):
-- ALTER TABLE pregunta_test ADD COLUMN chunk_origen_id BIGINT;
-- ALTER TABLE pregunta_test ADD CONSTRAINT fk_pt_chunk FOREIGN KEY (chunk_origen_id) REFERENCES documento_chunk (id) ON DELETE SET NULL;

-- Opciones de preguntas de test
CREATE TABLE IF NOT EXISTS pregunta_test_opcion (
    id               BIGINT AUTO_INCREMENT PRIMARY KEY,
    pregunta_test_id BIGINT       NOT NULL,
    texto            VARCHAR(400) NOT NULL,
    es_correcta      TINYINT(1)   NOT NULL DEFAULT 0,
    orden            INT          NOT NULL DEFAULT 0,
    CONSTRAINT fk_pto_pregunta FOREIGN KEY (pregunta_test_id) REFERENCES pregunta_test (id) ON DELETE CASCADE
);

-- Sesiones de estudio
CREATE TABLE IF NOT EXISTS sesion_estudio (
    id               BIGINT AUTO_INCREMENT PRIMARY KEY,
    usuario_id       BIGINT       NOT NULL,
    titulo           VARCHAR(200) NOT NULL,
    descripcion      LONGTEXT,
    fecha_inicio     DATETIME     NOT NULL,
    fecha_fin        DATETIME     NOT NULL,
    estado           VARCHAR(15)  NOT NULL DEFAULT 'PLANIFICADA',
    minutos_objetivo INT,
    minutos_real     INT,
    creado_en        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    actualizado_en   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_se_usuario FOREIGN KEY (usuario_id) REFERENCES usuario (id) ON DELETE CASCADE
);

-- Tabla intermedia sesion-tema
CREATE TABLE IF NOT EXISTS sesion_estudio_tema (
    sesion_estudio_id BIGINT NOT NULL,
    tema_id           BIGINT NOT NULL,
    PRIMARY KEY (sesion_estudio_id, tema_id),
    CONSTRAINT fk_set_sesion FOREIGN KEY (sesion_estudio_id) REFERENCES sesion_estudio (id) ON DELETE CASCADE,
    CONSTRAINT fk_set_tema   FOREIGN KEY (tema_id)           REFERENCES tema            (id) ON DELETE CASCADE
);

-- Recordatorios
CREATE TABLE IF NOT EXISTS recordatorio (
    id               BIGINT AUTO_INCREMENT PRIMARY KEY,
    usuario_id       BIGINT       NOT NULL,
    sesion_estudio_id BIGINT      NOT NULL,
    titulo           VARCHAR(200) NOT NULL,
    mensaje          VARCHAR(500),
    fecha_hora       DATETIME     NOT NULL,
    estado           VARCHAR(10)  NOT NULL DEFAULT 'PENDIENTE',
    creado_en        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    actualizado_en   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_rec_usuario FOREIGN KEY (usuario_id)        REFERENCES usuario        (id) ON DELETE CASCADE,
    CONSTRAINT fk_rec_sesion  FOREIGN KEY (sesion_estudio_id) REFERENCES sesion_estudio (id) ON DELETE CASCADE
);

-- Progreso por asignatura
CREATE TABLE IF NOT EXISTS progreso_asignatura (
    id                   BIGINT AUTO_INCREMENT PRIMARY KEY,
    usuario_id           BIGINT         NOT NULL,
    asignatura_id        BIGINT         NOT NULL,
    porcentaje           DECIMAL(5,2)   NOT NULL DEFAULT 0.00,
    sesiones_completadas INT            NOT NULL DEFAULT 0,
    minutos_estudiados   INT            NOT NULL DEFAULT 0,
    ultima_sesion        DATETIME,
    creado_en            DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    actualizado_en       DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_prog_asig (usuario_id, asignatura_id),
    CONSTRAINT fk_pa_usuario    FOREIGN KEY (usuario_id)    REFERENCES usuario    (id) ON DELETE CASCADE,
    CONSTRAINT fk_pa_asignatura FOREIGN KEY (asignatura_id) REFERENCES asignatura (id) ON DELETE CASCADE
);

-- Progreso por tema
CREATE TABLE IF NOT EXISTS progreso_tema (
    id                   BIGINT AUTO_INCREMENT PRIMARY KEY,
    usuario_id           BIGINT       NOT NULL,
    tema_id              BIGINT       NOT NULL,
    porcentaje           DECIMAL(5,2) NOT NULL DEFAULT 0.00,
    sesiones_completadas INT          NOT NULL DEFAULT 0,
    minutos_estudiados   INT          NOT NULL DEFAULT 0,
    ultima_sesion        DATETIME,
    creado_en            DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    actualizado_en       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_prog_tema (usuario_id, tema_id),
    CONSTRAINT fk_pt2_usuario FOREIGN KEY (usuario_id) REFERENCES usuario (id) ON DELETE CASCADE,
    CONSTRAINT fk_pt2_tema    FOREIGN KEY (tema_id)    REFERENCES tema    (id) ON DELETE CASCADE
);

-- Actividades de repaso
CREATE TABLE IF NOT EXISTS actividad_repaso (
    id             BIGINT AUTO_INCREMENT PRIMARY KEY,
    usuario_id     BIGINT      NOT NULL,
    tema_id        BIGINT      NOT NULL,
    documento_id   BIGINT,
    tipo           VARCHAR(20) NOT NULL DEFAULT 'EJERCICIO',
    enunciado      LONGTEXT    NOT NULL,
    solucion       LONGTEXT,
    creado_en      DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    actualizado_en DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_ar_usuario  FOREIGN KEY (usuario_id)   REFERENCES usuario   (id) ON DELETE CASCADE,
    CONSTRAINT fk_ar_tema     FOREIGN KEY (tema_id)      REFERENCES tema      (id) ON DELETE CASCADE,
    CONSTRAINT fk_ar_doc      FOREIGN KEY (documento_id) REFERENCES documento (id) ON DELETE SET NULL
);

-- =============================================
--  MIGRACIONES NOTEBOOKLM
-- =============================================

-- Migración: si vienes de una versión anterior con estado 'LISTO' (compatibilidad)
UPDATE documento SET estado_procesado = 'PROCESADO' WHERE estado_procesado = 'LISTO';

-- Conversaciones de chat
CREATE TABLE IF NOT EXISTS chat_conversacion (
    id             BIGINT AUTO_INCREMENT PRIMARY KEY,
    usuario_id     BIGINT       NOT NULL,
    asignatura_id  BIGINT,
    tema_id        BIGINT,
    titulo         VARCHAR(200) NOT NULL,
    fuentes_activas LONGTEXT,
    creado_en      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    actualizado_en DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY idx_chat_conversacion_usuario (usuario_id),
    KEY idx_chat_conversacion_asignatura (asignatura_id),
    KEY idx_chat_conversacion_tema (tema_id),
    CONSTRAINT fk_cc_usuario    FOREIGN KEY (usuario_id)    REFERENCES usuario    (id) ON DELETE CASCADE,
    CONSTRAINT fk_cc_asignatura FOREIGN KEY (asignatura_id) REFERENCES asignatura (id) ON DELETE SET NULL,
    CONSTRAINT fk_cc_tema       FOREIGN KEY (tema_id)       REFERENCES tema        (id) ON DELETE SET NULL
);

-- Mensajes de chat
CREATE TABLE IF NOT EXISTS chat_mensaje (
    id             BIGINT AUTO_INCREMENT PRIMARY KEY,
    conversacion_id BIGINT      NOT NULL,
    rol            VARCHAR(20)  NOT NULL,
    contenido      LONGTEXT     NOT NULL,
    fuentes_usadas LONGTEXT,
    creado_en      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    actualizado_en DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY idx_chat_mensaje_conversacion (conversacion_id),
    CONSTRAINT fk_cm_conversacion FOREIGN KEY (conversacion_id) REFERENCES chat_conversacion (id) ON DELETE CASCADE
);

-- Notas de usuario
CREATE TABLE IF NOT EXISTS nota (
    id             BIGINT AUTO_INCREMENT PRIMARY KEY,
    usuario_id     BIGINT       NOT NULL,
    documento_id   BIGINT,
    chunk_id       BIGINT,
    tema_id        BIGINT,
    titulo         VARCHAR(200) NOT NULL,
    contenido      LONGTEXT     NOT NULL,
    color_hex      VARCHAR(7),
    creado_en      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    actualizado_en DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY idx_nota_usuario (usuario_id),
    KEY idx_nota_documento (documento_id),
    KEY idx_nota_chunk (chunk_id),
    KEY idx_nota_tema (tema_id),
    CONSTRAINT fk_nota_usuario   FOREIGN KEY (usuario_id)   REFERENCES usuario        (id) ON DELETE CASCADE,
    CONSTRAINT fk_nota_documento FOREIGN KEY (documento_id) REFERENCES documento      (id) ON DELETE SET NULL,
    CONSTRAINT fk_nota_chunk     FOREIGN KEY (chunk_id)     REFERENCES documento_chunk (id) ON DELETE SET NULL,
    CONSTRAINT fk_nota_tema      FOREIGN KEY (tema_id)      REFERENCES tema           (id) ON DELETE SET NULL
);

-- Compartición de notebooks (asignaturas)
CREATE TABLE IF NOT EXISTS notebook_compartido (
    id                 BIGINT AUTO_INCREMENT PRIMARY KEY,
    asignatura_id      BIGINT      NOT NULL,
    propietario_id     BIGINT      NOT NULL,
    usuario_invitado_id BIGINT     NOT NULL,
    rol                VARCHAR(10) NOT NULL DEFAULT 'VIEWER',
    creado_en           DATETIME   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    actualizado_en      DATETIME   NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_notebook_compartido_asignatura_invitado (asignatura_id, usuario_invitado_id),
    KEY idx_notebook_compartido_asignatura (asignatura_id),
    KEY idx_notebook_compartido_invitado (usuario_invitado_id),
    CONSTRAINT fk_nc_asignatura  FOREIGN KEY (asignatura_id)       REFERENCES asignatura (id) ON DELETE CASCADE,
    CONSTRAINT fk_nc_propietario FOREIGN KEY (propietario_id)      REFERENCES usuario    (id) ON DELETE CASCADE,
    CONSTRAINT fk_nc_invitado    FOREIGN KEY (usuario_invitado_id) REFERENCES usuario    (id) ON DELETE CASCADE
);

-- Artefactos generados (P2 - esqueleto)
CREATE TABLE IF NOT EXISTS artefacto_generado (
    id             BIGINT AUTO_INCREMENT PRIMARY KEY,
    asignatura_id  BIGINT      NOT NULL,
    tipo           VARCHAR(20) NOT NULL,
    estado         VARCHAR(20) NOT NULL DEFAULT 'PENDIENTE',
    ruta_archivo   VARCHAR(600),
    metadatos      LONGTEXT,
    creado_en      DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    actualizado_en DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY idx_artefacto_generado_asignatura (asignatura_id),
    KEY idx_artefacto_generado_tipo (tipo),
    KEY idx_artefacto_generado_estado (estado),
    CONSTRAINT fk_ag_asignatura FOREIGN KEY (asignatura_id) REFERENCES asignatura (id) ON DELETE CASCADE
);

-- =============================================
--  DATOS INICIALES
--  NOTA: Si usas el DataLoader de Spring, los usuarios
--  se crean/actualizan al arrancar con contraseñas correctas.
--  Si ejecutas solo este SQL, usa el endpoint /dev/reset-admin
--  tras arrancar para corregir la contraseña.
-- =============================================

-- Roles base
INSERT IGNORE INTO rol (nombre, descripcion, creado_en, actualizado_en) VALUES
    ('ADMIN', 'Administrador del sistema', NOW(), NOW()),
    ('USER',  'Usuario estándar',          NOW(), NOW());

-- Usuario admin (contraseña se actualiza por DataLoader o /dev/reset-admin)
INSERT IGNORE INTO usuario (nombre, apellidos, email, password_hash, activo, verificado, creado_en, actualizado_en)
VALUES ('Administrador', 'Easy4You', 'admin@easy4you.com',
        '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p922MY8W0QFH0Y2VqLHBci',
        1, 1, NOW(), NOW());

-- Usuario de prueba (contraseña: Test1234!)
INSERT IGNORE INTO usuario (nombre, apellidos, email, password_hash, activo, verificado, creado_en, actualizado_en)
VALUES ('Carlos', 'García López', 'carlos@easy4you.com',
        '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p922MY8W0QFH0Y2VqLHBci',
        1, 1, NOW(), NOW());

-- Asignar roles
INSERT IGNORE INTO usuario_rol (usuario_id, rol_id)
SELECT u.id, r.id FROM usuario u, rol r
WHERE u.email = 'admin@easy4you.com' AND r.nombre = 'ADMIN';

INSERT IGNORE INTO usuario_rol (usuario_id, rol_id)
SELECT u.id, r.id FROM usuario u, rol r
WHERE u.email = 'admin@easy4you.com' AND r.nombre = 'USER';

INSERT IGNORE INTO usuario_rol (usuario_id, rol_id)
SELECT u.id, r.id FROM usuario u, rol r
WHERE u.email = 'carlos@easy4you.com' AND r.nombre = 'USER';

-- Asignatura de ejemplo para el usuario de prueba
INSERT IGNORE INTO asignatura (usuario_id, nombre, descripcion, color_hex, creado_en, actualizado_en)
SELECT u.id, 'Programación en Java',
       'Fundamentos de programación orientada a objetos con Java',
       '#2F80ED', NOW(), NOW()
FROM usuario u WHERE u.email = 'carlos@easy4you.com';
