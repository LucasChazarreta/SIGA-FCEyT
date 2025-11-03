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
import java.util.Objects;

public class TramiteService {

    private final TramiteDao tramiteDao;
    private final TramiteDetalleDao tramiteDetalleDao;
    private final MovimientoDao movimientoDao;
    private final InsumoDao insumoDao;

    public TramiteService(TramiteDao tramiteDao,
                          TramiteDetalleDao tramiteDetalleDao,
                          MovimientoDao movimientoDao,
                          InsumoDao insumoDao) {
        this.tramiteDao = Objects.requireNonNull(tramiteDao);
        this.tramiteDetalleDao = tramiteDetalleDao;
        this.movimientoDao = movimientoDao;
        this.insumoDao = insumoDao;
    }

    // ==== Consultas usadas por la UI ====
    public List<Tramite> tramitesRecientes(int limit) { return tramiteDao.listRecientes(limit); }
    public List<Tramite> listarTodos() { return tramiteDao.listAll(); }
    public void actualizarEstado(Long id, String nuevoEstado) { tramiteDao.updateEstado(id, nuevoEstado); }

    public List<Insumo> insumosConStockDisponible() {
        if (insumoDao == null) throw new IllegalStateException("InsumoDao no configurado");
        return insumoDao.listActivosConStock();
    }

    // ==== Alta de solicitud con renglones ====
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

        // Consolidar cantidades por insumo y validar
        LinkedHashMap<Long, BigDecimal> porInsumo = new LinkedHashMap<>();
        for (LineaTramite l : lineas) {
            if (l == null) throw new IllegalArgumentException("Línea inválida");
            if (l.cantidad <= 0) throw new IllegalArgumentException("Cantidad inválida para insumo " + l.insumoId);
            porInsumo.merge(l.insumoId, BigDecimal.valueOf(l.cantidad), BigDecimal::add);
        }

        // Normalizaciones
        String nro = generarNumeroTramite();
        String asunto = solicitud.trim();
        String solicitanteFinal = (solicitante == null || solicitante.isBlank()) ? "Informes" : solicitante.trim();
        String destinoFinal = (destino == null || destino.isBlank()) ? "Secretaría FCEyT" : destino.trim();
        String descripcionFinal = (descripcion == null || descripcion.isBlank()) ? null : descripcion.trim();

        try (Connection cn = DataSourceFactory.getConnection()) {
            boolean originalAuto = cn.getAutoCommit();
            cn.setAutoCommit(false);
            try {
                // Validar stock actual antes de tocar nada
                for (var e : porInsumo.entrySet()) {
                    long insumoId = e.getKey();
                    BigDecimal cantidad = e.getValue();
                    BigDecimal actual = movimientoDao.stockActual(insumoId);
                    if (actual.compareTo(cantidad) < 0) {
                        throw new IllegalStateException("Stock insuficiente para el insumo " + insumoId);
                    }
                }

                // Crear trámite
                Tramite tramite = new Tramite();
                tramite.setNro(nro);
                tramite.setAsunto(asunto);
                tramite.setSolicitante(solicitanteFinal);
                tramite.setDescripcion(descripcionFinal);
                tramite.setDestino(destinoFinal);
                tramite.setEstado("NUEVO");
                tramite.setFecha(LocalDateTime.now());
                Long tramiteId = tramiteDao.create(tramite, cn);

                // Por cada insumo: descontar stock, crear detalle y movimiento (SALIDA)
                for (var e : porInsumo.entrySet()) {
                    long insumoId = e.getKey();
                    BigDecimal cantidad = e.getValue();

                    // Descontar stock (optimista, con chequeo de cambio)
                    boolean ok = insumoDao.decrementStock(cn, insumoId, cantidad);
                    if (!ok) {
                        throw new IllegalStateException("El stock cambió. Revisá cantidades e intentá nuevamente.");
                    }

                    // Detalle
                    TramiteDetalle det = new TramiteDetalle();
                    det.setTramiteId(tramiteId);
                    Insumo ins = new Insumo();
                    ins.setId(insumoId);
                    det.setInsumo(ins);
                    det.setCantidad(cantidad);
                    tramiteDetalleDao.create(det, cn);

                    // Movimiento SALIDA (observación: el DAO actual no recibe 'destino')
                    movimientoDao.registrarSalida(cn, insumoId, cantidad, solicitanteFinal, tramiteId);
                }

                cn.commit();
                return tramiteId;
            } catch (Exception e) {
                try { cn.rollback(); } catch (SQLException ignored) {}
                if (e instanceof IllegalStateException) throw e; // mensaje claro al usuario
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

    // DTO interno para la UI
    public static class LineaTramite {
        private final long insumoId;
        private final int cantidad;
        public LineaTramite(long insumoId, int cantidad) { this.insumoId = insumoId; this.cantidad = cantidad; }
        public long getInsumoId() { return insumoId; }
        public int getCantidad() { return cantidad; }
    }
}
