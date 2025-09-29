package ar.edu.unse.siga.tools;

import ar.edu.unse.siga.config.AppServices;
import ar.edu.unse.siga.domain.Usuario;
import ar.edu.unse.siga.persistence.DataSourceFactory;
import ar.edu.unse.siga.persistence.dao.UsuarioDao;
import ar.edu.unse.siga.security.PasswordUtil;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLIntegrityConstraintViolationException;

public class CreateAdminUser {

    public static void main(String[] args) {
        System.out.println("DB URL = " + DataSourceFactory.getConfig("db.url"));

        AppServices.init();
        UsuarioDao usuarioDao = AppServices.get().getUsuarioDao();
        DataSource ds = AppServices.get().getDataSource();

        String username = (args.length > 0) ? args[0] : "admin";
        String rawPass  = (args.length > 1) ? args[1] : "admin123";
        String hash     = PasswordUtil.hash(rawPass);

        System.out.println("Creando/actualizando usuario admin: " + username);

        try {
            // Intentar insertar
            Usuario u = new Usuario(null, username, hash, true);
            Long id = usuarioDao.insert(u);
            System.out.println("Usuario creado. id=" + id + ", username=" + username);
        } catch (RuntimeException ex) {
            // Si ya existe, hacemos UPDATE de password_hash y activo=1
            Throwable cause = ex.getCause();
            boolean duplicate = (cause instanceof SQLIntegrityConstraintViolationException)
                    || (ex.getMessage() != null && ex.getMessage().toLowerCase().contains("duplicate entry"));

            if (!duplicate) {
                throw ex;
            }

            System.out.println("El usuario ya existe. Actualizando password_hash y activo=1...");
            String sql = "UPDATE usuario SET password_hash = ?, activo = 1 WHERE username = ?";
            try (Connection cn = ds.getConnection();
                 PreparedStatement ps = cn.prepareStatement(sql)) {
                ps.setString(1, hash);
                ps.setString(2, username);
                int n = ps.executeUpdate();
                if (n == 0) {
                    throw new RuntimeException("No se encontró el usuario para actualizar: " + username);
                }
                System.out.println("Actualización OK. username=" + username);
            } catch (Exception e) {
                throw new RuntimeException("Error actualizando usuario existente", e);
            }
        }
    }
}
