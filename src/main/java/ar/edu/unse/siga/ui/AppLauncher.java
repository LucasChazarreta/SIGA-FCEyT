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

import ar.edu.unse.siga.persistence.dao.TramiteDao;
import ar.edu.unse.siga.persistence.jdbc.JdbcTramiteDao;
import ar.edu.unse.siga.service.TramiteService;

public class AppLauncher {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            // Wiring simple
            InsumoDao insumoDao = new JdbcInsumoDao();
            MovimientoDao movimientoDao = new JdbcMovimientoDao();
            InventarioService service = new InventarioService(insumoDao);
            TramiteDao tramiteDao = new JdbcTramiteDao();
            TramiteService tramiteService = new TramiteService(tramiteDao);
            service.setMovimientoDao(movimientoDao);
            
            InventarioFrame frame = new InventarioFrame(service);
            frame.setVisible(true);
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

        });
    }
    
}

