package ar.edu.unse.siga.persistence.jdbc;

import ar.edu.unse.siga.domain.Insumo;
import ar.edu.unse.siga.domain.Movimiento;
import ar.edu.unse.siga.domain.Usuario;
import ar.edu.unse.siga.persistence.DataSourceFactory;
import ar.edu.unse.siga.persistence.dao.MovimientoDao;

import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class JdbcMovimientoDao implements MovimientoDao {

    private static final String ENTRADA = "ENTRADA";
    private static final String SALIDA = "SALIDA";

    private BigDecimal stockActual(Connection cn, long insumoId) throws SQLException {
        final String sql = """
                SELECT COALESCE(SUM(CASE WHEN tipo='ENTRADA' THEN cantidad ELSE -cantidad END),0) AS stock
                FROM movimiento WHERE insumo_id = ?
                """;
        try (PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setLong(1, insumoId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    BigDecimal stock = rs.getBigDecimal("stock");
                    return stock == null ? BigDecimal.ZERO : stock;
                }
                return BigDecimal.ZERO;
            }
        }
    }

    @Override
    public Long registrar(Movimiento m) {
        if (m == null || m.getInsumo() == null || m.getInsumo().getId() == null) {
            throw new IllegalArgumentException("Insumo no especificado.");
        }
        if (m.getCantidad() == null || m.getCantidad().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Cantidad inválida.");
        }
        if (m.getTipo() == null
                || (!ENTRADA.equalsIgnoreCase(m.getTipo()) && !SALIDA.equalsIgnoreCase(m.getTipo()))) {
            throw new IllegalArgumentException("Tipo de movimiento inválido.");
        }

        final String insertSql = """
            INSERT INTO movimiento (insumo_id, tipo, cantidad, destino_fuente, solicitante, fecha, usuario_id)
            VALUES (?, ?, ?, ?, ?, ?, ?)
        """;

        try (Connection cn = DataSourceFactory.getConnection()) {
            boolean originalAutoCommit = cn.getAutoCommit();
            try {
                cn.setAutoCommit(false);

                BigDecimal actual = stockActual(cn, m.getInsumo().getId());
                if (SALIDA.equalsIgnoreCase(m.getTipo()) && m.getCantidad().compareTo(actual) > 0) {
                    throw new IllegalStateException(String.format(
                            "No hay suficiente stock para realizar esta salida.%n%n" +
                                    "Cantidad disponible: %s%nCantidad solicitada: %s%n%n" +
                                    "Podés registrar una cantidad menor o agregar más stock con una ENTRADA.",
                            actual.stripTrailingZeros().toPlainString(),
                            m.getCantidad().stripTrailingZeros().toPlainString()));
                }

                if (m.getFecha() == null) {
                    m.setFecha(LocalDateTime.now());
                }

                try (PreparedStatement ps = cn.prepareStatement(insertSql, Statement.RETURN_GENERATED_KEYS)) {
                    ps.setLong(1, m.getInsumo().getId());
                    ps.setString(2, m.getTipo().toUpperCase());
                    ps.setBigDecimal(3, m.getCantidad());
                    ps.setString(4, m.getDestinoFuente());
                    ps.setString(5, m.getSolicitante());
                    ps.setTimestamp(6, Timestamp.valueOf(m.getFecha()));

                    if (m.getUsuario() != null && m.getUsuario().getId() != null) {
                        ps.setLong(7, m.getUsuario().getId());
                    } else {
                        ps.setNull(7, Types.BIGINT);
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
                try { cn.rollback(); } catch (SQLException ignored) {}
                throw new RuntimeException("Error registrando movimiento", e);
            } catch (RuntimeException e) {
                try { cn.rollback(); } catch (SQLException ignored) {}
                throw e;
            } finally {
                try { cn.setAutoCommit(originalAutoCommit); } catch (SQLException ignored) {}
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error registrando movimiento", e);
        }
    }

    @Override
    public List<Movimiento> ultimosPorInsumo(long insumoId, int limit) {
        final String sql = """
            SELECT id, insumo_id, tipo, cantidad, destino_fuente, solicitante, fecha, usuario_id
            FROM movimiento
            WHERE insumo_id = ?
            ORDER BY fecha DESC, id DESC
            LIMIT ?
        """;
        var lista = new ArrayList<Movimiento>();
        try (Connection cn = DataSourceFactory.getConnection();
             PreparedStatement ps = cn.prepareStatement(sql)) {

            ps.setLong(1, insumoId);
            ps.setInt(2, Math.max(1, limit));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Movimiento m = new Movimiento();
                    m.setId(rs.getLong("id"));

                    Insumo ins = new Insumo();
                    ins.setId(rs.getLong("insumo_id"));
                    m.setInsumo(ins);

                    m.setTipo(rs.getString("tipo"));
                    BigDecimal cant = rs.getBigDecimal("cantidad");
                    m.setCantidad(cant == null ? BigDecimal.ZERO : cant);
                    m.setDestinoFuente(rs.getString("destino_fuente"));
                    m.setSolicitante(rs.getString("solicitante"));

                    Timestamp ts = rs.getTimestamp("fecha");
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
    public BigDecimal stockActual(long insumoId) {
        try (Connection cn = DataSourceFactory.getConnection()) {
            return stockActual(cn, insumoId);
        } catch (SQLException e) {
            throw new RuntimeException("Error calculando stock actual", e);
        }
    }

    @Override
    public BigDecimal totalEntradas(long insumoId) {
        final String sql = """
            SELECT COALESCE(SUM(cantidad),0) AS total
            FROM movimiento
            WHERE insumo_id = ? AND UPPER(tipo) = 'ENTRADA'
        """;
        try (Connection cn = DataSourceFactory.getConnection();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setLong(1, insumoId);
            try (ResultSet rs = ps.executeQuery()) {
                BigDecimal total = rs.next() ? rs.getBigDecimal("total") : BigDecimal.ZERO;
                return total == null ? BigDecimal.ZERO : total;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error consultando total de ENTRADAS", e);
        }
    }

    @Override
    public BigDecimal totalSalidas(long insumoId) {
        final String sql = """
            SELECT COALESCE(SUM(cantidad),0) AS total
            FROM movimiento
            WHERE insumo_id = ? AND UPPER(tipo) = 'SALIDA'
        """;
        try (Connection cn = DataSourceFactory.getConnection();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setLong(1, insumoId);
            try (ResultSet rs = ps.executeQuery()) {
                BigDecimal total = rs.next() ? rs.getBigDecimal("total") : BigDecimal.ZERO;
                return total == null ? BigDecimal.ZERO : total;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error consultando total de SALIDAS", e);
        }
    }

    @Override
    public List<Movimiento> buscarPorFechaYTipo(LocalDateTime desde, LocalDateTime hasta, String tipo) {
        StringBuilder sql = new StringBuilder("""
            SELECT m.id, m.insumo_id, m.tipo, m.cantidad, m.destino_fuente, m.solicitante, m.fecha, m.usuario_id,
                   i.codigo, i.descripcion, i.ubicacion, i.tipo AS insumo_tipo, i.estado
            FROM movimiento m
            JOIN insumo i ON i.id = m.insumo_id
            WHERE 1 = 1
        """);

        List<Object> params = new ArrayList<>();

        if (tipo != null && !tipo.isBlank() && !"TODOS".equalsIgnoreCase(tipo)) {
            sql.append(" AND UPPER(m.tipo) = ?");
            params.add(tipo.trim().toUpperCase());
        }
        if (desde != null) {
            sql.append(" AND m.fecha >= ?");
            params.add(Timestamp.valueOf(desde));
        }
        if (hasta != null) {
            sql.append(" AND m.fecha <= ?");
            params.add(Timestamp.valueOf(hasta));
        }

        sql.append(" ORDER BY m.fecha DESC, m.id DESC");

        var lista = new ArrayList<Movimiento>();
        try (Connection cn = DataSourceFactory.getConnection();
             PreparedStatement ps = cn.prepareStatement(sql.toString())) {

            for (int i = 0; i < params.size(); i++) {
                Object value = params.get(i);
                if (value instanceof Timestamp ts) {
                    ps.setTimestamp(i + 1, ts);
                } else if (value instanceof String s) {
                    ps.setString(i + 1, s);
                } else {
                    ps.setObject(i + 1, value);
                }
            }

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Movimiento m = new Movimiento();
                    m.setId(rs.getLong("id"));
                    m.setTipo(rs.getString("tipo"));
                    BigDecimal cant = rs.getBigDecimal("cantidad");
                    m.setCantidad(cant == null ? BigDecimal.ZERO : cant);
                    m.setDestinoFuente(rs.getString("destino_fuente"));
                    m.setSolicitante(rs.getString("solicitante"));
                    Timestamp ts = rs.getTimestamp("fecha");
                    if (ts != null) {
                        m.setFecha(ts.toLocalDateTime());
                    }

                    long usuarioId = rs.getLong("usuario_id");
                    if (!rs.wasNull()) {
                        Usuario u = new Usuario();
                        u.setId(usuarioId);
                        m.setUsuario(u);
                    }

                    Insumo ins = new Insumo();
                    ins.setId(rs.getLong("insumo_id"));
                    ins.setCodigo(rs.getString("codigo"));
                    ins.setDescripcion(rs.getString("descripcion"));
                    ins.setUbicacion(rs.getString("ubicacion"));
                    ins.setTipo(rs.getString("insumo_tipo"));
                    ins.setEstado(rs.getString("estado"));
                    m.setInsumo(ins);

                    lista.add(m);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error buscando movimientos", e);
        }
        return lista;
    }
}
