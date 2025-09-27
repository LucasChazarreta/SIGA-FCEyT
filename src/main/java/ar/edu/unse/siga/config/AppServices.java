package ar.edu.unse.siga.config;

// DAOs concretos (usa el paquete que realmente tengas en tu repo)
import ar.edu.unse.siga.persistence.jdbc.JdbcInsumoDao;
import ar.edu.unse.siga.persistence.jdbc.JdbcMovimientoDao;
import ar.edu.unse.siga.persistence.jdbc.JdbcTramiteDao;

import ar.edu.unse.siga.service.InventarioService;
import ar.edu.unse.siga.service.TramiteService;

public final class AppServices {

    private static InventarioService inventarioService;
    private static TramiteService tramiteService;

    private AppServices() {}

    public static void init() {
        // Tus DAOs tienen constructor sin argumentos según el error previo.
        var insumoDao = new JdbcInsumoDao();
        var movDao    = new JdbcMovimientoDao();
        var tramDao   = new JdbcTramiteDao();

        // InventarioService: inyectar solo InsumoDao por constructor...
        inventarioService = new InventarioService(insumoDao);
        // ...y luego el MovimientoDao por setter:
        inventarioService.setMovimientoDao(movDao);

        // TramiteService: asumo que su constructor recibe TramiteDao.
        // Si tu TramiteService usa otro patrón, pegame su clase y lo ajusto.
        tramiteService = new TramiteService(tramDao);
    }

    public static InventarioService inventario() {
        if (inventarioService == null)
            throw new IllegalStateException("AppServices.init() no fue llamado");
        return inventarioService;
    }

    public static TramiteService tramite() {
        if (tramiteService == null)
            throw new IllegalStateException("AppServices.init() no fue llamado");
        return tramiteService;
    }
}
