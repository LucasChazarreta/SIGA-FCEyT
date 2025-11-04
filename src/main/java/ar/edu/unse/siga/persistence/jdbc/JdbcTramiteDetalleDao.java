package ar.edu.unse.siga.persistence.jdbc;

import ar.edu.unse.siga.domain.Insumo;
import ar.edu.unse.siga.domain.TramiteDetalle;
import ar.edu.unse.siga.persistence.DataSourceFactory;
import ar.edu.unse.siga.persistence.dao.TramiteDetalleDao;

import java.math.BigDecimal;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class JdbcTramiteDetalleDao implements TramiteDetalleDao {

    public Long create(TramiteDetalle det) {
        try (Connection cn = DataSourceFactory.getConnection()) {
            return create(det, cn);
        } catch (SQLException e) {
            throw new RuntimeException("Error insertando detalle de trámite", e);
        }
    }

    @Override
    public Long create(TramiteDetalle det, Connection cn) throws SQLException {
        // IMPORTANTE: la tabla tramite_detalle NO tiene columna 'observacion'
        final String sql = """
            INSERT INTO tramite_detalle
                (tramite_id, insumo_id, cantidad)
            VALUES
                (?,          ?,         ?)
            """;
        try (PreparedStatement ps = cn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setLong(1, det.getTramiteId());
            if (det.getInsumo() == null || det.getInsumo().getId() == null) {
                throw new IllegalArgumentException("Detalle inválido: falta insumo");
            }
            ps.setLong(2, det.getInsumo().getId());

            BigDecimal cant = det.getCantidad();
            if (cant == null || cant.signum() <= 0) {
                throw new IllegalArgumentException("Detalle inválido: cantidad debe ser > 0");
            }
            ps.setBigDecimal(3, cant);

            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    long id = rs.getLong(1);
                    det.setId(id);
                    return id;
                }
            }
        }
        throw new SQLException("No se pudo obtener el ID generado para tramite_detalle");
    }


    public List<TramiteDetalle> listByTramiteId(Long tramiteId) {
        final String sql = """
            SELECT td.id, td.tramite_id, td.insumo_id, td.cantidad
            FROM tramite_detalle td
            WHERE td.tramite_id = ?
            ORDER BY td.id
            """;
        try (Connection cn = DataSourceFactory.getConnection();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setLong(1, tramiteId);
            try (ResultSet rs = ps.executeQuery()) {
                List<TramiteDetalle> list = new ArrayList<>();
                while (rs.next()) {
                    TramiteDetalle d = new TramiteDetalle();
                    d.setId(rs.getLong("id"));
                    d.setTramiteId(rs.getLong("tramite_id"));

                    Insumo ins = new Insumo();
                    ins.setId(rs.getLong("insumo_id"));
                    d.setInsumo(ins);

                    d.setCantidad(rs.getBigDecimal("cantidad"));
                    list.add(d);
                }
                return list;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error listando detalles por trámite", e);
        }
    }

    @Override
    public Optional<String> findNombreInsumoByTramiteId(Long tramiteId) {
        if (tramiteId == null) {
            return Optional.empty();
        }
        final String sql = """
            SELECT i.nombre
            FROM tramite_detalle td
            JOIN insumo i ON i.id = td.insumo_id
            WHERE td.tramite_id = ?
            ORDER BY td.id
            LIMIT 1
            """;
        try (Connection cn = DataSourceFactory.getConnection();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setLong(1, tramiteId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.ofNullable(rs.getString(1));
                }
            }
            return Optional.empty();
        } catch (SQLException e) {
            throw new RuntimeException("Error obteniendo nombre de insumo para trámite id=" + tramiteId, e);
        }
    }

    // Si tu interfaz tiene otros métodos (delete/update), podés agregarlos acá.
}
