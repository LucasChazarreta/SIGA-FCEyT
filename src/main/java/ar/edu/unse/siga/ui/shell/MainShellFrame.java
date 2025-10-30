package ar.edu.unse.siga.ui.shell;

import ar.edu.unse.siga.ui.inventario.InventarioGestionPanel;
import ar.edu.unse.siga.ui.pages.TramiteEntradaPage;
import ar.edu.unse.siga.ui.reportes.InformesPanel;

import javax.swing.*;
import java.awt.*;


import ar.edu.unse.siga.config.AppServices;
import ar.edu.unse.siga.service.InventarioService;
import ar.edu.unse.siga.service.TramiteService;

public class MainShellFrame extends JFrame {

    private final CardLayout cards = new CardLayout();
    private final JPanel content = new JPanel(cards);

    public MainShellFrame() {
        super("SIGA - FCEyT");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(1200, 760);
        setLocationRelativeTo(null);

        // Sidebar (muy simple)
        var sidebar = new JPanel();
        sidebar.setLayout(new BoxLayout(sidebar, BoxLayout.Y_AXIS));
        sidebar.setBackground(new Color(0x0E5ECF)); // azul de ejemplo
        sidebar.setPreferredSize(new Dimension(260, 760));

        JButton btnInicio = navButton("INICIO");
        JButton btnInventario = navButton("INVENTARIO");
        JButton btnSolicitudes = navButton("SOLICITUDES");
        JButton btnInformes = navButton("INFORMES");

        sidebar.add(Box.createVerticalStrut(20));
        sidebar.add(btnInicio);
        sidebar.add(Box.createVerticalStrut(8));
        sidebar.add(btnInventario);
        sidebar.add(Box.createVerticalStrut(8));
        sidebar.add(btnSolicitudes);
        sidebar.add(Box.createVerticalStrut(8));
        sidebar.add(btnInformes);
        sidebar.add(Box.createVerticalGlue());

        
        InventarioService inv = AppServices.inventario();
        TramiteService tra   = AppServices.tramite();
        // Vistas
        
        JPanel inventario = new InventarioGestionPanel(inv);
        JPanel tramites   = new TramiteEntradaPage(tra, inv);
        JPanel informes   = new InformesPanel(inv, tra);
        JPanel home = buildHome();
        

        content.add(home, "HOME");
        content.add(inventario, "INV");
        content.add(tramites, "TRA");
        content.add(informes, "INF");

        // Eventos
        btnInicio.addActionListener(e -> cards.show(content, "HOME"));
        btnInventario.addActionListener(e -> cards.show(content, "INV"));
        btnSolicitudes.addActionListener(e -> cards.show(content, "TRA"));
        btnInformes.addActionListener(e -> cards.show(content, "INF"));

        // Layout principal
        var root = new JPanel(new BorderLayout());
        root.add(sidebar, BorderLayout.WEST);
        root.add(content, BorderLayout.CENTER);
        setContentPane(root);

        // Vista inicial
        cards.show(content, "HOME");
    }

    private JButton navButton(String text) {
        var b = new JButton(text);
        b.setAlignmentX(Component.CENTER_ALIGNMENT);
        b.setMaximumSize(new Dimension(220, 42));
        b.setFocusPainted(false);
        return b;
    }

    private JPanel buildHome() {
        var p = new JPanel(new GridBagLayout());
        var title = new JLabel("SIGA - FCEyT");
        title.setFont(title.getFont().deriveFont(Font.BOLD, 28f));
        p.add(title);
        return p;
    }
}
