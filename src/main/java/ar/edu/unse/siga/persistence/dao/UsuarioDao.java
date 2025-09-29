package ar.edu.unse.siga.persistence.dao;

import ar.edu.unse.siga.domain.Usuario;

import java.util.Optional;

public interface UsuarioDao {
    Optional<Usuario> findByUsername(String username);

    /** Inserta un usuario y devuelve el ID generado */
    Long insert(Usuario u);

    /** Actualiza el hash de password. Devuelve true si afectó filas. */
    boolean updatePassword(Long id, String newPasswordHash);
}
