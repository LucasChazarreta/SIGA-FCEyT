package ar.edu.unse.siga.persistence.dao;

import ar.edu.unse.siga.domain.Usuario;
import java.util.Optional;

public interface UsuarioDao {
    Optional<Usuario> findByUsername(String username);
    Long create(Usuario u);                 // crea con passwordHash ya seteado
    void updatePassword(Long id, String newHash);
}

