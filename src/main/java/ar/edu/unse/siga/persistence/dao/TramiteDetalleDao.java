package ar.edu.unse.siga.persistence.dao;

import ar.edu.unse.siga.domain.TramiteDetalle;

import java.sql.Connection;
import java.sql.SQLException;

public interface TramiteDetalleDao {

    Long create(TramiteDetalle detalle, Connection cn) throws SQLException;
}
