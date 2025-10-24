package ar.edu.unse.siga.persistence.jdbc;

import ar.edu.unse.siga.domain.Categoria;
import ar.edu.unse.siga.persistence.DataSourceFactory;
import ar.edu.unse.siga.persistence.dao.CategoriaDao;

import java.sql.*;
import java.util.*;

public class JdbcCategoriaDao implements CategoriaDao {

    @Override
    public List<Categoria> findAllOrderByNombre() {
        String sql = "SELECT id, nombre, activo FROM categoria WHERE activo = 1 ORDER BY nombre ASC";
        List<Categoria> out = new ArrayList<>();
        try (Connection c = DataSourceFactory.getConnection();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Categoria cat = new Categoria();
                cat.setId(rs.getInt("id")); // ← usar int para ser consistente
                cat.setNombre(rs.getString("nombre"));
                cat.setActiva(rs.getBoolean("activo"));
                out.add(cat);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error listando categorías", e);
        }
        return out;
    }

    @Override
    public void create(Categoria categoria) {
        String sql = "INSERT INTO categoria(nombre, activo) VALUES(?, 1)";
        try (Connection conn = DataSourceFactory.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, categoria.getNombre());
            ps.executeUpdate();
            categoria.setActiva(true);

            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    categoria.setId(rs.getInt(1));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error creando categoría", e);
        }
    }

    @Override
    public Optional<Categoria> findById(int id) {
        String sql = "SELECT id, nombre, activo FROM categoria WHERE id=?";
        try (Connection conn = DataSourceFactory.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(new Categoria(
                            rs.getInt("id"),
                            rs.getString("nombre"),
                            rs.getBoolean("activo")));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error buscando categoría", e);
        }
        return Optional.empty();
    }

    @Override
    public Optional<Categoria> findByNombre(String nombre) {
        String sql = "SELECT id, nombre, activo FROM categoria WHERE nombre=?";
        try (Connection conn = DataSourceFactory.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, nombre);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(new Categoria(
                            rs.getInt("id"),
                            rs.getString("nombre"),
                            rs.getBoolean("activo")));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error buscando categoría", e);
        }
        return Optional.empty();
    }

    @Override
    public List<Categoria> listAll() {
        String sql = "SELECT id, nombre, activo FROM categoria WHERE activo = 1 ORDER BY nombre";
        List<Categoria> result = new ArrayList<>();
        try (Connection conn = DataSourceFactory.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                result.add(new Categoria(
                        rs.getInt("id"),
                        rs.getString("nombre"),
                        rs.getBoolean("activo")));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error listando categorías", e);
        }
        return result;
    }

    @Override
    public void update(Categoria categoria) {
        String sql = "UPDATE categoria SET nombre=?, activo=? WHERE id=?";
        try (var conn = DataSourceFactory.getConnection();
             var ps = conn.prepareStatement(sql)) {
            ps.setString(1, categoria.getNombre());
            ps.setBoolean(2, categoria.isActiva());
            ps.setInt(3, categoria.getId());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error actualizando categoría id=" + categoria.getId(), e);
        }
    }

    @Override
    public void softDelete(int id) {
        String sql = "UPDATE categoria SET activo = 0 WHERE id = ?";
        try (var conn = DataSourceFactory.getConnection();
             var ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error dando de baja la categoría id=" + id, e);
        }
    }

    @Override
    public void restore(int id) {
        String sql = "UPDATE categoria SET activo = 1 WHERE id = ?";
        try (var conn = DataSourceFactory.getConnection();
             var ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error restaurando la categoría id=" + id, e);
        }
    }

    @Override
    public int countUsos(int id) {
        String sql = "SELECT COUNT(*) FROM insumo WHERE categoria_id=?";
        try (var conn = DataSourceFactory.getConnection();
             var ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (var rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error contando usos de categoría id=" + id, e);
        }
    }

    @Override
    public boolean deleteIfOrphan(int id) {
        if (countUsos(id) > 0) return false;
        softDelete(id);
        return true;
    }

    @Override
    public List<Categoria> listAllIncludingInactive() {
        String sql = "SELECT id, nombre, activo FROM categoria ORDER BY nombre";
        List<Categoria> result = new ArrayList<>();
        try (Connection conn = DataSourceFactory.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                result.add(new Categoria(
                        rs.getInt("id"),
                        rs.getString("nombre"),
                        rs.getBoolean("activo")));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error listando categorías", e);
        }
        return result;
    }
}
