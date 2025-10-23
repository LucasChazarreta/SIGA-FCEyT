-- tipo de ítem
ALTER TABLE insumo
  ADD COLUMN tipo ENUM('INSUMO','BIEN') NOT NULL DEFAULT 'INSUMO';

-- catálogo de ubicaciones
CREATE TABLE IF NOT EXISTS ubicacion (
  id INT AUTO_INCREMENT PRIMARY KEY,
  nombre VARCHAR(100) UNIQUE NOT NULL
);

INSERT IGNORE INTO ubicacion(nombre) VALUES
 ('Sede Central'),('El Zanjón'),('Parque Industrial');

-- permitir cantidades fraccionadas en movimientos
ALTER TABLE movimiento
  MODIFY COLUMN cantidad DECIMAL(12,2) NOT NULL;
