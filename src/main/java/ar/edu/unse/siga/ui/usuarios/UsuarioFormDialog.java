package ar.edu.unse.siga.ui.usuarios;

import ar.edu.unse.siga.common.RoleName;
import ar.edu.unse.siga.ui.base.Dialogs;
import ar.edu.unse.siga.ui.base.Ui;

import javax.swing.*;
import java.awt.*;

public class UsuarioFormDialog extends JDialog {
    private final JTextField txtUser = new JTextField(20);
    private final JPasswordField txtPass = new JPasswordField(20);
    private final JTextField txtEmail = new JTextField(25);
    private final JComboBox<String> cbRol;
    private boolean accepted = false;

    public UsuarioFormDialog(Window owner) {
        this(owner, new String[]{RoleName.ADMINISTRATIVO, RoleName.ADMIN});
    }

    public UsuarioFormDialog(Window owner, String[] roles) {
        super(owner, "Nuevo Usuario", ModalityType.APPLICATION_MODAL);

        String[] options = roles == null || roles.length == 0
                ? new String[]{RoleName.ADMINISTRATIVO}
                : roles;
        cbRol = new JComboBox<>(options);

        var form = Ui.grid2Cols(
                new String[]{"Usuario:","Contraseña:","Email:","Rol:"},
                new JComponent[]{txtUser, txtPass, txtEmail, cbRol}
        );
        var ok = new JButton("Crear");
        var cancel = new JButton("Cancelar");
        ok.addActionListener(e -> {
            try {
                Dialogs.require(txtUser,"Usuario");
                if (new String(txtPass.getPassword()).isBlank())
                    throw new IllegalArgumentException("La contraseña es obligatoria");
                accepted = true; setVisible(false);
            } catch (Exception ex){ Ui.error(this, ex); }
        });
        cancel.addActionListener(e -> setVisible(false));

        setLayout(new BorderLayout(10,10));
        add(form, BorderLayout.CENTER);
        add(Ui.flowRight(cancel, ok), BorderLayout.SOUTH);
        pack(); setLocationRelativeTo(owner);
    }

    public boolean isAccepted(){ return accepted; }
    public String getUsername(){ return txtUser.getText().trim(); }
    public String getPassword(){ return new String(txtPass.getPassword()); }
    public String getEmail(){ return txtEmail.getText().trim(); }
    public String getRol(){ return (String) cbRol.getSelectedItem(); }
}
