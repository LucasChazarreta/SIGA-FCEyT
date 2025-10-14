package ar.edu.unse.siga.persistence.jdbc;

import ar.edu.unse.siga.domain.Categoria;
import ar.edu.unse.siga.domain.Insumo;
import ar.edu.unse.siga.persistence.DataSourceFactory;
import ar.edu.unse.siga.persistence.dao.InsumoDao;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class JdbcInsumoDao implements InsumoDao {

    private Insumo mapRow(ResultSet rs) throws SQLException {
        Insumo i = new Insumo();
        i.setId(rs.getLong("id"));
        i.setCodigo(rs.getString("codigo"));
        i.setDescripcion(rs.getString("descripcion"));

        Categoria c = new Categoria();
        c.setId(rs.getInt("categoria_id"));
        c.setNombre(rs.getString("categoria_nombre")); // alias en SELECT
        i.setCategoria(c);

        i.setStockMinimo(rs.getInt("stock_minimo"));
        i.setUbicacion(rs.getString("ubicacion"));
        i.setEstado(rs.getString("estado"));

        // created_at → fechaAlta
        Timestamp ts = safeGetTimestamp(rs, "created_at");
        if (ts != null) {
            i.setCreatedAt(ts.toInstant());
            i.setFechaAlta(ts.toLocalDateTime().toLocalDate());
        }

        Date fa = safeGetDate(rs, "fecha_alta");
        if (fa != null) {
            i.setFechaAlta(fa.toLocalDate());
        }

        return i;
    }

    private Timestamp safeGetTimestamp(ResultSet rs, String col) {
        try {
            return rs.findColumn(col) > 0 ? rs.getTimestamp(col) : null;
        } catch (SQLException ignore) {
            return null;
        }
    }

    private Date safeGetDate(ResultSet rs, String col) {
        try {
            return rs.findColumn(col) > 0 ? rs.getDate(col) : null;
        } catch (SQLException ignore) {
            return null;
        }
    }

    // =================== CREATE ===================
    @Override
    public Long create(Insumo insumo) {
        final String sql = """
            INSERT INTO insumo(codigo, descripcion, categoria_id, stock_minimo, ubicacion, estado)
            VALUES (?, ?, ?, ?, ?, ?)
        """;
        try (Connection cn = DataSourceFactory.getConnection();
             PreparedStatement ps = cn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, insumo.getCodigo());
            ps.setString(2, insumo.getDescripcion());

            // id de categoría es primitivo, no puede ser null
            if (insumo.getCategoria() != null) {
                ps.setLong(3, insumo.getCategoria().getId());
            } else {
                ps.setNull(3, Types.BIGINT);
            }

            ps.setInt(4, insumo.getStockMinimo() == null ? 0 : insumo.getStockMinimo());
            ps.setString(5, insumo.getUbicacion());
            ps.setString(6, insumo.getEstado() == null ? "ACTIVO" : insumo.getEstado());

            ps.executeUpdate();

            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    long id = rs.getLong(1);
                    insumo.setId(id);
                    return id;
                }
            }
            throw new SQLException("No se pudo obtener el ID generado");

        } catch (SQLException e) {
            if (e.getMessage() != null && e.getMessage().contains("Duplicate")) {
                throw new IllegalStateException("El código ya existe: " + insumo.getCodigo(), e);
            }
            throw new RuntimeException("Error creando insumo", e);
        }
    }

    // =================== UPDATE ===================
    @Override
    public void update(Insumo insumo) {
        final String sql = """
            UPDATE insumo
            SET codigo = ?,
                descripcion = ?,
                categoria_id = ?,
                stock_minimo = ?,
                ubicacion = ?,
                estado = ?
            WHERE id = ?
        """;
        try (Connection cn = DataSourceFactory.getConnection();
             PreparedStatement ps = cn.prepareStatement(sql)) {

            ps.setString(1, insumo.getCodigo());
            ps.setString(2, insumo.getDescripcion());

            // id de categoría es primitivo, no puede ser null
            if (insumo.getCategoria() != null) {
                ps.setLong(3, insumo.getCategoria().getId());
            } else {
                ps.setNull(3, Types.BIGINT);
            }

            ps.setInt(4, insumo.getStockMinimo() == null ? 0 : insumo.getStockMinimo());
            ps.setString(5, insumo.getUbicacion());
            ps.setString(6, insumo.getEstado() == null ? "ACTIVO" : insumo.getEstado());
            ps.setLong(7, insumo.getId());

            ps.executeUpdate();

        } catch (SQLException e) {
            throw new RuntimeException("Error actualizando insumo id=" + insumo.getId(), e);
        }
    }

    // =================== SOFT DELETE ===================
    @Override
    public void softDelete(Long id) {
        final String sql = "UPDATE insumo SET estado='INACTIVO' WHERE id=?";
        try (Connection cn = DataSourceFactory.getConnection();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setLong(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error en baja lógica de insumo id=" + id, e);
        }
    }

    // =================== FIND BY CODIGO ===================
    @Override
    public Optional<Insumo> findByCodigo(String codigo) {
        final String sql = """
            SELECT i.*, c.nombre AS categoria_nombre
            FROM insumo i
            JOIN categoria c ON c.id = i.categoria_id
            WHERE i.codigo = ?
        """;
        try (Connection cn = DataSourceFactory.getConnection();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setString(1, codigo);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(mapRow(rs));
                return Optional.empty();
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error buscando insumo por código: " + codigo, e);
        }
    }

    // =================== LIST ALL ===================
    @Override
    public List<Insumo> listAll() {
        final String sql = """
            SELECT i.*, c.nombre AS categoria_nombre
            FROM insumo i
            JOIN categoria c ON c.id = i.categoria_id
            ORDER BY i.id DESC
        """;
        try (Connection cn = DataSourceFactory.getConnection();
             PreparedStatement ps = cn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            List<Insumo> list = new ArrayList<>();
            while (rs.next()) list.add(mapRow(rs));
            return list;

        } catch (SQLException e) {
            throw new RuntimeException("Error listando insumos", e);
        }
    }
}
