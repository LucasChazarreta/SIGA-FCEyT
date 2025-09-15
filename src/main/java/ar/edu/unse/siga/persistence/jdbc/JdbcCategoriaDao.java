package ar.edu.unse.siga.persistence.jdbc;

import ar.edu.unse.siga.domain.Categoria;
import ar.edu.unse.siga.persistence.DataSourceFactory;
import ar.edu.unse.siga.persistence.dao.CategoriaDao;

import java.sql.*;
import java.util.*;

public class JdbcCategoriaDao implements CategoriaDao {

    @Override
    public List<Categoria> findAll() {
        String sql = "SELECT id, nombre FROM categoria ORDER BY nombre";
        List<Categoria> result = new ArrayList<>();
        try (Connection conn = DataSourceFactory.getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                result.add(new Categoria(rs.getInt("id"), rs.getString("nombre")));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error en findAll", e);
        }
        return result;
    }

    @Override
    public Optional<Categoria> findById(int id) {
        String sql = "SELECT id, nombre FROM categoria WHERE id=?";
        try (Connection conn = DataSourceFactory.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return Optional.of(new Categoria(rs.getInt("id"), rs.getString("nombre")));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error en findById", e);
        }
        return Optional.empty();
    }

    @Override
    public Optional<Categoria> findByNombre(String nombre) {
        String sql = "SELECT id, nombre FROM categoria WHERE nombre=?";
        try (Connection conn = DataSourceFactory.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, nombre);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return Optional.of(new Categoria(rs.getInt("id"), rs.getString("nombre")));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error en findByNombre", e);
        }
        return Optional.empty();
    }

    @Override
    public Categoria create(Categoria categoria) {
        String sql = "INSERT INTO categoria(nombre) VALUES (?)";
        try (Connection conn = DataSourceFactory.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, categoria.getNombre());
            ps.executeUpdate();
            ResultSet keys = ps.getGeneratedKeys();
            if (keys.next()) {
                categoria.setId(keys.getInt(1));
            }
            return categoria;
        } catch (SQLException e) {
            throw new RuntimeException("Error en create", e);
        }
    }

    @Override
    public void delete(int id) {
        String sql = "DELETE FROM categoria WHERE id=?";
        try (Connection conn = DataSourceFactory.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error en delete", e);
        }
    }
}
