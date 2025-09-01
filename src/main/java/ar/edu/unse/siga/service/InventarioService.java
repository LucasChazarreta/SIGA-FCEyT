package ar.edu.unse.siga.service;

import ar.edu.unse.siga.domain.Insumo;
import ar.edu.unse.siga.persistence.dao.InsumoDao;

import java.util.List;
import java.util.Optional;

import ar.edu.unse.siga.domain.Movimiento;
import ar.edu.unse.siga.persistence.dao.MovimientoDao;

public class InventarioService {
    private final InsumoDao insumoDao;
    private MovimientoDao movimientoDao; // inyectable
    
    public InventarioService(InsumoDao insumoDao) {
        this.insumoDao = insumoDao;
    }

    public void setMovimientoDao(MovimientoDao movimientoDao) { this.movimientoDao = movimientoDao; }
    
    public Long registrarInsumo(Insumo i) {
        if (i.getCodigo() == null || i.getCodigo().isBlank())
            throw new IllegalArgumentException("El código es obligatorio");
        if (i.getDescripcion() == null || i.getDescripcion().isBlank())
            throw new IllegalArgumentException("La descripción es obligatoria");
        return insumoDao.create(i);
    }
    
    public void editarInsumo(Insumo i) { insumoDao.update(i); }

    public void bajaLogica(Long id) { insumoDao.softDelete(id); }

    public Optional<Insumo> buscarPorCodigo(String codigo) { return insumoDao.findByCodigo(codigo); }

    public List<Insumo> listarTodos() { return insumoDao.listAll(); }
    
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
        return movimientoDao.registrar(m);
    }
    
}


//al hacer el swing, la UI solo hablara con este servicio, nunca con JDBC directo :D atte luka.

