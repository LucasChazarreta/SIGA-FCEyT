package ar.edu.unse.siga.domain;

import java.math.BigDecimal;
import java.util.Objects;

public class TramiteDetalle {

    private Long id;
    private Long tramiteId;
    private Insumo insumo;
    private BigDecimal cantidad;
    private String observacion;

    public TramiteDetalle() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getTramiteId() { return tramiteId; }
    public void setTramiteId(Long tramiteId) { this.tramiteId = tramiteId; }

    public Insumo getInsumo() { return insumo; }
    public void setInsumo(Insumo insumo) { this.insumo = insumo; }

    public BigDecimal getCantidad() { return cantidad; }
    public void setCantidad(BigDecimal cantidad) { this.cantidad = cantidad; }

    public String getObservacion() { return observacion; }
    public void setObservacion(String observacion) { this.observacion = observacion; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TramiteDetalle)) return false;
        TramiteDetalle that = (TramiteDetalle) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() { return Objects.hash(id); }
}
