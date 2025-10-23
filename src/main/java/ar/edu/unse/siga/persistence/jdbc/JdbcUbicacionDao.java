package ar.edu.unse.siga.persistence.jdbc;

import ar.edu.unse.siga.domain.Ubicacion;
import ar.edu.unse.siga.persistence.DataSourceFactory;
import ar.edu.unse.siga.persistence.dao.UbicacionDao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class JdbcUbicacionDao implements UbicacionDao {

    @Override
    public List<Ubicacion> listAll() {
        final String sql = "SELECT id, nombre FROM ubicacion ORDER BY nombre";
        try (Connection cn = DataSourceFactory.getConnection();
             PreparedStatement ps = cn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            List<Ubicacion> list = new ArrayList<>();
            while (rs.next()) {
                Ubicacion u = new Ubicacion();
                u.setId(rs.getInt("id"));
                u.setNombre(rs.getString("nombre"));
                list.add(u);
            }
            return list;
        } catch (SQLException e) {
            throw new RuntimeException("Error listando ubicaciones", e);
        }
    }
}
