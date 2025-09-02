package ar.edu.unse.siga.persistence.jdbc;

import ar.edu.unse.siga.domain.Categoria;
import ar.edu.unse.siga.domain.Insumo;
import ar.edu.unse.siga.persistence.DataSourceFactory;
import ar.edu.unse.siga.persistence.dao.InsumoDao;
import org.junit.jupiter.api.*;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class JdbcInsumoDaoTest {

    private final InsumoDao dao = new JdbcInsumoDao();

    // 👉 Código ÚNICO por corrida (evita choque con UNIQUE)
    private static final String COD = "A4-001-" + System.currentTimeMillis();

    private Insumo nuevoInsumo(String codigo) {
        Insumo i = new Insumo();
        i.setCodigo(codigo);
        i.setDescripcion("Resma A4 80gr");
        Categoria c = new Categoria();
        c.setId(1); // asumimos que existe una categoría con id=1 (semilla)
        i.setCategoria(c);
        i.setStockMinimo(10);
        i.setUbicacion("Depósito 1");
        i.setEstado("ACTIVO");
        return i;
    }

    // 👉 Limpieza dura antes de empezar (por si quedó algo de otra corrida)
    @BeforeAll
    static void cleanup() throws Exception {
        try (Connection cn = DataSourceFactory.getConnection();
             PreparedStatement ps = cn.prepareStatement("DELETE FROM insumo WHERE codigo = ?")) {
            ps.setString(1, COD);
            ps.executeUpdate();
        }
    }

    @Test @Order(1)
    void createAndFindByCodigo() {
        Long id = dao.create(nuevoInsumo(COD));
        assertNotNull(id);

        Optional<Insumo> opt = dao.findByCodigo(COD);
        assertTrue(opt.isPresent());
        assertEquals(COD, opt.get().getCodigo());
        assertEquals("ACTIVO", opt.get().getEstado());
    }

    @Test @Order(2)
    void update() {
        Insumo i = dao.findByCodigo(COD).orElseThrow();
        i.setDescripcion("Resma A4 80gr premium");
        dao.update(i);

        Insumo j = dao.findByCodigo(COD).orElseThrow();
        assertEquals("Resma A4 80gr premium", j.getDescripcion());
    }

    @Test @Order(3)
    void softDelete() {
        Insumo i = dao.findByCodigo(COD).orElseThrow();
        dao.softDelete(i.getId());

        Insumo j = dao.findByCodigo(COD).orElseThrow();
        assertEquals("INACTIVO", j.getEstado());
    }
}

