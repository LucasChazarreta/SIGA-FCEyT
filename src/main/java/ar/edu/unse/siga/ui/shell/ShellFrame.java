package ar.edu.unse.siga.ui.shell;

import ar.edu.unse.siga.common.CurrentSession;
import ar.edu.unse.siga.service.AuthService;
import ar.edu.unse.siga.service.InventarioService;
import ar.edu.unse.siga.service.TramiteService;
import ar.edu.unse.siga.ui.base.CardPanel;
import ar.edu.unse.siga.ui.base.GradientPanel;
import ar.edu.unse.siga.ui.base.ThemeManager;
import ar.edu.unse.siga.ui.pages.InventoryPage;
import ar.edu.unse.siga.ui.pages.TramiteEntradaPage;

import javax.swing.*;
import java.awt.*;
import java.time.format.DateTimeFormatter;

//import ar.edu.unse.siga.ui.pages.HomePage;

public class ShellFrame extends JFrame {
    private final JPanel cards = new CardLayoutPanel();
    private final JLabel lblTitle = new JLabel("Inicio");
    private final JLabel lblUser = new JLabel();

    // services
    private final InventarioService inventarioService;
    private final TramiteService tramiteService;
    private final AuthService authService;

    public ShellFrame(InventarioService inv, TramiteService tra, AuthService auth) {
        super("SIGA-FCEyT");
        this.inventarioService = inv;
        this.tramiteService = tra;
        this.authService = auth;

        setDefaultCloseOperation(EXIT_ON_CLOSE);
        ThemeManager.installDefaults();

        // Fondo general con gradiente
        var root = new GradientPanel();
        root.setLayout(new BorderLayout(16,16));
        setContentPane(root);

        // Sidebar
        var sidebar = buildSidebar();
        root.add(sidebar, BorderLayout.WEST);

        // Header (título + usuario)
        var header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        lblTitle.setFont(lblTitle.getFont().deriveFont(Font.BOLD, 22f));
        header.add(lblTitle, BorderLayout.WEST);
        var u = CurrentSession.getUser();
        lblUser.setText(" " + (u!=null? u.getUsername() : "-") + "   |   " +
                java.time.LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
        header.add(lblUser, BorderLayout.EAST);

        // Card container (cada página va adentro de una “tarjeta”)
        var cardHolder = new CardPanel();
        cardHolder.setLayout(new BorderLayout(12,12));
        cardHolder.add(header, BorderLayout.NORTH);
        cardHolder.add(cards, BorderLayout.CENTER);

        root.add(cardHolder, BorderLayout.CENTER);

        // Páginas
        //addPage("home", new HomePage()); 
        addPage("inventario", new InventoryPage(inventarioService));
        addPage("tramites", new TramiteEntradaPage(tramiteService));
        // Reportes y Usuarios los agregamos en el siguiente paso

        setSize(1200, 760);
        setLocationRelativeTo(null);
    }

    private JPanel buildSidebar() {
        var side = new JPanel();
        side.setOpaque(true);
        side.setBackground(new Color(19, 49, 86));
        side.setPreferredSize(new Dimension(240, 100));
        side.setLayout(new BorderLayout());

        // logo + nombre
        var brand = new JPanel(new BorderLayout());
        brand.setOpaque(false);
        var lbl = new JLabel("  SIGA-FCEyT");
        lbl.setForeground(Color.WHITE);
        lbl.setFont(lbl.getFont().deriveFont(Font.BOLD, 18f));
        brand.add(lbl, BorderLayout.WEST);
        brand.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));
        side.add(brand, BorderLayout.NORTH);

        // menú (toggle buttons)
        var menu = new JPanel();
        menu.setOpaque(false);
        menu.setLayout(new GridLayout(0,1,0,4));

        ButtonGroup grp = new ButtonGroup();

        var bHome = new NavButton("Inicio", "icons/home.svg");
        var bInv  = new NavButton("Inventario", "icons/box.svg");
        var bTra  = new NavButton("Mesa de Entrada", "icons/inbox.svg");
        var bRep  = new NavButton("Reportes", "icons/report.svg");
        var bUsr  = new NavButton("Usuarios", "icons/users.svg");

        for (var b : new JToggleButton[]{bHome,bInv,bTra,bRep,bUsr}) {
            b.setForeground(Color.WHITE);
            b.setBackground(new Color(19,49,86));
            b.setOpaque(false);
            b.addActionListener(e -> onNavClick((JToggleButton)e.getSource()));
            grp.add(b); menu.add(b);
        }
        bHome.setSelected(true);

        var center = new JPanel(new BorderLayout());
        center.setOpaque(false);
        center.add(menu, BorderLayout.NORTH);
        side.add(center, BorderLayout.CENTER);

        // logout
        var south = new JPanel(new BorderLayout());
        south.setOpaque(false);
        var btnLogout = new JButton("  Cerrar sesión", (Icon) ThemeManager.svg("icons/logout.svg",16));
        btnLogout.setFocusPainted(false);
        btnLogout.addActionListener(e -> {
            ar.edu.unse.siga.common.CurrentSession.clear();
            dispose();
            //ar.edu.unse.siga.ui.AppLauncher.launch(); // volver al login
        });
        btnLogout.setBorder(BorderFactory.createEmptyBorder(10,16,10,12));
        south.add(btnLogout, BorderLayout.SOUTH);
        side.add(south, BorderLayout.SOUTH);

        return side;
    }

    private void onNavClick(JToggleButton btn) {
        String text = btn.getText().trim();
        lblTitle.setText(text);
        switch (text) {
            case "Inicio" -> showPage("home");
            case "Inventario" -> showPage("inventario");
            case "Mesa de Entrada" -> showPage("tramites");
            case "Reportes" -> showPage("reportes");
            case "Usuarios" -> showPage("usuarios");
        }
    }

    private void addPage(String key, Component c) {
        cards.add(c, key);
    }
    private void showPage(String key) {
        ((CardLayout)cards.getLayout()).show(cards, key);
    }

    // panel con CardLayout
    static class CardLayoutPanel extends JPanel {
        CardLayoutPanel() { super(new CardLayout()); setOpaque(false); }
    }
}
