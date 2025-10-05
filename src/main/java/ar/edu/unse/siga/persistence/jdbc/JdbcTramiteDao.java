package ar.edu.unse.siga.persistence.jdbc;

import ar.edu.unse.siga.domain.Tramite;
import ar.edu.unse.siga.persistence.DataSourceFactory;
import ar.edu.unse.siga.persistence.dao.TramiteDao;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class JdbcTramiteDao implements TramiteDao {

private Tramite mapRow(ResultSet rs) throws SQLException {
    Tramite t = new Tramite();
    t.setId(rs.getLong("id"));
    t.setNro(rs.getString("nro"));
    t.setAsunto(rs.getString("asunto"));
    t.setEstado(rs.getString("estado"));
    Timestamp ts = rs.getTimestamp("fecha");
    if (ts != null) t.setFecha(ts.toLocalDateTime());
    t.setSolicitante(rs.getString("solicitante"));

    // 🔹 NUEVO: leer la descripción si existe en la tabla
    try {
        t.setDescripcion(rs.getString("descripcion"));
    } catch (SQLException ignore) {
        // en caso de que la columna no exista (por compatibilidad)
    }

    return t;
}

@Override
public Long create(Tramite t) {
    // 🔹 Agregamos el campo descripcion
    String sql = "INSERT INTO tramite(nro, asunto, estado, fecha, solicitante, descripcion) VALUES (?,?,?,?,?,?)";
    try (Connection cn = DataSourceFactory.getConnection();
         PreparedStatement ps = cn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

        ps.setString(1, t.getNro());
        ps.setString(2, t.getAsunto());
        ps.setString(3, t.getEstado());
        ps.setTimestamp(4, Timestamp.valueOf(t.getFecha() != null ? t.getFecha() : LocalDateTime.now()));
        ps.setString(5, t.getSolicitante());
        ps.setString(6, t.getDescripcion()); // 🔹 NUEVO

        ps.executeUpdate();
        try (ResultSet rs = ps.getGeneratedKeys()) {
            if (rs.next()) {
                long id = rs.getLong(1);
                t.setId(id);
                return id;
            }
            throw new SQLException("No se obtuvo ID generado");
        }
    } catch (SQLException e) {
        throw new RuntimeException("Error creando trámite", e);
    }
}

    @Override
    public void updateEstado(Long id, String nuevoEstado) {
        String sql = "UPDATE tramite SET estado=? WHERE id=?";
        try (Connection cn = DataSourceFactory.getConnection();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setString(1, nuevoEstado);
            ps.setLong(2, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error cambiando estado de trámite id=" + id, e);
        }
    }

    @Override
    public Optional<Tramite> findByNro(String nro) {
        String sql = "SELECT * FROM tramite WHERE nro=?";
        try (Connection cn = DataSourceFactory.getConnection();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setString(1, nro);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(mapRow(rs));
                return Optional.empty();
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error buscando trámite nro=" + nro, e);
        }
    }

    @Override
    public List<Tramite> listAll() {
        String sql = "SELECT * FROM tramite ORDER BY fecha DESC";
        try (Connection cn = DataSourceFactory.getConnection();
             PreparedStatement ps = cn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            List<Tramite> list = new ArrayList<>();
            while (rs.next()) list.add(mapRow(rs));
            return list;
        } catch (SQLException e) {
            throw new RuntimeException("Error listando trámites", e);
        }
    }
}

