package ar.edu.unse.siga.persistence.dao;

import ar.edu.unse.siga.domain.TramiteDetalle;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Optional;

public interface TramiteDetalleDao {

    Long create(TramiteDetalle detalle, Connection cn) throws SQLException;

    Optional<String> findNombreInsumoByTramiteId(Long tramiteId);
}
