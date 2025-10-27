package ar.edu.unse.siga.persistence.jdbc;

import ar.edu.unse.siga.domain.TramiteDetalle;
import ar.edu.unse.siga.persistence.dao.TramiteDetalleDao;

import java.sql.*;

public class JdbcTramiteDetalleDao implements TramiteDetalleDao {

    @Override
    public Long create(TramiteDetalle detalle, Connection cn) throws SQLException {
        if (detalle == null) {
            throw new IllegalArgumentException("Detalle inválido");
        }
        if (detalle.getTramiteId() == null) {
            throw new IllegalArgumentException("Trámite requerido");
        }
        if (detalle.getInsumo() == null || detalle.getInsumo().getId() == null) {
            throw new IllegalArgumentException("Insumo requerido");
        }
        if (detalle.getCantidad() == null || detalle.getCantidad().signum() <= 0) {
            throw new IllegalArgumentException("Cantidad inválida");
        }

        final String sql = """
            INSERT INTO tramite_detalle (tramite_id, insumo_id, cantidad, observacion)
            VALUES (?, ?, ?, ?)
        """;

        try (PreparedStatement ps = cn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setLong(1, detalle.getTramiteId());
            ps.setLong(2, detalle.getInsumo().getId());
            ps.setBigDecimal(3, detalle.getCantidad());
            if (detalle.getObservacion() == null || detalle.getObservacion().isBlank()) {
                ps.setNull(4, Types.VARCHAR);
            } else {
                ps.setString(4, detalle.getObservacion());
            }
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    long id = rs.getLong(1);
                    detalle.setId(id);
                    return id;
                }
            }
        }
        throw new SQLException("No se pudo generar el ID del detalle del trámite");
    }
}
