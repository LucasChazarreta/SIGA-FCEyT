package ar.edu.unse.siga.persistence;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import java.sql.Connection;
import java.sql.ResultSet;

@org.junit.jupiter.api.Disabled("Requiere DB local configurada; test manual")

class ConnectionLiveTest {

    @Test
    void canConnectAndUseSchema() throws Exception {
        try (Connection cn = DataSourceFactory.getConnection()) {
            assertNotNull(cn, "La conexión no debe ser null");
            try (var st = cn.createStatement();
                 ResultSet rs = st.executeQuery("SELECT DATABASE()")) {
                assertTrue(rs.next());
                String db = rs.getString(1);
                System.out.println("Conectado a DB = " + db);
                assertEquals("siga", db.toLowerCase(), "Debe estar usando el schema 'siga'");
            }
        }
    }
}

