package ar.edu.unse.siga.ui;

import ar.edu.unse.siga.domain.Tramite;
import javax.swing.table.AbstractTableModel;
import java.util.ArrayList;
import java.util.List;

public class TramiteTableModel extends AbstractTableModel {
    private final String[] cols = {"ID","Nro","Asunto","Estado","Fecha","Solicitante"};
    private final List<Tramite> data = new ArrayList<>();

    public void setData(List<Tramite> list) {
        data.clear();
        if (list != null) data.addAll(list);
        fireTableDataChanged();
    }

    public Tramite getAt(int row) { return data.get(row); }

    @Override public int getRowCount() { return data.size(); }
    @Override public int getColumnCount() { return cols.length; }
    @Override public String getColumnName(int c) { return cols[c]; }

    @Override
    public Object getValueAt(int r, int c) {
        Tramite t = data.get(r);
        return switch (c) {
            case 0 -> t.getId();
            case 1 -> t.getNro();
            case 2 -> t.getAsunto();
            case 3 -> t.getEstado();
            case 4 -> t.getFecha();
            case 5 -> t.getSolicitante();
            default -> "";
        };
    }
}
