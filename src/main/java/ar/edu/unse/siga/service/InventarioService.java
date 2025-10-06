package ar.edu.unse.siga.service;

import ar.edu.unse.siga.common.CurrentSession;
import ar.edu.unse.siga.domain.Insumo;
import ar.edu.unse.siga.domain.Movimiento;
import ar.edu.unse.siga.domain.Usuario;
import ar.edu.unse.siga.persistence.dao.InsumoDao;
import ar.edu.unse.siga.persistence.dao.MovimientoDao;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Servicio de Inventario ---------------------- Es la capa intermedia entre la
 * UI (Swing) y los DAOs (JDBC). Se encarga de validar, orquestar operaciones y
 * aplicar reglas de negocio, como el control de stock mínimo.
 */
public class InventarioService {

    private final InsumoDao insumoDao;
    private MovimientoDao movimientoDao; // inyectable

    public InventarioService(InsumoDao insumoDao) {
        this.insumoDao = insumoDao;
    }

    public void setMovimientoDao(MovimientoDao movimientoDao) {
        this.movimientoDao = movimientoDao;
    }

    // =====================================================
    // === CRUD DE INSUMOS
    // =====================================================
    public Long registrarInsumo(Insumo i) {
        if (i.getCodigo() == null || i.getCodigo().isBlank()) {
            throw new IllegalArgumentException("El código es obligatorio");
        }
        if (i.getDescripcion() == null || i.getDescripcion().isBlank()) {
            throw new IllegalArgumentException("La descripción es obligatoria");
        }
        return insumoDao.create(i);
    }

    public void editarInsumo(Insumo i) {
        insumoDao.update(i);
    }

    public void bajaLogica(Long id) {
        insumoDao.softDelete(id);
    }

    public Optional<Insumo> buscarPorCodigo(String codigo) {
        return insumoDao.findByCodigo(codigo);
    }

    public List<Insumo> listarTodos() {
        return insumoDao.listAll();
    }

    // =====================================================
    // === MOVIMIENTOS + CONTROL DE STOCK MÍNIMO
    // =====================================================
    /**
     * Resultado del control de stock luego de registrar un movimiento.
     */
    public static class StockCheckResult {

        public final long insumoId;
        public final int stockActual;
        public final Integer stockMinimo;
        public final boolean bajoMinimo;

        public StockCheckResult(long insumoId, int stockActual, Integer stockMinimo) {
            this.insumoId = insumoId;
            this.stockActual = stockActual;
            this.stockMinimo = stockMinimo;
            this.bajoMinimo = (stockMinimo != null) && (stockActual < stockMinimo);
        }
    }

    /**
     * Registra un movimiento de inventario (ENTRADA o SALIDA) y devuelve
     * información de control de stock.
     */
    public StockCheckResult registrarMovimiento(Long insumoId, String tipo, int cantidad, String destinoFuente) {
        if (movimientoDao == null) {
            throw new IllegalStateException("MovimientoDao no inicializado");
        }

        // Buscar insumo asociado
        var insumo = insumoDao.listAll().stream()
                .filter(i -> i.getId().equals(insumoId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Insumo no encontrado id=" + insumoId));

        // Crear movimiento
        Movimiento m = new Movimiento();
        m.setInsumo(insumo);
        m.setTipo(tipo);               // ENTRADA o SALIDA
        m.setCantidad(cantidad);
        m.setDestinoFuente(destinoFuente);

        Usuario u = CurrentSession.getUser();
        if (u != null) {
            m.setUsuario(u);
        }

        // Registrar en BD
        movimientoDao.registrar(m);

        // Calcular stock actual y comparar con stock mínimo
        int stockAct = movimientoDao.stockActual(insumoId);
        return new StockCheckResult(insumoId, stockAct, insumo.getStockMinimo());
    }

    // =====================================================
    // === CONSULTAS
    // =====================================================
    public int totalInsumos() {
        return listarTodos().size();
    }

    public BigDecimal gastosMensuales(LocalDate desde, LocalDate hasta) {
        // placeholder: implementar cuando definan el modelo de gastos/movimientos
        return BigDecimal.ZERO;
    }

    public List<Insumo> buscarInsumos(String categoriaLike, LocalDate desde, LocalDate hasta) {
        // Por ahora filtramos solo por categoría en memoria. Ignoramos fechas.
        return listarTodos().stream()
                .filter(i -> {
                    if (categoriaLike == null || categoriaLike.isBlank()) {
                        return true;
                    }
                    var c = i.getCategoria();
                    return c != null && c.getNombre() != null
                            && c.getNombre().toLowerCase().contains(categoriaLike.toLowerCase());
                })
                .toList();
    }

    public List<Movimiento> ultimosMovimientos(long insumoId, int limit) {
        if (movimientoDao == null) {
            throw new IllegalStateException("MovimientoDao no inicializado");
        }
        return movimientoDao.ultimosPorInsumo(insumoId, limit);
    }

    public int stockActual(long insumoId) {
        if (movimientoDao == null) {
            throw new IllegalStateException("MovimientoDao no inicializado");
        }
        return movimientoDao.stockActual(insumoId);
    }
}
