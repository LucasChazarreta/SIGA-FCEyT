package ar.edu.unse.siga.service;

import ar.edu.unse.siga.common.CurrentSession;
import ar.edu.unse.siga.common.PasswordUtil;
import ar.edu.unse.siga.domain.Usuario;
import ar.edu.unse.siga.persistence.dao.UsuarioDao;

public class AuthService {
    private final UsuarioDao usuarioDao;

    public AuthService(UsuarioDao usuarioDao) {
        this.usuarioDao = usuarioDao;
    }

    public Usuario login(String username, String plainPassword) {
        var opt = usuarioDao.findByUsername(username);
        if (opt.isEmpty()) throw new IllegalArgumentException("Usuario/contraseña inválidos");

        var u = opt.get();
        if (!"ACTIVO".equalsIgnoreCase(u.getEstado())) {
            throw new IllegalStateException("Usuario inactivo");
        }
        if (!PasswordUtil.check(plainPassword, u.getPasswordHash())) {
            throw new IllegalArgumentException("Usuario/contraseña inválidos");
        }
        CurrentSession.setUser(u);
        return u;
    }

    public void logout() {
        CurrentSession.clear();
    }
}
