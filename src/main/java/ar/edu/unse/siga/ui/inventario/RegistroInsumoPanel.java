package ar.edu.unse.siga.ui.inventario;

import ar.edu.unse.siga.domain.Insumo;
import ar.edu.unse.siga.service.InventarioService;

import javax.swing.*;
import java.awt.*;

class RegistroInsumoPanel extends JPanel {
    private final JTextField txtCodigo = new JTextField();
    private final JTextField txtDescripcion = new JTextField();
    private final JTextField txtCategoria = new JTextField();
    private final JSpinner spMinimo = new JSpinner(new SpinnerNumberModel(0, 0, 1_000_000, 1));

    RegistroInsumoPanel(InventarioService service) {
        setLayout(new GridBagLayout());
        setOpaque(false);

        var box = new JPanel();
        box.setOpaque(false);
        box.setLayout(new GridBagLayout());
        box.setBorder(BorderFactory.createTitledBorder("REGISTRO DE INVENTARIO"));

        GridBagConstraints gc = new GridBagConstraints();
        gc.insets = new Insets(8, 12, 8, 12);
        gc.fill = GridBagConstraints.HORIZONTAL;

        int r = 0;
        addRow(box, gc, r++, "CODIGO", txtCodigo);
        addRow(box, gc, r++, "DESCRIPCIÓN", txtDescripcion);
        addRow(box, gc, r++, "CATEGORIA", txtCategoria);
        addRow(box, gc, r++, "STOCK MINIMO", spMinimo);

        var btn = new JButton("REGISTRAR");
        btn.putClientProperty("JButton.buttonType", "roundRect");
        btn.setPreferredSize(new Dimension(220, 40));
        gc.gridx = 0; gc.gridy = r; gc.gridwidth = 2; gc.anchor = GridBagConstraints.CENTER;
        box.add(btn, gc);

        var wrap = new JPanel(new GridBagLayout());
        wrap.setOpaque(false);
        wrap.add(box, new GridBagConstraints());
        add(wrap, new GridBagConstraints());

        btn.addActionListener(e -> {
            try {
                var i = new Insumo();
                i.setCodigo(txtCodigo.getText().trim());
                i.setDescripcion(txtDescripcion.getText().trim());
                /*var cat = new ar.edu.unse.siga.domain.Categoria();
                cat.setId(0); // usar 0 como placeholder
                cat.setNombre(txtCategoria.getText().trim());
                i.setCategoria(cat);*/ //esto esta comentado, pero suplanta lo de abajo que setea categoria
                i.setCategoria(new ar.edu.unse.siga.domain.Categoria(0, txtCategoria.getText().trim()));
                i.setStockMinimo((Integer) spMinimo.getValue());
                i.setEstado("ACTIVO");
                i.setUbicacion("");
                service.registrarInsumo(i);
                JOptionPane.showMessageDialog(this, "Insumo registrado");
                clear();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        });
    }

    private void addRow(JPanel p, GridBagConstraints gc, int row, String label, JComponent field) {
        gc.gridx = 0; gc.gridy = row; gc.gridwidth = 1; gc.weightx = 0;
        p.add(new JLabel(label), gc);
        gc.gridx = 1; gc.weightx = 1;
        field.setPreferredSize(new Dimension(260, 36));
        p.add(field, gc);
    }

    private void clear() {
        txtCodigo.setText(""); txtDescripcion.setText(""); txtCategoria.setText("");
        spMinimo.setValue(0);
    }
}
