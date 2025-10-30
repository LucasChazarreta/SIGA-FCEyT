-- ==========================================================
--  H2 TEST SCHEMA – IDEMPOTENTE (NO DESTRUCTIVO)
--  Archivo: src/test/resources/schema-h2.sql
--  Importante: NO usar DROP aquí, porque INIT se ejecuta en cada conexión.
-- ==========================================================

SET MODE MySQL;
SET IGNORECASE TRUE;

-- Categoría
CREATE TABLE IF NOT EXISTS categoria (
    id        BIGINT AUTO_INCREMENT PRIMARY KEY,
    nombre    VARCHAR(100) NOT NULL,
    activo    BOOLEAN      NOT NULL DEFAULT TRUE
);

-- Insumo
CREATE TABLE IF NOT EXISTS insumo (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    codigo        VARCHAR(80)  NOT NULL UNIQUE,
    descripcion   VARCHAR(255),
    categoria_id  BIGINT       NOT NULL,
    stock_minimo  INT          NOT NULL DEFAULT 0,
    ubicacion     VARCHAR(120),
    estado        VARCHAR(30),
    tipo          VARCHAR(30),
    stock_actual  DECIMAL(18,2) NOT NULL DEFAULT 100,
    activo        BOOLEAN       NOT NULL DEFAULT TRUE
);

-- FK (H2 crea la tabla sin FK si ya existía; aseguramos las claves luego)
ALTER TABLE insumo
    ADD CONSTRAINT IF NOT EXISTS fk_insumo_categoria
    FOREIGN KEY (categoria_id) REFERENCES categoria(id);

-- Trámite
CREATE TABLE IF NOT EXISTS tramite (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    nro          VARCHAR(40),
    asunto       VARCHAR(200),
    descripcion  VARCHAR(500),
    solicitante  VARCHAR(120),
    destino      VARCHAR(120),
    estado       VARCHAR(40),
    fecha        TIMESTAMP DEFAULT CURRENT_TIMESTAMP   -- 👈 faltaba esta columna
);

-- Movimiento
CREATE TABLE IF NOT EXISTS movimiento (
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    insumo_id  BIGINT NOT NULL,
    tipo       VARCHAR(20) NOT NULL,     -- ENTRADA / SALIDA
    cantidad   INT         NOT NULL,
    fecha      TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP
);

ALTER TABLE movimiento
    ADD CONSTRAINT IF NOT EXISTS fk_mov_insumo
    FOREIGN KEY (insumo_id) REFERENCES insumo(id);

-- Detalle de trámite
CREATE TABLE IF NOT EXISTS tramite_detalle (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    tramite_id  BIGINT NOT NULL,
    insumo_id   BIGINT NOT NULL,
    cantidad    INT    NOT NULL,
    observacion VARCHAR(255)
);

ALTER TABLE tramite_detalle
    ADD CONSTRAINT IF NOT EXISTS fk_td_tramite
    FOREIGN KEY (tramite_id) REFERENCES tramite(id);

ALTER TABLE tramite_detalle
    ADD CONSTRAINT IF NOT EXISTS fk_td_insumo
    FOREIGN KEY (insumo_id)  REFERENCES insumo(id);

-- Migración idempotente por si la tabla ya existía sin esta columna
ALTER TABLE tramite_detalle
    ADD COLUMN IF NOT EXISTS observacion VARCHAR(255);


-- Índices (idempotentes)
CREATE INDEX IF NOT EXISTS idx_insumo_categoria ON insumo(categoria_id);
CREATE INDEX IF NOT EXISTS idx_mov_insumo      ON movimiento(insumo_id);
CREATE INDEX IF NOT EXISTS idx_td_tramite      ON tramite_detalle(tramite_id);
CREATE INDEX IF NOT EXISTS idx_td_insumo       ON tramite_detalle(insumo_id);

-- Seed mínimo, también idempotente
MERGE INTO categoria (id, nombre, activo) KEY(id)
VALUES (1, 'General', TRUE);

-- NOTA:
-- - No insertamos filas en 'tramite' ni en 'movimiento' para no interferir
--   con tests que esperan conteos en cero.
-- - stock_actual tiene DEFAULT 100 para que los tests de salida no fallen
--   por stock insuficiente al crear insumos sin movimientos previos.
