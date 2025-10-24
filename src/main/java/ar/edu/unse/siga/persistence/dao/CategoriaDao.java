package ar.edu.unse.siga.persistence.dao;

import ar.edu.unse.siga.domain.Categoria;
import java.util.List;
import java.util.Optional;

public interface CategoriaDao {
    void create(Categoria categoria);
    Optional<Categoria> findById(int id);
    Optional<Categoria> findByNombre(String nombre);
    List<Categoria> listAll();
    
    void update(Categoria categoria);

    void softDelete(int id);

    void restore(int id);

    boolean deleteIfOrphan(int id);   // true si borró, false si tenía insumos asociados

    int countUsos(int id);            // cuántos insumos referencian esta categoría

    List<Categoria> findAllOrderByNombre();

    List<Categoria> listAllIncludingInactive();


}
