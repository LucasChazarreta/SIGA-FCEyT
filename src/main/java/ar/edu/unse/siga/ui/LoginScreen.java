package ar.edu.unse.siga.ui;

import ar.edu.unse.siga.domain.Usuario;
import ar.edu.unse.siga.service.AuthService;
import ar.edu.unse.siga.ui.base.ImageCoverPanel;
import ar.edu.unse.siga.ui.base.SvgFixedLabel;
import ar.edu.unse.siga.ui.base.ThemeManager;
import ar.edu.unse.siga.ui.base.Ui;
import com.formdev.flatlaf.extras.FlatSVGIcon;

import javax.swing.*;
import java.awt.*; // <-- AWT imports para Font, Dimension, Layouts, etc.
import java.util.prefs.Preferences;

public class LoginScreen extends JDialog {

    // Devuelve un JLabel con el logo escalado a w×h.
// Intenta SVG y, si no puede renderizarlo, cae a PNG.
    private JLabel loadLogo(String svgPath, String pngPath, int w, int h) {
        // 1) ¿Existe el SVG?
        java.net.URL svgUrl = getClass().getClassLoader().getResource(svgPath);
        if (svgUrl != null) {
            try {
                // Usa constructor por ruta (String) — tu FlatLaf no acepta URL
                com.formdev.flatlaf.extras.FlatSVGIcon svg = new com.formdev.flatlaf.extras.FlatSVGIcon(svgPath, w, h);
                return new JLabel(svg);
            } catch (Throwable renderFail) {
                System.err.println("[SVG] No se pudo renderizar: " + svgPath + " -> " + renderFail);
            }
        } else {
            System.err.println("[SVG] No se encontró en el classpath: " + svgPath);
        }

        // 2) ¿Existe el PNG?
        java.net.URL pngUrl = getClass().getClassLoader().getResource(pngPath);
        if (pngUrl != null) {
            ImageIcon base = new ImageIcon(pngUrl);
            Image scaled = base.getImage().getScaledInstance(w, h, Image.SCALE_SMOOTH);
            return new JLabel(new ImageIcon(scaled));
        } else {
            System.err.println("[PNG] No se encontró en el classpath: " + pngPath);
        }

        // 3) Fallback textual
        JLabel fallback = new JLabel("SIGA");
        fallback.setFont(new Font("Segoe UI", Font.BOLD, 28));
        return fallback;
    }

// Carga un icono pequeño para los campos (sólo SVG; si quisieras PNG, hacemos lo mismo que arriba)
    private com.formdev.flatlaf.extras.FlatSVGIcon svgIcon(String cpPath, int w, int h) {
        java.net.URL url = getClass().getClassLoader().getResource(cpPath);
        if (url == null) {
            System.err.println("[SVG] No se encontró en el classpath: " + cpPath);
            return null;
        }
        try {
            return new com.formdev.flatlaf.extras.FlatSVGIcon(cpPath, w, h);
        } catch (Throwable t) {
            System.err.println("[SVG] No se pudo renderizar: " + cpPath + " -> " + t);
            return null;
        }
    }

    // =============================================================
    private final AuthService auth;
    private Usuario logged;

    private final JTextField txtUser = new JTextField(24);
    private final JPasswordField txtPass = new JPasswordField(24);
    private final JCheckBox chkRemember = new JCheckBox("Recordar usuario");
    private final JButton btnLogin = new JButton("INGRESAR");

    // prefs (guardamos SOLO usuario)
    private final Preferences prefs = Preferences.userRoot().node("ar.edu.unse.siga.ui.LoginScreen");

    private JLabel loadLogoPng(String pngPath, int w, int h) {
        java.net.URL url = getClass().getClassLoader().getResource(pngPath);
        if (url == null) {
            System.err.println("[PNG] No se encontró en el classpath: " + pngPath);
            JLabel fallback = new JLabel("SIGA");
            fallback.setFont(new Font("Segoe UI", Font.BOLD, 28));
            return fallback;
        }
        // imprimimos la URL real para depurar
        System.out.println("[PNG] Cargando desde: " + url);

        ImageIcon base = new ImageIcon(url);
        // escalado suave al tamaño pedido
        Image scaled = base.getImage().getScaledInstance(w, h, Image.SCALE_SMOOTH);
        return new JLabel(new ImageIcon(scaled));
    }

