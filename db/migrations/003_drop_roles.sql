-- Quitar roles del esquema
ALTER TABLE usuario DROP COLUMN IF EXISTS rol_id;
DROP TABLE IF EXISTS rol;
