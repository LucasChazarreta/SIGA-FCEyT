package ar.edu.unse.siga.ui.inventario;

import ar.edu.unse.siga.service.InventarioService;
import ar.edu.unse.siga.ui.base.Ui;

import javax.swing.*;
import java.awt.*;

public class EliminarInsumoPanel extends JPanel {

    private final InventarioService service;
    private final JTextField txtId = new JTextField(8);
    private final JTextField txtCodigo = new JTextField(12);
    private final JButton btnEliminar = new JButton("Eliminar (baja lógica)");

    public EliminarInsumoPanel(InventarioService service) {
        this.service = service;
        setLayout(new BorderLayout(12,12));
        setOpaque(false);

        var form = new JPanel(new GridBagLayout());
        var gbc = new GridBagConstraints();
        gbc.insets = new Insets(6,6,6,6);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        gbc.gridx=0; gbc.gridy=0; form.add(new JLabel("ID:"), gbc);
        gbc.gridx=1; form.add(txtId, gbc);

        gbc.gridx=0; gbc.gridy=1; form.add(new JLabel("Código (opcional):"), gbc);
        gbc.gridx=1; form.add(txtCodigo, gbc);

        add(form, BorderLayout.CENTER);

        var south = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        south.add(btnEliminar);
        add(south, BorderLayout.SOUTH);

        btnEliminar.addActionListener(e -> onEliminar());
    }

    private void onEliminar() {
        try {
            String idTxt = txtId.getText().trim();
            if (idTxt.isEmpty()) throw new IllegalArgumentException("Ingresá el ID del insumo a dar de baja");
            long id = Long.parseLong(idTxt);
            if (!Ui.confirm(this, "¿Confirmás la baja lógica del insumo id=" + id + "?")) return;
            service.bajaLogica(id);
            Ui.info(this, "Baja lógica realizada.");
            txtId.setText(""); txtCodigo.setText("");
        } catch (Exception ex) {
            Ui.error(this, ex);
        }
    }
}
