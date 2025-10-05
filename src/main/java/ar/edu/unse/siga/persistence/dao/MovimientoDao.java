package ar.edu.unse.siga.persistence.dao;

import ar.edu.unse.siga.domain.Movimiento;
import java.util.List;

public interface MovimientoDao {
    Long registrar(Movimiento movimiento);

    /** Últimos movimientos de un insumo (ordenados por fecha desc). */
    List<Movimiento> ultimosPorInsumo(long insumoId, int limit);
}
