package ar.edu.unse.siga.config;

import ar.edu.unse.siga.persistence.DataSourceFactory;

// Inventario / Trámite (ya existen en tu proyecto)
import ar.edu.unse.siga.persistence.jdbc.JdbcInsumoDao;
import ar.edu.unse.siga.persistence.jdbc.JdbcMovimientoDao;
import ar.edu.unse.siga.persistence.jdbc.JdbcTramiteDao;
import ar.edu.unse.siga.service.InventarioService;
import ar.edu.unse.siga.service.TramiteService;

// Usuarios / Auth
import ar.edu.unse.siga.persistence.dao.UsuarioDao;
import ar.edu.unse.siga.persistence.jdbc.JdbcUsuarioDao;
import ar.edu.unse.siga.service.AuthService;

// Finanzas (nuevo)
import ar.edu.unse.siga.persistence.dao.FinanzaDao;
import ar.edu.unse.siga.persistence.jdbc.JdbcFinanzaDao;
import ar.edu.unse.siga.service.FinanzasService;

import javax.sql.DataSource;

public class AppServices {

    private static AppServices INSTANCE;

    private DataSource dataSource;

    private InventarioService inventarioService;
    private TramiteService tramiteService;

    private UsuarioDao usuarioDao;
    private AuthService authService;

    private FinanzaDao finanzaDao;
    private FinanzasService finanzasService;

    private AppServices() {}

    public static AppServices init() {
        if (INSTANCE == null) {
            INSTANCE = new AppServices();
            INSTANCE.bootstrap();
        }
        return INSTANCE;
    }

    private void bootstrap() {
        // Usa TU DataSourceFactory existente (paquete ar.edu.unse.siga.persistence)
        this.dataSource = DataSourceFactory.createDataSource();

        // Inventario
        var insumoDao = new JdbcInsumoDao();     // tus DAOs ya manejan la conexión internamente
        var movDao    = new JdbcMovimientoDao();
        this.inventarioService = new InventarioService(insumoDao);
        this.inventarioService.setMovimientoDao(movDao);

        // Trámites
        var tramDao = new JdbcTramiteDao();
        this.tramiteService = new TramiteService(tramDao);

        // Usuario/Auth
        this.usuarioDao = new JdbcUsuarioDao(dataSource);  // este sí recibe DataSource
        this.authService = new AuthService(usuarioDao);

        // Finanzas
        this.finanzaDao = new JdbcFinanzaDao(dataSource); // el que hicimos nuevo
        this.finanzasService = new FinanzasService(finanzaDao);
    }

    public static AppServices get() { return init(); }

    public DataSource getDataSource() { return dataSource; }

    public InventarioService inventario() { return inventarioService; }
    public TramiteService tramite() { return tramiteService; }

    public UsuarioDao getUsuarioDao() { return usuarioDao; }
    public AuthService getAuthService() { return authService; }

    public FinanzaDao getFinanzaDao() { return finanzaDao; }
    public FinanzasService getFinanzasService() { return finanzasService; }
}
