package ar.edu.unse.siga.ui;

import ar.edu.unse.siga.common.CurrentSession;
import ar.edu.unse.siga.persistence.dao.*;
import ar.edu.unse.siga.persistence.jdbc.*;
import ar.edu.unse.siga.service.AuthService;
import ar.edu.unse.siga.service.InventarioService;
import ar.edu.unse.siga.service.TramiteService;

import javax.swing.*;

public class AppLauncher {

    public static void launch() {
        ar.edu.unse.siga.ui.base.ThemeManager.installDefaults();

        // DAOs
        InsumoDao insumoDao = new JdbcInsumoDao();
        MovimientoDao movDao = new JdbcMovimientoDao();
        UsuarioDao usuarioDao = new JdbcUsuarioDao();
        TramiteDao tramiteDao = new JdbcTramiteDao();

        // Services
        AuthService auth = new AuthService(usuarioDao);
        InventarioService inv = new InventarioService(insumoDao);
        inv.setMovimientoDao(movDao);
        TramiteService tra = new TramiteService(tramiteDao);

        // Login
        var login = new LoginDialog(null, auth);
        login.setVisible(true);

        var user = login.getLogged();
        if (user == null) {
            System.exit(0);
            return;
        }
        CurrentSession.setUser(user);

        var main = new ar.edu.unse.siga.ui.shell.ShellFrame(inv, tra, auth);
        main.setVisible(true);

    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(AppLauncher::launch);
    }
}
