package ar.edu.unse.siga.ui;

import ar.edu.unse.siga.common.CurrentSession;
import ar.edu.unse.siga.persistence.dao.*;
import ar.edu.unse.siga.persistence.jdbc.*;
import ar.edu.unse.siga.service.AuthService;
import ar.edu.unse.siga.service.InventarioService;
import ar.edu.unse.siga.service.TramiteService;
import ar.edu.unse.siga.persistence.jdbc.JdbcCategoriaDao;
import ar.edu.unse.siga.persistence.jdbc.JdbcUbicacionDao;

import javax.swing.*;

public class AppLauncher {

    public static void launch() {
        ar.edu.unse.siga.ui.base.ThemeManager.installDefaults();

        // DAOs
        InsumoDao insumoDao = new JdbcInsumoDao();
        MovimientoDao movDao = new JdbcMovimientoDao();
        UsuarioDao usuarioDao = new JdbcUsuarioDao();
        TramiteDao tramiteDao = new JdbcTramiteDao();
        TramiteDetalleDao tramiteDetalleDao = new JdbcTramiteDetalleDao();
        CategoriaDao categoriaDao = new JdbcCategoriaDao();
        UbicacionDao ubicacionDao = new JdbcUbicacionDao();

        // Services
        AuthService auth = new AuthService(usuarioDao);
        InventarioService inv = new InventarioService(insumoDao, movDao, categoriaDao, ubicacionDao);
        TramiteService tra = new TramiteService(tramiteDao, tramiteDetalleDao, movDao, insumoDao);
        
        

        // Login (nueva pantalla)
        var login = new LoginScreen(null, auth);
        var user = login.showDialog();
        if (user == null) {
            System.exit(0);
            return;
        }
        ar.edu.unse.siga.common.CurrentSession.setUser(user);

        // Shell principal
        var main = new ar.edu.unse.siga.ui.shell.ShellFrame(inv, tra, auth);
        main.setVisible(true);

    }
    
    

    public static void main(String[] args) {
        SwingUtilities.invokeLater(AppLauncher::launch);
    }
    
    
}
