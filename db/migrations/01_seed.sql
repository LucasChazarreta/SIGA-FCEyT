USE siga;

-- Rol ADMIN
INSERT INTO rol (nombre)
SELECT 'ADMIN' WHERE NOT EXISTS (SELECT 1 FROM rol WHERE nombre='ADMIN');

-- Ubicación + Categoría base
INSERT INTO ubicacion (nombre, activo)
SELECT 'Depósito Principal', 1
WHERE NOT EXISTS (SELECT 1 FROM ubicacion WHERE nombre='Depósito Principal');

INSERT INTO categoria (nombre, activo)
SELECT 'Genérica', 1
WHERE NOT EXISTS (SELECT 1 FROM categoria WHERE nombre='Genérica');

-- Insumo semilla (opcional)
INSERT INTO insumo
  (codigo, nombre, descripcion, tipo, categoria_id, ubicacion_id, ubicacion,
   stock, stock_min, stock_minimo, activo, estado)
SELECT
  'SKU-000001', 'Insumo demo', 'Semilla de prueba', 'INSUMO',
  c.id, u.id, 'Depósito Principal',
  10, 2, 2, 1, 'ACTIVO'
FROM categoria c, ubicacion u
WHERE c.nombre='Genérica' AND u.nombre='Depósito Principal'
  AND NOT EXISTS (SELECT 1 FROM insumo WHERE codigo='SKU-000001');

-- Movimiento semilla (sin usuario para evitar FK si todavía no existe admin)
SET @insumo_id := (SELECT id FROM insumo WHERE codigo='SKU-000001' LIMIT 1);
INSERT INTO movimiento (insumo_id, tipo, cantidad, fecha, solicitante, observacion, activo, estado, usuario_id, tramite_id)
SELECT @insumo_id, 'ENTRADA', 3.000, NOW(), 'Usuario Demo', 'Alta inicial', 1, 'CONFIRMADO', NULL, NULL
WHERE @insumo_id IS NOT NULL;
