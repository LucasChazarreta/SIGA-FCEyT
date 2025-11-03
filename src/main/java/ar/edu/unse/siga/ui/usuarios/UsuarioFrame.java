package ar.edu.unse.siga.ui.usuarios;

import ar.edu.unse.siga.common.PasswordUtil;
import ar.edu.unse.siga.common.RoleName;
import ar.edu.unse.siga.domain.Rol;
import ar.edu.unse.siga.domain.Usuario;
import ar.edu.unse.siga.persistence.DataSourceFactory;
import ar.edu.unse.siga.service.AuthService;
import ar.edu.unse.siga.ui.base.BaseCrudFrame;
import ar.edu.unse.siga.ui.base.CrudTableModel;
import ar.edu.unse.siga.ui.base.Ui;

import javax.swing.*;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class UsuarioFrame extends BaseCrudFrame<Usuario> {
    private final AuthService authService;
    private final UsuarioTableModel model = new UsuarioTableModel();

    public UsuarioFrame(AuthService authService) {
        super("ABM Usuarios");
        this.authService = authService;
        table.setModel(model);
        loadData();
    }

    @Override protected void loadData() {
        List<Usuario> list = new ArrayList<>();
        String sql = "SELECT u.id, u.username, u.password, u.email, u.estado, r.id rol_id, r.nombre rol_nombre " +
                "FROM usuario u JOIN rol r ON r.id=u.rol_id ORDER BY u.username";
        try (var cn = DataSourceFactory.getConnection();
             var ps = cn.prepareStatement(sql);
             var rs = ps.executeQuery()) {
            while (rs.next()) {
                var u = new Usuario();
                u.setId(rs.getLong("id"));
                u.setUsername(rs.getString("username"));
                u.setPassword(rs.getString("password"));
                u.setEmail(rs.getString("email"));
                u.setEstado(rs.getString("estado"));
                var r = new Rol();
                r.setId(rs.getInt("rol_id")); r.setNombre(rs.getString("rol_nombre"));
                u.setRol(r);
                list.add(u);
            }
            model.setData(list);
        } catch (Exception e){ Ui.error(this,e); }
    }

    @Override protected void onNuevo() {
        var dlg = new UsuarioFormDialog(SwingUtilities.getWindowAncestor(this));
        dlg.setVisible(true);
        if (dlg.isAccepted()) {
            try (var cn = DataSourceFactory.getConnection();
                 var ps = cn.prepareStatement(
                         "INSERT INTO usuario(username,password,password_hash,email,estado,rol_id) VALUES (?,?,?,?,?,?)")) {
                ps.setString(1, dlg.getUsername());
                ps.setString(2, dlg.getPassword());
                ps.setString(3, PasswordUtil.hash(dlg.getPassword()));
                ps.setString(4, dlg.getEmail());
                ps.setString(5, "ACTIVO");
                int rolId = findRolId(dlg.getRol());
                ps.setInt(6, rolId);
                ps.executeUpdate();
                loadData();
            } catch (Exception e){ Ui.error(this,e); }
        }
    }

    @Override protected void onEditar() {
        Ui.info(this, "Edición no implementada. Use cambio de contraseña / estado en una futura iteración.");
    }

    @Override protected void onBaja() {
        int r = selectedRowOrWarn(); if (r<0) return;
        var sel = model.getAt(r);
        if (Ui.confirm(this, "¿Marcar INACTIVO al usuario " + sel.getUsername() + "?")) {
            try (var cn = DataSourceFactory.getConnection();
                 var ps = cn.prepareStatement("UPDATE usuario SET estado='INACTIVO', activo=0 WHERE id=?")) {
                ps.setLong(1, sel.getId());
                ps.executeUpdate(); loadData();
            } catch (Exception e){ Ui.error(this,e); }
        }
    }

    private int findRolId(String roleName) throws SQLException {
        String sql = "SELECT id FROM rol WHERE UPPER(nombre)=?";
        try (var cn = DataSourceFactory.getConnection();
             var ps = cn.prepareStatement(sql)) {
            ps.setString(1, roleName == null ? RoleName.ADMINISTRATIVO : roleName.toUpperCase());
            try (var rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        }
        throw new SQLException("Rol no encontrado: " + roleName);
    }

    static class UsuarioTableModel extends CrudTableModel<Usuario> {
        UsuarioTableModel(){ super(new String[]{"ID","Usuario","Contraseña","Email","Estado","Rol"}); }
        @Override public Object getValueAt(int row, int col) {
            var u = data.get(row);
            return switch (col) {
                case 0 -> u.getId();
                case 1 -> u.getUsername();
                case 2 -> u.getPassword() == null ? "" : u.getPassword();
                case 3 -> u.getEmail();
                case 4 -> u.getEstado();
                case 5 -> u.getRol()!=null? u.getRol().getNombre():"-";
                default -> "";
            };
        }
    }
}