    public LoginScreen(Window owner, AuthService auth) {
        super(owner, "Ingresar", ModalityType.APPLICATION_MODAL);
        this.auth = auth;

        ThemeManager.installDefaults();
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setMinimumSize(new Dimension(1100, 700));
        setPreferredSize(new Dimension(1200, 760));
        setSize(getPreferredSize());
        setLocationRelativeTo(owner);

        var root = new JPanel(new BorderLayout());
        setContentPane(root);
        root.add(buildLeft(), BorderLayout.CENTER);

        var right = buildRight();
        right.setPreferredSize(new Dimension(520, getHeight())); // columna derecha fija
        root.add(right, BorderLayout.EAST);

        // accesos
        getRootPane().setDefaultButton(btnLogin);
        getRootPane().registerKeyboardAction(
                e -> {
                    logged = null;
                    dispose();
                },
                KeyStroke.getKeyStroke("ESCAPE"),
                JComponent.WHEN_IN_FOCUSED_WINDOW
        );
        btnLogin.addActionListener(e -> tryLogin());

        // cargar preferencia (recordar usuario)
        String remembered = prefs.get("username", "");
        if (!remembered.isEmpty()) {
            txtUser.setText(remembered);
            chkRemember.setSelected(true);
        }
    }

    private JComponent buildLeft() {
        var left = new ImageCoverPanel("hero/background_siga.jpg");
        left.setLayout(new GridBagLayout());

        var box = new JPanel();
        box.setOpaque(false);
        box.setLayout(new BoxLayout(box, BoxLayout.Y_AXIS));
        box.setBorder(BorderFactory.createEmptyBorder(40, 56, 40, 56));

        var hola = new SvgFixedLabel("hero/Hola,.svg", 380);
        var bienv = new SvgFixedLabel("hero/Bienvenido!.svg", 460);

        var desc = new JLabel("<html><div style='width:420px'>"
                + "Sistema Integrado de Gestión Administrativa diseñado para facilitar "
                + "las actividades de nuestra universidad."
                + "</div></html>");
        desc.setForeground(Color.WHITE);
        desc.setFont(new Font("Segoe UI", Font.PLAIN, 18));

        box.add(hola);
        box.add(Box.createVerticalStrut(6));
        box.add(bienv);
        box.add(Box.createVerticalStrut(18));
        box.add(desc);

        var gc = new GridBagConstraints();
        gc.anchor = GridBagConstraints.LINE_START;
        left.add(box, gc);
        return left;
    }

    private JComponent buildRight() {
        JPanel right = new JPanel(new GridBagLayout());
        right.setBackground(Color.WHITE);
        right.setPreferredSize(new Dimension(520, getHeight()));

        GridBagConstraints gc = new GridBagConstraints();
        gc.gridx = 0;
        gc.fill = GridBagConstraints.NONE;
        gc.anchor = GridBagConstraints.CENTER;
        gc.weightx = 1.0;
        gc.weighty = 1.0; // <- hace que se centre verticalmente

        JPanel col = new JPanel();
        col.setOpaque(false);
        col.setLayout(new BoxLayout(col, BoxLayout.Y_AXIS));
        col.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel logo = loadLogoPng("branding/logo_sigav2.png", 260, 84);
        logo.setAlignmentX(Component.CENTER_ALIGNMENT);

        col.add(logo);
        col.add(Box.createVerticalStrut(20));
        col.add(buildForm());

        right.add(col, gc);

        return right;
    }

    private JLabel labelSmall(String text) {
        JLabel l = new JLabel(text);
        l.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        l.setForeground(new Color(110, 110, 110));
        l.setBorder(BorderFactory.createEmptyBorder(8, 4, 4, 4));
        l.setAlignmentX(Component.LEFT_ALIGNMENT);  // <--- clave
        return l;
    }

