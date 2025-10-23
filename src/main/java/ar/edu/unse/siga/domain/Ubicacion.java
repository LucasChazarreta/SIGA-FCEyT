package ar.edu.unse.siga.domain;

import java.util.Objects;

public class Ubicacion {
    private Integer id;
    private String nombre;

    public Ubicacion() {}

    public Ubicacion(Integer id, String nombre) {
        this.id = id;
        this.nombre = nombre;
    }

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    @Override public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Ubicacion)) return false;
        Ubicacion that = (Ubicacion) o;
        return Objects.equals(id, that.id) && Objects.equals(nombre, that.nombre);
    }

    @Override public int hashCode() {
        return Objects.hash(id, nombre);
    }

    @Override public String toString() {
        return nombre == null ? "" : nombre;
    }
}
