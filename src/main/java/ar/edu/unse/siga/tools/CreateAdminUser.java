package ar.edu.unse.siga.tools;

import ar.edu.unse.siga.config.AppServices;
import ar.edu.unse.siga.domain.Usuario;
import ar.edu.unse.siga.persistence.dao.UsuarioDao;
import ar.edu.unse.siga.security.PasswordUtil;

public class CreateAdminUser {
    public static void main(String[] args) {
        AppServices.init();
        UsuarioDao usuarioDao = AppServices.get().getUsuarioDao();

        String username = args.length > 0 ? args[0] : "admin";
        String rawPass  = args.length > 1 ? args[1] : "admin123";

        Usuario u = new Usuario(null, username, PasswordUtil.hash(rawPass), true);
        Long id = usuarioDao.insert(u);
        System.out.println("Usuario admin creado. id=" + id + ", username=" + username);
    }
}
