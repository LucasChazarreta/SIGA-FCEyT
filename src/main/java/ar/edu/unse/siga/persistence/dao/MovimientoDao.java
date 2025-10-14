package ar.edu.unse.siga.persistence.dao;

import ar.edu.unse.siga.domain.Movimiento;
import java.util.List;

public interface MovimientoDao {

    Long registrar(Movimiento movimiento);

    // ya lo agregamos antes:
    java.util.List<Movimiento> ultimosPorInsumo(long insumoId, int limit);

    // NUEVO: consultar stock actual de un insumo
    int stockActual(long insumoId);
    // Totales por insumo
int totalEntradas(long insumoId);
int totalSalidas(long insumoId);

}
