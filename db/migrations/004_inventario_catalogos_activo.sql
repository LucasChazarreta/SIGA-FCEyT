-- Agrega bandera de estado a catálogos y solicitante en movimientos
ALTER TABLE categoria
    ADD COLUMN IF NOT EXISTS activo TINYINT(1) NOT NULL DEFAULT 1 AFTER nombre;

ALTER TABLE ubicacion
    ADD COLUMN IF NOT EXISTS activo TINYINT(1) NOT NULL DEFAULT 1 AFTER nombre;

-- Las filas existentes quedan activas
UPDATE categoria SET activo = 1 WHERE activo IS NULL;
UPDATE ubicacion SET activo = 1 WHERE activo IS NULL;

-- Nuevo campo para guardar el solicitante real del movimiento
ALTER TABLE movimiento
    ADD COLUMN IF NOT EXISTS solicitante VARCHAR(150) NULL AFTER destino_fuente;
