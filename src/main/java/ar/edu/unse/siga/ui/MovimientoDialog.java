package ar.edu.unse.siga.ui;

import javax.swing.*;
import java.awt.*;

public class MovimientoDialog extends JDialog {
    private final JComboBox<String> cbTipo = new JComboBox<>(new String[]{"ENTRADA","SALIDA"});
    private final JSpinner spCantidad = new JSpinner(new SpinnerNumberModel(1,1,1_000_000,1));
    private final JTextField txtDestino = new JTextField(25);
    private boolean accepted = false;

    public MovimientoDialog(Frame owner) {
        super(owner, "Registrar Movimiento", true);
        setLayout(new BorderLayout(10,10));

        JPanel form = new JPanel(new GridLayout(0,2,8,8));
        form.add(new JLabel("Tipo:")); form.add(cbTipo);
        form.add(new JLabel("Cantidad:")); form.add(spCantidad);
        form.add(new JLabel("Destino/Fuente:")); form.add(txtDestino);
        add(form, BorderLayout.CENTER);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton btnOk = new JButton("Guardar");
        JButton btnCancel = new JButton("Cancelar");
        buttons.add(btnCancel); buttons.add(btnOk);
        add(buttons, BorderLayout.SOUTH);

        btnOk.addActionListener(e -> { accepted = true; setVisible(false); });
        btnCancel.addActionListener(e -> setVisible(false));

        pack();
        setLocationRelativeTo(owner);
    }
    public boolean isAccepted() { return accepted; }
    public String getTipo() { return (String) cbTipo.getSelectedItem(); }
    public int getCantidad() { return (Integer) spCantidad.getValue(); }
    public String getDestinoFuente() { return txtDestino.getText().trim(); }
}

