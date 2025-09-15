package ar.edu.unse.siga.ui;

import ar.edu.unse.siga.domain.Usuario;
import ar.edu.unse.siga.service.AuthService;
import ar.edu.unse.siga.ui.base.Ui;

import javax.swing.*;
import java.awt.*;

public class LoginDialog extends JDialog {
    private final JTextField txtUser = new JTextField(15);
    private final JPasswordField txtPass = new JPasswordField(15);
    private final AuthService auth;
    private Usuario logged;

    public LoginDialog(Window owner, AuthService auth) {
        super(owner, "Login", ModalityType.APPLICATION_MODAL);
        this.auth = auth;

        setLayout(new BorderLayout(10,10));
        var form = Ui.grid2Cols(new String[]{"Usuario:", "Contraseña:"},
                new JComponent[]{txtUser, txtPass});
        add(form, BorderLayout.CENTER);

        var btnOk = new JButton("Entrar");
        var btnCancel = new JButton("Cancelar");
        var actions = Ui.flowRight(btnCancel, btnOk);
        add(actions, BorderLayout.SOUTH);

        btnOk.addActionListener(e -> {
            try {
                var u = txtUser.getText().trim();
                var p = new String(txtPass.getPassword());
                logged = auth.login(u, p); // lanza excepción si falla
                dispose();
            } catch (Exception ex) {
                Ui.error(this, ex);
            }
        });
        btnCancel.addActionListener(e -> { logged = null; dispose(); });

        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        pack();
        setLocationRelativeTo(owner);
    }

    public Usuario getLogged(){ return logged; }
}