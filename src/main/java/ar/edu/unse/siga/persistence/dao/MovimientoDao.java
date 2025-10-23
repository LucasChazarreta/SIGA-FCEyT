package ar.edu.unse.siga.persistence.dao;

import ar.edu.unse.siga.domain.Movimiento;
import java.util.List;

public interface MovimientoDao {

    Long registrar(Movimiento movimiento);

    // ya lo agregamos antes:
    java.util.List<Movimiento> ultimosPorInsumo(long insumoId, int limit);

    java.math.BigDecimal stockActual(long insumoId);
    java.math.BigDecimal totalEntradas(long insumoId);
    java.math.BigDecimal totalSalidas(long insumoId);

    java.util.List<Movimiento> buscarPorFechaYTipo(java.time.LocalDateTime desde,
                                                   java.time.LocalDateTime hasta,
                                                   String tipo);

}
