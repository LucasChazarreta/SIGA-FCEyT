package ar.edu.unse.siga.ui.inventario;

import ar.edu.unse.siga.domain.Categoria;
import ar.edu.unse.siga.domain.Insumo;
import ar.edu.unse.siga.persistence.DataSourceFactory;
import ar.edu.unse.siga.ui.base.Dialogs;
import ar.edu.unse.siga.ui.base.Ui;

import javax.swing.*;
import java.awt.*;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class InsumoFormDialog extends JDialog {
    private final JTextField txtCodigo = new JTextField(15);
    private final JTextField txtDescripcion = new JTextField(30);
    private final JComboBox<Categoria> cbCategoria = new JComboBox<>();
    private final JSpinner spStockMin = new JSpinner(new SpinnerNumberModel(0,0,999999,1));
    private final JTextField txtUbicacion = new JTextField(20);
    private final JComboBox<String> cbEstado = new JComboBox<>(new String[]{"ACTIVO","INACTIVO"});

    private boolean accepted = false;
    private final Insumo editing;

    public InsumoFormDialog(Window owner, Insumo editing) {
        super(owner, editing==null? "Nuevo Insumo" : "Editar Insumo", ModalityType.APPLICATION_MODAL);
        this.editing = editing;

        loadCategorias();
        if (editing!=null) {
            txtCodigo.setText(editing.getCodigo());
            txtCodigo.setEnabled(false);
            txtDescripcion.setText(editing.getDescripcion());
            cbCategoria.setSelectedItem(editing.getCategoria());
            spStockMin.setValue(editing.getStockMinimo()==null?0:editing.getStockMinimo());
            txtUbicacion.setText(editing.getUbicacion());
            cbEstado.setSelectedItem(editing.getEstado()==null?"ACTIVO":editing.getEstado());
        } else {
            cbEstado.setSelectedItem("ACTIVO");
        }

        var form = Ui.grid2Cols(
                new String[]{"Código:","Descripción:","Categoría:","Stock mínimo:","Ubicación:","Estado:"},
                new JComponent[]{txtCodigo, txtDescripcion, cbCategoria, spStockMin, txtUbicacion, cbEstado}
        );
        var ok = new JButton("Guardar");
        var cancel = new JButton("Cancelar");
        ok.addActionListener(e -> onSave());
        cancel.addActionListener(e -> dispose());

        setLayout(new BorderLayout(10,10));
        add(form, BorderLayout.CENTER);
        add(Ui.flowRight(cancel, ok), BorderLayout.SOUTH);

        pack();
        setLocationRelativeTo(owner);
    }

    private void loadCategorias() {
        List<Categoria> list = new ArrayList<>();
        String sql="SELECT id, nombre FROM categoria ORDER BY nombre";
        try (var cn = DataSourceFactory.getConnection();
             var ps = cn.prepareStatement(sql);
             var rs = ps.executeQuery()) {
            while(rs.next()) list.add(new Categoria(rs.getInt("id"), rs.getString("nombre")));
        } catch(Exception e) { Ui.error(this,e); }

        DefaultComboBoxModel<Categoria> model = new DefaultComboBoxModel<>();
        list.forEach(model::addElement);
        cbCategoria.setModel(model);
    }

    private void onSave() {
        try {
            Dialogs.require(txtCodigo, "Código");
            Dialogs.require(txtDescripcion, "Descripción");
            accepted = true;
            setVisible(false);
        } catch (Exception e) { Ui.error(this,e); }
    }

    public boolean isAccepted(){ return accepted; }

    public String getCodigo(){ return txtCodigo.getText().trim(); }
    public String getDescripcion(){ return txtDescripcion.getText().trim(); }
    public Categoria getCategoria(){ return (Categoria) cbCategoria.getSelectedItem(); }
    public int getStockMinimo(){ return Dialogs.intFromSpinner(spStockMin); }
    public String getUbicacion(){ return txtUbicacion.getText().trim(); }
    public String getEstado(){ return (String) cbEstado.getSelectedItem(); }
}
