package ar.edu.unse.siga.service;

import ar.edu.unse.siga.common.CurrentSession;
import ar.edu.unse.siga.domain.Categoria;
import ar.edu.unse.siga.domain.Insumo;
import ar.edu.unse.siga.domain.Movimiento;
import ar.edu.unse.siga.domain.Usuario;
import ar.edu.unse.siga.persistence.dao.CategoriaDao;
import ar.edu.unse.siga.persistence.dao.InsumoDao;
import ar.edu.unse.siga.persistence.dao.MovimientoDao;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class InventarioService {

    private final InsumoDao insumoDao;
    private final MovimientoDao movimientoDao;
    private final CategoriaDao categoriaDao;

    public InventarioService(InsumoDao insumoDao, MovimientoDao movimientoDao, CategoriaDao categoriaDao) {
        this.insumoDao = insumoDao;
        this.movimientoDao = movimientoDao;
        this.categoriaDao = categoriaDao;
    }

    // === Categorías ===
    public List<Categoria> listarCategorias() {
        // si preferís sin orden alfabético, podés usar categoriaDao.listAll()
        return categoriaDao.findAllOrderByNombre();
    }

    // === Insumos ===
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

    // === Movimientos ===
    public Long registrarMovimiento(Long insumoId, String tipo, int cantidad, String destinoFuente) {
        var insumo = insumoDao.listAll().stream()
                .filter(i -> i.getId().equals(insumoId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Insumo no encontrado id=" + insumoId));

        Movimiento m = new Movimiento();
        m.setInsumo(insumo);
        m.setTipo(tipo);
        m.setCantidad(cantidad);
        m.setDestinoFuente(destinoFuente);

        Usuario u = CurrentSession.getUser();
        if (u != null) m.setUsuario(u);

        return movimientoDao.registrar(m);
    }

    public int totalInsumos() {
        return listarTodos().size();
    }

    public java.math.BigDecimal gastosMensuales(LocalDate desde, LocalDate hasta) {
        // placeholder hasta definir métrica de gastos
        return java.math.BigDecimal.ZERO;
    }

    // Búsqueda simple (filtro local por nombre de categoría)
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
}
