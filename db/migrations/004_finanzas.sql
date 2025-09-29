-- Movimientos de finanzas: ingresos/egresos y resumen simple por fecha
CREATE TABLE IF NOT EXISTS finanza_mov (
  id            BIGINT PRIMARY KEY AUTO_INCREMENT,
  tipo          ENUM('INGRESO','EGRESO') NOT NULL,
  categoria     VARCHAR(100)             NOT NULL,
  monto         DECIMAL(12,2)            NOT NULL,
  fecha         DATE                     NOT NULL,
  referencia    VARCHAR(255)             NULL,
  creado_en     TIMESTAMP                NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_finanza_mov_fecha ON finanza_mov(fecha);
CREATE INDEX idx_finanza_mov_tipo ON finanza_mov(tipo);
CREATE INDEX idx_finanza_mov_categoria ON finanza_mov(categoria);
