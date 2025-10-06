package ar.edu.unse.siga.ui.shell;

import ar.edu.unse.siga.common.CurrentSession;
import ar.edu.unse.siga.service.AuthService;
import ar.edu.unse.siga.service.InventarioService;
import ar.edu.unse.siga.service.TramiteService;
import ar.edu.unse.siga.ui.base.CardPanel;
import ar.edu.unse.siga.ui.base.GradientPanel;
import ar.edu.unse.siga.ui.base.NavButton;
import ar.edu.unse.siga.ui.base.ThemeManager;
import ar.edu.unse.siga.ui.base.WaveSidebarPanel;
import ar.edu.unse.siga.ui.pages.FinanzasPage;
import ar.edu.unse.siga.ui.pages.HomePage;
import ar.edu.unse.siga.ui.pages.InventoryMovementsPage;
import ar.edu.unse.siga.ui.pages.InventoryPage;
import ar.edu.unse.siga.ui.pages.TramiteEntradaPage;
import ar.edu.unse.siga.ui.reportes.InformesPanel;
import ar.edu.unse.siga.ui.pages.ProfilePage;

import javax.swing.*;
import java.awt.*;
import java.time.format.DateTimeFormatter;

public class ShellFrame extends JFrame {

    private final CardLayout cardLayout = new CardLayout();
    private final JPanel cards = new JPanel(cardLayout);
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
        root.setLayout(new BorderLayout(24, 24));
        setContentPane(root);

        // Sidebar
        var sidebar = buildSidebar();
        root.add(sidebar, BorderLayout.WEST);

        // Card container (cada página va adentro de una “tarjeta”)
        var cardHolder = new CardPanel();
        cardHolder.setLayout(new BorderLayout(20, 20));
        cardHolder.add(buildHeader(), BorderLayout.NORTH);
        cards.setOpaque(false);
        cardHolder.add(cards, BorderLayout.CENTER);
        root.add(cardHolder, BorderLayout.CENTER);

        // Páginas
        addPage("home", new HomePage(
                CurrentSession.getUser(), // usuario para el saludo
                inventarioService, // métricas dinámicas (insumos críticos)
                this::showPage // callback navegación desde Home
        ));
        addPage("inventario", new InventoryPage(inventarioService));
        addPage("movimientos", new InventoryMovementsPage(inventarioService));
        addPage("tramites", new TramiteEntradaPage(tramiteService));
        addPage("reportes", new InformesPanel(inventarioService, tramiteService));
        addPage("finanzas", new FinanzasPage());
        addPage("perfil", new ProfilePage(CurrentSession.getUser()));

        // Mostrar Home al iniciar
        showPage("home");

