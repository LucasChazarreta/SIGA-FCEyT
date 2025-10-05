package ar.edu.unse.siga.ui.inventario;

import javax.swing.*;
import java.awt.*;

/**
 * Diálogo reutilizable para registrar un movimiento (ENTRADA / SALIDA).
 * Puede usarse desde la pestaña de Movimientos o desde Inventario.
 */
public class MovimientoDialog extends JDialog {

    private final JLabel lblContexto = new JLabel();
    private final JComboBox<String> cbTipo = new JComboBox<>(new String[]{"ENTRADA", "SALIDA"});
    private final JSpinner spCantidad = new JSpinner(new SpinnerNumberModel(1, 1, 1_000_000, 1));
    private final JTextField txtDestino = new JTextField(25);
    private boolean accepted = false;

    /**
     * Constructor simple (sin contexto, arranca en ENTRADA).
     */
    public MovimientoDialog(Window owner) {
        this(owner, null, "ENTRADA", null, 1);
    }

    /**
     * Constructor completo para reuso.
     * @param owner ventana padre
     * @param contexto Texto a mostrar arriba (ej. "Insumo: A4-001 · Papel A4"). Puede ser null.
     * @param tipoInicial "ENTRADA" o "SALIDA"
     * @param destinoInicial texto inicial de destino/fuente (opcional)
     * @param cantidadInicial valor inicial de cantidad (mín. 1)
     */
    public MovimientoDialog(Window owner, String contexto, String tipoInicial, String destinoInicial, int cantidadInicial) {
        super(owner, "Registrar Movimiento", ModalityType.APPLICATION_MODAL);
        setLayout(new BorderLayout(10, 10));

        // contexto opcional
        if (contexto != null && !contexto.isBlank()) {
            lblContexto.setText(contexto);
            lblContexto.setFont(lblContexto.getFont().deriveFont(Font.BOLD));
            lblContexto.setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));
            add(lblContexto, BorderLayout.NORTH);
        }

        // panel central
        var panel = new JPanel(new GridLayout(0, 2, 8, 8));
        panel.add(new JLabel("Tipo:"));
        panel.add(cbTipo);
        panel.add(new JLabel("Cantidad:"));
        panel.add(spCantidad);
        panel.add(new JLabel("Destino/Fuente:"));
        panel.add(txtDestino);
        add(panel, BorderLayout.CENTER);

        // botones
        var ok = new JButton("Registrar");
        var cancel = new JButton("Cancelar");
        ok.addActionListener(e -> {
            accepted = true;
            setVisible(false);
        });
        cancel.addActionListener(e -> setVisible(false));

        var south = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        south.add(cancel);
        south.add(ok);
        add(south, BorderLayout.SOUTH);

        // inicialización de valores
        if (tipoInicial != null) cbTipo.setSelectedItem(tipoInicial);
        if (destinoInicial != null) txtDestino.setText(destinoInicial);
        if (cantidadInicial > 0) spCantidad.setValue(cantidadInicial);

        pack();
        setLocationRelativeTo(owner);
    }

    // getters
    public boolean isAccepted() { return accepted; }
    public String getTipo() { return (String) cbTipo.getSelectedItem(); }
    public int getCantidad() { return ((Number) spCantidad.getValue()).intValue(); }
    public String getDestinoFuente() { return txtDestino.getText().trim(); }

    // setters opcionales por si querés usarlos
    public void setContexto(String texto) { lblContexto.setText(texto); }
    public void setCantidad(int cant) { spCantidad.setValue(Math.max(1, cant)); }
}
