package ar.edu.unse.siga.persistence.jdbc;

import ar.edu.unse.siga.domain.Ubicacion;
import ar.edu.unse.siga.persistence.DataSourceFactory;
import ar.edu.unse.siga.persistence.dao.UbicacionDao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class JdbcUbicacionDao implements UbicacionDao {

    @Override
    public List<Ubicacion> listAll() {
        final String sql = "SELECT id, nombre, activo FROM ubicacion WHERE activo = 1 ORDER BY nombre";
        try (Connection cn = DataSourceFactory.getConnection();
             PreparedStatement ps = cn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            List<Ubicacion> list = new ArrayList<>();
            while (rs.next()) {
                Ubicacion u = new Ubicacion();
                u.setId(rs.getInt("id"));
                u.setNombre(rs.getString("nombre"));
                u.setActiva(rs.getBoolean("activo"));
                list.add(u);
            }
            return list;
        } catch (SQLException e) {
            throw new RuntimeException("Error listando ubicaciones", e);
        }
    }

    @Override
    public List<Ubicacion> listAllIncludingInactive() {
        final String sql = "SELECT id, nombre, activo FROM ubicacion ORDER BY nombre";
        try (Connection cn = DataSourceFactory.getConnection();
             PreparedStatement ps = cn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            List<Ubicacion> list = new ArrayList<>();
            while (rs.next()) {
                Ubicacion u = new Ubicacion();
                u.setId(rs.getInt("id"));
                u.setNombre(rs.getString("nombre"));
                u.setActiva(rs.getBoolean("activo"));
                list.add(u);
            }
            return list;
        } catch (SQLException e) {
            throw new RuntimeException("Error listando ubicaciones", e);
        }
    }

    @Override
    public Ubicacion create(Ubicacion ubicacion) {
        final String sql = "INSERT INTO ubicacion(nombre, activo) VALUES(?, 1)";
        try (Connection cn = DataSourceFactory.getConnection();
             PreparedStatement ps = cn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, ubicacion.getNombre());
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    ubicacion.setId(rs.getInt(1));
                }
            }
            ubicacion.setActiva(true);
            return ubicacion;
        } catch (SQLException e) {
            throw new RuntimeException("Error creando ubicación", e);
        }
    }

    @Override
    public void update(Ubicacion ubicacion) {
        final String sql = "UPDATE ubicacion SET nombre=?, activo=? WHERE id=?";
        try (Connection cn = DataSourceFactory.getConnection();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setString(1, ubicacion.getNombre());
            ps.setBoolean(2, ubicacion.isActiva());
            ps.setInt(3, ubicacion.getId());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error actualizando ubicación id=" + ubicacion.getId(), e);
        }
    }

    @Override
    public void softDelete(int id) {
        final String sql = "UPDATE ubicacion SET activo = 0 WHERE id = ?";
        try (Connection cn = DataSourceFactory.getConnection();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error dando de baja la ubicación id=" + id, e);
        }
    }

    @Override
    public void restore(int id) {
        final String sql = "UPDATE ubicacion SET activo = 1 WHERE id = ?";
        try (Connection cn = DataSourceFactory.getConnection();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error restaurando la ubicación id=" + id, e);
        }
    }
}
