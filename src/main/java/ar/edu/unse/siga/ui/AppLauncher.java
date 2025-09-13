package ar.edu.unse.siga.ui;

import ar.edu.unse.siga.persistence.dao.InsumoDao;
import ar.edu.unse.siga.persistence.dao.MovimientoDao;
import ar.edu.unse.siga.persistence.dao.UsuarioDao;
import ar.edu.unse.siga.persistence.dao.TramiteDao;

import ar.edu.unse.siga.persistence.jdbc.JdbcInsumoDao;
import ar.edu.unse.siga.persistence.jdbc.JdbcMovimientoDao;
import ar.edu.unse.siga.persistence.jdbc.JdbcUsuarioDao;
import ar.edu.unse.siga.persistence.jdbc.JdbcTramiteDao;

import ar.edu.unse.siga.service.AuthService;
import ar.edu.unse.siga.service.InventarioService;
import ar.edu.unse.siga.service.TramiteService;

import javax.swing.*;

public class AppLauncher {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            // DAOs
            InsumoDao insumoDao = new JdbcInsumoDao();
            MovimientoDao movimientoDao = new JdbcMovimientoDao();
            UsuarioDao usuarioDao = new JdbcUsuarioDao();
            TramiteDao tramiteDao = new JdbcTramiteDao();

            // Servicios
            AuthService auth = new AuthService(usuarioDao);
            InventarioService service = new InventarioService(insumoDao);
            service.setMovimientoDao(movimientoDao);
            TramiteService tramiteService = new TramiteService(tramiteDao);

            // Login
            LoginDialog login = new LoginDialog(null, auth);
            login.setVisible(true);

            // Ventana principal: Inventario
            InventarioFrame frame = new InventarioFrame(service);

            // Menú Módulos
            JMenuBar bar = new JMenuBar();
            JMenu mod = new JMenu("Módulos");
            JMenuItem miInventario = new JMenuItem("Inventario");
            JMenuItem miTramites = new JMenuItem("Trámites");
            mod.add(miInventario);
            mod.add(miTramites);
            bar.add(mod);
            frame.setJMenuBar(bar);

            // Abrir Inventario (re-usa la ventana ya creada)
            miInventario.addActionListener(ev -> frame.setVisible(true));

            // Abrir Trámites (crea y muestra la ventana de trámites)
            miTramites.addActionListener(ev -> {
                TramiteFrame tf = new TramiteFrame(tramiteService);
                tf.setVisible(true);
            });

            frame.setVisible(true);
        });
    }
}
