package ar.edu.unse.siga.service;

import ar.edu.unse.siga.domain.Usuario;
import ar.edu.unse.siga.persistence.dao.UsuarioDao;
import ar.edu.unse.siga.security.PasswordUtil;

import java.util.Objects;

public class AuthService {

    private final UsuarioDao usuarioDao;

    public AuthService(UsuarioDao usuarioDao) {
        this.usuarioDao = Objects.requireNonNull(usuarioDao);
    }

    public Usuario login(String username, String rawPassword) {
        Usuario u = usuarioDao.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("Usuario o contraseña inválidos"));
        if (!u.isActivo()) {
            throw new IllegalStateException("Usuario inactivo");
        }
        if (!PasswordUtil.check(rawPassword, u.getPasswordHash())) {
            throw new IllegalArgumentException("Usuario o contraseña inválidos");
        }
        return u;
    }
}
