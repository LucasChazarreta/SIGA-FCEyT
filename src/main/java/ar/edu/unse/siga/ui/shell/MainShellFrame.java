package ar.edu.unse.siga.ui.shell;

import ar.edu.unse.siga.config.AppServices;
import ar.edu.unse.siga.service.InventarioService;
import ar.edu.unse.siga.service.TramiteService;
import ar.edu.unse.siga.service.FinanzasService;

import ar.edu.unse.siga.ui.inventario.InventarioGestionPanel;
import ar.edu.unse.siga.ui.reportes.InformesPanel;
import ar.edu.unse.siga.ui.finanzas.FinanzasResumenPanel;
import ar.edu.unse.siga.ui.finanzas.FinanzasTransaccionesPanel;

import javax.swing.*;
import java.awt.*;
import java.util.LinkedHashMap;
import java.util.Map;

public class MainShellFrame extends JFrame {

    private final CardLayout cards = new CardLayout();
    private final JPanel cardPanel = new JPanel(cards);
    private final Map<String, JButton> navButtons = new LinkedHashMap<>();

    public MainShellFrame() {
        super("SIGA - FCEyT");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(1100, 700));
        setLocationRelativeTo(null);

        // Servicios
        AppServices srv = AppServices.get();
        InventarioService inv = srv.inventario();
        TramiteService tra   = srv.tramite();
        FinanzasService fin  = srv.getFinanzasService();

        // --- Sidebar ---
        JPanel sidebar = new JPanel();
        sidebar.setLayout(new BoxLayout(sidebar, BoxLayout.Y_AXIS));
        sidebar.setBorder(BorderFactory.createEmptyBorder(12,12,12,12));
        addNavButton(sidebar, "HOME", "Inicio");
        addNavButton(sidebar, "INV",  "Inventario");
        addNavButton(sidebar, "FIN",  "Finanzas");
        addNavButton(sidebar, "INF",  "Informes");

        // --- Cards ---
        cardPanel.add(buildHome(), "HOME");
        cardPanel.add(new InventarioGestionPanel(inv), "INV");

        // Finanzas (dos pestañas)
        var finTabs = new JTabbedPane();
        finTabs.addTab("Resumen", new FinanzasResumenPanel(fin));
        finTabs.addTab("Transacciones", new FinanzasTransaccionesPanel(fin));
        cardPanel.add(finTabs, "FIN");

        // Informes (usa inv y tra)
        cardPanel.add(new InformesPanel(inv, tra), "INF");

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, wrap(sidebar), cardPanel);
        split.setDividerLocation(220);
        split.setOneTouchExpandable(true);
        setContentPane(split);

        // Eventos
        navButtons.get("HOME").addActionListener(e -> showCard("HOME"));
        navButtons.get("INV").addActionListener(e -> showCard("INV"));
        navButtons.get("FIN").addActionListener(e -> showCard("FIN"));
        navButtons.get("INF").addActionListener(e -> showCard("INF"));

        showCard("HOME");
    }

    private void showCard(String key) {
        cards.show(cardPanel, key);
        navButtons.forEach((k, b) -> b.setEnabled(true));
        JButton current = navButtons.get(key);
        if (current != null) current.setEnabled(false);
    }

    private void addNavButton(JPanel sidebar, String key, String text) {
        JButton b = new JButton(text);
        b.setAlignmentX(Component.LEFT_ALIGNMENT);
        b.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
        sidebar.add(b);
        sidebar.add(Box.createVerticalStrut(8));
        navButtons.put(key, b);
    }

    private JPanel wrap(JComponent comp) {
        JPanel p = new JPanel(new BorderLayout());
        p.add(comp, BorderLayout.NORTH);
        return p;
    }

    private JPanel buildHome() {
        JPanel p = new JPanel(new GridBagLayout());
        JLabel t = new JLabel("SIGA - FCEyT");
        t.setFont(t.getFont().deriveFont(Font.BOLD, 28f));
        p.add(t);
        return p;
    }
}
