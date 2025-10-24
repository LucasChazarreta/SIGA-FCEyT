package ar.edu.unse.siga.ui.inventario;

import ar.edu.unse.siga.domain.Categoria;
import ar.edu.unse.siga.persistence.DataSourceFactory;
import ar.edu.unse.siga.ui.base.BaseCrudFrame;
import ar.edu.unse.siga.ui.base.CrudTableModel;
import ar.edu.unse.siga.ui.base.Ui;

import javax.swing.*;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class CategoriaFrame extends BaseCrudFrame<Categoria> {

    private final CategoriaTableModel model = new CategoriaTableModel();

    public CategoriaFrame(Object ignoredServiceForNow) {
        super("ABM Categorías");
        table.setModel(model);
        loadData();
    }

    @Override protected void loadData() {
        try (var cn = DataSourceFactory.getConnection();
             var ps = cn.prepareStatement("SELECT id,nombre FROM categoria WHERE activo = 1 ORDER BY nombre");
             var rs = ps.executeQuery()) {
            List<Categoria> list = new ArrayList<>();
            while (rs.next()) list.add(new Categoria(rs.getInt("id"), rs.getString("nombre")));
            model.setData(list);
        } catch (Exception e){ Ui.error(this,e); }
    }

    @Override protected void onNuevo() {
        String nombre = JOptionPane.showInputDialog(this, "Nombre de categoría:");
        if (nombre==null || nombre.isBlank()) return;
        try (var cn = DataSourceFactory.getConnection();
             var ps = cn.prepareStatement("INSERT INTO categoria(nombre) VALUES(?)")) {
            ps.setString(1, nombre.trim());
            ps.executeUpdate();
            loadData();
        } catch (Exception e){ Ui.error(this,e); }
    }

    @Override protected void onEditar() {
        int r = selectedRowOrWarn(); if (r<0) return;
        var sel = model.getAt(r);
        String nombre = JOptionPane.showInputDialog(this, "Nuevo nombre:", sel.getNombre());
        if (nombre==null || nombre.isBlank()) return;
        try (var cn = DataSourceFactory.getConnection();
             var ps = cn.prepareStatement("UPDATE categoria SET nombre=? WHERE id=?")) {
            ps.setString(1, nombre.trim()); ps.setInt(2, sel.getId());
            ps.executeUpdate(); loadData();
        } catch (Exception e){ Ui.error(this,e); }
    }

    @Override protected void onBaja() {
        Ui.info(this, "No se implementa baja de categoría (evitar orfanatos).");
    }

    static class CategoriaTableModel extends CrudTableModel<Categoria> {
        CategoriaTableModel(){ super(new String[]{"ID","Nombre"}); }
        @Override public Object getValueAt(int row, int col) {
            var c = data.get(row);
            return col==0? c.getId() : c.getNombre();
        }
    }
}
