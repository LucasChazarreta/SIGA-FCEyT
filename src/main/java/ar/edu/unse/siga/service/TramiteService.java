package ar.edu.unse.siga.service;

import ar.edu.unse.siga.domain.Tramite;
import ar.edu.unse.siga.persistence.dao.TramiteDao;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Collectors;

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
        t.setDescripcion(descripcion != null ? descripcion.trim() : null);
        t.setDestino(destino != null && !destino.isBlank() ? destino.trim() : "Secretaria FCEyT");

        return tramiteDao.create(t);
    }

    private String canonicalEstado(String estado) {
        if (estado == null || estado.isBlank()) throw new IllegalArgumentException("Estado inválido");
        String e = estado.trim().toUpperCase(Locale.ROOT);
        return switch (e) {
            case "EN PROCESO" -> "EN_PROCESO";
            case "COMPLETADO" -> "COMPLETADO";
            case "PENDIENTE"  -> "PENDIENTE";
            case "EN_PROCESO", "CERRADO" -> e;
            default -> e;
        };
    }

    public void actualizarEstado(Long id, String nuevoEstado) {
        tramiteDao.updateEstado(id, canonicalEstado(nuevoEstado));
    }

    public void actualizarEstadoPorNro(String nro, String nuevoEstado) {
        if (nro == null || nro.isBlank()) throw new IllegalArgumentException("Nro inválido");
        tramiteDao.updateEstadoByNro(nro, canonicalEstado(nuevoEstado));
    }

    public Optional<Tramite> buscarPorNro(String nro) { return tramiteDao.findByNro(nro); }

    public List<Tramite> listarTodos() { return tramiteDao.listAll(); }

    public List<Tramite> listarActivos() { return tramiteDao.listActivos(); }

    public int totalTramites() { return listarTodos().size(); }

    public int totalPendientes() {
        return (int) listarTodos().stream()
                .filter(t -> t.getEstado() != null && t.getEstado().equalsIgnoreCase("PENDIENTE"))
                .count();
    }

    // ====== NUEVO: últimos N trámites (por fecha descendente) ======
// En TramiteService
public java.util.List<Tramite> tramitesRecientes(int limit) {
    return tramiteDao.listRecientes(limit); // requiere Dao (paso 2)
}


    
    
}
