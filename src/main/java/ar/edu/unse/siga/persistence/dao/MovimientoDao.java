package ar.edu.unse.siga.persistence.dao;

import ar.edu.unse.siga.domain.Movimiento;

public interface MovimientoDao {
    Long registrar(Movimiento movimiento);
}
