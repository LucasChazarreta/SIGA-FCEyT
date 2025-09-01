package ar.edu.unse.siga.persistence.jdbc;

import ar.edu.unse.siga.domain.Rol;
import ar.edu.unse.siga.domain.Usuario;
import ar.edu.unse.siga.persistence.DataSourceFactory;
import ar.edu.unse.siga.persistence.dao.UsuarioDao;

import java.sql.*;
import java.util.Optional;

public class JdbcUsuarioDao implements UsuarioDao {

    private Usuario mapRow(ResultSet rs) throws SQLException {
        Usuario u = new Usuario();
        u.setId(rs.getLong("id"));
        u.setUsername(rs.getString("username"));
        u.setPasswordHash(rs.getString("password_hash"));
        u.setEmail(rs.getString("email"));
        u.setEstado(rs.getString("estado"));

        Rol r = new Rol();
        r.setId(rs.getInt("rol_id"));
        r.setNombre(rs.getString("rol_nombre"));
        u.setRol(r);
        return u;
    }

    @Override
    public Optional<Usuario> findByUsername(String username) {
        String sql = """
            SELECT u.*, r.nombre AS rol_nombre
            FROM usuario u JOIN rol r ON r.id = u.rol_id
            WHERE u.username = ?
        """;
        try (var cn = DataSourceFactory.getConnection();
             var ps = cn.prepareStatement(sql)) {
            ps.setString(1, username);
            try (var rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(mapRow(rs));
                return Optional.empty();
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error buscando usuario por username", e);
        }
    }

    @Override
    public Long create(Usuario u) {
        String sql = """
            INSERT INTO usuario(username, password_hash, email, estado, rol_id)
            VALUES (?,?,?,?,?)
        """;
        try (var cn = DataSourceFactory.getConnection();
             var ps = cn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, u.getUsername());
            ps.setString(2, u.getPasswordHash());
            ps.setString(3, u.getEmail());
            ps.setString(4, u.getEstado() == null ? "ACTIVO" : u.getEstado());
            ps.setInt(5, u.getRol().getId());

            ps.executeUpdate();
            try (var rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    long id = rs.getLong(1);
                    u.setId(id);
                    return id;
                }
                throw new SQLException("No se obtuvo ID generado");
            }
        } catch (SQLException e) {
            if (e.getMessage() != null && e.getMessage().contains("Duplicate")) {
                throw new IllegalStateException("Username ya existe: " + u.getUsername(), e);
            }
            throw new RuntimeException("Error creando usuario", e);
        }
    }

    @Override
    public void updatePassword(Long id, String newHash) {
        String sql = "UPDATE usuario SET password_hash=? WHERE id=?";
        try (var cn = DataSourceFactory.getConnection();
             var ps = cn.prepareStatement(sql)) {
            ps.setString(1, newHash);
            ps.setLong(2, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error actualizando password del usuario id=" + id, e);
        }
    }
}

