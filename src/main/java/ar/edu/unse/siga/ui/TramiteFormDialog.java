package ar.edu.unse.siga.ui;

import ar.edu.unse.siga.domain.Tramite;
import javax.swing.*;
import java.awt.*;

public class TramiteFormDialog extends JDialog {
    private final JTextField txtNro = new JTextField(10);
    private final JTextField txtAsunto = new JTextField(25);
    private final JTextField txtSolicitante = new JTextField(20);
    private boolean accepted = false;

    public TramiteFormDialog(Frame owner) {
        super(owner, "Nuevo Trámite", true);
        setLayout(new BorderLayout(10,10));

        JPanel form = new JPanel(new GridLayout(0,2,8,8));
        form.add(new JLabel("Número:")); form.add(txtNro);
        form.add(new JLabel("Asunto:")); form.add(txtAsunto);
        form.add(new JLabel("Solicitante:")); form.add(txtSolicitante);
        add(form, BorderLayout.CENTER);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton btnOk = new JButton("Guardar");
        JButton btnCancel = new JButton("Cancelar");
        buttons.add(btnCancel); buttons.add(btnOk);
        add(buttons, BorderLayout.SOUTH);

        btnOk.addActionListener(e -> {
            if (txtNro.getText().isBlank() || txtAsunto.getText().isBlank()) {
                JOptionPane.showMessageDialog(this, "Número y Asunto son obligatorios");
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
    public String getNro() { return txtNro.getText().trim(); }
    public String getAsunto() { return txtAsunto.getText().trim(); }
    public String getSolicitante() { return txtSolicitante.getText().trim(); }
}
