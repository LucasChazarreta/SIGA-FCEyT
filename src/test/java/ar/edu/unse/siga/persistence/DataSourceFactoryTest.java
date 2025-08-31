package ar.edu.unse.siga.persistence;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

class DataSourceFactoryTest {

    @Test
    void loadsPropertiesAndDriver() {
        // Verifica que lea el archivo y tenga claves básicas
        assertNotNull(DataSourceFactory.getConfig("db.url"));
        assertNotNull(DataSourceFactory.getConfig("db.user"));
        assertNotNull(DataSourceFactory.getConfig("db.password"));

        // Si llegó hasta acá, el bloque static cargó el driver sin lanzar excepción.
        assertTrue(true);
    }

    // Más adelante activamos este test cuando tengamos MySQL andando:
    // @Test
    // void canOpenConnection() throws Exception {
    //     try (var cn = DataSourceFactory.getConnection()) {
    //         assertNotNull(cn);
    //     }
    // }
}
