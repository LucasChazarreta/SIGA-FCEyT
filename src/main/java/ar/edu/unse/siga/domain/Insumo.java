package ar.edu.unse.siga.domain;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;

public class Insumo {
    private Long id;
    private String codigo;
    private String descripcion;
    private Categoria categoria;
    private Integer stockMinimo;
    private String ubicacion;
    private String estado;           // ACTIVO / INACTIVO
    private String tipo;             // INSUMO / BIEN
    private Instant createdAt;       // timestamp de creación (BD)
    private LocalDate fechaAlta;     // vista amigable para informes

    public Insumo() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getCodigo() { return codigo; }
    public void setCodigo(String codigo) { this.codigo = codigo; }

    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }

    public Categoria getCategoria() { return categoria; }
    public void setCategoria(Categoria categoria) { this.categoria = categoria; }

    public Integer getStockMinimo() { return stockMinimo; }
    public void setStockMinimo(Integer stockMinimo) { this.stockMinimo = stockMinimo; }

    public String getUbicacion() { return ubicacion; }
    public void setUbicacion(String ubicacion) { this.ubicacion = ubicacion; }

    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }

    public String getTipo() { return tipo; }
    public void setTipo(String tipo) { this.tipo = tipo; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    /** Fecha de alta “linda” para UI/Informes (derivada de BD). */
    public LocalDate getFechaAlta() { return fechaAlta; }
    public void setFechaAlta(LocalDate fechaAlta) { this.fechaAlta = fechaAlta; }

    @Override public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Insumo)) return false;
        Insumo that = (Insumo) o;
        return Objects.equals(id, that.id) && Objects.equals(codigo, that.codigo);
    }

    @Override public int hashCode() { return Objects.hash(id, codigo); }

    @Override public String toString() { return "Insumo{id=" + id + ", codigo='" + codigo + "'}"; }
}
