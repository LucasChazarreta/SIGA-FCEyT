package ar.edu.unse.siga.persistence.jdbc;

import ar.edu.unse.siga.domain.Categoria;
import ar.edu.unse.siga.persistence.dao.CategoriaDao;
import org.junit.jupiter.api.*;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class JdbcCategoriaDaoTest {
    private CategoriaDao dao;

    @BeforeEach
    void setup() {
        dao = new JdbcCategoriaDao();
    }

    @Test
    void testCreateAndFind() {
        Categoria nueva = new Categoria("Prueba");
        Categoria creada = dao.create(nueva);

        assertTrue(creada.getId() > 0);
        assertEquals("Prueba", creada.getNombre());

        var encontrada = dao.findById(creada.getId());
        assertTrue(encontrada.isPresent());
        assertEquals("Prueba", encontrada.get().getNombre());
    }

    @Test
    void testFindAll() {
        List<Categoria> lista = dao.findAll();
        assertNotNull(lista);
        assertFalse(lista.isEmpty());
    }
}
