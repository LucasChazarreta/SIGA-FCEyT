package ar.edu.unse.siga.persistence.dao;

import ar.edu.unse.siga.domain.Ubicacion;

import java.util.List;

public interface UbicacionDao {
    List<Ubicacion> listAll();

    List<Ubicacion> listAllIncludingInactive();

    Ubicacion create(Ubicacion ubicacion);

    void update(Ubicacion ubicacion);

    void softDelete(int id);

    void restore(int id);
}
