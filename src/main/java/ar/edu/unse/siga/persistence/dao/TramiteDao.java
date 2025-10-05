package ar.edu.unse.siga.persistence.dao;

import ar.edu.unse.siga.domain.Tramite;
import java.util.List;
import java.util.Optional;

public interface TramiteDao {
    Long create(Tramite t);
    void updateEstado(Long id, String nuevoEstado);
    Optional<Tramite> findByNro(String nro);
    List<Tramite> listAll();
    List<Tramite> listActivos();

    // ✅ NUEVO: actualizar estado directamente por NRO (sin SELECT previo)
    void updateEstadoByNro(String nro, String nuevoEstado);
}
