package ar.edu.unse.siga.persistence.dao;

import ar.edu.unse.siga.domain.Insumo;
import java.util.List;
import java.util.Optional;

public interface InsumoDao {
    Long create(Insumo insumo);
    void update(Insumo insumo);
    void softDelete(Long id);
    Optional<Insumo> findByCodigo(String codigo);
    Optional<Insumo> findById(Long id);
    List<Insumo> listAll();
}

