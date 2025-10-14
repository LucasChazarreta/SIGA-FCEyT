package ar.edu.unse.siga.ui.inventario;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.text.DefaultFormatterFactory;
import javax.swing.text.NumberFormatter;
import java.awt.*;
import java.text.DecimalFormat;
import java.text.ParseException;

/**
 * Diálogo reutilizable para registrar un movimiento (ENTRADA / SALIDA).
 * - Campo Cantidad SOLO numérico (1 .. 1_000_000)
 * - Muestra "Destino/Fuente" SOLO cuando el tipo es SALIDA
 */
public class MovimientoDialog extends JDialog {

    private final JLabel lblContexto = new JLabel();
    private final JComboBox<String> cbTipo = new JComboBox<>(new String[]{"ENTRADA", "SALIDA"});
    private final JSpinner spCantidad = new JSpinner(new SpinnerNumberModel(1, 1, 1_000_000, 1));

    // Campo condicional (solo visible para SALIDA)
    private final JLabel lblDestino = new JLabel("Destino/Fuente:");
    private final JComboBox<String> cbDestino = new JComboBox<>(new String[]{
            "Sede Central",
            "Sede Zanjón",
            "Sede Parque Industrial"
    });

    private boolean accepted = false;
    private final JPanel panel = new JPanel(new GridLayout(0, 2, 8, 8));

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
        panel.setBorder(new EmptyBorder(12, 16, 0, 16));
        panel.add(new JLabel("Tipo:"));
        panel.add(cbTipo);
        panel.add(new JLabel("Cantidad:"));
        panel.add(spCantidad);

        // campo destino (solo se añade si tipo = SALIDA)
        cbDestino.setEditable(false);
        panel.add(lblDestino);
        panel.add(cbDestino);

        add(panel, BorderLayout.CENTER);

        // === Editor numérico ESTRICTO ===
        JSpinner.NumberEditor numEd = new JSpinner.NumberEditor(spCantidad, "#");
        spCantidad.setEditor(numEd);
        JFormattedTextField tf = numEd.getTextField();

        NumberFormatter nf = new NumberFormatter(new DecimalFormat("#"));
        nf.setValueClass(Integer.class);
        nf.setAllowsInvalid(false);
        nf.setCommitsOnValidEdit(true);
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
            cbDestino.setSelectedItem(destinoInicial);
        }
        if (cantidadInicial > 0) {
            spCantidad.setValue(cantidadInicial);
        }

        // comportamiento dinámico
        cbTipo.addItemListener(e -> actualizarVisibilidadDestino());
        actualizarVisibilidadDestino(); // inicial

        setMinimumSize(new Dimension(680, getMinimumSize().height));
        pack();
        setLocationRelativeTo(owner);
    }

    // ==== lógica para mostrar u ocultar el campo destino ====
    private void actualizarVisibilidadDestino() {
        String tipo = (String) cbTipo.getSelectedItem();
        boolean esSalida = "SALIDA".equalsIgnoreCase(tipo);

        lblDestino.setVisible(esSalida);
        cbDestino.setVisible(esSalida);

        // refresca la UI sin necesidad de recrear todo
        panel.revalidate();
        panel.repaint();
    }

    // ==== getters / setters ====
    public boolean isAccepted() {
        return accepted;
    }

    public String getTipo() {
        return (String) cbTipo.getSelectedItem();
    }

    public int getCantidad() {
        try {
            JComponent ed = spCantidad.getEditor();
            if (ed instanceof JSpinner.DefaultEditor de) {
                de.commitEdit();
            }
        } catch (ParseException ignore) {}
        return ((Number) spCantidad.getValue()).intValue();
    }

    public String getDestinoFuente() {
        String tipo = (String) cbTipo.getSelectedItem();
        if ("SALIDA".equalsIgnoreCase(tipo)) {
            return (String) cbDestino.getSelectedItem();
        }
        return ""; // si es entrada, devuelve vacío
    }

    public void setContexto(String texto) {
        lblContexto.setText(texto);
    }

    public void setCantidad(int cant) {
        spCantidad.setValue(Math.max(1, cant));
    }
}
