package ar.edu.unse.siga.domain;

import java.time.LocalDateTime;
import java.util.Objects;

public class Tramite {
    private Long id;
    private String nro;
    private String asunto;
    private String estado; // p.ej. NUEVO, EN_PROCESO, CERRADO
    private LocalDateTime fecha;
    private String solicitante;
    private String descripcion; //7
    private String destino; //8
    

    public Tramite() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getNro() { return nro; }
    public void setNro(String nro) { this.nro = nro; }
    public String getAsunto() { return asunto; }
    public void setAsunto(String asunto) { this.asunto = asunto; }
    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }
    public LocalDateTime getFecha() { return fecha; }
    public void setFecha(LocalDateTime fecha) { this.fecha = fecha; }
    public String getSolicitante() { return solicitante; }
    public void setSolicitante(String solicitante) { this.solicitante = solicitante; }
    public String getDescripcion() {return descripcion;}
    public void setDescripcion(String descripcion) {this.descripcion = descripcion;}
    public String getDestino() {return destino;}
    public void setDestino(String destino) {this.destino = destino;}

    @Override public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Tramite)) return false;
        Tramite t = (Tramite) o;
        return Objects.equals(id, t.id) && Objects.equals(nro, t.nro);
    }

    @Override public int hashCode() { return Objects.hash(id, nro); }

    @Override public String toString() {
        return "Tramite{id=" + id + ", nro='" + nro + "', asunto='" + asunto + "', estado='" + estado + "', descripcion= " + descripcion+ "destino= " +destino ;
    }
}

