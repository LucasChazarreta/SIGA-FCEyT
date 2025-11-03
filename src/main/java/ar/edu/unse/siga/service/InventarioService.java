package ar.edu.unse.siga.service;

import ar.edu.unse.siga.common.CurrentSession;
import ar.edu.unse.siga.common.RoleName;
import ar.edu.unse.siga.domain.Categoria;
import ar.edu.unse.siga.domain.Insumo;
import ar.edu.unse.siga.domain.Movimiento;
import ar.edu.unse.siga.domain.Ubicacion;
import ar.edu.unse.siga.domain.Usuario;
import ar.edu.unse.siga.persistence.dao.CategoriaDao;
import ar.edu.unse.siga.persistence.dao.InsumoDao;
import ar.edu.unse.siga.persistence.dao.MovimientoDao;
import ar.edu.unse.siga.persistence.dao.UbicacionDao;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class InventarioService {

    private final InsumoDao insumoDao;
    private final MovimientoDao movimientoDao;
    private final CategoriaDao categoriaDao;
    private final UbicacionDao ubicacionDao;

    public InventarioService(InsumoDao insumoDao,
                             MovimientoDao movimientoDao,
                             CategoriaDao categoriaDao,
                             UbicacionDao ubicacionDao) {
        this.insumoDao = insumoDao;
        this.movimientoDao = movimientoDao;
        this.categoriaDao = categoriaDao;
        this.ubicacionDao = ubicacionDao;
    }

    // === Categorías ===
    public List<Categoria> listarCategorias() {
        return categoriaDao.findAllOrderByNombre();
    }

    public List<Categoria> listarCategoriasIncluyendoInactivas() {
        return categoriaDao.listAllIncludingInactive();
    }

    public Categoria crearCategoria(String nombre) {
        if (nombre == null || nombre.isBlank()) {
            throw new IllegalArgumentException("El nombre de la categoría es obligatorio");
        }
        Categoria c = new Categoria();
        c.setNombre(nombre.trim());
        categoriaDao.create(c);
        return c;
    }

    public void actualizarCategoria(Categoria categoria) {
        if (categoria == null || categoria.getId() <= 0) {
            throw new IllegalArgumentException("Categoría inválida");
        }
        categoriaDao.update(categoria);
    }

    public void bajaLogicaCategoria(int id) {
        categoriaDao.softDelete(id);
    }

    public void restaurarCategoria(int id) {
        categoriaDao.restore(id);
    }

    public List<Ubicacion> listarUbicaciones() {
        if (ubicacionDao == null) return java.util.List.of();
        return ubicacionDao.listAll();
    }

    public List<Ubicacion> listarUbicacionesIncluyendoInactivas() {
        if (ubicacionDao == null) return java.util.List.of();
        return ubicacionDao.listAllIncludingInactive();
    }

    public Ubicacion crearUbicacion(String nombre) {
        if (ubicacionDao == null) {
            throw new IllegalStateException("Gestión de ubicaciones no disponible");
        }
        if (nombre == null || nombre.isBlank()) {
            throw new IllegalArgumentException("El nombre de la ubicación es obligatorio");
        }
        Ubicacion u = new Ubicacion();
        u.setNombre(nombre.trim());
        return ubicacionDao.create(u);
    }

    public void actualizarUbicacion(Ubicacion ubicacion) {
        if (ubicacionDao == null) {
            throw new IllegalStateException("Gestión de ubicaciones no disponible");
        }
        if (ubicacion == null || ubicacion.getId() == null) {
            throw new IllegalArgumentException("Ubicación inválida");
        }
        ubicacionDao.update(ubicacion);
    }

    public void bajaLogicaUbicacion(int id) {
        if (ubicacionDao == null) return;
        ubicacionDao.softDelete(id);
    }

    public void restaurarUbicacion(int id) {
        if (ubicacionDao == null) return;
        ubicacionDao.restore(id);
    }

    // === Insumos ===
    public Long registrarInsumo(Insumo i) {
        if (i == null) throw new IllegalArgumentException("Insumo inválido");
        if (i.getCodigo() == null || i.getCodigo().isBlank()) {
            throw new IllegalArgumentException("El código es obligatorio");
        }
        if (i.getDescripcion() == null || i.getDescripcion().isBlank()) {
            throw new IllegalArgumentException("La descripción es obligatoria");
        }
        i.setEstado(normalizeEstado(i.getEstado()));
        i.setTipo(normalizeTipo(i.getTipo()));
        return insumoDao.create(i);
    }

    public void editarInsumo(Insumo i) {
        if (i == null || i.getId() == null) {
            throw new IllegalArgumentException("Insumo inválido");
        }
        Insumo original = insumoDao.findById(i.getId())
                .orElseThrow(() -> new IllegalArgumentException("Insumo no encontrado id=" + i.getId()));

        String nuevoEstado = normalizeEstado(i.getEstado());
        String estadoOriginal = normalizeEstado(original.getEstado());
        if (!estadoOriginal.equals(nuevoEstado) && !usuarioActualEsAdmin()) {
            throw new IllegalStateException("Solo usuarios ADMIN pueden cambiar el estado del insumo.");
        }

        i.setEstado(nuevoEstado);
        i.setTipo(normalizeTipo(i.getTipo()));
        insumoDao.update(i);
    }

    public void bajaLogica(Long id) {
        if (id == null) throw new IllegalArgumentException("Id requerido");
        BigDecimal stock = movimientoDao.stockActual(id);
        if (stock != null && stock.compareTo(BigDecimal.ZERO) > 0) {
            throw new IllegalStateException("Stock actual: " + formatCantidad(stock)
                    + ". No se puede dar de baja");
        }
        insumoDao.softDelete(id);
    }

    public Optional<Insumo> buscarPorCodigo(String codigo) { return insumoDao.findByCodigo(codigo); }

    public List<Insumo> listarTodos() { return insumoDao.listAll(); }

    public List<Insumo> insumosConStockDisponible() {
        return insumoDao.listActivosConStock();
    }

    // === Movimientos ===
    public Long registrarMovimiento(Long insumoId,
                                    String tipo,
                                    BigDecimal cantidad,
                                    String destinoFuente,
                                    String solicitante) {
        if (insumoId == null) {
            throw new IllegalArgumentException("Insumo no especificado");
        }
        Insumo insumo = insumoDao.findById(insumoId)
                .orElseThrow(() -> new IllegalArgumentException("Insumo no encontrado id=" + insumoId));

        if (!"ACTIVO".equalsIgnoreCase(normalizeEstado(insumo.getEstado()))) {
            throw new IllegalStateException("El insumo está INACTIVO; no se pueden registrar movimientos.");
        }

        String tipoNorm = tipo == null ? "" : tipo.trim().toUpperCase();
        if (!tipoNorm.equals("ENTRADA") && !tipoNorm.equals("SALIDA")) {
            throw new IllegalArgumentException("Tipo de movimiento inválido");
        }

        if (cantidad == null) {
            throw new IllegalArgumentException("Cantidad inválida");
        }
        BigDecimal qty = cantidad.stripTrailingZeros();
        if (qty.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("La cantidad debe ser mayor que cero");
        }

        boolean esBien = "BIEN".equalsIgnoreCase(normalizeTipo(insumo.getTipo()));
        if (esBien && qty.scale() > 0) {
            throw new IllegalArgumentException("Para Bienes patrimoniales, la cantidad debe ser un entero.");
        }

        BigDecimal stockActual = movimientoDao.stockActual(insumoId);
        if (tipoNorm.equals("SALIDA") && qty.compareTo(stockActual) > 0) {
            throw new IllegalStateException(String.format(
                    "Stock insuficiente: actual %s, salida %s.",
                    formatCantidad(stockActual), formatCantidad(qty)));
        }

        String destino = destinoFuente == null ? "" : destinoFuente.trim();
        String solicitanteNorm = solicitante == null ? "" : solicitante.trim();

        if ("SALIDA".equalsIgnoreCase(tipoNorm)) {
            if (solicitanteNorm.isBlank()) {
                throw new IllegalArgumentException("Debe indicar el solicitante para registrar la salida.");
            }
        }

        Movimiento m = new Movimiento();
        m.setInsumo(insumo);
        m.setTipo(tipoNorm);
        m.setCantidad(qty);
        m.setDestinoFuente(destino);
        m.setSolicitante(solicitanteNorm.isBlank() ? null : solicitanteNorm);

        Usuario u = CurrentSession.getUser();
        if (u != null) m.setUsuario(u);

        return movimientoDao.registrar(m);
    }

    public int totalInsumos() { return listarTodos().size(); }

    public java.math.BigDecimal gastosMensuales(LocalDate desde, LocalDate hasta) {
        return java.math.BigDecimal.ZERO;
    }

    public List<Insumo> buscarInsumos(String categoriaLike, LocalDate desde, LocalDate hasta) {
        return listarTodos().stream()
                .filter(i -> {
                    if (categoriaLike == null || categoriaLike.isBlank()) return true;
                    var c = i.getCategoria();
                    return c != null && c.getNombre() != null
                            && c.getNombre().toLowerCase().contains(categoriaLike.toLowerCase());
                })
                .collect(Collectors.toList());
    }

    // --- BEGIN API compatible con la UI ---

    public static class StockCheckResult {
        public final Long insumoId;
        public final BigDecimal stockActual;
        public final Integer stockMinimo;
        public final boolean bajoMinimo;

        public StockCheckResult(Long insumoId, BigDecimal stockActual, Integer stockMinimo) {
            this.insumoId = insumoId;
            this.stockActual = stockActual == null ? BigDecimal.ZERO : stockActual;
            this.stockMinimo = stockMinimo;
            this.bajoMinimo = stockMinimo != null
                    && this.stockActual.compareTo(BigDecimal.valueOf(stockMinimo)) < 0;
        }

        public static StockCheckResult of(Long insumoId, BigDecimal actual, Integer minimo) {
            return new StockCheckResult(insumoId, actual, minimo);
        }

        public Long getInsumoId() { return insumoId; }
        public BigDecimal getStockActualDecimal() { return stockActual; }
        public int getStockActual() { return stockActual.setScale(0, RoundingMode.DOWN).intValue(); }
        public Integer getStockMinimo() { return stockMinimo; }
        public boolean isBajoMinimo() { return bajoMinimo; }
    }

    // Consulta “entera” (para llamadas antiguas)
    public BigDecimal stockActualExacto(long insumoId) {
        BigDecimal stock = movimientoDao.stockActual(insumoId);
        return stock == null ? BigDecimal.ZERO : stock;
    }

    public int stockActual(long insumoId) {
        return stockActualExacto(insumoId).setScale(0, RoundingMode.DOWN).intValue();
    }

    // Para pantallas que quieren también el mínimo
    public StockCheckResult stockActual(Long insumoId) {
        BigDecimal actual = (insumoId == null) ? BigDecimal.ZERO : stockActualExacto(insumoId);
        Integer minimo = null;
        if (insumoId != null) {
            minimo = insumoDao.listAll().stream()
                    .filter(i -> insumoId.equals(i.getId()))
                    .map(Insumo::getStockMinimo)
                    .findFirst()
                    .orElse(null);
        }
        return StockCheckResult.of(insumoId, actual, minimo);
    }

    public StockCheckResult stockCheck(Long insumoId) { return stockActual(insumoId); }

    public List<Movimiento> ultimosMovimientos(Long insumoId, int limit) {
        if (insumoId == null) return java.util.Collections.emptyList();
        return movimientoDao.ultimosPorInsumo(insumoId, limit);
    }

    public List<Movimiento> movimientosPorFechaYTipo(LocalDate desde, LocalDate hasta, String tipo) {
        LocalDateTime d1 = desde == null ? null : desde.atStartOfDay();
        LocalDateTime d2 = hasta == null ? null : hasta.plusDays(1).atStartOfDay().minusNanos(1);
        return movimientoDao.buscarPorFechaYTipo(d1, d2, tipo);
    }

    // --- END API compatible con la UI ---
    
    public BigDecimal totalEntradasExactas(long insumoId) {
        BigDecimal total = movimientoDao.totalEntradas(insumoId);
        return total == null ? BigDecimal.ZERO : total;
    }

    public BigDecimal totalSalidasExactas(long insumoId) {
        BigDecimal total = movimientoDao.totalSalidas(insumoId);
        return total == null ? BigDecimal.ZERO : total;
    }

    public int totalEntradasDeInsumo(long insumoId) {
        return totalEntradasExactas(insumoId).setScale(0, RoundingMode.DOWN).intValue();
    }

    public int totalSalidasDeInsumo(long insumoId) {
        return totalSalidasExactas(insumoId).setScale(0, RoundingMode.DOWN).intValue();
    }

    private String normalizeEstado(String estado) {
        if (estado == null || estado.isBlank()) return "ACTIVO";
        String up = estado.trim().toUpperCase();
        return up.equals("INACTIVO") ? "INACTIVO" : "ACTIVO";
    }

    private String normalizeTipo(String tipo) {
        if (tipo == null || tipo.isBlank()) return "INSUMO";
        String up = tipo.trim().toUpperCase();
        return up.equals("BIEN") ? "BIEN" : "INSUMO";
    }

    private boolean usuarioActualEsAdmin() {
        Usuario u = CurrentSession.getUser();
        return RoleName.isAdmin(u);
    }

    private String formatCantidad(BigDecimal valor) {
        if (valor == null) return "0";
        return valor.stripTrailingZeros().toPlainString();
    }
}
