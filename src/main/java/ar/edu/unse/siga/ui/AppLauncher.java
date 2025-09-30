package ar.edu.unse.siga.ui;

import ar.edu.unse.siga.common.CurrentSession;
import ar.edu.unse.siga.config.AppServices;
import ar.edu.unse.siga.domain.Usuario;
import ar.edu.unse.siga.service.AuthService;
import ar.edu.unse.siga.service.InventarioService;
import ar.edu.unse.siga.service.TramiteService;
import ar.edu.unse.siga.ui.auth.LoginScreen;
import ar.edu.unse.siga.ui.base.ThemeManager;
import ar.edu.unse.siga.ui.shell.ShellFrame;

import javax.swing.*;

public class AppLauncher {

    public static void launch() {
        ThemeManager.installDefaults();

        // Inicializar servicios unificados (usa tu DataSourceFactory)
        AppServices.init();
        var services = AppServices.get();

        // Services
        AuthService auth         = services.getAuthService();
        InventarioService inv    = services.inventario();
        TramiteService tra       = services.tramite();

        // Login (igual al flujo anterior)
        var login = new LoginScreen(null, auth);
        Usuario user = login.showDialog();
        if (user == null) {
            System.exit(0);
            return;
        }
        CurrentSession.setUser(user);

        // Shell principal (tu shell estilado con servicios)
        var main = new ShellFrame(inv, tra, auth);
        main.setVisible(true);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(AppLauncher::launch);
    }
}
