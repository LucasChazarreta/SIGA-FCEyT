/*tableModel para mostrar insumos */
package ar.edu.unse.siga.ui;

import ar.edu.unse.siga.domain.Insumo;
import java.util.ArrayList;
import java.util.List;
import javax.swing.table.AbstractTableModel;

public class InsTableModel extends AbstractTableModel {
    private final String[] cols = {"ID","Código","Descripción","Categoría","Stock mín.","Ubicación","Estado"};
    private final List<Insumo> data = new ArrayList<>();

    public void setData(List<Insumo> insumos) {
        data.clear();
        if (insumos != null) data.addAll(insumos);
        fireTableDataChanged();
    }
    public Insumo getAt(int row) { return data.get(row); }

    @Override public int getRowCount() { return data.size(); }
    @Override public int getColumnCount() { return cols.length; }
    @Override public String getColumnName(int c) { return cols[c]; }

    @Override
    public Object getValueAt(int row, int col) {
        Insumo i = data.get(row);
        switch (col) {
            case 0: return i.getId();
            case 1: return i.getCodigo();
            case 2: return i.getDescripcion();
            case 3: return (i.getCategoria()!=null? i.getCategoria().getNombre(): "-");
            case 4: return i.getStockMinimo();
            case 5: return i.getUbicacion();
            case 6: return i.getEstado();
            default: return "";
        }
    }
}

