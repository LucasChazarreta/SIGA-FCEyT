package ar.edu.unse.siga.persistence.dao;

import ar.edu.unse.siga.domain.Categoria;
import java.util.List;
import java.util.Optional;

public interface CategoriaDao {
    void create(Categoria categoria);
    Optional<Categoria> findById(int id);
    Optional<Categoria> findByNombre(String nombre);
    List<Categoria> listAll();
}
