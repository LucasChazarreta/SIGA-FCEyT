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

    public TramiteFormDialog(Window owner) {
        super(owner, "Nuevo Trámite", ModalityType.APPLICATION_MODAL);

        var form = new JPanel(new GridLayout(0, 2, 8, 8));
        form.add(new JLabel("Número:"));
        form.add(txtNro);
        form.add(new JLabel("Asunto:"));
        form.add(txtAsunto);
        form.add(new JLabel("Solicitante:"));
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
            accepted = true;
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
    } // <- NUEVO

    public String getDestino() {
        return txtDestino.getText().trim();
    }         // <- NUEVO
}
