package ar.edu.unse.siga.tools;

import ar.edu.unse.siga.common.PasswordUtil;
import ar.edu.unse.siga.common.RoleName;
import ar.edu.unse.siga.domain.Rol;
import ar.edu.unse.siga.domain.Usuario;
import ar.edu.unse.siga.persistence.dao.UsuarioDao;
import ar.edu.unse.siga.persistence.jdbc.JdbcUsuarioDao;

public class CreateAdminUser {
    public static void main(String[] args) {
        try {
            UsuarioDao dao = new JdbcUsuarioDao();
            Usuario u = new Usuario();
            String password = "admin";
            u.setUsername("admin");
            u.setPassword(password);
            u.setPasswordHash(PasswordUtil.hash(password));
            u.setEmail("admin@siga.local");
            u.setEstado("ACTIVO");
            Rol r = new Rol(); r.setId(1); r.setNombre(RoleName.ADMIN); // asumiendo rol 1=ADMIN
            u.setRol(r);
            Long id = dao.create(u);
            System.out.println("Admin creado con id=" + id + " (user=admin / pass=admin)");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

