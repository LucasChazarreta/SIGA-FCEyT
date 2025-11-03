package ar.edu.unse.siga.ui;

import ar.edu.unse.siga.persistence.dao.*;
import ar.edu.unse.siga.persistence.jdbc.*;
import ar.edu.unse.siga.service.AuthService;
import ar.edu.unse.siga.service.InventarioService;
import ar.edu.unse.siga.service.TramiteService;
import ar.edu.unse.siga.config.AppServices;
import javax.swing.*;
import java.awt.*;

public class AppLauncher {

    public static void launch() {
        try {
            // Look & Feel (opcional)
            // com.formdev.flatlaf.FlatLightLaf.setup();

            // ==== DAOs / Servicios ====
            UsuarioDao usuarioDao = new JdbcUsuarioDao();
            CategoriaDao categoriaDao = new JdbcCategoriaDao();
            InsumoDao insumoDao = new JdbcInsumoDao();
            MovimientoDao movDao = new JdbcMovimientoDao();
            TramiteDao tramiteDao = new JdbcTramiteDao();
            UbicacionDao ubicDao = new JdbcUbicacionDao();

            AuthService auth = new AuthService(usuarioDao);
            AppServices.init();                       // crea DAOs y Services internamente
            InventarioService inv = AppServices.inventario();
            TramiteService tra = AppServices.tramite();
            Window owner = null; // sin owner
            var login = new LoginScreen(owner, auth);
            var usuario = login.showDialog(); // bloquea hasta cerrar

            if (usuario == null) {
                // canceló o falló => cerramos la app de forma limpia
                System.exit(0);
                return;
            }

            // ==== SHELL PRINCIPAL ====
            var main = new ar.edu.unse.siga.ui.shell.ShellFrame(inv, tra, auth);
            main.setLocationRelativeTo(null);
            main.setVisible(true);

        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(null,
                    ex.getMessage(), "Error al iniciar", JOptionPane.ERROR_MESSAGE);
            System.exit(1);
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(AppLauncher::launch);
    }
}
