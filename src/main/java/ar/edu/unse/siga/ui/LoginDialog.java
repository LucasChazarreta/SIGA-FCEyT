package ar.edu.unse.siga.ui;

import ar.edu.unse.siga.domain.Usuario;
import ar.edu.unse.siga.service.AuthService;

import javax.swing.*;
import java.awt.*;

public class LoginDialog extends JDialog {
    private final JTextField txtUser = new JTextField(14);
    private final JPasswordField txtPass = new JPasswordField(14);
    private final AuthService authService;
    private Usuario usuarioAutenticado;

    public LoginDialog(Frame owner, AuthService authService) {
        super(owner, "Iniciar sesión", true);
        this.authService = authService;

        JPanel form = new JPanel(new GridLayout(0,2,8,8));
        form.add(new JLabel("Usuario:"));
        form.add(txtUser);
        form.add(new JLabel("Contraseña:"));
        form.add(txtPass);

        JButton btnOk = new JButton("Entrar");
        JButton btnCancel = new JButton("Cancelar");

        btnOk.addActionListener(e -> login());
        btnCancel.addActionListener(e -> { usuarioAutenticado = null; dispose(); });

        JPanel south = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        south.add(btnOk);
        south.add(btnCancel);

        JPanel root = new JPanel(new BorderLayout(10,10));
        root.setBorder(BorderFactory.createEmptyBorder(12,12,12,12));
        root.add(form, BorderLayout.CENTER);
        root.add(south, BorderLayout.SOUTH);

        setContentPane(root);
        pack();
        setLocationRelativeTo(owner);
        getRootPane().setDefaultButton(btnOk);
    }

    private void login() {
        String u = txtUser.getText().trim();
        String p = new String(txtPass.getPassword());
        try {
            usuarioAutenticado = authService.login(u, p);
            dispose();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Login", JOptionPane.ERROR_MESSAGE);
            txtPass.setText("");
            txtPass.requestFocusInWindow();
        }
    }

    public Usuario getUsuarioAutenticado() {
        return usuarioAutenticado;
    }
}
