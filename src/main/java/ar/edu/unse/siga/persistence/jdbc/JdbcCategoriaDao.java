package ar.edu.unse.siga.persistence.jdbc;

import ar.edu.unse.siga.domain.Categoria;
import ar.edu.unse.siga.persistence.DataSourceFactory;
import ar.edu.unse.siga.persistence.dao.CategoriaDao;

import java.sql.*;
import java.util.*;

public class JdbcCategoriaDao implements CategoriaDao {

    @Override
    public List<Categoria> findAllOrderByNombre() {
        String sql = "SELECT id, nombre FROM categoria ORDER BY nombre ASC";
        List<Categoria> out = new ArrayList<>();
        try (Connection c = DataSourceFactory.getConnection();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Categoria cat = new Categoria();
                cat.setId(rs.getInt("id")); // ← usar int para ser consistente
                cat.setNombre(rs.getString("nombre"));
                out.add(cat);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error listando categorías", e);
        }
        return out;
    }

    @Override
    public void create(Categoria categoria) {
        String sql = "INSERT INTO categoria(nombre) VALUES(?)";
        try (Connection conn = DataSourceFactory.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, categoria.getNombre());
            ps.executeUpdate();

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
        String sql = "SELECT * FROM categoria WHERE id=?";
        try (Connection conn = DataSourceFactory.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(new Categoria(rs.getInt("id"), rs.getString("nombre")));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error buscando categoría", e);
        }
        return Optional.empty();
    }

    @Override
    public Optional<Categoria> findByNombre(String nombre) {
        String sql = "SELECT * FROM categoria WHERE nombre=?";
        try (Connection conn = DataSourceFactory.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, nombre);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(new Categoria(rs.getInt("id"), rs.getString("nombre")));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error buscando categoría", e);
        }
        return Optional.empty();
    }

    @Override
    public List<Categoria> listAll() {
        String sql = "SELECT * FROM categoria ORDER BY nombre";
        List<Categoria> result = new ArrayList<>();
        try (Connection conn = DataSourceFactory.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                result.add(new Categoria(rs.getInt("id"), rs.getString("nombre")));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error listando categorías", e);
        }
        return result;
    }

    @Override
    public void update(Categoria categoria) {
        String sql = "UPDATE categoria SET nombre=? WHERE id=?";
        try (var conn = DataSourceFactory.getConnection();
             var ps = conn.prepareStatement(sql)) {
            ps.setString(1, categoria.getNombre());
            ps.setInt(2, categoria.getId());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error actualizando categoría id=" + categoria.getId(), e);
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
        String sql = "DELETE FROM categoria WHERE id=?";
        try (var conn = DataSourceFactory.getConnection();
             var ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
            return true;
        } catch (SQLException e) {
            throw new RuntimeException("Error eliminando categoría id=" + id, e);
        }
    }
}
