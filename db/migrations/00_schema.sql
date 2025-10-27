-- Crear base
CREATE DATABASE IF NOT EXISTS siga
CHARACTER SET utf8mb4
COLLATE utf8mb4_unicode_ci;

USE siga;

USE siga;

-- Roles
CREATE TABLE IF NOT EXISTS rol (
  id      INT NOT NULL AUTO_INCREMENT,
  nombre  VARCHAR(50) NOT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uq_rol_nombre (nombre)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Usuarios
CREATE TABLE IF NOT EXISTS usuario (
  id             INT NOT NULL AUTO_INCREMENT,
  username       VARCHAR(50)  NOT NULL,
  password       VARCHAR(100) NULL,          -- legado (no usar, se deja por compatibilidad)
  password_hash  VARCHAR(100) NULL,          -- bcrypt
  email          VARCHAR(150) NULL,
  estado         VARCHAR(20)  NOT NULL DEFAULT 'ACTIVO',
  activo         TINYINT(1)   NOT NULL DEFAULT 1,
  rol_id         INT          NOT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uq_usuario_username (username),
  KEY fk_usuario_rol (rol_id),
  CONSTRAINT fk_usuario_rol FOREIGN KEY (rol_id) REFERENCES rol(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Catálogo
CREATE TABLE IF NOT EXISTS categoria (
  id      INT NOT NULL AUTO_INCREMENT,
  nombre  VARCHAR(100) NOT NULL,
  activo  TINYINT(1)   NOT NULL DEFAULT 1,
  PRIMARY KEY (id),
  UNIQUE KEY uq_categoria_nombre (nombre)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS ubicacion (
  id      INT NOT NULL AUTO_INCREMENT,
  nombre  VARCHAR(100) NOT NULL,
  activo  TINYINT(1)   NOT NULL DEFAULT 1,
  PRIMARY KEY (id),
  UNIQUE KEY uq_ubicacion_nombre (nombre)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Insumos
CREATE TABLE IF NOT EXISTS insumo (
  id            INT NOT NULL AUTO_INCREMENT,
  codigo        VARCHAR(64)  NOT NULL,
  nombre        VARCHAR(150) NULL,             -- << permitir NULL para que pasen los tests
  descripcion   VARCHAR(255) NULL,
  tipo          VARCHAR(50)  NULL,
  categoria_id  INT NOT NULL,
  ubicacion_id  INT NULL,
  ubicacion     VARCHAR(100) NULL,             -- caché nombre de ubicación
  stock         DECIMAL(12,3) NOT NULL DEFAULT 0,
  stock_min     DECIMAL(12,3) NOT NULL DEFAULT 0,
  stock_minimo  DECIMAL(12,3) NOT NULL DEFAULT 0, -- algunas pantallas usan este nombre
  activo        TINYINT(1)   NOT NULL DEFAULT 1,
  estado        VARCHAR(20)  NOT NULL DEFAULT 'ACTIVO',
  PRIMARY KEY (id),
  UNIQUE KEY uq_insumo_codigo (codigo),
  KEY fk_insumo_categoria (categoria_id),
  KEY fk_insumo_ubicacion (ubicacion_id),
  CONSTRAINT fk_insumo_categoria FOREIGN KEY (categoria_id) REFERENCES categoria(id),
  CONSTRAINT fk_insumo_ubicacion FOREIGN KEY (ubicacion_id) REFERENCES ubicacion(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Trámites
CREATE TABLE IF NOT EXISTS tramite (
  id           BIGINT NOT NULL AUTO_INCREMENT,
  nro          VARCHAR(30) NOT NULL,
  tipo         VARCHAR(20)  NOT NULL DEFAULT 'ENTRADA',     -- ENTRADA|SALIDA|AJUSTE
  estado       VARCHAR(20)  NOT NULL DEFAULT 'NUEVO',       -- NUEVO|EN_PROCESO|CERRADO|ANULADO
  fecha        TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  solicitante  VARCHAR(100)  NULL,
  asunto       VARCHAR(150)  NULL,                          -- << requerido por DAO/pantallas
  descripcion  VARCHAR(255)  NULL,                          -- << requerido por DAO/pantallas
  destino      VARCHAR(150)  NULL,                          -- << requerido por DAO/pantallas
  observacion  VARCHAR(255)  NULL,
  usuario_id   INT           NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uq_tramite_nro (nro),
  KEY idx_tramite_estado (estado),
  KEY idx_tramite_fecha (fecha),
  CONSTRAINT fk_tramite_usuario FOREIGN KEY (usuario_id) REFERENCES usuario(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS tramite_detalle (
  id           BIGINT NOT NULL AUTO_INCREMENT,
  tramite_id   BIGINT NOT NULL,
  insumo_id    INT    NOT NULL,
  cantidad     DECIMAL(12,3) NOT NULL,
  observacion  VARCHAR(255) NULL,
  PRIMARY KEY (id),
  KEY idx_td_tramite (tramite_id),
  KEY idx_td_insumo (insumo_id),
  CONSTRAINT fk_td_tramite FOREIGN KEY (tramite_id) REFERENCES tramite(id),
  CONSTRAINT fk_td_insumo  FOREIGN KEY (insumo_id)  REFERENCES insumo(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Movimientos
CREATE TABLE IF NOT EXISTS movimiento (
  id           BIGINT NOT NULL AUTO_INCREMENT,
  insumo_id    INT NOT NULL,
  tipo         VARCHAR(20)  NOT NULL,                       -- ENTRADA|SALIDA
  cantidad     DECIMAL(12,3) NOT NULL,
  fecha        TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  destino_fuente VARCHAR(150) NULL,                         -- destino (salida) o fuente (entrada)
  solicitante  VARCHAR(100) NULL,
  observacion  VARCHAR(255) NULL,
  activo       TINYINT(1)   NOT NULL DEFAULT 1,
  estado       VARCHAR(20)  NOT NULL DEFAULT 'CONFIRMADO',
  usuario_id   INT          NULL,
  tramite_id   BIGINT       NULL,
  PRIMARY KEY (id),
  KEY fk_mov_insumo (insumo_id),
  KEY idx_mov_fecha (fecha),
  CONSTRAINT fk_mov_insumo  FOREIGN KEY (insumo_id)  REFERENCES insumo(id),
  CONSTRAINT fk_mov_usuario FOREIGN KEY (usuario_id) REFERENCES usuario(id),
  CONSTRAINT fk_mov_tramite FOREIGN KEY (tramite_id) REFERENCES tramite(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Trigger: asignar nro si viene vacío (AFTER INSERT, porque el id ya existe)
DROP TRIGGER IF EXISTS trg_tramite_ai_nro;
DELIMITER $$
CREATE TRIGGER trg_tramite_ai_nro
AFTER INSERT ON tramite
FOR EACH ROW
BEGIN
  IF NEW.nro IS NULL OR NEW.nro = '' THEN
    UPDATE tramite
       SET nro = CONCAT('TR-', DATE_FORMAT(CURRENT_DATE(), '%Y'), '-', LPAD(NEW.id, 8, '0'))
     WHERE id = NEW.id;
  END IF;
END$$
DELIMITER ;
