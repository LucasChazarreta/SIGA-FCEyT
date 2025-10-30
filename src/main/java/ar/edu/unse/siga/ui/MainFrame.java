package ar.edu.unse.siga.ui;

import ar.edu.unse.siga.common.CurrentSession;
import ar.edu.unse.siga.persistence.dao.*;
import ar.edu.unse.siga.persistence.jdbc.*;
import ar.edu.unse.siga.service.AuthService;
import ar.edu.unse.siga.service.InventarioService;
import ar.edu.unse.siga.service.TramiteService;
import ar.edu.unse.siga.ui.base.ThemeManager;
import ar.edu.unse.siga.ui.inventario.AjustesAvanzadosDialog;
import ar.edu.unse.siga.ui.inventario.CategoriaFrame;
import ar.edu.unse.siga.ui.inventario.InsumoFrame;
import ar.edu.unse.siga.ui.reportes.ReporteMovimientosDialog;
import ar.edu.unse.siga.ui.tramites.TramiteFrame;
import ar.edu.unse.siga.ui.usuarios.UsuarioFrame;
import ar.edu.unse.siga.config.AppServices;
import ar.edu.unse.siga.persistence.jdbc.JdbcCategoriaDao;

import javax.swing.*;
import java.awt.*;

public class MainFrame extends JFrame {

    private final JDesktopPane desktop = new JDesktopPane();

    // Servicios y DAOs inyectados
    private final InventarioService inventarioService;
    private final TramiteService tramiteService;
    private final AuthService authService;

    public MainFrame(InventarioService invSrv, TramiteService traSrv, AuthService authSrv) {
        super("SIGA - Sistema de Gestión (usuario: "
                + (CurrentSession.getUser() != null ? CurrentSession.getUser().getUsername() : "-") + ")");
        this.inventarioService = invSrv;
        this.tramiteService = traSrv;
        this.authService = authSrv;

        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLayout(new BorderLayout());
        setJMenuBar(buildMenuBar());
        add(desktop, BorderLayout.CENTER);
        add(buildStatusBar(), BorderLayout.SOUTH);
        add(buildToolbar(), BorderLayout.NORTH);
        add(desktop, BorderLayout.CENTER);
        setSize(1100, 700);
        setLocationRelativeTo(null);

        InventarioService inv = AppServices.inventario();
        TramiteService tra = AppServices.tramite();

        JPanel inventario = new ar.edu.unse.siga.ui.inventario.InventarioGestionPanel(inv);
        JPanel informes = new ar.edu.unse.siga.ui.reportes.InformesPanel(inv, tra);
    }

    private JToolBar buildToolbar() {
        var tb = new JToolBar();
        tb.setFloatable(false);

        boolean isAdmin = CurrentSession.getUser() != null
                && "ADMIN".equalsIgnoreCase(CurrentSession.getUser().getRol().getNombre());

        var btnIns = new JButton("Insumos", ThemeManager.svg("icons/add.svg", 16));
        btnIns.addActionListener(e -> openInDesktop(new ar.edu.unse.siga.ui.inventario.InsumoFrame(inventarioService)));

        var btnTra = new JButton("Solicitudes", ThemeManager.svg("icons/search.svg", 16));
        btnTra.addActionListener(e -> openInDesktop(new TramiteFrame(tramiteService, inventarioService)));

        var btnCsv = new JButton("Movimientos CSV", ThemeManager.svg("icons/csv.svg", 16));
        btnCsv.addActionListener(e -> new ar.edu.unse.siga.ui.reportes.ReporteMovimientosDialog(this).setVisible(true));

        tb.add(btnIns);
        tb.add(btnTra);
        tb.add(btnCsv);

        if (isAdmin) {
            var btnAjustes = new JButton("Ajustes");
            btnAjustes.addActionListener(e -> new AjustesAvanzadosDialog(this, inventarioService).setVisible(true));
            tb.add(btnAjustes);
        }
        return tb;
    }

