package ar.edu.unse.siga.service;

import ar.edu.unse.siga.domain.FinanzaMovimiento;
import ar.edu.unse.siga.domain.FinanzaTipo;
import ar.edu.unse.siga.persistence.dao.FinanzaDao;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public class FinanzasService {

    private final FinanzaDao dao;

    public FinanzasService(FinanzaDao dao) {
        this.dao = dao;
    }

    public Long registrar(FinanzaMovimiento mov) {
        if (mov.getMonto() == null || mov.getMonto().compareTo(BigDecimal.ZERO) <= 0)
            throw new IllegalArgumentException("El monto debe ser positivo");
        if (mov.getFecha() == null) mov.setFecha(LocalDate.now());
        if (mov.getTipo() == null) throw new IllegalArgumentException("Tipo es requerido");
        if (mov.getCategoria() == null || mov.getCategoria().isBlank())
            throw new IllegalArgumentException("Categoría es requerida");
        return dao.insert(mov);
    }

    public boolean actualizar(FinanzaMovimiento mov) {
        if (mov.getId() == null) throw new IllegalArgumentException("Id requerido");
        return dao.update(mov);
    }

    public boolean eliminar(Long id) {
        return dao.delete(id);
    }

    public List<FinanzaMovimiento> buscar(LocalDate desde, LocalDate hasta, FinanzaTipo tipo, String categoriaLike) {
        return dao.search(desde, hasta, tipo, categoriaLike);
    }

    public BigDecimal totalIngresos(LocalDate desde, LocalDate hasta) {
        return dao.totalIngresos(desde, hasta);
    }

    public BigDecimal totalEgresos(LocalDate desde, LocalDate hasta) {
        return dao.totalEgresos(desde, hasta);
    }

    public Map<String, BigDecimal> totalPorCategoria(LocalDate desde, LocalDate hasta, FinanzaTipo tipo) {
        return dao.totalPorCategoria(desde, hasta, tipo);
    }
}
