-- Agrega columna 'tipo' al maestro de insumos
ALTER TABLE insumo
  ADD COLUMN tipo VARCHAR(20) NOT NULL DEFAULT 'BIEN';

-- Opcional: normalizá valores existentes si fuera necesario
-- UPDATE insumo SET tipo = 'BIEN' WHERE tipo IS NULL OR tipo = '';

