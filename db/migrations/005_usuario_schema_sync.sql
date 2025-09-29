-- Agrega columnas faltantes al esquema de usuario

-- 1) password_hash (si no existe)
ALTER TABLE usuario
  ADD COLUMN IF NOT EXISTS password_hash VARCHAR(255) NULL AFTER username;

-- Si tu tabla tenía 'password' en texto plano, migralo (opcional):
-- UPDATE usuario SET password_hash = password WHERE password_hash IS NULL OR password_hash = '';

-- 2) activo (si no existe)
ALTER TABLE usuario
  ADD COLUMN IF NOT EXISTS activo TINYINT(1) NOT NULL DEFAULT 1 AFTER password_hash;

-- 3) Si existe 'estado' y querés unificar, podés borrarlo o dejarlo:
-- ALTER TABLE usuario DROP COLUMN estado;

-- 4) Si existe rol_id y ya no usás roles, podés dropearlo:
-- ALTER TABLE usuario DROP COLUMN rol_id;
