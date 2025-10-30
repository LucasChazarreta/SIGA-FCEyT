package ar.edu.unse.siga.ui.shell;

import ar.edu.unse.siga.common.CurrentSession;
import ar.edu.unse.siga.service.AuthService;
import ar.edu.unse.siga.service.InventarioService;
import ar.edu.unse.siga.service.TramiteService;
import ar.edu.unse.siga.ui.base.CardPanel;
import ar.edu.unse.siga.ui.base.GradientPanel;
import ar.edu.unse.siga.ui.base.ThemeManager;
import ar.edu.unse.siga.ui.base.WaveSidebarPanel;
import ar.edu.unse.siga.ui.pages.HomePage;
import ar.edu.unse.siga.ui.pages.InventoryMovementsPage;
import ar.edu.unse.siga.ui.pages.InventoryPage;
import ar.edu.unse.siga.ui.pages.TramiteEntradaPage;
import ar.edu.unse.siga.ui.reportes.InformesPanel;

import javax.swing.*;
import java.awt.*;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

public class ShellFrame extends JFrame {

    private final CardLayout cardLayout = new CardLayout();
    private final JPanel cards = new JPanel(cardLayout);
    private final JLabel lblTitle = new JLabel("Inicio");
    private final JLabel lblUser = new JLabel();

    // services
    private final InventarioService inventarioService;
    private final TramiteService tramiteService;
    private final AuthService authService;

    private HomePage homePage;
    private InventoryPage inventoryPage;
    private InventoryMovementsPage movimientosPage;
    private TramiteEntradaPage tramitesPage;
    private InformesPanel informesPanel;

    // mapa de clave de tarjeta -> botón del sidebar
    private final Map<String, NavButton> navByKey = new HashMap<>();
    private final ButtonGroup navGroup = new ButtonGroup();

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

        // ===== Navegación centralizada =====
        Consumer<String> nav = key -> {
            String title = switch (key) {
                case "home" -> "Inicio";
                case "inventario" -> "Inventario";
                case "movimientos" -> "Movimientos";
                case "reportes" -> "Informes";
                case "tramites" -> "Solicitudes";
                case "finanzas" -> "Finanzas";
                default -> key;
            };
            showCard(key, title);
        };

        inventoryPage = new InventoryPage(inventarioService);
        informesPanel = new InformesPanel(inventarioService, tramiteService);

        final TramiteEntradaPage[] tramitesHolder = new TramiteEntradaPage[1];
        homePage = new HomePage(CurrentSession.getUser(), inventarioService, tramiteService, new HomePage.HomeNavigation() {
            @Override
            public void navigateTo(String key) {
                nav.accept(key);
            }

            @Override
            public void openInventarioBajoMinimo() {
                nav.accept("inventario");
                if (inventoryPage != null) {
                    inventoryPage.mostrarInsumosBajoMinimo();
                }
            }

            @Override
            public void openSolicitudesNuevas() {
                nav.accept("tramites");
                if (tramitesHolder[0] != null) {
                    tramitesHolder[0].mostrarTramitesEstado("NUEVO");
                }
            }

            @Override
            public void openMovimientosHoy() {
                nav.accept("reportes");
                if (informesPanel != null) {
                    informesPanel.mostrarMovimientosSalidasHoy();
                }
            }
        });

        tramitesPage = new TramiteEntradaPage(tramiteService, homePage::recargarTramitesRecientes);
        tramitesHolder[0] = tramitesPage;

        movimientosPage = new InventoryMovementsPage(inventarioService);

        addPage("home", homePage);
        addPage("inventario", inventoryPage);
        addPage("movimientos", movimientosPage);
        addPage("tramites", tramitesPage);
        addPage("reportes", informesPanel);
        // addPage("finanzas",    new FinanzasPage()); // si la usás, descomentar

        setSize(1200, 760);
        setLocationRelativeTo(null);

        // Estado inicial (resalta botón activo y muestra la card)
        showCard("home", "Inicio");
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

        // Marca / branding
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

        NavButton bHome = nav("Inicio", "ui/icons/home.svg", "home");
        NavButton bInv = nav("Inventario", "ui/icons/inventory.svg", "inventario");
        NavButton bMov = nav("Movimientos", "ui/icons/movements.svg", "movimientos", 1);
        NavButton bInf = nav("Informes", "ui/icons/reports.svg", "reportes");
        NavButton bTra = nav("Solicitudes", "ui/icons/tramites.svg", "tramites");
        // NavButton bFin  = nav("Finanzas",     "ui/icons/finanzas.svg",   "finanzas");

        navGroup.add(bHome);
        navGroup.add(bInv);
        navGroup.add(bMov);
        navGroup.add(bInf);
        navGroup.add(bTra);
        // navGroup.add(bFin);

        menu.add(bHome);
        menu.add(Box.createVerticalStrut(8));
        menu.add(bInv);
        menu.add(Box.createVerticalStrut(4));
        menu.add(bMov);
        menu.add(Box.createVerticalStrut(12));
        menu.add(bInf);
        menu.add(Box.createVerticalStrut(8));
        menu.add(bTra);
        menu.add(Box.createVerticalStrut(8));
        // menu.add(bFin);
        menu.add(Box.createVerticalGlue());

        side.add(menu, BorderLayout.CENTER);

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

    /**
     * Crea y registra un NavButton asociado a la clave de CardLayout.
     */
    private NavButton nav(String text, String icon, String key) {
        return nav(text, icon, key, 0);
    }

    private NavButton nav(String text, String icon, String key, int level) {
        NavButton btn = new NavButton(text, icon, level);
        // registrar en el mapa para poder seleccionar el botón cuando cambiemos de card
        navByKey.put(key, btn);

        // SIEMPRE navegar con showCard(...) para mantener sincronía visual
        btn.addActionListener(e -> showCard(key, text));
        return btn;
    }

    /**
     * Muestra la card y resalta el botón activo (pastilla blanca).
     * Además, si se entra a "movimientos", refresca la lista de insumos.
     */
    private void showCard(String key, String title) {
        cardLayout.show(cards, key);
        lblTitle.setText(title);

        // marcar seleccionado el botón correspondiente
        for (Map.Entry<String, NavButton> e : navByKey.entrySet()) {
            NavButton b = e.getValue();
            boolean sel = e.getKey().equals(key);
            b.setSelected(sel);
        }

        // === NUEVO: refrescar insumos al entrar a Movimientos ===
        if ("movimientos".equals(key) && movimientosPage != null) {
            try {
                movimientosPage.refreshInsumos();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    private void addPage(String key, Component c) {
        cards.add(c, key);
    }
}
