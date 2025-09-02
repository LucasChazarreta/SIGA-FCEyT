/* main para arrancar la app
 aqui tenemos presente la Regla de oro: UI → Service → DAO (nunca UI→DAO directo).
 lo que hacemos es creaar un ensamblador simple para inyectar JdbcInsumoDao al InventarioService y lanzar la UI.*/

package ar.edu.unse.siga.ui;

import ar.edu.unse.siga.persistence.dao.InsumoDao;
import ar.edu.unse.siga.persistence.dao.MovimientoDao;
import ar.edu.unse.siga.persistence.dao.UsuarioDao;
import ar.edu.unse.siga.persistence.jdbc.JdbcInsumoDao;
import ar.edu.unse.siga.persistence.jdbc.JdbcMovimientoDao;
import ar.edu.unse.siga.persistence.jdbc.JdbcUsuarioDao;
import ar.edu.unse.siga.service.AuthService;
import ar.edu.unse.siga.service.InventarioService;

import javax.swing.*;

public class AppLauncher {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            // DAOs
            InsumoDao insumoDao = new JdbcInsumoDao();
            MovimientoDao movimientoDao = new JdbcMovimientoDao();
            UsuarioDao usuarioDao = new JdbcUsuarioDao();

            // Servicios
            AuthService auth = new AuthService(usuarioDao);
            InventarioService service = new InventarioService(insumoDao);
            service.setMovimientoDao(movimientoDao);

            // Login
            LoginDialog login = new LoginDialog(null, auth);
            login.setVisible(true);

            // Si loguea, abrimos la app
            InventarioFrame frame = new InventarioFrame(service);
            frame.setVisible(true);
        });
    }
}

