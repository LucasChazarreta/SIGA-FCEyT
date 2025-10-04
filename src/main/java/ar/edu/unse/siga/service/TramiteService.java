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

    public Long registrarTramite(String nro, String asunto, String solicitante, String descripcion, String destino) {
        if (nro == null || nro.isBlank()) {
            throw new IllegalArgumentException("Número obligatorio");
        }
        if (asunto == null || asunto.isBlank()) {
            throw new IllegalArgumentException("Asunto obligatorio");
        }
        if (descripcion == null || descripcion.isBlank()) {
            throw new IllegalArgumentException("Descripcion obligatorio");
        }
        if (destino == null || destino.isBlank()) {
            throw new IllegalArgumentException("Destino obligatorio");
        }
        Tramite t = new Tramite();
        t.setNro(nro);
        t.setAsunto(asunto);
        t.setSolicitante(solicitante);
        t.setDescripcion(descripcion);
        t.setDestino(destino);        
        t.setEstado("NUEVO");
        t.setFecha(LocalDateTime.now());
        return tramiteDao.create(t);
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
