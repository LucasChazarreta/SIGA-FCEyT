import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import ar.edu.unse.siga.persistence.jdbc.JdbcCategoriaDao;
import ar.edu.unse.siga.domain.Categoria;

class CategoriaDaoTest {

    @Test
    void testCreateAndFind() {
        JdbcCategoriaDao dao = new JdbcCategoriaDao();
        Categoria c = new Categoria("Herramientas");
        dao.create(c);

        assertTrue(dao.findByNombre("Herramientas").isPresent());
    }
}

