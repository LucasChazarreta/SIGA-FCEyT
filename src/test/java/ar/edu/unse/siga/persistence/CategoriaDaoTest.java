
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import ar.edu.unse.siga.persistence.jdbc.JdbcCategoriaDao;
import ar.edu.unse.siga.domain.Categoria;

class CategoriaDaoTest {

//    @Test
//    void testCreateAndFind() {
//        JdbcCategoriaDao dao = new JdbcCategoriaDao();
//        Categoria c = new Categoria("Herramientas");
//        dao.create(c);
//
//        assertTrue(dao.findByNombre("Herramientas").isPresent());
//    }

    @Test
    void updateAndDeleteGuard() {
        var dao = new JdbcCategoriaDao();
        var c = new Categoria("Temporal_X");
        dao.create(c);

        c.setNombre("Temporal_Y");
        dao.update(c);
        assertTrue(dao.findByNombre("Temporal_Y").isPresent());

        // Borrado: debería ser true si no está usada por ningún insumo
        boolean deleted = dao.deleteIfOrphan(c.getId());
        assertTrue(deleted, "Si falla, hay un insumo apuntando a esta categoría");
    }

}
