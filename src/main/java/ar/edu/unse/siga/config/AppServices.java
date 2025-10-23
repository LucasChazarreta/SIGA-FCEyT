package ar.edu.unse.siga.config;

import ar.edu.unse.siga.persistence.dao.CategoriaDao;
import ar.edu.unse.siga.persistence.dao.InsumoDao;
import ar.edu.unse.siga.persistence.dao.MovimientoDao;
import ar.edu.unse.siga.persistence.dao.TramiteDao;
import ar.edu.unse.siga.persistence.dao.UbicacionDao;
import ar.edu.unse.siga.persistence.jdbc.JdbcInsumoDao;
import ar.edu.unse.siga.persistence.jdbc.JdbcMovimientoDao;
import ar.edu.unse.siga.persistence.jdbc.JdbcCategoriaDao;
import ar.edu.unse.siga.persistence.jdbc.JdbcTramiteDao;
import ar.edu.unse.siga.persistence.jdbc.JdbcUbicacionDao;

import ar.edu.unse.siga.service.InventarioService;
import ar.edu.unse.siga.service.TramiteService;
public final class AppServices {

    private static InventarioService inventarioService;
    private static TramiteService tramiteService;
    

    private AppServices() {}

    public static void init() {
        // DAOs concretos
        InsumoDao insumoDao = new JdbcInsumoDao();
        MovimientoDao movDao = new JdbcMovimientoDao();
        CategoriaDao categoriaDao = new JdbcCategoriaDao();
        UbicacionDao ubicacionDao = new JdbcUbicacionDao();
        TramiteDao tramDao = new JdbcTramiteDao();


        // Services (constructor con las dependencias que usa)
        inventarioService = new InventarioService(insumoDao, movDao, categoriaDao, ubicacionDao);
        tramiteService    = new TramiteService(tramDao);
        
        
         
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
