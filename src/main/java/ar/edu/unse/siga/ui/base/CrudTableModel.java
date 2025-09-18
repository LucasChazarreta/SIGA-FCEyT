package ar.edu.unse.siga.ui.base;

import javax.swing.table.AbstractTableModel;
import java.util.ArrayList;
import java.util.List;

public abstract class CrudTableModel<T> extends AbstractTableModel {
    protected final String[] columns;
    protected final List<T> data = new ArrayList<>();

    protected CrudTableModel(String[] columns){ this.columns = columns; }

    public void setData(List<T> list) {
        data.clear();
        if (list!=null) data.addAll(list);
        fireTableDataChanged();
    }
    public T getAt(int row){ return data.get(row); }
    @Override public int getRowCount(){ return data.size(); }
    @Override public int getColumnCount(){ return columns.length; }
    @Override public String getColumnName(int c){ return columns[c]; }
}
