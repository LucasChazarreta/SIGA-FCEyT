package ar.edu.unse.siga.persistence.jdbc;

import ar.edu.unse.siga.domain.Insumo;
import ar.edu.unse.siga.domain.Movimiento;
import ar.edu.unse.siga.domain.Usuario;
import ar.edu.unse.siga.persistence.DataSourceFactory;
import ar.edu.unse.siga.persistence.dao.MovimientoDao;

import java.sql.*;
import java.time.LocalDateTime;

public class JdbcMovimientoDao implements MovimientoDao {

    private int stockActual(Connection cn, long insumoId) throws SQLException {
        final String sql = """
                SELECT COALESCE(SUM(CASE WHEN tipo='ENTRADA' THEN cantidad ELSE -cantidad END),0) AS stock
                FROM movimiento WHERE insumo_id = ?
                """;
        try (PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setLong(1, insumoId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt("stock");
                return 0;
            }
        }
    }

    private boolean existeInsumo(Connection cn, long insumoId) throws SQLException {
        try (PreparedStatement ps = cn.prepareStatement("SELECT 1 FROM insumo WHERE id=?")) {
            ps.setLong(1, insumoId);
            try (ResultSet rs = ps.executeQuery()) { return rs.next(); }
        }
    }

    @Override
    public Long registrar(Movimiento m) {
        if (m == null) throw new IllegalArgumentException("Movimiento nulo");
        if (m.getInsumo() == null || m.getInsumo().getId() == null)
            throw new IllegalArgumentException("Debe indicar insumo");
        if (m.getCantidad() == null || m.getCantidad() <= 0)
            throw new IllegalArgumentException("Cantidad debe ser > 0");
        if (!"ENTRADA".equals(m.getTipo()) && !"SALIDA".equals(m.getTipo()))
            throw new IllegalArgumentException("Tipo debe ser ENTRADA o SALIDA");

        final String sql = """
                INSERT INTO movimiento(insumo_id, tipo, cantidad, destino_fuente, fecha, usuario_id)
                VALUES(?,?,?,?,?,?)
                """;

        try (Connection cn = DataSourceFactory.getConnection()) {
            cn.setAutoCommit(false);
            try {
                long insumoId = m.getInsumo().getId();

                if (!existeInsumo(cn, insumoId)) {
                    throw new IllegalStateException("El insumo no existe (id=" + insumoId + ")");
                }

                if ("SALIDA".equals(m.getTipo())) {
                    int actual = stockActual(cn, insumoId);
                    if (m.getCantidad() > actual) {
                        throw new IllegalStateException("Stock insuficiente. Actual=" + actual +
                                ", salida=" + m.getCantidad());
                    }
                }

                try (PreparedStatement ps = cn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                    ps.setLong(1, insumoId);
                    ps.setString(2, m.getTipo());
                    ps.setInt(3, m.getCantidad());
                    ps.setString(4, m.getDestinoFuente());
                    ps.setTimestamp(5, Timestamp.valueOf(m.getFecha() != null ? m.getFecha() : LocalDateTime.now()));
                    Long usuarioId = (m.getUsuario()!=null ? m.getUsuario().getId() : null);
                    if (usuarioId != null) ps.setLong(6, usuarioId); else ps.setNull(6, Types.BIGINT);

                    ps.executeUpdate();
                    try (ResultSet rs = ps.getGeneratedKeys()) {
                        if (rs.next()) {
                            long id = rs.getLong(1);
                            m.setId(id);
                            cn.commit();
                            return id;
                        } else throw new SQLException("No se obtuvo ID generado");
                    }
                }
            } catch (Exception ex) {
                cn.rollback();
                throw ex;
            } finally {
                cn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error registrando movimiento", e);
        }
    }
}
