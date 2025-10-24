package ar.edu.unse.siga.ui.inventario;

import ar.edu.unse.siga.ui.base.CardPanel;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.math.BigDecimal;
import java.util.List;

public class MovimientoDialog extends JDialog {

    private final JLabel lblContexto = new JLabel();
    private final JLabel lblTipo = new JLabel();
    private final JTextField txtCantidad = new JTextField();
    private final JComboBox<String> cbDestino = new JComboBox<>();
    private final JTextField txtSolicitante = new JTextField();

    private final String tipoMovimiento;
    private final boolean allowDecimal;
    private boolean accepted = false;

    public MovimientoDialog(Window owner) {
        this(owner, null, "SALIDA", true, java.util.List.of());
    }

    public MovimientoDialog(Window owner,
                            String contexto,
                            String tipoMovimiento,
                            boolean allowDecimal,
                            List<String> ubicaciones) {
        super(owner, "Registrar " + tipoMovimiento, ModalityType.APPLICATION_MODAL);
        this.tipoMovimiento = tipoMovimiento == null ? "ENTRADA" : tipoMovimiento.trim().toUpperCase();
        this.allowDecimal = allowDecimal;

        setLayout(new BorderLayout(10, 10));
        setMinimumSize(new Dimension(420, 220));

        if (contexto != null && !contexto.isBlank()) {
            lblContexto.setText(contexto);
            lblContexto.setBorder(new EmptyBorder(12, 16, 0, 16));
            add(lblContexto, BorderLayout.NORTH);
        }

        CardPanel panel = new CardPanel();
        panel.setLayout(new GridBagLayout());
        panel.setBorder(new EmptyBorder(16, 16, 0, 16));

        GridBagConstraints gc = new GridBagConstraints();
        gc.gridx = 0;
        gc.gridy = 0;
        gc.anchor = GridBagConstraints.LINE_START;
        gc.insets = new Insets(0, 0, 10, 0);

        lblTipo.setText("Tipo: " + this.tipoMovimiento);
        lblTipo.setFont(lblTipo.getFont().deriveFont(Font.BOLD));
        panel.add(lblTipo, gc);

        gc.gridy++;
        panel.add(label("Cantidad"), gc);

        gc.gridx = 1;
        gc.fill = GridBagConstraints.HORIZONTAL;
        gc.weightx = 1;
        txtCantidad.putClientProperty("JTextField.placeholderText", allowDecimal ? "Ej: 3.5" : "Ej: 2");
        panel.add(txtCantidad, gc);

        if (esSalida()) {
            gc.gridx = 0;
            gc.gridy++;
            gc.weightx = 0;
            gc.fill = GridBagConstraints.NONE;
            panel.add(label("Destino"), gc);

            gc.gridx = 1;
            gc.fill = GridBagConstraints.HORIZONTAL;
            gc.weightx = 1;
            cbDestino.putClientProperty("JComponent.roundRect", true);
            panel.add(cbDestino, gc);

            if (ubicaciones != null && !ubicaciones.isEmpty()) {
                ubicaciones.forEach(cbDestino::addItem);
            }

            gc.gridx = 0;
            gc.gridy++;
            gc.weightx = 0;
            gc.fill = GridBagConstraints.NONE;
            panel.add(label("Solicitante"), gc);

            gc.gridx = 1;
            gc.fill = GridBagConstraints.HORIZONTAL;
            gc.weightx = 1;
            txtSolicitante.putClientProperty("JTextField.placeholderText", "Persona que retira");
            panel.add(txtSolicitante, gc);
        }

        add(panel, BorderLayout.CENTER);

        JButton btnAceptar = new JButton("Registrar");
        JButton btnCancelar = new JButton("Cancelar");
        Dimension size = new Dimension(130, 36);
        btnAceptar.setPreferredSize(size);
        btnCancelar.setPreferredSize(size);

        btnAceptar.addActionListener(e -> {
            if (validarCantidad()) {
                accepted = true;
                setVisible(false);
            }
        });
        btnCancelar.addActionListener(e -> setVisible(false));

        JPanel south = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        south.setBorder(new EmptyBorder(0, 0, 12, 12));
        south.add(btnCancelar);
        south.add(btnAceptar);
        add(south, BorderLayout.SOUTH);

        pack();
        setLocationRelativeTo(owner);
    }

    private JLabel label(String text) {
        JLabel lbl = new JLabel(text);
        lbl.setFont(lbl.getFont().deriveFont(Font.PLAIN, 12f));
        return lbl;
    }

    private boolean esSalida() {
        return "SALIDA".equalsIgnoreCase(tipoMovimiento);
    }

    private boolean validarCantidad() {
        try {
            BigDecimal qty = getCantidad();
            if (qty.compareTo(BigDecimal.ZERO) <= 0) {
                JOptionPane.showMessageDialog(this, "La cantidad debe ser mayor a cero.", "Atención", JOptionPane.WARNING_MESSAGE);
                return false;
            }
            if (!allowDecimal && qty.stripTrailingZeros().scale() > 0) {
                JOptionPane.showMessageDialog(this, "La cantidad debe ser un entero.", "Atención", JOptionPane.WARNING_MESSAGE);
                return false;
            }
            if (esSalida()) {
                String solicitante = getSolicitante();
                if (solicitante.isBlank()) {
                    JOptionPane.showMessageDialog(this, "Indicá el solicitante al registrar la salida.", "Atención", JOptionPane.WARNING_MESSAGE);
                    return false;
                }
            }
            return true;
        } catch (IllegalArgumentException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Atención", JOptionPane.WARNING_MESSAGE);
            return false;
        }
    }

    public boolean isAccepted() {
        return accepted;
    }

    public String getTipo() {
        return tipoMovimiento;
    }

    public BigDecimal getCantidad() {
        String text = txtCantidad.getText();
        if (text == null || text.isBlank()) {
            throw new IllegalArgumentException("Ingresá una cantidad válida.");
        }
        try {
            String normalized = text.replace(',', '.');
            return new BigDecimal(normalized).stripTrailingZeros();
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("Cantidad inválida.");
        }
    }

    public String getDestinoFuente() {
        if (!esSalida()) {
            return "";
        }
        Object sel = cbDestino.getSelectedItem();
        return sel == null ? "" : sel.toString();
    }

    public String getSolicitante() {
        return txtSolicitante.getText() == null ? "" : txtSolicitante.getText().trim();
    }
}
