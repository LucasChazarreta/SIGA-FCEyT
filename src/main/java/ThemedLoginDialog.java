package ar.edu.unse.siga.ui.auth;

import ar.edu.unse.siga.service.AuthService;
import ar.edu.unse.siga.ui.base.GradientPanel;
import ar.edu.unse.siga.ui.base.ThemeManager;

import javax.swing.*;
import java.awt.*;

public class ThemedLoginDialog extends JDialog {
    private final JTextField txtUser = new JTextField(16);
    private final JPasswordField txtPass = new JPasswordField(16);
    private final AuthService authService;
    private boolean loggedIn = false;

    public ThemedLoginDialog(Frame owner, AuthService authService) {
        super(owner, "SIGA-FCEyT · Iniciar sesión", true);
        this.authService = authService;

        ThemeManager.installDefaults();

        var root = new GradientPanel();
        root.setLayout(new BorderLayout());
        root.setBorder(BorderFactory.createEmptyBorder(18,18,18,18));

        var card = new JPanel(new GridBagLayout());
        card.setOpaque(true);
        card.setBackground(new Color(255,255,255,230));
        card.setBorder(BorderFactory.createEmptyBorder(20,20,20,20));

        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(6,6,6,6);
        c.gridx = 0; c.gridy = 0; c.anchor = GridBagConstraints.WEST;
        var title = new JLabel("Bienvenido");
        title.setFont(title.getFont().deriveFont(Font.BOLD, 20f));
        card.add(title, c);

        c.gridy++; card.add(new JLabel("Usuario"), c);
        c.gridx = 1; card.add(txtUser, c);

        c.gridx = 0; c.gridy++; card.add(new JLabel("Contraseña"), c);
        c.gridx = 1; card.add(txtPass, c);

        var btnLogin = new JButton("Entrar", ThemeManager.svg("icons/login.svg",16));
        var btnCancel = new JButton("Cancelar");
        btnLogin.addActionListener(e -> doLogin());
        btnCancel.addActionListener(e -> { loggedIn = false; dispose(); });

        var buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT,8,0));
        buttons.setOpaque(false);
        buttons.add(btnLogin);
        buttons.add(btnCancel);

        c.gridx = 0; c.gridy++; c.gridwidth = 2; c.fill = GridBagConstraints.HORIZONTAL;
        card.add(buttons, c);

        root.add(card, BorderLayout.CENTER);
        setContentPane(root);
        pack();
        setLocationRelativeTo(owner);
        getRootPane().setDefaultButton(btnLogin);
    }

    private void doLogin() {
        String u = txtUser.getText().trim();
        String p = new String(txtPass.getPassword());
        try {
            authService.login(u, p);
            loggedIn = true;
            dispose();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Login", JOptionPane.ERROR_MESSAGE);
            txtPass.setText("");
            txtPass.requestFocusInWindow();
        }
    }

    public boolean isLoggedIn() { return loggedIn; }
}
