package ar.edu.unse.siga.ui.inventario;

import javax.swing.*;
import java.awt.*;

public class MovimientoDialog extends JDialog {
    private final JComboBox<String> cbTipo = new JComboBox<>(new String[]{"ENTRADA","SALIDA"});
    private final JSpinner spCantidad = new JSpinner(new SpinnerNumberModel(1,1,1_000_000,1));
    private final JTextField txtDestino = new JTextField(25);
    private boolean accepted = false;

    public MovimientoDialog(Window owner) {
        super(owner, "Registrar Movimiento", ModalityType.APPLICATION_MODAL);
        setLayout(new BorderLayout(10,10));

        var panel = new JPanel(new GridLayout(0,2,8,8));
        panel.add(new JLabel("Tipo:")); panel.add(cbTipo);
        panel.add(new JLabel("Cantidad:")); panel.add(spCantidad);
        panel.add(new JLabel("Destino/Fuente:")); panel.add(txtDestino);
        add(panel, BorderLayout.CENTER);

        var ok = new JButton("Guardar");
        var cancel = new JButton("Cancelar");
        ok.addActionListener(e -> { accepted = true; setVisible(false); });
        cancel.addActionListener(e -> setVisible(false));
        add(new JPanel(new FlowLayout(FlowLayout.RIGHT)) {{ add(cancel); add(ok); }}, BorderLayout.SOUTH);

        pack();
        setLocationRelativeTo(owner);
    }

    public boolean isAccepted(){ return accepted; }
    public String getTipo(){ return (String) cbTipo.getSelectedItem(); }
    public int getCantidad(){ return ((Number)spCantidad.getValue()).intValue(); }
    public String getDestinoFuente(){ return txtDestino.getText().trim(); }
}
