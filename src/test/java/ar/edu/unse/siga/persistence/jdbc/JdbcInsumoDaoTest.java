package ar.edu.unse.siga.persistence.jdbc;

import ar.edu.unse.siga.domain.Categoria;
import ar.edu.unse.siga.domain.Insumo;
import ar.edu.unse.siga.persistence.DataSourceFactory;
import ar.edu.unse.siga.persistence.dao.InsumoDao;
import org.junit.jupiter.api.*;

import java.sql.ResultSet;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class JdbcInsumoDaoTest {

    private final InsumoDao dao = new JdbcInsumoDao();

    // Busca un id de categoria existente, o crea una por si acaso
    private int ensureCategoriaId() throws Exception {
        try (var cn = DataSourceFactory.getConnection();
             var st = cn.createStatement()) {
            try (ResultSet rs = st.executeQuery("SELECT id FROM categoria ORDER BY id LIMIT 1")) {
                if (rs.next()) return rs.getInt(1);
            }
            // Si no había, creamos una default
            st.executeUpdate("INSERT INTO categoria(nombre) VALUES ('Temporal')");
            try (ResultSet rs2 = st.executeQuery("SELECT id FROM categoria WHERE nombre='Temporal'")) {
                rs2.next();
                return rs2.getInt(1);
            }
        }
    }

    private Insumo nuevoInsumo(String codigo, int categoriaId) {
        Insumo i = new Insumo();
        i.setCodigo(codigo);
        i.setDescripcion("Resma A4 80gr");
        Categoria c = new Categoria();
        c.setId(categoriaId);
        i.setCategoria(c);
        i.setStockMinimo(10);
        i.setUbicacion("Depósito 1");
        i.setEstado("ACTIVO");
        return i;
    }

    @Test @Order(1)
    void createAndFindByCodigo() throws Exception {
        int catId = ensureCategoriaId();
        String cod = "A4-001";
        dao.findByCodigo(cod).ifPresent(i -> dao.softDelete(i.getId())); // limpieza
        Long id = dao.create(nuevoInsumo(cod, catId));
        assertNotNull(id);

        Optional<Insumo> opt = dao.findByCodigo(cod);
        assertTrue(opt.isPresent());
        assertEquals(cod, opt.get().getCodigo());
        assertEquals("ACTIVO", opt.get().getEstado());
    }

    @Test @Order(2)
    void update() throws Exception {
        String cod = "A4-001";
        Insumo i = dao.findByCodigo(cod).orElseThrow();
        i.setDescripcion("Resma A4 80gr premium");
        dao.update(i);

        Insumo j = dao.findByCodigo(cod).orElseThrow();
        assertEquals("Resma A4 80gr premium", j.getDescripcion());
    }

    @Test @Order(3)
    void softDelete() throws Exception {
        String cod = "A4-001";
        Insumo i = dao.findByCodigo(cod).orElseThrow();
        dao.softDelete(i.getId());

        Insumo j = dao.findByCodigo(cod).orElseThrow();
        assertEquals("INACTIVO", j.getEstado());
    }
}
