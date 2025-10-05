/* dialogo para crear/editar un insumo */
package ar.edu.unse.siga.ui;

import ar.edu.unse.siga.domain.Categoria;
import ar.edu.unse.siga.domain.Insumo;

import javax.swing.*;
import java.awt.*;
import java.util.List;

public class InsFormDialog extends JDialog {
    private final JTextField txtCodigo = new JTextField(15);
    private final JTextField txtDescripcion = new JTextField(25);
    private final JComboBox<Categoria> cbCategoria = new JComboBox<>();
    private final JSpinner spStockMin = new JSpinner(new SpinnerNumberModel(0,0,999999,1));
    private final JTextField txtUbicacion = new JTextField(20);
    private final JComboBox<String> cbEstado = new JComboBox<>(new String[]{"ACTIVO","INACTIVO"});
    private boolean accepted = false;

    public InsFormDialog(Frame owner, String title, List<Categoria> categorias, Insumo edit) {
        super(owner, title, true);
        setLayout(new BorderLayout(10,10));

        // Panel form
        JPanel form = new JPanel(new GridLayout(0,2,8,8));
        form.add(new JLabel("Código:"));
        form.add(txtCodigo);
        form.add(new JLabel("Descripción:"));
        form.add(txtDescripcion);
        form.add(new JLabel("Categoría:"));
        form.add(cbCategoria);
        form.add(new JLabel("Stock mínimo:"));
        form.add(spStockMin);
        form.add(new JLabel("Ubicación:"));
        form.add(txtUbicacion);
        form.add(new JLabel("Estado:"));
        form.add(cbEstado);
        add(form, BorderLayout.CENTER);

        // Cargar categorías
        if (categorias != null) {
            DefaultComboBoxModel<Categoria> model = new DefaultComboBoxModel<>();
            for (Categoria c : categorias) model.addElement(c);
            cbCategoria.setModel(model);
        }

        // Si es edición, setear valores
        if (edit != null) {
            txtCodigo.setText(edit.getCodigo());
            txtCodigo.setEnabled(false); // no permitir cambiar código en edición
            txtDescripcion.setText(edit.getDescripcion());
            if (edit.getCategoria()!=null) cbCategoria.setSelectedItem(edit.getCategoria());
            spStockMin.setValue(edit.getStockMinimo()!=null? edit.getStockMinimo():0);
            txtUbicacion.setText(edit.getUbicacion());
            cbEstado.setSelectedItem(edit.getEstado()!=null? edit.getEstado():"ACTIVO");
        } else {
            cbEstado.setSelectedItem("ACTIVO");
        }

        // Botones
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton btnOk = new JButton("Guardar");
        JButton btnCancel = new JButton("Cancelar");
        buttons.add(btnCancel);
        buttons.add(btnOk);
        add(buttons, BorderLayout.SOUTH);

        btnOk.addActionListener(e -> {
            if (txtCodigo.getText().isBlank()) {
                JOptionPane.showMessageDialog(this, "El código es obligatorio");
                return;
            }
            if (txtDescripcion.getText().isBlank()) {
                JOptionPane.showMessageDialog(this, "La descripción es obligatoria");
                return;
            }
            accepted = true;
            setVisible(false);
        });
        btnCancel.addActionListener(e -> setVisible(false));

        pack();
        setLocationRelativeTo(owner);
    }

    public boolean isAccepted() { return accepted; }

    // Métodos para leer lo que ingresó el usuario
    public String getCodigo() { return txtCodigo.getText().trim(); }
    public String getDescripcion() { return txtDescripcion.getText().trim(); }
    public Categoria getCategoria() { return (Categoria) cbCategoria.getSelectedItem(); }
    public int getStockMinimo() { return (Integer) spStockMin.getValue(); }
    public String getUbicacion() { return txtUbicacion.getText().trim(); }
    public String getEstado() { return (String) cbEstado.getSelectedItem(); }
}

// Importante: Para que el combo de categorías muestre bien Categoria, 
// se puede sobrescribir toString() en Categoria (ya está en el DTO). 
// si se quiere seleccionar por igualdad, el equals/hashCode del DTO ya ayuda.