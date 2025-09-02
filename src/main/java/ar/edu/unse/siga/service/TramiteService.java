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

    public Long registrarTramite(String nro, String asunto, String solicitante) {
        if (nro == null || nro.isBlank()) throw new IllegalArgumentException("Número obligatorio");
        if (asunto == null || asunto.isBlank()) throw new IllegalArgumentException("Asunto obligatorio");
        Tramite t = new Tramite();
        t.setNro(nro);
        t.setAsunto(asunto);
        t.setSolicitante(solicitante);
        t.setEstado("NUEVO");
        t.setFecha(LocalDateTime.now());
        return tramiteDao.create(t);
    }

    public void cambiarEstado(Long id, String nuevoEstado) {
        if (nuevoEstado==null || nuevoEstado.isBlank()) throw new IllegalArgumentException("Estado inválido");
        tramiteDao.updateEstado(id, nuevoEstado);
    }

    public Optional<Tramite> buscarPorNro(String nro) { return tramiteDao.findByNro(nro); }

    public List<Tramite> listarTodos() { return tramiteDao.listAll(); }
}

