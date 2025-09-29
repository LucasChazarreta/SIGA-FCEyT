package ar.edu.unse.siga.persistence.jdbc;

import ar.edu.unse.siga.domain.FinanzaMovimiento;
import ar.edu.unse.siga.domain.FinanzaTipo;
import ar.edu.unse.siga.persistence.dao.FinanzaDao;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDate;
import java.util.*;

public class JdbcFinanzaDao implements FinanzaDao {

    private final DataSource ds;

    public JdbcFinanzaDao(DataSource ds) {
        this.ds = ds;
    }

    @Override
    public Long insert(FinanzaMovimiento mov) {
        String sql = "INSERT INTO finanza_mov (tipo, categoria, monto, fecha, referencia) VALUES (?,?,?,?,?)";
        try (Connection cn = ds.getConnection();
             PreparedStatement ps = cn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, mov.getTipo().name());
            ps.setString(2, mov.getCategoria());
            ps.setBigDecimal(3, mov.getMonto());
            ps.setDate(4, java.sql.Date.valueOf(mov.getFecha()));
            ps.setString(5, mov.getReferencia());
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) return rs.getLong(1);
            }
            return null;
        } catch (SQLException e) {
            throw new RuntimeException("Error insertando finanza_mov", e);
        }
    }

    @Override
    public boolean update(FinanzaMovimiento mov) {
        String sql = "UPDATE finanza_mov SET tipo=?, categoria=?, monto=?, fecha=?, referencia=? WHERE id=?";
        try (Connection cn = ds.getConnection();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setString(1, mov.getTipo().name());
            ps.setString(2, mov.getCategoria());
            ps.setBigDecimal(3, mov.getMonto());
            ps.setDate(4, java.sql.Date.valueOf(mov.getFecha()));
            ps.setString(5, mov.getReferencia());
            ps.setLong(6, mov.getId());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new RuntimeException("Error actualizando finanza_mov", e);
        }
    }

    @Override
    public boolean delete(Long id) {
        String sql = "DELETE FROM finanza_mov WHERE id=?";
        try (Connection cn = ds.getConnection();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setLong(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new RuntimeException("Error eliminando finanza_mov", e);
        }
    }

    @Override
    public Optional<FinanzaMovimiento> findById(Long id) {
        String sql = "SELECT id, tipo, categoria, monto, fecha, referencia FROM finanza_mov WHERE id=?";
        try (Connection cn = ds.getConnection();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(map(rs));
                return Optional.empty();
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error consultando finanza_mov", e);
        }
    }

    @Override
    public List<FinanzaMovimiento> search(LocalDate desde, LocalDate hasta, FinanzaTipo tipo, String categoriaLike) {
        StringBuilder sb = new StringBuilder(
            "SELECT id, tipo, categoria, monto, fecha, referencia FROM finanza_mov WHERE 1=1");
        List<Object> params = new ArrayList<>();
        if (desde != null) { sb.append(" AND fecha >= ?"); params.add(java.sql.Date.valueOf(desde)); }
        if (hasta != null) { sb.append(" AND fecha <= ?"); params.add(java.sql.Date.valueOf(hasta)); }
        if (tipo != null)  { sb.append(" AND tipo = ?"); params.add(tipo.name()); }
        if (categoriaLike != null && !categoriaLike.isBlank()) {
            sb.append(" AND categoria LIKE ?");
            params.add("%" + categoriaLike + "%");
        }
        sb.append(" ORDER BY fecha DESC, id DESC");

        try (Connection cn = ds.getConnection();
             PreparedStatement ps = cn.prepareStatement(sb.toString())) {
            for (int i = 0; i < params.size(); i++) ps.setObject(i + 1, params.get(i));
            try (ResultSet rs = ps.executeQuery()) {
                List<FinanzaMovimiento> out = new ArrayList<>();
                while (rs.next()) out.add(map(rs));
                return out;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error buscando finanza_mov", e);
        }
    }

    @Override
    public BigDecimal totalIngresos(LocalDate desde, LocalDate hasta) {
        return totalByTipo(desde, hasta, FinanzaTipo.INGRESO);
    }

    @Override
    public BigDecimal totalEgresos(LocalDate desde, LocalDate hasta) {
        return totalByTipo(desde, hasta, FinanzaTipo.EGRESO);
    }

    private BigDecimal totalByTipo(LocalDate desde, LocalDate hasta, FinanzaTipo tipo) {
        String sql = "SELECT COALESCE(SUM(monto),0) FROM finanza_mov WHERE tipo=?"
                   + (desde != null ? " AND fecha>=?" : "")
                   + (hasta != null ? " AND fecha<=?" : "");
        try (Connection cn = ds.getConnection();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            int idx = 1;
            ps.setString(idx++, tipo.name());
            if (desde != null) ps.setDate(idx++, java.sql.Date.valueOf(desde));
            if (hasta != null) ps.setDate(idx, java.sql.Date.valueOf(hasta));
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getBigDecimal(1);
                return BigDecimal.ZERO;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error calculando total por tipo", e);
        }
    }

    @Override
    public Map<String, BigDecimal> totalPorCategoria(LocalDate desde, LocalDate hasta, FinanzaTipo tipo) {
        StringBuilder sb = new StringBuilder("SELECT categoria, COALESCE(SUM(monto),0) total FROM finanza_mov WHERE 1=1 ");
        List<Object> params = new ArrayList<>();
        if (tipo != null) { sb.append(" AND tipo=?"); params.add(tipo.name()); }
        if (desde != null){ sb.append(" AND fecha>=?"); params.add(java.sql.Date.valueOf(desde)); }
        if (hasta != null){ sb.append(" AND fecha<=?"); params.add(java.sql.Date.valueOf(hasta)); }
        sb.append(" GROUP BY categoria ORDER BY total DESC");

        try (Connection cn = ds.getConnection();
             PreparedStatement ps = cn.prepareStatement(sb.toString())) {
            for (int i=0;i<params.size();i++) ps.setObject(i+1, params.get(i));
            Map<String, BigDecimal> out = new LinkedHashMap<>();
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) out.put(rs.getString(1), rs.getBigDecimal(2));
            }
            return out;
        } catch (SQLException e) {
            throw new RuntimeException("Error total por categoria", e);
        }
    }

    private FinanzaMovimiento map(ResultSet rs) throws SQLException {
        FinanzaMovimiento m = new FinanzaMovimiento();
        m.setId(rs.getLong("id"));
        m.setTipo(FinanzaTipo.valueOf(rs.getString("tipo")));
        m.setCategoria(rs.getString("categoria"));
        m.setMonto(rs.getBigDecimal("monto"));
        m.setFecha(rs.getDate("fecha").toLocalDate());
        m.setReferencia(rs.getString("referencia"));
        return m;
    }
}
