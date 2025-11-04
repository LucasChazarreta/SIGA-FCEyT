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
import java.util.ArrayList;
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
    public RegistroTramite registrarNuevoTramite(String solicitud,
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

        String solicitudBase = solicitud.trim();
        String solicitanteFinal = (solicitante == null || solicitante.isBlank()) ? "Informes" : solicitante.trim();
        String destinoFinal = (destino == null || destino.isBlank()) ? "Secretaría FCEyT" : destino.trim();
        String descripcionFinal = (descripcion == null || descripcion.isBlank()) ? null : descripcion.trim();
        String numeroBase = generarNumeroTramite();

        List<Tramite> generados = new ArrayList<>();
        int correlativo = 1;

        try (Connection cn = DataSourceFactory.getConnection()) {
            boolean originalAuto = cn.getAutoCommit();
            cn.setAutoCommit(false);
            try {
                for (LineaTramite linea : lineas) {
                    if (linea == null) {
                        throw new IllegalArgumentException("Línea inválida");
                    }
                    linea.validar();

                    String numero = numeroBase + "-xxx" + correlativo++;
                    String nombreItem = linea.getNombre();
                    String asuntoFinal = solicitudBase;
                    if (nombreItem != null && !nombreItem.isBlank()) {
                        asuntoFinal = solicitudBase.isBlank()
                                ? nombreItem.trim()
                                : solicitudBase + " - " + nombreItem.trim();
                    }

                    Tramite tramite = new Tramite();
                    tramite.setNro(numero);
                    tramite.setAsunto(asuntoFinal);
                    tramite.setSolicitante(solicitanteFinal);
                    tramite.setDescripcion(descripcionFinal);
                    tramite.setDestino(destinoFinal);
                    tramite.setFecha(LocalDateTime.now());

                    if (linea.esEspecial()) {
                        tramite.setEstado("PENDIENTE");
                        Long id = tramiteDao.create(tramite, cn);
                        tramite.setId(id);
                        generados.add(tramite);
                        continue;
                    }

                    long insumoId = linea.getInsumoId();
                    BigDecimal cantidad = BigDecimal.valueOf(linea.getCantidad());
                    BigDecimal actual = movimientoDao.stockActual(insumoId);
                    boolean hayStock = actual != null && actual.compareTo(cantidad) >= 0;

                    tramite.setEstado(hayStock ? "COMPLETADO" : "PENDIENTE");
                    Long tramiteId = tramiteDao.create(tramite, cn);
                    tramite.setId(tramiteId);
                    generados.add(tramite);

                    TramiteDetalle det = new TramiteDetalle();
                    det.setTramiteId(tramiteId);
                    Insumo ins = new Insumo();
                    ins.setId(insumoId);
                    det.setInsumo(ins);
                    det.setCantidad(cantidad);
                    tramiteDetalleDao.create(det, cn);

                    if (hayStock) {
                        boolean ok = insumoDao.decrementStock(cn, insumoId, cantidad);
                        if (!ok) {
                            throw new IllegalStateException("El stock cambió. Revisá cantidades e intentá nuevamente.");
                        }
                        movimientoDao.registrarSalida(cn, insumoId, cantidad, solicitanteFinal, tramiteId);
                    }
                }

                cn.commit();
                return new RegistroTramite(numeroBase, generados);
            } catch (Exception e) {
                try { cn.rollback(); } catch (SQLException ignored) {}
                if (e instanceof IllegalStateException) throw e;
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
        private final Long insumoId;
        private final String nombre;
        private final int cantidad;
        private final boolean especial;

        public LineaTramite(long insumoId, int cantidad) {
            this(insumoId, null, cantidad, false);
        }

        public LineaTramite(Long insumoId, String nombre, int cantidad, boolean especial) {
            this.insumoId = insumoId;
            this.nombre = nombre;
            this.cantidad = cantidad;
            this.especial = especial;
        }

        public static LineaTramite deInsumo(long insumoId, String nombre, int cantidad) {
            return new LineaTramite(insumoId, nombre, cantidad, false);
        }

        public static LineaTramite pedidoEspecial(String nombre, int cantidad) {
            return new LineaTramite(null, nombre, cantidad, true);
        }

        public void validar() {
            if (cantidad <= 0) {
                throw new IllegalArgumentException("Cantidad inválida para la línea");
            }
            if (especial) {
                if (nombre == null || nombre.isBlank()) {
                    throw new IllegalArgumentException("El pedido especial debe tener un nombre");
                }
            } else if (insumoId == null || insumoId <= 0) {
                throw new IllegalArgumentException("La línea debe referenciar un insumo válido");
            }
        }

        public Long getInsumoId() { return insumoId; }
        public String getNombre() { return nombre; }
        public int getCantidad() { return cantidad; }
        public boolean esEspecial() { return especial; }
    }

    public static class RegistroTramite {
        private final String numeroBase;
        private final List<Tramite> tramites;

        public RegistroTramite(String numeroBase, List<Tramite> tramites) {
            this.numeroBase = numeroBase;
            this.tramites = List.copyOf(tramites);
        }

        public String getNumeroBase() { return numeroBase; }
        public List<Tramite> getTramites() { return tramites; }
    }
}
