package ar.edu.unse.siga.service;

import ar.edu.unse.siga.domain.Insumo;
import ar.edu.unse.siga.domain.Tramite;
import ar.edu.unse.siga.domain.TramiteDetalle;
import ar.edu.unse.siga.persistence.DataSourceFactory;
import ar.edu.unse.siga.persistence.dao.InsumoDao;
import ar.edu.unse.siga.persistence.dao.MovimientoDao;
import ar.edu.unse.siga.persistence.dao.TramiteDao;
import ar.edu.unse.siga.persistence.dao.TramiteDetalleDao;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

public class TramiteService {

    private final TramiteDao tramiteDao;
    private final TramiteDetalleDao tramiteDetalleDao;
    private final MovimientoDao movimientoDao;
    private final InsumoDao insumoDao;

    public TramiteService(TramiteDao tramiteDao) {
        this(tramiteDao, null, null, null);
    }

    public TramiteService(TramiteDao tramiteDao,
                          TramiteDetalleDao tramiteDetalleDao,
                          MovimientoDao movimientoDao,
                          InsumoDao insumoDao) {
        this.tramiteDao = tramiteDao;
        this.tramiteDetalleDao = tramiteDetalleDao;
        this.movimientoDao = movimientoDao;
        this.insumoDao = insumoDao;
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


    public List<Insumo> insumosConStockDisponible() {
        if (insumoDao == null) {
            throw new IllegalStateException("InsumoDao no configurado");
        }
        return insumoDao.listActivosConStock();
    }

    public Long registrarNuevoTramite(String solicitud,
                                      String solicitante,
                                      String descripcion,
                                      String destino,
                                      List<LineaTramite> lineas) {
        if (movimientoDao == null || insumoDao == null || tramiteDetalleDao == null) {
            throw new IllegalStateException("Dependencias no configuradas para registrar trámite con insumos");
        }
        if (solicitud == null || solicitud.isBlank()) {
            throw new IllegalArgumentException("La solicitud es obligatoria");
        }
        if (lineas == null || lineas.isEmpty()) {
            throw new IllegalArgumentException("Debe indicar al menos un insumo");
        }

        LinkedHashMap<Long, BigDecimal> porInsumo = new LinkedHashMap<>();
        lineas.forEach(l -> {
            if (l == null) {
                throw new IllegalArgumentException("Línea inválida");
            }
            if (l.cantidad <= 0) {
                throw new IllegalArgumentException("Cantidad inválida para insumo " + l.insumoId);
            }
            porInsumo.merge(l.insumoId, BigDecimal.valueOf(l.cantidad), BigDecimal::add);
        });

        String solicitudTrim = solicitud.trim();
        String solicitanteTrim = solicitante != null ? solicitante.trim() : "";
        String destinoTrim = destino != null ? destino.trim() : "";
        String descripcionTrim = descripcion != null ? descripcion.trim() : "";

        String solicitanteFinal = solicitanteTrim.isEmpty() ? "Sin solicitante" : solicitanteTrim;
        String destinoFinal = destinoTrim.isEmpty() ? "Secretaría FCEyT" : destinoTrim;
        String descripcionFinal = descripcionTrim.isEmpty() ? null : descripcionTrim;

        try (Connection cn = DataSourceFactory.getConnection()) {
            boolean originalAuto = cn.getAutoCommit();
            try {
                cn.setAutoCommit(false);

                Tramite tramite = new Tramite();
                tramite.setNro(generarNumeroTramite());
                tramite.setAsunto(solicitudTrim);
                tramite.setSolicitante(solicitanteFinal);
                tramite.setDescripcion(descripcionFinal);
                tramite.setDestino(destinoFinal);
                tramite.setEstado("NUEVO");
                tramite.setFecha(LocalDateTime.now());
                Long tramiteId = tramiteDao.create(tramite, cn);

                for (var entry : porInsumo.entrySet()) {
                    Long insumoId = entry.getKey();
                    BigDecimal cantidad = entry.getValue();

                    boolean updated = insumoDao.decrementStock(cn, insumoId, cantidad);
                    if (!updated) {
                        throw new IllegalStateException("El stock cambió. Revisá cantidades e intentá nuevamente.");
                    }

                    TramiteDetalle detalle = new TramiteDetalle();
                    detalle.setTramiteId(tramiteId);
                    Insumo ins = new Insumo();
                    ins.setId(insumoId);
                    detalle.setInsumo(ins);
                    detalle.setCantidad(cantidad);
                    tramiteDetalleDao.create(detalle, cn);

                    try {
                        movimientoDao.registrarSalida(cn, insumoId, cantidad, solicitanteFinal, tramiteId);
                    } catch (IllegalStateException ex) {
                        throw new IllegalStateException("El stock cambió. Revisá cantidades e intentá nuevamente.", ex);
                    }
                }

                cn.commit();
                return tramiteId;
            } catch (Exception e) {
                try { cn.rollback(); } catch (SQLException ignored) {}
                if (e instanceof IllegalStateException) {
                    throw e;
                }
                throw new RuntimeException("No se pudo registrar la solicitud", e);
            } finally {
                try { cn.setAutoCommit(originalAuto); } catch (SQLException ignored) {}
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error registrando la solicitud", e);
        }
    }

    private String generarNumeroTramite() {
        String fecha = java.time.LocalDate.now().format(java.time.format.DateTimeFormatter.BASIC_ISO_DATE);
        int random = (int) (Math.random() * 90000) + 10000;
        return fecha + "-" + random;
    }

    public static class LineaTramite {
        private final long insumoId;
        private final int cantidad;

        public LineaTramite(long insumoId, int cantidad) {
            this.insumoId = insumoId;
            this.cantidad = cantidad;
        }

        public long getInsumoId() { return insumoId; }
        public int getCantidad() { return cantidad; }
    }

    
    
}