        setSize(1200, 760);
        setLocationRelativeTo(null);
    }

    private JPanel buildHeader() {
        var header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        lblTitle.setFont(lblTitle.getFont().deriveFont(Font.BOLD, 26f));
        lblTitle.setForeground(new Color(30, 45, 92));
        header.add(lblTitle, BorderLayout.WEST);

        var u = CurrentSession.getUser();
        lblUser.setText((u != null ? u.getUsername() : "-")
                + "  |  " + java.time.LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
        lblUser.setForeground(new Color(90, 110, 150));
        header.add(lblUser, BorderLayout.EAST);
        return header;
    }

    private JPanel buildSidebar() {
        WaveSidebarPanel side = new WaveSidebarPanel();
        side.setPreferredSize(new Dimension(260, 720));
        side.setLayout(new BorderLayout());

        // Marca
        JPanel brand = new JPanel();
        brand.setOpaque(false);
        brand.setLayout(new BoxLayout(brand, BoxLayout.Y_AXIS));
        brand.setBorder(BorderFactory.createEmptyBorder(24, 24, 24, 24));
        JLabel name = new JLabel("SIGA-FCEyT");
        name.setForeground(Color.WHITE);
        name.setFont(name.getFont().deriveFont(Font.BOLD, 20f));
        JLabel tagline = new JLabel("Sistema de gestión administrativa");
        tagline.setForeground(new Color(235, 245, 255));
        tagline.setFont(tagline.getFont().deriveFont(Font.PLAIN, 12f));
        brand.add(name);
        brand.add(Box.createVerticalStrut(4));
        brand.add(tagline);
        side.add(brand, BorderLayout.NORTH);

        // Menú
        JPanel menu = new JPanel();
        menu.setOpaque(false);
        menu.setLayout(new BoxLayout(menu, BoxLayout.Y_AXIS));
        menu.setBorder(BorderFactory.createEmptyBorder(8, 16, 16, 16));

        ButtonGroup grp = new ButtonGroup();
        NavButton bHome = nav("Inicio", "ui/icons/home.svg", "home");
        NavButton bInv = nav("Inventario", "ui/icons/inventory.svg", "inventario");
        NavButton bMov = nav("Movimientos", "ui/icons/movements.svg", "movimientos");
        NavButton bInf = nav("Informes", "ui/icons/reports.svg", "reportes");
        NavButton bTra = nav("Trámites", "ui/icons/tramites.svg", "tramites");
        NavButton bFin = nav("Finanzas", "ui/icons/finanzas.svg", "finanzas");

        grp.add(bHome);
        grp.add(bInv);
        grp.add(bMov);
        grp.add(bInf);
        grp.add(bTra);
        grp.add(bFin);

        menu.add(bHome);
        menu.add(Box.createVerticalStrut(8));
        menu.add(bInv);
        menu.add(Box.createVerticalStrut(8));
        menu.add(bMov);
        menu.add(Box.createVerticalStrut(12));
        menu.add(bInf);
        menu.add(Box.createVerticalStrut(8));
        menu.add(bTra);
        menu.add(Box.createVerticalStrut(8));
        menu.add(bFin);
        menu.add(Box.createVerticalGlue());

        // Seleccionado por defecto
        bHome.setSelected(true);

        // Scroll para evitar cortes en pantallas chicas (opcional)
        JScrollPane sp = new JScrollPane(menu);
        sp.setBorder(null);
        sp.setOpaque(false);
        sp.getViewport().setOpaque(false);
        sp.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        side.add(sp, BorderLayout.CENTER);

        // Logout
        JButton btnLogout = new JButton("  Cerrar sesión", (Icon) ThemeManager.svg("ui/icons/logout.svg", 18));
        btnLogout.setFocusPainted(false);
        btnLogout.setForeground(Color.WHITE);
        btnLogout.setOpaque(false);
        btnLogout.setContentAreaFilled(false);
        btnLogout.setBorder(BorderFactory.createEmptyBorder(12, 24, 18, 24));
        btnLogout.addActionListener(e -> {
            CurrentSession.clear();
            dispose();
            ar.edu.unse.siga.ui.AppLauncher.launch();
        });
        side.add(btnLogout, BorderLayout.SOUTH);

        return side;
    }

    private NavButton nav(String text, String icon, String key) {
        NavButton btn = new NavButton(text, icon);
        btn.addActionListener(e -> showPage(key, text));
        return btn;
    }

    private void addPage(String key, Component c) {
        cards.add(c, key);
    }

    /**
     * Mostrar página y actualizar título (utilizada por el sidebar y por
     * HomePage vía callback).
     */
    private void showPage(String key) {
        // Si viene sin label, resolvemos acá el texto estándar
        String text = switch (key) {
            case "home" ->
                "Inicio";
            case "inventario" ->
                "Inventario";
            case "movimientos" ->
                "Movimientos";
            case "reportes" ->
                "Informes";
            case "tramites" ->
                "Trámites";
            case "finanzas" ->
                "Finanzas";
            case "perfil" ->
                "Perfil";
            default ->
                key;
        };
        showPage(key, text);
    }

    private void showPage(String key, String titleText) {
        lblTitle.setText(titleText);
        cardLayout.show(cards, key);
    }
}