    /**
     * Campo con icono a la izquierda y (si es password) “ojo” a la derecha
     */
    private JComponent fieldWithIcon(JTextField field, String iconPath, boolean isPassword) {
        // Contenedor con borde del campo completo
        JPanel wrap = new JPanel(new GridBagLayout());
        wrap.setOpaque(false);
        wrap.setAlignmentX(Component.LEFT_ALIGNMENT); // <--- alinear con la etiqueta

        // Borde y padding unificados (para que el ojo quede dentro)
        wrap.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(180, 180, 180)),
                BorderFactory.createEmptyBorder(6, 8, 6, 8)
        ));

        // Forzamos mismo alto/ancho para ambos campos
        Dimension fieldSize = new Dimension(360, 40); // ajustá 360 si querés más ancho
        wrap.setPreferredSize(fieldSize);
        wrap.setMaximumSize(fieldSize);
        wrap.setMinimumSize(fieldSize);

        // Icono izquierdo
        JLabel icon = new JLabel(svgIcon(iconPath, 20, 20));

        // El propio JTextField/JPasswordField sin borde (lo tiene el wrap)
        field.putClientProperty("JComponent.minimumHeight", 38);
        field.setBorder(BorderFactory.createEmptyBorder(4, 6, 4, 6)); // padding interno
        field.putClientProperty("JTextField.placeholderText",
                iconPath.contains("password") ? "admin123" : "admin");
        if (field instanceof JPasswordField) {
            ((JPasswordField) field).setEchoChar('\u2022'); // •
        }

        // Ojo (si corresponde)
        AbstractButton eye = null;
        if (isPassword && field instanceof JPasswordField pwd) {
            eye = new JToggleButton(svgIcon("icons/Vector.svg", 18, 18));
            eye.setToolTipText("Mostrar/ocultar contraseña");
            eye.setBorder(BorderFactory.createEmptyBorder());
            eye.setContentAreaFilled(false);
            eye.setFocusPainted(false);
            eye.addActionListener(e -> {
                boolean show = ((JToggleButton) e.getSource()).isSelected();
                pwd.setEchoChar(show ? (char) 0 : '\u2022');
            });
        }

        // Layout en 3 columnas: [icono] [campo (expande)] [ojo]
        GridBagConstraints c = new GridBagConstraints();
        c.gridy = 0;
        c.insets = new Insets(0, 0, 0, 0);
        c.anchor = GridBagConstraints.CENTER;

        // Col 0: icono
        c.gridx = 0;
        c.weightx = 0;
        c.fill = GridBagConstraints.NONE;
        wrap.add(icon, c);

        // Col 1: campo (se expande)
        c.gridx = 1;
        c.weightx = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        wrap.add(field, c);

        // Col 2: ojo (opcional)
        if (eye != null) {
            c.gridx = 2;
            c.weightx = 0;
            c.fill = GridBagConstraints.NONE;
            wrap.add(eye, c);
        }

        return wrap;
    }

    private JPanel buildForm() {
        final int FIELD_WIDTH = 360; // mismo ancho visual que los campos
        JPanel form = new JPanel();
        form.setOpaque(false);
        form.setLayout(new BoxLayout(form, BoxLayout.Y_AXIS));
        form.setAlignmentX(Component.CENTER_ALIGNMENT);

        // USUARIO
        form.add(labelSmall("USUARIO"));
        form.add(fieldWithIcon(txtUser, "icons/mdi_user-key.svg", false));

        // CONTRASEÑA
        form.add(Box.createVerticalStrut(12));
        form.add(labelSmall("CONTRASEÑA"));
        form.add(fieldWithIcon(txtPass, "icons/mdi_password.svg", true));

        // --- Fila: [ Recordar usuario ]........................[ ¿Olvidaste tu contraseña? ] ---
        form.add(Box.createVerticalStrut(10));

        JPanel rememberRow = new JPanel(new BorderLayout());
        rememberRow.setOpaque(false);
        rememberRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        rememberRow.setMaximumSize(new Dimension(FIELD_WIDTH, 26)); // alinear bordes con el campo

        chkRemember.setText("Recordar usuario");
        chkRemember.setOpaque(false);
        rememberRow.add(chkRemember, BorderLayout.WEST);

        JLabel forgot = new JLabel("<html><a href='#'>¿Olvidaste tu contraseña?</a></html>");
        forgot.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        forgot.setForeground(new Color(60, 60, 60));
        forgot.setOpaque(false);
        forgot.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                JOptionPane.showMessageDialog(LoginScreen.this,
                        "Pedí el reseteo al administrador del sistema.",
                        "Recuperar contraseña", JOptionPane.INFORMATION_MESSAGE);
            }
        });
        rememberRow.add(forgot, BorderLayout.EAST);

        form.add(rememberRow);

        // Botón
        form.add(Box.createVerticalStrut(12));
        btnLogin.setPreferredSize(new Dimension(220, 40));
        btnLogin.setFocusPainted(false);
        btnLogin.setBackground(new Color(40, 40, 40));
        btnLogin.setForeground(Color.WHITE);
        btnLogin.putClientProperty("JButton.buttonType", "roundRect");
        btnLogin.setBackground(new java.awt.Color(71, 115, 252));
        btnLogin.setForeground(java.awt.Color.WHITE);
        btnLogin.setOpaque(true);
        btnLogin.setFocusPainted(false);
        btnLogin.putClientProperty("JButton.buttonType", "roundRect");

        JPanel btnWrap = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        btnWrap.setOpaque(false);
        btnWrap.add(btnLogin);
        form.add(btnWrap);

        form.add(Box.createVerticalStrut(20)); // aire inferior

        return form;
    }

    private void tryLogin() {
        try {
            String u = txtUser.getText().trim();
            String p = new String(txtPass.getPassword());
            if (u.isEmpty()) {
                throw new IllegalArgumentException("Ingresá tu usuario");
            }
            if (p.isEmpty()) {
                throw new IllegalArgumentException("Ingresá tu contraseña");
            }

            // guardar preferencia
            if (chkRemember.isSelected()) {
                prefs.put("username", u);
            } else {
                prefs.remove("username");
            }

            logged = auth.login(u, p);
            dispose();
        } catch (Exception ex) {
            Ui.error(this, ex);
        }
    }

    public Usuario showDialog() {
        setVisible(true);
        return logged;
    }
}
