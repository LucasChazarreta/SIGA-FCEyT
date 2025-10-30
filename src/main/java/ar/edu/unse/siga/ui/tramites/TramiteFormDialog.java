package ar.edu.unse.siga.ui.tramites;

import ar.edu.unse.siga.ui.base.Dialogs;
import ar.edu.unse.siga.ui.base.Ui;

import javax.swing.*;
import java.awt.*;

public class TramiteFormDialog extends JDialog {

    private final JTextField txtNro = new JTextField(20);
    private final JTextField txtAsunto = new JTextField(30);
    private final JTextField txtSolicitante = new JTextField(30);
    private final JTextField txtDescripcion = new JTextField(30);
    private final JTextField txtDestino = new JTextField(30);
    private boolean accepted = false;

    private final Runnable onSaved; // callback para refrescar UI del caller

    /** Nuevo constructor con callback (recomendado) */
    public TramiteFormDialog(Window owner, Runnable onSaved) {
        super(owner, "Nueva Solicitud", ModalityType.APPLICATION_MODAL);
        this.onSaved = onSaved;
        buildUI(owner);
    }

    /** Constructor legacy (compatibilidad) */
    public TramiteFormDialog(Window owner) {
        this(owner, null);
    }

    private void buildUI(Window owner) {
        var form = new JPanel(new GridLayout(0, 2, 8, 8));

        form.add(new JLabel("Número:"));
        form.add(txtNro);

        form.add(new JLabel("Solicitud:"));
        form.add(txtAsunto);

        form.add(new JLabel("Solicitante:"));
        txtSolicitante.setText("Informes");      // solicitante fijo
        txtSolicitante.setEditable(false);       // no editable
        form.add(txtSolicitante);

        form.add(new JLabel("Descripción:"));
        form.add(txtDescripcion);

        form.add(new JLabel("Destino:"));
        form.add(txtDestino);

        JButton ok = new JButton("Guardar");
        JButton cancel = new JButton("Cancelar");

        ok.addActionListener(e -> {
            if (txtNro.getText().isBlank() || txtAsunto.getText().isBlank()) {
                Dialogs.warn(this, "Número y Asunto son obligatorios.");
                return;
            }
            // Aquí deberías invocar tu Service para:
            // - crear Solicitud + Detalles
            // - crear Movimientos SALIDA por los insumos seleccionados
            // - descontar stock
            // (todo en una transacción). Este diálogo sólo valida y notifica.

            accepted = true;
            if (onSaved != null) {
                try { onSaved.run(); } catch (Exception ignore) {}
            }
            setVisible(false);
        });

        cancel.addActionListener(e -> setVisible(false));

        setLayout(new BorderLayout(10, 10));
        add(form, BorderLayout.CENTER);
        add(Ui.flowRight(cancel, ok), BorderLayout.SOUTH);
        pack();
        setLocationRelativeTo(owner);
    }

    public boolean isAccepted() {
        return accepted;
    }

    public String getNro() {
        return txtNro.getText().trim();
    }

    public String getAsunto() {
        return txtAsunto.getText().trim();
    }

    public String getSolicitante() {
        return txtSolicitante.getText().trim();
    }

    public String getDescripcion() {
        return txtDescripcion.getText().trim();
    }

    public String getDestino() {
        return txtDestino.getText().trim();
    }
}
