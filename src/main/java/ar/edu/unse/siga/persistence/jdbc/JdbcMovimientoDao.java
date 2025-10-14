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
                if (rs.next()) {
                    return rs.getInt("stock");
                }
                return 0;
            }
        }
    }

    private boolean existeInsumo(Connection cn, long insumoId) throws SQLException {
        try (PreparedStatement ps = cn.prepareStatement("SELECT 1 FROM insumo WHERE id=?")) {
            ps.setLong(1, insumoId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    @Override
    public Long registrar(Movimiento m) {
        if (m == null || m.getInsumo() == null || m.getInsumo().getId() == null) {
            throw new IllegalArgumentException("Insumo no especificado.");
        }
        if (m.getCantidad() == null || m.getCantidad() <= 0) {
            throw new IllegalArgumentException("Cantidad inválida.");
        }
        if (m.getTipo() == null || (!m.getTipo().equalsIgnoreCase("ENTRADA") && !m.getTipo().equalsIgnoreCase("SALIDA"))) {
            throw new IllegalArgumentException("Tipo de movimiento inválido.");
        }

        final String insertSql = """
        INSERT INTO movimiento (insumo_id, tipo, cantidad, destino_fuente, fecha, usuario_id)
        VALUES (?, ?, ?, ?, ?, ?)
    """;

        try (Connection cn = DataSourceFactory.getConnection()) {
            boolean originalAutoCommit = cn.getAutoCommit();
            try {
                cn.setAutoCommit(false);

                int actual = stockActual(cn, m.getInsumo().getId());
                if (m.getTipo().equalsIgnoreCase("SALIDA") && m.getCantidad() > actual) {
                    throw new IllegalStateException(
                            String.format(
                                    "No hay suficiente stock para realizar esta salida.%n%n"
                                            + "Cantidad disponible: %d%n"
                                            + "Cantidad solicitada: %d%n%n"
                                            + "Podés registrar una cantidad menor o agregar más stock con una ENTRADA.",
                                    actual, m.getCantidad()
                            )
                    );
                }

                if (m.getFecha() == null) {
                    m.setFecha(java.time.LocalDateTime.now());
                }

                try (PreparedStatement ps = cn.prepareStatement(insertSql, Statement.RETURN_GENERATED_KEYS)) {
                    ps.setLong(1, m.getInsumo().getId());
                    ps.setString(2, m.getTipo().toUpperCase());
                    ps.setInt(3, m.getCantidad());
                    ps.setString(4, m.getDestinoFuente());
                    ps.setTimestamp(5, java.sql.Timestamp.valueOf(m.getFecha()));

                    if (m.getUsuario() != null && m.getUsuario().getId() != null) {
                        ps.setLong(6, m.getUsuario().getId());
                    } else {
                        ps.setNull(6, java.sql.Types.BIGINT);
                    }

                    ps.executeUpdate();
                    try (ResultSet rs = ps.getGeneratedKeys()) {
                        if (rs.next()) {
                            long id = rs.getLong(1);
                            cn.commit();
                            return id;
                        }
                    }
                }

                cn.rollback();
                throw new RuntimeException("No se pudo registrar el movimiento.");
            } catch (SQLException e) {
                try {
                    cn.rollback();
                } catch (SQLException ignored) {
                }
                throw new RuntimeException("Error registrando movimiento", e);
            } catch (RuntimeException e) {
                try {
                    cn.rollback();
                } catch (SQLException ignored) {
                }
                throw e;
            } finally {
                try {
                    cn.setAutoCommit(originalAutoCommit);
                } catch (SQLException ignored) {
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error registrando movimiento", e);
        }
    }

    @Override
    public java.util.List<Movimiento> ultimosPorInsumo(long insumoId, int limit) {
        final String sql = """
            SELECT id, insumo_id, tipo, cantidad, destino_fuente, fecha, usuario_id
            FROM movimiento
            WHERE insumo_id = ?
            ORDER BY fecha DESC, id DESC
            LIMIT ?
            """;
        var lista = new java.util.ArrayList<Movimiento>();
        try (Connection cn = DataSourceFactory.getConnection(); PreparedStatement ps = cn.prepareStatement(sql)) {

            ps.setLong(1, insumoId);
            ps.setInt(2, Math.max(1, limit));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Movimiento m = new Movimiento();
                    m.setId(rs.getLong("id"));

                    // set insumo con solo el id
                    Insumo ins = new Insumo();
                    ins.setId(rs.getLong("insumo_id"));
                    m.setInsumo(ins);

                    m.setTipo(rs.getString("tipo"));
                    m.setCantidad(rs.getInt("cantidad"));
                    m.setDestinoFuente(rs.getString("destino_fuente"));

                    var ts = rs.getTimestamp("fecha");
                    if (ts != null) {
                        m.setFecha(ts.toLocalDateTime());
                    }

                    long uid = rs.getLong("usuario_id");
                    if (!rs.wasNull()) {
                        Usuario u = new Usuario();
                        u.setId(uid);
                        m.setUsuario(u);
                    }

                    lista.add(m);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error consultando últimos movimientos", e);
        }
        return lista;
    }

    @Override
    public int stockActual(long insumoId) {
        final String sql = """
        SELECT COALESCE(SUM(CASE
            WHEN tipo='ENTRADA' THEN cantidad
            WHEN tipo='SALIDA'  THEN -cantidad
            ELSE 0 END), 0) AS stock
        FROM movimiento
        WHERE insumo_id = ?
    """;
        try (Connection cn = DataSourceFactory.getConnection(); PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setLong(1, insumoId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt("stock") : 0;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error calculando stock actual", e);
        }
    }
    
    @Override
public int totalEntradas(long insumoId) {
    final String sql = """
        SELECT COALESCE(SUM(cantidad),0) AS total
        FROM movimiento
        WHERE insumo_id = ? AND UPPER(tipo) = 'ENTRADA'
    """;
    try (Connection cn = DataSourceFactory.getConnection();
         PreparedStatement ps = cn.prepareStatement(sql)) {
        ps.setLong(1, insumoId);
        try (ResultSet rs = ps.executeQuery()) {
            return rs.next() ? rs.getInt("total") : 0;
        }
    } catch (SQLException e) {
        throw new RuntimeException("Error consultando total de ENTRADAS", e);
    }
}

@Override
public int totalSalidas(long insumoId) {
    final String sql = """
        SELECT COALESCE(SUM(cantidad),0) AS total
        FROM movimiento
        WHERE insumo_id = ? AND UPPER(tipo) = 'SALIDA'
    """;
    try (Connection cn = DataSourceFactory.getConnection();
         PreparedStatement ps = cn.prepareStatement(sql)) {
        ps.setLong(1, insumoId);
        try (ResultSet rs = ps.executeQuery()) {
            return rs.next() ? rs.getInt("total") : 0;
        }
    } catch (SQLException e) {
        throw new RuntimeException("Error consultando total de SALIDAS", e);
    }
}


}
