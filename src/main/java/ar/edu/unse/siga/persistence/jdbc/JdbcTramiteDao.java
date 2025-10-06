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
        if (ts != null) {
            t.setFecha(ts.toLocalDateTime());
        }

        t.setSolicitante(rs.getString("solicitante"));
        t.setDescripcion(rs.getString("descripcion"));
        t.setDestino(rs.getString("destino"));
        return t;
    }

    @Override
    public Long create(Tramite t) {
        final String sql = """
            INSERT INTO tramite (nro, asunto, estado, fecha, solicitante, descripcion, destino)
            VALUES (?,?,?,?,?,?,?)
            """;
        try (Connection cn = DataSourceFactory.getConnection();
             PreparedStatement ps = cn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, t.getNro());
            ps.setString(2, t.getAsunto());
            ps.setString(3, t.getEstado());
            ps.setTimestamp(4, Timestamp.valueOf(t.getFecha() != null ? t.getFecha() : LocalDateTime.now()));
            ps.setString(5, t.getSolicitante());
            ps.setString(6, t.getDescripcion()); // puede ser null
            ps.setString(7, t.getDestino());     // puede ser null según esquema

            ps.executeUpdate();

            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    long id = rs.getLong(1);
                    t.setId(id);
                    return id;
                }
            }
            throw new SQLException("No se pudo obtener el ID generado para trámite");
        } catch (SQLException e) {
            throw new RuntimeException("Error insertando trámite", e);
        }
    }

    @Override
    public void updateEstado(Long id, String nuevoEstado) {
        final String sql = "UPDATE tramite SET estado=? WHERE id=?";
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
    public void updateEstadoByNro(String nro, String nuevoEstado) {
        final String sql = "UPDATE tramite SET estado=? WHERE nro=?";
        try (Connection cn = DataSourceFactory.getConnection();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setString(1, nuevoEstado);
            ps.setString(2, nro);
            int updated = ps.executeUpdate();
            if (updated == 0) {
                throw new SQLException("No existe trámite con nro=" + nro);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error actualizando estado por nro=" + nro, e);
        }
    }

    @Override
    public Optional<Tramite> findByNro(String nro) {
        final String sql = """
            SELECT id, nro, asunto, estado, fecha, solicitante, descripcion, destino
            FROM tramite
            WHERE nro=?
            """;
        try (Connection cn = DataSourceFactory.getConnection();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setString(1, nro);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
                return Optional.empty();
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error buscando trámite nro=" + nro, e);
        }
    }

    @Override
    public List<Tramite> listAll() {
        final String sql = """
            SELECT id, nro, asunto, estado, fecha, solicitante, descripcion, destino
            FROM tramite
            ORDER BY fecha DESC
            """;
        try (Connection cn = DataSourceFactory.getConnection();
             PreparedStatement ps = cn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            List<Tramite> list = new ArrayList<>();
            while (rs.next()) {
                list.add(mapRow(rs));
            }
            return list;
        } catch (SQLException e) {
            throw new RuntimeException("Error listando trámites", e);
        }
    }

    @Override
    public List<Tramite> listActivos() {
        final String sql = """
            SELECT id, nro, asunto, estado, fecha, solicitante, descripcion, destino
            FROM tramite
            WHERE estado IN ('PENDIENTE','EN_PROCESO','NUEVO')
            ORDER BY fecha DESC
            """;
        try (Connection cn = DataSourceFactory.getConnection();
             PreparedStatement ps = cn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            List<Tramite> list = new ArrayList<>();
            while (rs.next()) {
                list.add(mapRow(rs));
            }
            return list;
        } catch (SQLException e) {
            throw new RuntimeException("Error listando trámites activos", e);
        }
    }

    /** NUEVO: últimos N trámites por fecha desc (para “Trámites recientes”). */
    @Override
    public List<Tramite> listRecientes(int limit) {
        final String sql = """
            SELECT id, nro, asunto, estado, fecha, solicitante, descripcion, destino
            FROM tramite
            ORDER BY fecha DESC
            LIMIT ?
            """;
        List<Tramite> list = new ArrayList<>();
        try (Connection cn = DataSourceFactory.getConnection();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setInt(1, Math.max(1, limit));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
            }
            return list;
        } catch (SQLException e) {
            throw new RuntimeException("Error listando trámites recientes", e);
        }
    }
}
