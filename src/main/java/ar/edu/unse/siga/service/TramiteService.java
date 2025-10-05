package ar.edu.unse.siga.service;

import ar.edu.unse.siga.domain.Tramite;
import ar.edu.unse.siga.persistence.dao.TramiteDao;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

public class TramiteService {

    private final TramiteDao tramiteDao;

    public TramiteService(TramiteDao tramiteDao) {
        this.tramiteDao = tramiteDao;
    }

    // ✅ Método principal: guarda exactamente la descripción que venga de la UI
    public Long registrarTramite(String nro, String asunto, String solicitante, String descripcion) {
        if (nro == null || nro.isBlank()) throw new IllegalArgumentException("Número obligatorio");
        if (asunto == null || asunto.isBlank()) throw new IllegalArgumentException("Asunto obligatorio");

        Tramite t = new Tramite();
        t.setNro(nro.trim());
        t.setAsunto(asunto.trim());
        t.setSolicitante(solicitante != null ? solicitante.trim() : "Desconocido");
        t.setEstado("PENDIENTE");
        t.setFecha(LocalDateTime.now());
        t.setDescripcion(descripcion != null ? descripcion.trim() : null);

        return tramiteDao.create(t);
    }

    // ✅ Overload para compatibilidad
    public Long registrarTramite(String nro, String asunto, String solicitante) {
        return registrarTramite(nro, asunto, solicitante, null);
    }

    // ========= ESTADOS =========

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

    // ✅ NUEVO: update por NRO (evita el SELECT y el popup de error)
    public void actualizarEstadoPorNro(String nro, String nuevoEstado) {
        if (nro == null || nro.isBlank()) throw new IllegalArgumentException("Nro inválido");
        tramiteDao.updateEstadoByNro(nro, canonicalEstado(nuevoEstado));
    }

    // Compatibilidad: delega
    public void cambiarEstado(Long id, String nuevoEstado) {
        actualizarEstado(id, nuevoEstado);
    }

    // ========= CONSULTAS =========

    public Optional<Tramite> buscarPorNro(String nro) {
        return tramiteDao.findByNro(nro);
    }

    public List<Tramite> listarTodos() {
        return tramiteDao.listAll();
    }

    public List<Tramite> listarActivos() {
        return tramiteDao.listActivos();
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
