package ar.edu.unse.siga.service;

import ar.edu.unse.siga.persistence.DataSourceFactory;
import ar.edu.unse.siga.persistence.dao.InsumoDao;
import ar.edu.unse.siga.persistence.dao.MovimientoDao;
import ar.edu.unse.siga.persistence.dao.TramiteDao;
import ar.edu.unse.siga.persistence.dao.TramiteDetalleDao;
import ar.edu.unse.siga.persistence.jdbc.JdbcInsumoDao;
import ar.edu.unse.siga.persistence.jdbc.JdbcMovimientoDao;
import ar.edu.unse.siga.persistence.jdbc.JdbcTramiteDao;
import ar.edu.unse.siga.persistence.jdbc.JdbcTramiteDetalleDao;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TramiteServiceTest {

    private TramiteService service;

    @BeforeEach
    void setup() throws Exception {
        try (Connection cn = DataSourceFactory.getConnection(); Statement st = cn.createStatement()) {
            st.execute("DROP TABLE IF EXISTS tramite_detalle");
            st.execute("DROP TABLE IF EXISTS movimiento");
            st.execute("DROP TABLE IF EXISTS tramite");
            st.execute("DROP TABLE IF EXISTS insumo");
            st.execute("DROP TABLE IF EXISTS categoria");

            st.execute("CREATE TABLE categoria (id INT AUTO_INCREMENT PRIMARY KEY, nombre VARCHAR(100), activo TINYINT)");
            st.execute("CREATE TABLE insumo (" +
                    "id INT AUTO_INCREMENT PRIMARY KEY, " +
                    "codigo VARCHAR(64), " +
                    "descripcion VARCHAR(255), " +
                    "categoria_id INT, " +
                    "stock_minimo INT, " +
                    "ubicacion VARCHAR(100), " +
                    "estado VARCHAR(20), " +
                    "tipo VARCHAR(50), " +
                    "stock DECIMAL(12,3) DEFAULT 0, " +
                    "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                    "fecha_alta DATE, " +
                    "CONSTRAINT fk_cat FOREIGN KEY (categoria_id) REFERENCES categoria(id))");
            st.execute("CREATE TABLE tramite (" +
                    "id BIGINT AUTO_INCREMENT PRIMARY KEY, " +
                    "nro VARCHAR(30), " +
                    "asunto VARCHAR(150), " +
                    "estado VARCHAR(20), " +
                    "fecha TIMESTAMP, " +
                    "solicitante VARCHAR(100), " +
                    "descripcion VARCHAR(255), " +
                    "destino VARCHAR(150))");
            st.execute("CREATE TABLE tramite_detalle (" +
                    "id BIGINT AUTO_INCREMENT PRIMARY KEY, " +
                    "tramite_id BIGINT, " +
                    "insumo_id INT, " +
                    "cantidad DECIMAL(12,3), " +
                    "CONSTRAINT fk_td_tram FOREIGN KEY (tramite_id) REFERENCES tramite(id), " +
                    "CONSTRAINT fk_td_ins FOREIGN KEY (insumo_id) REFERENCES insumo(id))");
            st.execute("CREATE TABLE movimiento (" +
                    "id BIGINT AUTO_INCREMENT PRIMARY KEY, " +
                    "insumo_id INT, " +
                    "tipo VARCHAR(20), " +
                    "cantidad DECIMAL(12,3), " +
                    "destino_fuente VARCHAR(150), " +
                    "solicitante VARCHAR(100), " +
                    "fecha TIMESTAMP, " +
                    "usuario_id INT, " +
                    "tramite_id BIGINT)");

            st.execute("INSERT INTO categoria(id, nombre, activo) VALUES (1, 'General', 1)");
            st.execute("INSERT INTO insumo(id, codigo, descripcion, categoria_id, stock_minimo, ubicacion, estado, tipo, stock) " +
                    "VALUES (1, 'A', 'Insumo A', 1, 0, 'Depósito', 'ACTIVO', 'INSUMO', 10)");
            st.execute("INSERT INTO insumo(id, codigo, descripcion, categoria_id, stock_minimo, ubicacion, estado, tipo, stock) " +
                    "VALUES (2, 'B', 'Insumo B', 1, 0, 'Depósito', 'ACTIVO', 'INSUMO', 5)");
            st.execute("INSERT INTO movimiento(insumo_id, tipo, cantidad, fecha) VALUES (1, 'ENTRADA', 10, CURRENT_TIMESTAMP())");
            st.execute("INSERT INTO movimiento(insumo_id, tipo, cantidad, fecha) VALUES (2, 'ENTRADA', 5, CURRENT_TIMESTAMP())");
        }

        TramiteDao tramiteDao = new JdbcTramiteDao();
        TramiteDetalleDao detalleDao = new JdbcTramiteDetalleDao();
        MovimientoDao movimientoDao = new JdbcMovimientoDao();
        InsumoDao insumoDao = new JdbcInsumoDao();
        service = new TramiteService(tramiteDao, detalleDao, movimientoDao, insumoDao);
    }

    @Test
    void registrarTramiteConMultiplesInsumos() throws Exception {
        Long id = service.registrarNuevoTramite(
                "Pedido de insumos de laboratorio",
                "Informes",
                "Reposición de materiales para prácticas",
                "Laboratorio Central",
                List.of(
                new TramiteService.LineaTramite(1, 3),
                new TramiteService.LineaTramite(2, 2)
        ));

        assertNotNull(id);

        try (Connection cn = DataSourceFactory.getConnection()) {
            try (PreparedStatement ps = cn.prepareStatement("SELECT solicitante FROM tramite WHERE id=?")) {
                ps.setLong(1, id);
                try (ResultSet rs = ps.executeQuery()) {
                    assertTrue(rs.next());
                    assertEquals("Informes", rs.getString(1));
                }
            }

            try (PreparedStatement ps = cn.prepareStatement("SELECT COUNT(*) FROM tramite_detalle WHERE tramite_id=?")) {
                ps.setLong(1, id);
                try (ResultSet rs = ps.executeQuery()) {
                    assertTrue(rs.next());
                    assertEquals(2, rs.getInt(1));
                }
            }

            try (PreparedStatement ps = cn.prepareStatement("SELECT cantidad FROM movimiento WHERE tramite_id=? ORDER BY insumo_id")) {
                ps.setLong(1, id);
                try (ResultSet rs = ps.executeQuery()) {
                    assertTrue(rs.next());
                    assertEquals(3, rs.getBigDecimal(1).intValue());
                    assertTrue(rs.next());
                    assertEquals(2, rs.getBigDecimal(1).intValue());
                }
            }

            try (Statement st = cn.createStatement(); ResultSet rs = st.executeQuery("SELECT stock FROM insumo WHERE id = 1")) {
                assertTrue(rs.next());
                assertEquals(7, rs.getBigDecimal(1).intValue());
            }
        }
    }

    @Test
    void rollbackPorStockInsuficiente() {
        assertThrows(IllegalStateException.class, () ->
                service.registrarNuevoTramite(
                        "Solicitud extraordinaria",
                        "Informes",
                        "Pedido que excede el stock disponible",
                        "Depósito",
                        List.of(new TramiteService.LineaTramite(2, 6))));

        try (Connection cn = DataSourceFactory.getConnection(); Statement st = cn.createStatement()) {
            try (ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM tramite")) {
                assertTrue(rs.next());
                assertEquals(0, rs.getInt(1));
            }
        } catch (Exception e) {
            fail(e);
        }
    }

    @Test
    void unificaLineasDuplicadas() throws Exception {
        Long id = service.registrarNuevoTramite(
                "Pedido de reposición",
                "Informes",
                "Unificación de pedidos repetidos",
                "Laboratorio Central",
                List.of(
                new TramiteService.LineaTramite(1, 2),
                new TramiteService.LineaTramite(1, 3)
        ));

        try (Connection cn = DataSourceFactory.getConnection()) {
            try (PreparedStatement ps = cn.prepareStatement("SELECT COUNT(*) FROM tramite_detalle WHERE tramite_id=?")) {
                ps.setLong(1, id);
                try (ResultSet rs = ps.executeQuery()) {
                    assertTrue(rs.next());
                    assertEquals(1, rs.getInt(1));
                }
            }

            try (PreparedStatement ps = cn.prepareStatement("SELECT cantidad FROM movimiento WHERE tramite_id=?")) {
                ps.setLong(1, id);
                try (ResultSet rs = ps.executeQuery()) {
                    assertTrue(rs.next());
                    assertEquals(5, rs.getBigDecimal(1).intValue());
                }
            }
        }
    }
}