    private JMenuBar buildMenuBar() {
        var mb = new JMenuBar();

        var mInventario = new JMenu("Inventario");
        var miInsumos = new JMenuItem("ABM Insumos");
        var miCategorias = new JMenuItem("ABM Categorías");
        var miReporte = new JMenuItem("Reporte Movimientos (CSV)");

        miInsumos.addActionListener(e -> openInDesktop(new InsumoFrame(inventarioService)));
        miCategorias.addActionListener(e -> openInDesktop(new CategoriaFrame(inventarioService)));
        miReporte.addActionListener(e -> new ReporteMovimientosDialog(this).setVisible(true));

        mInventario.add(miInsumos);
        mInventario.add(miCategorias);
        mInventario.addSeparator();
        mInventario.add(miReporte);

        var mTramites = new JMenu("Solicitudes");
        var miTramites = new JMenuItem("ABM Solicitudes");
        miTramites.addActionListener(e -> openInDesktop(new TramiteFrame(tramiteService, inventarioService)));
        mTramites.add(miTramites);

        var mUsuarios = new JMenu("Usuarios");
        var miUsuarios = new JMenuItem("ABM Usuarios");
        miUsuarios.addActionListener(e -> openInDesktop(new UsuarioFrame(authService)));
        mUsuarios.add(miUsuarios);

        var mVista = new JMenu("Vista");
        var miTema = new JCheckBoxMenuItem("Modo oscuro");
        miTema.setState(ThemeManager.isDark());
        miTema.addActionListener(e -> {
            if (miTema.getState()) {
                ThemeManager.setDark();
            } else {
                ThemeManager.setLight();
            }
        });
        mVista.add(miTema);

        mb.add(mVista); // agrégalo antes del menú Sesión

        var mSesion = new JMenu("Sesión");
        var miLogout = new JMenuItem("Cerrar sesión");
        miLogout.addActionListener(e -> {
            CurrentSession.clear();
            dispose();
            AppLauncher.launch(); // vuelve a login
        });
        mSesion.add(miLogout);

        // Autorización básica por rol
        boolean isAdmin = CurrentSession.getUser() != null
                && "ADMIN".equalsIgnoreCase(CurrentSession.getUser().getRol().getNombre());
        mUsuarios.setEnabled(isAdmin);

        mb.add(mInventario);
        mb.add(mTramites);
        mb.add(mUsuarios);
        mb.add(Box.createHorizontalGlue());
        mb.add(mSesion);

        return mb;
    }

    private JPanel buildStatusBar() {
        JPanel p = new JPanel(new BorderLayout());
        var u = ar.edu.unse.siga.common.CurrentSession.getUser();
        String txt = u == null ? "Sin sesión" : "Conectado: " + u.getUsername() + "  | Rol: " + u.getRol().getNombre();
        var lblLeft = new JLabel("  " + txt);
        var lblClock = new JLabel(" ");
        lblClock.setHorizontalAlignment(SwingConstants.RIGHT);
        p.add(lblLeft, BorderLayout.WEST);
        p.add(lblClock, BorderLayout.EAST);

        // reloj simple
        new javax.swing.Timer(1000, e -> {
            var now = java.time.LocalDateTime.now();
            lblClock.setText(" " + now.format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) + "  ");
        }).start();
        return p;
    }

    private void openInDesktop(JInternalFrame f) {
        for (var fr : desktop.getAllFrames()) {
            if (fr.getClass().equals(f.getClass())) {
                try {
                    fr.setSelected(true);
                } catch (Exception ignore) {
                };
                return;
            }
        }
        desktop.add(f);
        f.setVisible(true);
    }

// Wiring por defecto (si alguien da run a este frame directo)
public static MainFrame defaultWired() {
    // DAOs
    UsuarioDao usuarioDao = new JdbcUsuarioDao();
    TramiteDao tramiteDao = new JdbcTramiteDao();

    // Services
    AuthService auth = new AuthService(usuarioDao);

    // Inventario y Solicitudes salen de AppServices
    var inv = AppServices.inventario();
    var tra = AppServices.tramite();

    return new MainFrame(inv, tra, auth);
}

}
