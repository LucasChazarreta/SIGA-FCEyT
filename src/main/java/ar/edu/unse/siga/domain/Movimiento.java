/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package ar.edu.unse.siga.domain;

import java.time.LocalDateTime;
import java.util.Objects;

public class Movimiento {
    private Long id;
    private Insumo insumo;
    private String tipo; // ENTRADA / SALIDA
    private java.math.BigDecimal cantidad;
    private String destinoFuente;
    private LocalDateTime fecha;
    private Usuario usuario;

    public Movimiento() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Insumo getInsumo() { return insumo; }
    public void setInsumo(Insumo insumo) { this.insumo = insumo; }
    public String getTipo() { return tipo; }
    public void setTipo(String tipo) { this.tipo = tipo; }
    public java.math.BigDecimal getCantidad() { return cantidad; }
    public void setCantidad(java.math.BigDecimal cantidad) { this.cantidad = cantidad; }
    public String getDestinoFuente() { return destinoFuente; }
    public void setDestinoFuente(String destinoFuente) { this.destinoFuente = destinoFuente; }
    public LocalDateTime getFecha() { return fecha; }
    public void setFecha(LocalDateTime fecha) { this.fecha = fecha; }
    public Usuario getUsuario() { return usuario; }
    public void setUsuario(Usuario usuario) { this.usuario = usuario; }

    @Override public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Movimiento)) return false;
        Movimiento that = (Movimiento) o;
        return Objects.equals(id, that.id);
    }
    @Override public int hashCode() { return Objects.hash(id); }
    @Override public String toString() { return "Movimiento{id=" + id + ", tipo='" + tipo + "'}"; }
}

