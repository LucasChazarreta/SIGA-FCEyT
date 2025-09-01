package ar.edu.unse.siga.ui;

import ar.edu.unse.siga.domain.Usuario;
import ar.edu.unse.siga.service.AuthService;

import javax.swing.*;
import java.awt.*;

public class LoginDialog extends JDialog {
    private final JTextField txtUser = new JTextField(15);
    private final JPasswordField txtPass = new JPasswordField(15);
    private final AuthService auth;
    private Usuario logged;

    public LoginDialog(Frame owner, AuthService auth) {
        super(owner, "Login", true);
        this.auth = auth;

        setLayout(new BorderLayout(10,10));
        JPanel form = new JPanel(new GridLayout(0,2,8,8));
        form.add(new JLabel("Usuario:")); form.add(txtUser);
        form.add(new JLabel("Contraseña:")); form.add(txtPass);
        add(form, BorderLayout.CENTER);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton btnCancel = new JButton("Cancelar");
        JButton btnOk = new JButton("Entrar");
        actions.add(btnCancel); actions.add(btnOk);
        add(actions, BorderLayout.SOUTH);

        btnOk.addActionListener(e -> {
            try {
                String u = txtUser.getText().trim();
                String p = new String(txtPass.getPassword());
                if (u.isBlank() || p.isBlank()) {
                    JOptionPane.showMessageDialog(this, "Complete usuario y contraseña");
                    return;
                }
                logged = auth.login(u, p);
                setVisible(false);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        });
        btnCancel.addActionListener(e -> System.exit(0));

        pack();
        setLocationRelativeTo(owner);
    }

    public Usuario getLogged() { return logged; }
}

