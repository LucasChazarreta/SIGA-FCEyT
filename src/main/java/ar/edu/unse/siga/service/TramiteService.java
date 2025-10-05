package ar.edu.unse.siga.service;

import ar.edu.unse.siga.domain.Tramite;
import ar.edu.unse.siga.persistence.dao.TramiteDao;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public class TramiteService {

    private final TramiteDao tramiteDao;

    public TramiteService(TramiteDao tramiteDao) {
        this.tramiteDao = tramiteDao;
    }

// ✅ Método principal: guarda exactamente la descripción que venga de la UI
public Long registrarTramite(String nro, String asunto, String solicitante, String descripcion) {
    if (nro == null || nro.isBlank()) {
        throw new IllegalArgumentException("Número obligatorio");
    }
    if (asunto == null || asunto.isBlank()) {
        throw new IllegalArgumentException("Asunto obligatorio");
    }

    Tramite t = new Tramite();
    t.setNro(nro.trim());
    t.setAsunto(asunto.trim());
    t.setSolicitante(solicitante != null ? solicitante.trim() : "Desconocido");
    t.setEstado("NUEVO");
    t.setFecha(LocalDateTime.now());

    // 👉 Guardar tal cual lo ingresó el usuario (puede ser null o vacío)
    t.setDescripcion(descripcion != null ? descripcion.trim() : null);

    return tramiteDao.create(t);
}

// ✅ Overload para compatibilidad (si en algún punto todavía no pasás la descripción)
public Long registrarTramite(String nro, String asunto, String solicitante) {
    // no inventamos “Trámite sobre…”, simplemente delegamos con null
    return registrarTramite(nro, asunto, solicitante, null);
}



    public void cambiarEstado(Long id, String nuevoEstado) {
        if (nuevoEstado == null || nuevoEstado.isBlank()) {
            throw new IllegalArgumentException("Estado inválido");
        }
        tramiteDao.updateEstado(id, nuevoEstado);
    }

    public Optional<Tramite> buscarPorNro(String nro) {
        return tramiteDao.findByNro(nro);
    }

    public List<Tramite> listarTodos() {
        return tramiteDao.listAll();
    }

    public int totalTramites() {
        return listarTodos().size();
    }

    public int totalPendientes() {
        return (int) listarTodos().stream()
                .filter(t -> t.getEstado() != null && t.getEstado().equalsIgnoreCase("PENDIENTE"))
                .count();
    }

}
