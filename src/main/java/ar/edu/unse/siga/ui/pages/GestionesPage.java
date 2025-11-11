package ar.edu.unse.siga.ui.pages;

import ar.edu.unse.siga.common.PasswordUtil;
import ar.edu.unse.siga.common.RoleName;
import ar.edu.unse.siga.domain.Categoria;
import ar.edu.unse.siga.domain.Rol;
import ar.edu.unse.siga.domain.Ubicacion;
import ar.edu.unse.siga.domain.Usuario;
import ar.edu.unse.siga.persistence.DataSourceFactory;
import ar.edu.unse.siga.service.InventarioService;
import ar.edu.unse.siga.ui.base.CardPanel;
import ar.edu.unse.siga.ui.base.CrudTableModel;
import ar.edu.unse.siga.ui.base.Ui;
import ar.edu.unse.siga.ui.usuarios.UsuarioFormDialog;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.table.JTableHeader;
import java.awt.*;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class GestionesPage extends JPanel {

    private final InventarioService inventarioService;

    private final CategoriaTableModel categoriaModel = new CategoriaTableModel();
    private final JTable tblCategorias = new JTable(categoriaModel);

    private final UbicacionTableModel ubicacionModel = new UbicacionTableModel();
    private final JTable tblUbicaciones = new JTable(ubicacionModel);

    private final UsuarioTableModel usuarioModel = new UsuarioTableModel();
    private final JTable tblUsuarios = new JTable(usuarioModel);

    private static final Color BRAND = new Color(58, 96, 224);
    private static final Color HEADER_TEXT = new Color(35, 55, 120);

    public GestionesPage(InventarioService inventarioService) {
        this.inventarioService = inventarioService;

        setOpaque(false);
        setLayout(new BorderLayout(20, 20));
        setBorder(new EmptyBorder(12, 12, 12, 12));

        add(buildHeader(), BorderLayout.NORTH);
        add(buildContent(), BorderLayout.CENTER);

        loadCategorias();
        loadUbicaciones();
        loadUsuarios();
    }

    private JComponent buildHeader() {
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);

        JPanel text = new JPanel();
        text.setOpaque(false);
        text.setLayout(new BoxLayout(text, BoxLayout.Y_AXIS));

        JLabel title = new JLabel("Gestiones administrativas");
        title.setFont(title.getFont().deriveFont(Font.BOLD, 32f));
        title.setForeground(HEADER_TEXT);

        JLabel subtitle = new JLabel("Administra catálogos y usuarios de apoyo del sistema");
        subtitle.setFont(subtitle.getFont().deriveFont(Font.PLAIN, 14f));
        subtitle.setForeground(new Color(110, 126, 165));

        text.add(title);
        text.add(Box.createVerticalStrut(4));
        text.add(subtitle);

        header.add(text, BorderLayout.WEST);
        return header;
    }

    private JComponent buildContent() {
        CardPanel container = new CardPanel();
        container.setLayout(new BorderLayout());
        container.setOpaque(false);

        JTabbedPane tabs = new JTabbedPane();
        tabs.setFont(tabs.getFont().deriveFont(Font.BOLD, 14f));
        tabs.setForeground(HEADER_TEXT);
        tabs.setOpaque(false);
        tabs.setBackground(Color.WHITE);
        tabs.setBorder(new EmptyBorder(12, 12, 12, 12));

        tabs.addTab("Categorías", buildCategoriasTab());
        tabs.addTab("Ubicaciones", buildUbicacionesTab());
        tabs.addTab("Usuarios", buildUsuariosTab());

        container.add(tabs, BorderLayout.CENTER);
        return container;
    }

    private JComponent buildCategoriasTab() {
        CardPanel panel = new CardPanel();
        panel.setLayout(new BorderLayout(18, 18));
        panel.setOpaque(false);
        panel.setBorder(new EmptyBorder(18, 18, 18, 18));

        panel.add(sectionHeader("Catálogo de categorías", "Organiza los grupos disponibles para los insumos."), BorderLayout.NORTH);

        styleTable(tblCategorias);
        tblCategorias.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        JScrollPane scroll = tableScroll(tblCategorias);
        panel.add(scroll, BorderLayout.CENTER);

        JButton btnNueva = primaryButton("Nueva");
        JButton btnRenombrar = outlineButton("Renombrar");
        JButton btnBaja = outlineButton("Baja lógica");
        JButton btnRestaurar = outlineButton("Restaurar");

        JPanel buttons = actionsRow(btnNueva, btnRenombrar, btnBaja, btnRestaurar);

        btnNueva.addActionListener(e -> onNuevaCategoria());
        btnRenombrar.addActionListener(e -> onRenombrarCategoria());
        btnBaja.addActionListener(e -> onBajaCategoria());
        btnRestaurar.addActionListener(e -> onRestaurarCategoria());

        panel.add(buttons, BorderLayout.SOUTH);

        return panel;
    }

    private JComponent buildUbicacionesTab() {
        CardPanel panel = new CardPanel();
        panel.setLayout(new BorderLayout(18, 18));
        panel.setOpaque(false);
        panel.setBorder(new EmptyBorder(18, 18, 18, 18));

        panel.add(sectionHeader("Ubicaciones", "Gestioná los espacios físicos de almacenamiento."), BorderLayout.NORTH);

        styleTable(tblUbicaciones);
        tblUbicaciones.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        JScrollPane scroll = tableScroll(tblUbicaciones);
        panel.add(scroll, BorderLayout.CENTER);

        JButton btnNueva = primaryButton("Nueva");
        JButton btnRenombrar = outlineButton("Renombrar");
        JButton btnBaja = outlineButton("Baja lógica");
        JButton btnRestaurar = outlineButton("Restaurar");

        JPanel buttons = actionsRow(btnNueva, btnRenombrar, btnBaja, btnRestaurar);

        btnNueva.addActionListener(e -> onNuevaUbicacion());
        btnRenombrar.addActionListener(e -> onRenombrarUbicacion());
        btnBaja.addActionListener(e -> onBajaUbicacion());
        btnRestaurar.addActionListener(e -> onRestaurarUbicacion());

        panel.add(buttons, BorderLayout.SOUTH);

        return panel;
    }

    private JComponent buildUsuariosTab() {
        CardPanel panel = new CardPanel();
        panel.setLayout(new BorderLayout(18, 18));
        panel.setOpaque(false);
        panel.setBorder(new EmptyBorder(18, 18, 18, 18));

        panel.add(sectionHeader("Usuarios administrativos", "Gestioná los accesos del personal"), BorderLayout.NORTH);

        styleTable(tblUsuarios);
        tblUsuarios.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        JScrollPane scroll = tableScroll(tblUsuarios);
        panel.add(scroll, BorderLayout.CENTER);

        JButton btnNuevo = primaryButton("Nuevo administrativo");
        JButton btnBaja = outlineButton("Dar de baja");
        JButton btnRestaurar = outlineButton("Restaurar");

        JPanel buttons = actionsRow(btnNuevo, btnBaja, btnRestaurar);

        btnNuevo.addActionListener(e -> onNuevoAdministrativo());
        btnBaja.addActionListener(e -> onBajaUsuario());
        btnRestaurar.addActionListener(e -> onRestaurarUsuario());

        panel.add(buttons, BorderLayout.SOUTH);

        return panel;
    }

    public void refreshData() {
        loadCategorias();
        loadUbicaciones();
        loadUsuarios();
    }

    /* === Categorías === */
    private void loadCategorias() {
        try {
            categoriaModel.setData(inventarioService.listarCategoriasIncluyendoInactivas());
        } catch (Exception e) {
            Ui.error(this, e);
        }
    }

    private void onNuevaCategoria() {
        String nombre = JOptionPane.showInputDialog(this, "Nombre de la categoría:");
        if (nombre == null || nombre.isBlank()) return;
        try {
            inventarioService.crearCategoria(nombre);
            loadCategorias();
        } catch (Exception e) {
            Ui.error(this, e);
        }
    }

    private void onRenombrarCategoria() {
        int row = tblCategorias.getSelectedRow();
        if (row < 0) {
            Ui.info(this, "Seleccioná una categoría");
            return;
        }
        Categoria sel = categoriaModel.getAt(row);
        String nombre = JOptionPane.showInputDialog(this, "Nuevo nombre:", sel.getNombre());
        if (nombre == null || nombre.isBlank()) return;
        try {
            sel.setNombre(nombre.trim());
            inventarioService.actualizarCategoria(sel);
            loadCategorias();
        } catch (Exception e) {
            Ui.error(this, e);
        }
    }

    private void onBajaCategoria() {
        ejecutarSobreCategoria(sel -> {
            if (!sel.isActiva()) {
                Ui.info(this, "La categoría ya está dada de baja.");
                return;
            }
            if (Ui.confirm(this, "¿Dar de baja la categoría '" + sel.getNombre() + "'?")) {
                inventarioService.bajaLogicaCategoria(sel.getId());
                loadCategorias();
            }
        });
    }

    private void onRestaurarCategoria() {
        ejecutarSobreCategoria(sel -> {
            if (sel.isActiva()) {
                Ui.info(this, "La categoría ya está activa.");
                return;
            }
            inventarioService.restaurarCategoria(sel.getId());
            loadCategorias();
        });
    }

    private void ejecutarSobreCategoria(java.util.function.Consumer<Categoria> action) {
        int row = tblCategorias.getSelectedRow();
        if (row < 0) {
            Ui.info(this, "Seleccioná una categoría");
            return;
        }
        Categoria sel = categoriaModel.getAt(row);
        try {
            action.accept(sel);
        } catch (Exception e) {
            Ui.error(this, e);
        }
    }

    /* === Ubicaciones === */
    private void loadUbicaciones() {
        try {
            ubicacionModel.setData(inventarioService.listarUbicacionesIncluyendoInactivas());
        } catch (Exception e) {
            Ui.error(this, e);
        }
    }

    private void onNuevaUbicacion() {
        String nombre = JOptionPane.showInputDialog(this, "Nombre de la ubicación:");
        if (nombre == null || nombre.isBlank()) return;
        try {
            inventarioService.crearUbicacion(nombre);
            loadUbicaciones();
        } catch (Exception e) {
            Ui.error(this, e);
        }
    }

    private void onRenombrarUbicacion() {
        int row = tblUbicaciones.getSelectedRow();
        if (row < 0) {
            Ui.info(this, "Seleccioná una ubicación");
            return;
        }
        Ubicacion sel = ubicacionModel.getAt(row);
        String nombre = JOptionPane.showInputDialog(this, "Nuevo nombre:", sel.getNombre());
        if (nombre == null || nombre.isBlank()) return;
        try {
            sel.setNombre(nombre.trim());
            inventarioService.actualizarUbicacion(sel);
            loadUbicaciones();
        } catch (Exception e) {
            Ui.error(this, e);
        }
    }

    private void onBajaUbicacion() {
        ejecutarSobreUbicacion(sel -> {
            if (!sel.isActiva()) {
                Ui.info(this, "La ubicación ya está dada de baja.");
                return;
            }
            if (Ui.confirm(this, "¿Dar de baja la ubicación '" + sel.getNombre() + "'?")) {
                inventarioService.bajaLogicaUbicacion(sel.getId());
                loadUbicaciones();
            }
        });
    }

    private void onRestaurarUbicacion() {
        ejecutarSobreUbicacion(sel -> {
            if (sel.isActiva()) {
                Ui.info(this, "La ubicación ya está activa.");
                return;
            }
            inventarioService.restaurarUbicacion(sel.getId());
            loadUbicaciones();
        });
    }

    private void ejecutarSobreUbicacion(java.util.function.Consumer<Ubicacion> action) {
        int row = tblUbicaciones.getSelectedRow();
        if (row < 0) {
            Ui.info(this, "Seleccioná una ubicación");
            return;
        }
        Ubicacion sel = ubicacionModel.getAt(row);
        try {
            action.accept(sel);
        } catch (Exception e) {
            Ui.error(this, e);
        }
    }

    /* === Usuarios === */
    private void loadUsuarios() {
        List<Usuario> list = new ArrayList<>();
        String sql = "SELECT u.id, u.username, u.password, u.email, u.estado, r.id rol_id, r.nombre rol_nombre " +
                "FROM usuario u JOIN rol r ON r.id=u.rol_id ORDER BY u.username";
        try (var cn = DataSourceFactory.getConnection();
             var ps = cn.prepareStatement(sql);
             var rs = ps.executeQuery()) {
            while (rs.next()) {
                Usuario u = new Usuario();
                u.setId(rs.getLong("id"));
                u.setUsername(rs.getString("username"));
                u.setPassword(rs.getString("password"));
                u.setEmail(rs.getString("email"));
                u.setEstado(rs.getString("estado"));
                Rol rol = new Rol();
                rol.setId(rs.getInt("rol_id"));
                rol.setNombre(rs.getString("rol_nombre"));
                u.setRol(rol);
                list.add(u);
            }
            usuarioModel.setData(list);
        } catch (Exception e) {
            Ui.error(this, e);
        }
    }

    private void onNuevoAdministrativo() {
        var dlg = new UsuarioFormDialog(SwingUtilities.getWindowAncestor(this), new String[]{RoleName.ADMINISTRATIVO});
        dlg.setVisible(true);
        if (!dlg.isAccepted()) {
            return;
        }
        String plainPassword = dlg.getPassword();
        if (plainPassword == null || plainPassword.isBlank()) {
            Ui.warn(this, "La contraseña es obligatoria.");
            return;
        }
        try (var cn = DataSourceFactory.getConnection();
             var ps = cn.prepareStatement(
                     "INSERT INTO usuario(username,password,password_hash,email,estado,rol_id) VALUES (?,?,?,?,?,?)")) {
            ps.setString(1, dlg.getUsername());
            ps.setString(2, plainPassword);
            ps.setString(3, PasswordUtil.hash(plainPassword));
            ps.setString(4, dlg.getEmail());
            ps.setString(5, "ACTIVO");
            ps.setInt(6, findRolId(RoleName.ADMINISTRATIVO));
            ps.executeUpdate();
            loadUsuarios();
        } catch (Exception e) {
            Ui.error(this, e);
        }
    }

    private void onBajaUsuario() {
        int row = tblUsuarios.getSelectedRow();
        if (row < 0) {
            Ui.info(this, "Seleccioná un usuario");
            return;
        }
        Usuario sel = usuarioModel.getAt(row);
        if (sel.getRol() != null && RoleName.ADMIN.equalsIgnoreCase(sel.getRol().getNombre())) {
            Ui.warn(this, "No podés dar de baja a un administrador desde aquí.");
            return;
        }
        if (Ui.confirm(this, "¿Dar de baja al usuario '" + sel.getUsername() + "'?")) {
            try (var cn = DataSourceFactory.getConnection();
                 var ps = cn.prepareStatement("UPDATE usuario SET estado='INACTIVO', activo=0 WHERE id=?")) {
                ps.setLong(1, sel.getId());
                ps.executeUpdate();
                loadUsuarios();
            } catch (Exception e) {
                Ui.error(this, e);
            }
        }
    }

    private void onRestaurarUsuario() {
        int row = tblUsuarios.getSelectedRow();
        if (row < 0) {
            Ui.info(this, "Seleccioná un usuario");
            return;
        }
        Usuario sel = usuarioModel.getAt(row);
        if (Ui.confirm(this, "¿Restaurar al usuario '" + sel.getUsername() + "'?")) {
            try (var cn = DataSourceFactory.getConnection();
                 var ps = cn.prepareStatement("UPDATE usuario SET estado='ACTIVO', activo=1 WHERE id=?")) {
                ps.setLong(1, sel.getId());
                ps.executeUpdate();
                loadUsuarios();
            } catch (Exception e) {
                Ui.error(this, e);
            }
        }
    }

    private int findRolId(String roleName) throws SQLException {
        String sql = "SELECT id FROM rol WHERE UPPER(nombre)=?";
        try (var cn = DataSourceFactory.getConnection();
             var ps = cn.prepareStatement(sql)) {
            ps.setString(1, roleName.toUpperCase());
            try (var rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        }
        throw new SQLException("Rol no encontrado: " + roleName);
    }

    /* === Table models === */
    static class CategoriaTableModel extends CrudTableModel<Categoria> {
        CategoriaTableModel() { super(new String[]{"ID", "Nombre", "Estado"}); }

        @Override
        public Object getValueAt(int row, int col) {
            Categoria c = data.get(row);
            return switch (col) {
                case 0 -> c.getId();
                case 1 -> c.getNombre();
                case 2 -> c.isActiva() ? "Activa" : "Inactiva";
                default -> "";
            };
        }
    }

    static class UbicacionTableModel extends CrudTableModel<Ubicacion> {
        UbicacionTableModel() { super(new String[]{"ID", "Nombre", "Estado"}); }

        @Override
        public Object getValueAt(int row, int col) {
            Ubicacion u = data.get(row);
            return switch (col) {
                case 0 -> u.getId();
                case 1 -> u.getNombre();
                case 2 -> u.isActiva() ? "Activa" : "Inactiva";
                default -> "";
            };
        }
    }

    static class UsuarioTableModel extends CrudTableModel<Usuario> {
        UsuarioTableModel() { super(new String[]{"ID", "Usuario", "Contraseña", "Email", "Estado", "Rol"}); }

        @Override
        public Object getValueAt(int row, int col) {
            Usuario u = data.get(row);
            return switch (col) {
                case 0 -> u.getId();
                case 1 -> u.getUsername();
                case 2 -> u.getPassword() == null ? "" : u.getPassword();
                case 3 -> u.getEmail();
                case 4 -> u.getEstado();
                case 5 -> u.getRol() != null ? u.getRol().getNombre() : "-";
                default -> "";
            };
        }
    }

    private JPanel sectionHeader(String title, String subtitle) {
        JPanel header = new JPanel();
        header.setOpaque(false);
        header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));

        JLabel lblTitle = new JLabel(title);
        lblTitle.setFont(lblTitle.getFont().deriveFont(Font.BOLD, 18f));
        lblTitle.setForeground(HEADER_TEXT);

        JLabel lblSubtitle = new JLabel(subtitle);
        lblSubtitle.setFont(lblSubtitle.getFont().deriveFont(Font.PLAIN, 13f));
        lblSubtitle.setForeground(new Color(110, 126, 165));

        header.add(lblTitle);
        header.add(Box.createVerticalStrut(4));
        header.add(lblSubtitle);
        return header;
    }

    private void styleTable(JTable table) {
        table.setRowHeight(36);
        table.setIntercellSpacing(new Dimension(0, 6));
        table.setShowHorizontalLines(true);
        table.setShowVerticalLines(false);
        table.setGridColor(new Color(230, 235, 250));
        table.setFillsViewportHeight(true);
        table.setFont(table.getFont().deriveFont(Font.PLAIN, 13f));
        table.setSelectionBackground(new Color(210, 222, 255));
        table.setSelectionForeground(HEADER_TEXT);

        JTableHeader header = table.getTableHeader();
        header.setReorderingAllowed(false);
        header.setFont(header.getFont().deriveFont(Font.BOLD, 13f));
        header.setOpaque(true);
        header.setBackground(new Color(242, 246, 255));
        header.setForeground(HEADER_TEXT);
        header.setBorder(new EmptyBorder(12, 12, 12, 12));
    }

    private JScrollPane tableScroll(JTable table) {
        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(new Color(225, 230, 246), 1, true),
                new EmptyBorder(0, 0, 0, 0)
        ));
        scroll.getViewport().setBackground(Color.WHITE);
        scroll.setOpaque(false);
        return scroll;
    }

    private JPanel actionsRow(JButton... buttons) {
        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        row.setOpaque(false);
        for (JButton b : buttons) {
            row.add(b);
        }
        return row;
    }

    private JButton primaryButton(String text) {
        JButton b = new JButton(text);
        b.setFocusPainted(false);
        b.setBackground(BRAND);
        b.setForeground(Color.WHITE);
        b.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(new Color(42, 80, 190), 1, true),
                new EmptyBorder(10, 20, 10, 20)
        ));
        b.putClientProperty("JButton.buttonType", "roundRect");
        return b;
    }

    private JButton outlineButton(String text) {
        JButton b = new JButton(text);
        b.setFocusPainted(false);
        b.setBackground(Color.WHITE);
        b.setForeground(BRAND.darker());
        b.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(new Color(194, 206, 255), 1, true),
                new EmptyBorder(10, 20, 10, 20)
        ));
        b.putClientProperty("JButton.buttonType", "roundRect");
        return b;
    }
}
