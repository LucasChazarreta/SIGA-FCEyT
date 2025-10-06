package ar.edu.unse.siga.ui.inventario;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.text.DefaultFormatterFactory;
import javax.swing.text.NumberFormatter;
import java.awt.*;
import java.text.DecimalFormat;
import java.text.ParseException;

/**
 * Diálogo reutilizable para registrar un movimiento (ENTRADA / SALIDA). - Campo
 * Cantidad SOLO numérico (1 .. 1_000_000) - Diseño con márgenes y tamaños de
 * botón prolijos
 */
public class MovimientoDialog extends JDialog {

    private final JLabel lblContexto = new JLabel();
    private final JComboBox<String> cbTipo = new JComboBox<>(new String[]{"ENTRADA", "SALIDA"});
    private final JSpinner spCantidad = new JSpinner(new SpinnerNumberModel(1, 1, 1_000_000, 1));
    private final JTextField txtDestino = new JTextField(25);
    private boolean accepted = false;

    public MovimientoDialog(Window owner) {
        this(owner, null, "ENTRADA", null, 1);
    }

    public MovimientoDialog(Window owner, String contexto, String tipoInicial, String destinoInicial, int cantidadInicial) {
        super(owner, "Registrar Movimiento", ModalityType.APPLICATION_MODAL);
        setLayout(new BorderLayout(10, 10));

        // --- encabezado opcional ---
        if (contexto != null && !contexto.isBlank()) {
            lblContexto.setText(contexto);
            lblContexto.setFont(lblContexto.getFont().deriveFont(Font.BOLD));
            lblContexto.setBorder(new EmptyBorder(10, 12, 0, 12));
            add(lblContexto, BorderLayout.NORTH);
        }

        // --- panel central ---
        var panel = new JPanel(new GridLayout(0, 2, 8, 8));
        panel.setBorder(new EmptyBorder(12, 16, 0, 16));
        panel.add(new JLabel("Tipo:"));
        panel.add(cbTipo);
        panel.add(new JLabel("Cantidad:"));
        panel.add(spCantidad);
        panel.add(new JLabel("Destino/Fuente:"));
        panel.add(txtDestino);
        add(panel, BorderLayout.CENTER);

        // === Editor numérico ESTRICTO para el spinner ===
        //  (impide letras y símbolos; solo dígitos)
        JSpinner.NumberEditor numEd = new JSpinner.NumberEditor(spCantidad, "#");
        spCantidad.setEditor(numEd);
        JFormattedTextField tf = numEd.getTextField();

        NumberFormatter nf = new NumberFormatter(new DecimalFormat("#"));
        nf.setValueClass(Integer.class);
        nf.setAllowsInvalid(false);        // bloquea cualquier carácter no numérico
        nf.setCommitsOnValidEdit(true);    // aplica en Enter/Tab
        nf.setMinimum(1);
        nf.setMaximum(1_000_000);

        tf.setFormatterFactory(new DefaultFormatterFactory(nf));
        tf.setColumns(8);

        // --- botones ---
        var ok = new JButton("Registrar");
        var cancel = new JButton("Cancelar");
        ok.setPreferredSize(new Dimension(110, 30));
        cancel.setPreferredSize(new Dimension(110, 30));

        ok.addActionListener(e -> {
            accepted = true;
            setVisible(false);
        });
        cancel.addActionListener(e -> setVisible(false));

        var south = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        south.setBorder(new EmptyBorder(8, 8, 8, 8));
        south.add(cancel);
        south.add(ok);
        add(south, BorderLayout.SOUTH);

        // --- valores iniciales ---
        if (tipoInicial != null) {
            cbTipo.setSelectedItem(tipoInicial);
        }
        if (destinoInicial != null) {
            txtDestino.setText(destinoInicial);
        }
        if (cantidadInicial > 0) {
            spCantidad.setValue(cantidadInicial);
        }

        setMinimumSize(new Dimension(680, getMinimumSize().height));
        pack();
        setLocationRelativeTo(owner);
    }

    // ==== getters / setters ====
    public boolean isAccepted() {
        return accepted;
    }

    public String getTipo() {
        return (String) cbTipo.getSelectedItem();
    }

    /**
     * Asegura tomar lo tipeado si el usuario confirma muy rápido.
     */
    public int getCantidad() {
        try {
            JComponent ed = spCantidad.getEditor();
            if (ed instanceof JSpinner.DefaultEditor de) {
                de.commitEdit();
            }
        } catch (ParseException ignore) {
        }
        return ((Number) spCantidad.getValue()).intValue();
    }

    public String getDestinoFuente() {
        return txtDestino.getText().trim();
    }

    public void setContexto(String texto) {
        lblContexto.setText(texto);
    }

    public void setCantidad(int cant) {
        spCantidad.setValue(Math.max(1, cant));
    }
}
