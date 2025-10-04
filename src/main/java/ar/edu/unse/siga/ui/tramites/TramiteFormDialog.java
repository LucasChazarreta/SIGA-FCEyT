package ar.edu.unse.siga.ui.tramites;

import ar.edu.unse.siga.ui.base.Dialogs;
import ar.edu.unse.siga.ui.base.Ui;

import javax.swing.*;
import java.awt.*;

public class TramiteFormDialog extends JDialog {
    private final JTextField txtNro = new JTextField(20);
    private final JTextField txtAsunto = new JTextField(30);
    private final JTextField txtSolicitante = new JTextField(30);
    private final JTextField txtDescripcion= new JTextField(30);
    private final JTextField txtDestino= new JTextField(30);
    private boolean accepted = false;

    public TramiteFormDialog(Window owner) {
        super(owner, "Nuevo Trámite", ModalityType.APPLICATION_MODAL);
        var form = Ui.grid2Cols(new String[]{"Número:","Asunto:","Solicitante:", "Descripcion:", "Destino:"},
                new JComponent[]{txtNro, txtAsunto, txtSolicitante, txtDescripcion, txtDestino});
        var ok = new JButton("Guardar");
        var cancel = new JButton("Cancelar");
        ok.addActionListener(e -> {
            try {
                Dialogs.require(txtNro, "Número");
                Dialogs.require(txtAsunto, "Asunto");
                Dialogs.require(txtDescripcion, "Descripcion");
                Dialogs.require(txtDestino, "Destino");
                
                accepted = true; setVisible(false);
            } catch (Exception ex) { Ui.error(this, ex); }
        });
        cancel.addActionListener(e -> setVisible(false));

        setLayout(new BorderLayout(10,10));
        add(form, BorderLayout.CENTER);
        add(Ui.flowRight(cancel, ok), BorderLayout.SOUTH);
        pack(); setLocationRelativeTo(owner);
    }

    public boolean isAccepted(){ return accepted; }
    public String getNro(){ return txtNro.getText().trim(); }
    public String getAsunto(){ return txtAsunto.getText().trim(); }
    public String getSolicitante(){ return txtSolicitante.getText().trim(); }
    public String getDescripcion(){ return txtDescripcion.getText().trim(); }
    public String getDestino(){ return txtDestino.getText().trim(); }
}
