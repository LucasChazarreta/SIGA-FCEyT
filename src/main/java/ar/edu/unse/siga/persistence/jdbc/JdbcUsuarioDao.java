package ar.edu.unse.siga.persistence.jdbc;

import ar.edu.unse.siga.domain.Usuario;
import ar.edu.unse.siga.persistence.dao.UsuarioDao;

import javax.sql.DataSource;
import java.sql.*;
import java.util.Optional;

public class JdbcUsuarioDao implements UsuarioDao {

    private final DataSource ds;

    public JdbcUsuarioDao(DataSource ds) { this.ds = ds; }

    @Override
    public Optional<Usuario> findByUsername(String username) {
        String sql = "SELECT id, username, password_hash, activo FROM usuario WHERE username=?";
        try (Connection cn = ds.getConnection();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Usuario u = new Usuario(
                            rs.getLong("id"),
                            rs.getString("username"),
                            rs.getString("password_hash"),
                            rs.getBoolean("activo")
                    );
                    return Optional.of(u);
                }
                return Optional.empty();
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error consultando usuario", e);
        }
    }

    @Override
    public Long insert(Usuario u) {
        String sql = "INSERT INTO usuario (username, password_hash, activo) VALUES (?,?,?)";
        try (Connection cn = ds.getConnection();
             PreparedStatement ps = cn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, u.getUsername());
            ps.setString(2, u.getPasswordHash());
            ps.setBoolean(3, u.isActivo());
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) return rs.getLong(1);
            }
            return null;
        } catch (SQLException e) {
            throw new RuntimeException("Error insertando usuario", e);
        }
    }

    @Override
    public boolean updatePassword(Long id, String newPasswordHash) {
        String sql = "UPDATE usuario SET password_hash=? WHERE id=?";
        try (Connection cn = ds.getConnection();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setString(1, newPasswordHash);
            ps.setLong(2, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new RuntimeException("Error actualizando password", e);
        }
    }
}
