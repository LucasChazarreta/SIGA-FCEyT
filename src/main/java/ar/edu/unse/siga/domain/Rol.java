/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package ar.edu.unse.siga.domain;

import java.util.Objects;

public class Rol {
    private Integer id;
    private String nombre;

    public Rol() {}
    public Rol(Integer id, String nombre) { this.id = id; this.nombre = nombre; }

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    @Override public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Rol)) return false;
        Rol that = (Rol) o;
        return Objects.equals(id, that.id) && Objects.equals(nombre, that.nombre);
    }
    @Override public int hashCode() { return Objects.hash(id, nombre); }
    @Override public String toString() { return "Rol{id=" + id + ", nombre='" + nombre + "'}"; }
}
