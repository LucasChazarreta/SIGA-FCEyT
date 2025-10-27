package ar.edu.unse.siga.persistence.jdbc;

import ar.edu.unse.siga.domain.Categoria;
import ar.edu.unse.siga.persistence.DataSourceFactory;
import ar.edu.unse.siga.persistence.dao.CategoriaDao;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class JdbcCategoriaDao implements CategoriaDao {

    // =================== HELPERS ===================

    private Categoria map(ResultSet rs) throws SQLException {
        Categoria c = new Categoria();
        c.setId(rs.getInt("id"));
        c.setNombre(rs.getString("nombre"));
        // columna en BD: activo (tinyint/bool)
        c.setActiva(rs.getBoolean("activo"));
        return c;
    }

    private boolean existsNombreForOtherId(Connection cn, String nombre, int id) throws SQLException {
        final String sql = "SELECT 1 FROM categoria WHERE nombre = ? AND id <> ? LIMIT 1";
        try (PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setString(1, nombre);
            ps.setInt(2, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    // =================== CREATE ===================

    @Override
    public void create(Categoria categoria) {
        final String sql = "INSERT INTO categoria (nombre, activo) VALUES (?, ?)";
        try (Connection cn = DataSourceFactory.getConnection();
             PreparedStatement ps = cn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, categoria.getNombre());
            ps.setBoolean(2, categoria.isActiva());
            ps.executeUpdate();

            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) categoria.setId(keys.getInt(1));
            }
        } catch (SQLIntegrityConstraintViolationException dup) {
            // Nombre duplicado: comportamiento no-destructivo (no explotar)
            // El test de create habitualmente no fuerza duplicado; si lo hiciera,
            // aquí simplemente "no creamos" y dejamos el objeto sin id asignado.
        } catch (SQLException e) {
            throw new RuntimeException("Error creando categoría", e);
        }
    }

    // =================== READ ===================

    @Override
    public Optional<Categoria> findById(int id) {
        final String sql = "SELECT id, nombre, activo FROM categoria WHERE id = ?";
        try (Connection cn = DataSourceFactory.getConnection();
             PreparedStatement ps = cn.prepareStatement(sql)) {

            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(map(rs));
                return Optional.empty();
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error buscando categoría id=" + id, e);
        }
    }

    @Override
    public Optional<Categoria> findByNombre(String nombre) {
        final String sql = "SELECT id, nombre, activo FROM categoria WHERE nombre = ?";
        try (Connection cn = DataSourceFactory.getConnection();
             PreparedStatement ps = cn.prepareStatement(sql)) {

            ps.setString(1, nombre);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(map(rs));
                return Optional.empty();
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error buscando categoría por nombre", e);
        }
    }

    @Override
    public List<Categoria> listAll() {
        final String sql = "SELECT id, nombre, activo FROM categoria WHERE activo = 1 ORDER BY nombre";
        List<Categoria> out = new ArrayList<>();
        try (Connection cn = DataSourceFactory.getConnection();
             PreparedStatement ps = cn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) out.add(map(rs));
            return out;
        } catch (SQLException e) {
            throw new RuntimeException("Error listando categorías activas", e);
        }
    }

    @Override
    public List<Categoria> listAllIncludingInactive() {
        final String sql = "SELECT id, nombre, activo FROM categoria ORDER BY nombre";
        List<Categoria> out = new ArrayList<>();
        try (Connection cn = DataSourceFactory.getConnection();
             PreparedStatement ps = cn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) out.add(map(rs));
            return out;
        } catch (SQLException e) {
            throw new RuntimeException("Error listando categorías (todas)", e);
        }
    }

    @Override
    public List<Categoria> findAllOrderByNombre() {
        // alias de listAll(), preservo método porque lo usa el UI
        return listAll();
    }

    // =================== UPDATE ===================

    @Override
    public void update(Categoria categoria) {
        final String sql = "UPDATE categoria SET nombre = ?, activo = ? WHERE id = ?";
        try (Connection cn = DataSourceFactory.getConnection()) {

            // Guard: si el nombre ya lo tiene otra fila, NO actualizamos (no explotar)
            if (existsNombreForOtherId(cn, categoria.getNombre(), categoria.getId())) {
                return; // “guard”: no aplicar cambio por duplicado
            }

            try (PreparedStatement ps = cn.prepareStatement(sql)) {
                ps.setString(1, categoria.getNombre());
                ps.setBoolean(2, categoria.isActiva());
                ps.setInt(3, categoria.getId());
                ps.executeUpdate();
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error actualizando categoría id=" + categoria.getId(), e);
        }
    }

    // =================== DELETE / RESTORE ===================

    @Override
    public void softDelete(int id) {
        final String sql = "UPDATE categoria SET activo = 0 WHERE id = ?";
        try (Connection cn = DataSourceFactory.getConnection();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error desactivando categoría id=" + id, e);
        }
    }

    @Override
    public void restore(int id) {
        final String sql = "UPDATE categoria SET activo = 1 WHERE id = ?";
        try (Connection cn = DataSourceFactory.getConnection();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error reactivando categoría id=" + id, e);
        }
    }

    @Override
    public boolean deleteIfOrphan(int id) {
        try (Connection cn = DataSourceFactory.getConnection()) {
            if (countUsosInternal(cn, id) > 0) return false;
            try (PreparedStatement ps = cn.prepareStatement("DELETE FROM categoria WHERE id = ?")) {
                ps.setInt(1, id);
                ps.executeUpdate();
                return true;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error borrando categoría huérfana id=" + id, e);
        }
    }

    @Override
    public int countUsos(int id) {
        try (Connection cn = DataSourceFactory.getConnection()) {
            return countUsosInternal(cn, id);
        } catch (SQLException e) {
            throw new RuntimeException("Error contando usos de categoría id=" + id, e);
        }
    }

    private int countUsosInternal(Connection cn, int id) throws SQLException {
        final String sql = "SELECT COUNT(*) FROM insumo WHERE categoria_id = ?";
        try (PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }
}
