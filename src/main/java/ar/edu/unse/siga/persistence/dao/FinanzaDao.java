package ar.edu.unse.siga.persistence.dao;

import ar.edu.unse.siga.domain.FinanzaMovimiento;
import ar.edu.unse.siga.domain.FinanzaTipo;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface FinanzaDao {
    Long insert(FinanzaMovimiento mov);
    boolean update(FinanzaMovimiento mov);
    boolean delete(Long id);
    Optional<FinanzaMovimiento> findById(Long id);
    List<FinanzaMovimiento> search(LocalDate desde, LocalDate hasta, FinanzaTipo tipo, String categoriaLike);
    // Resumen por rango
    java.math.BigDecimal totalIngresos(LocalDate desde, LocalDate hasta);
    java.math.BigDecimal totalEgresos(LocalDate desde, LocalDate hasta);
    java.util.Map<String, java.math.BigDecimal> totalPorCategoria(LocalDate desde, LocalDate hasta, FinanzaTipo tipo);
}

