/* main para arrancar la app
 aqui tenemos presente la Regla de oro: UI → Service → DAO (nunca UI→DAO directo).
 lo que hacemos es creaar un ensamblador simple para inyectar JdbcInsumoDao al InventarioService y lanzar la UI.*/

package ar.edu.unse.siga.ui;

import ar.edu.unse.siga.persistence.dao.InsumoDao;
import ar.edu.unse.siga.persistence.jdbc.JdbcInsumoDao;
import ar.edu.unse.siga.service.InventarioService;

import javax.swing.*;

import ar.edu.unse.siga.persistence.dao.MovimientoDao;
import ar.edu.unse.siga.persistence.jdbc.JdbcMovimientoDao;

public class AppLauncher {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            // Wiring simple
            InsumoDao insumoDao = new JdbcInsumoDao();
            MovimientoDao movimientoDao = new JdbcMovimientoDao();
            InventarioService service = new InventarioService(insumoDao);

            service.setMovimientoDao(movimientoDao);
            
            InventarioFrame frame = new InventarioFrame(service);
            frame.setVisible(true);
        });
    }
}

