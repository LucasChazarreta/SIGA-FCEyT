package ar.edu.unse.siga.domain;

import java.math.BigDecimal;
import java.time.LocalDate;

public class FinanzaMovimiento {
    private Long id;
    private FinanzaTipo tipo;
    private String categoria;
    private BigDecimal monto;
    private LocalDate fecha;
    private String referencia;

    public FinanzaMovimiento() {}

    public FinanzaMovimiento(Long id, FinanzaTipo tipo, String categoria, BigDecimal monto, LocalDate fecha, String referencia) {
        this.id = id;
        this.tipo = tipo;
        this.categoria = categoria;
        this.monto = monto;
        this.fecha = fecha;
        this.referencia = referencia;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public FinanzaTipo getTipo() { return tipo; }
    public void setTipo(FinanzaTipo tipo) { this.tipo = tipo; }
    public String getCategoria() { return categoria; }
    public void setCategoria(String categoria) { this.categoria = categoria; }
    public java.math.BigDecimal getMonto() { return monto; }
    public void setMonto(java.math.BigDecimal monto) { this.monto = monto; }
    public java.time.LocalDate getFecha() { return fecha; }
    public void setFecha(java.time.LocalDate fecha) { this.fecha = fecha; }
    public String getReferencia() { return referencia; }
    public void setReferencia(String referencia) { this.referencia = referencia; }
}
