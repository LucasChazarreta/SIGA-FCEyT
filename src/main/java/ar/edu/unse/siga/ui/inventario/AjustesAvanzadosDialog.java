package ar.edu.unse.siga.ui.inventario;

import ar.edu.unse.siga.domain.Categoria;
import ar.edu.unse.siga.domain.Ubicacion;
import ar.edu.unse.siga.service.InventarioService;
import ar.edu.unse.siga.ui.base.CrudTableModel;
import ar.edu.unse.siga.ui.base.Ui;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

public class AjustesAvanzadosDialog extends JDialog {

    private final InventarioService service;

    private final CategoriaTableModel categoriaModel = new CategoriaTableModel();
    private final UbicacionTableModel ubicacionModel = new UbicacionTableModel();

    private final JTable tblCategorias = new JTable(categoriaModel);
    private final JTable tblUbicaciones = new JTable(ubicacionModel);

    public AjustesAvanzadosDialog(Window owner, InventarioService service) {
        super(owner, "Ajustes avanzados", ModalityType.APPLICATION_MODAL);
        this.service = service;

        setLayout(new BorderLayout(12, 12));
        setPreferredSize(new Dimension(640, 420));

        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Categorías", buildCategoriasPanel());
        tabs.addTab("Ubicaciones", buildUbicacionesPanel());
        add(tabs, BorderLayout.CENTER);

        JButton btnCerrar = new JButton("Cerrar");
        btnCerrar.addActionListener(e -> dispose());
        JPanel south = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        south.setBorder(new EmptyBorder(0, 0, 12, 12));
        south.add(btnCerrar);
        add(south, BorderLayout.SOUTH);

        loadCategorias();
        loadUbicaciones();

        pack();
        setLocationRelativeTo(owner);
    }

    private JPanel buildCategoriasPanel() {
        JPanel panel = new JPanel(new BorderLayout(8, 8));
        panel.setBorder(new EmptyBorder(12, 12, 12, 12));

        tblCategorias.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        JScrollPane scroll = new JScrollPane(tblCategorias);
        panel.add(scroll, BorderLayout.CENTER);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        JButton btnNueva = new JButton("Nueva");
        JButton btnRenombrar = new JButton("Renombrar");
        JButton btnBaja = new JButton("Baja lógica");
        JButton btnRestaurar = new JButton("Restaurar");

        buttons.add(btnNueva);
        buttons.add(btnRenombrar);
        buttons.add(btnBaja);
        buttons.add(btnRestaurar);
        panel.add(buttons, BorderLayout.NORTH);

        btnNueva.addActionListener(e -> onNuevaCategoria());
        btnRenombrar.addActionListener(e -> onRenombrarCategoria());
        btnBaja.addActionListener(e -> onBajaCategoria());
        btnRestaurar.addActionListener(e -> onRestaurarCategoria());

        return panel;
    }

    private JPanel buildUbicacionesPanel() {
        JPanel panel = new JPanel(new BorderLayout(8, 8));
        panel.setBorder(new EmptyBorder(12, 12, 12, 12));

        tblUbicaciones.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        JScrollPane scroll = new JScrollPane(tblUbicaciones);
        panel.add(scroll, BorderLayout.CENTER);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        JButton btnNueva = new JButton("Nueva");
        JButton btnRenombrar = new JButton("Renombrar");
        JButton btnBaja = new JButton("Baja lógica");
        JButton btnRestaurar = new JButton("Restaurar");

        buttons.add(btnNueva);
        buttons.add(btnRenombrar);
        buttons.add(btnBaja);
        buttons.add(btnRestaurar);
        panel.add(buttons, BorderLayout.NORTH);

        btnNueva.addActionListener(e -> onNuevaUbicacion());
        btnRenombrar.addActionListener(e -> onRenombrarUbicacion());
        btnBaja.addActionListener(e -> onBajaUbicacion());
        btnRestaurar.addActionListener(e -> onRestaurarUbicacion());

        return panel;
    }

    private void loadCategorias() {
        try {
            categoriaModel.setData(service.listarCategoriasIncluyendoInactivas());
        } catch (Exception e) {
            Ui.error(this, e);
        }
    }

    private void loadUbicaciones() {
        try {
            ubicacionModel.setData(service.listarUbicacionesIncluyendoInactivas());
        } catch (Exception e) {
            Ui.error(this, e);
        }
    }

    private void onNuevaCategoria() {
        String nombre = JOptionPane.showInputDialog(this, "Nombre de la categoría:");
        if (nombre == null || nombre.isBlank()) return;
        try {
            service.crearCategoria(nombre);
            loadCategorias();
        } catch (Exception e) {
            Ui.error(this, e);
        }
    }

    private void onRenombrarCategoria() {
        int row = tblCategorias.getSelectedRow();
        if (row < 0) { Ui.info(this, "Seleccioná una categoría"); return; }
        Categoria sel = categoriaModel.getAt(row);
        String nombre = JOptionPane.showInputDialog(this, "Nuevo nombre:", sel.getNombre());
        if (nombre == null || nombre.isBlank()) return;
        try {
            sel.setNombre(nombre.trim());
            service.actualizarCategoria(sel);
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
                service.bajaLogicaCategoria(sel.getId());
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
            service.restaurarCategoria(sel.getId());
            loadCategorias();
        });
    }

    private void ejecutarSobreCategoria(java.util.function.Consumer<Categoria> action) {
        int row = tblCategorias.getSelectedRow();
        if (row < 0) { Ui.info(this, "Seleccioná una categoría"); return; }
        Categoria sel = categoriaModel.getAt(row);
        try {
            action.accept(sel);
        } catch (Exception e) {
            Ui.error(this, e);
        }
    }

    private void onNuevaUbicacion() {
        String nombre = JOptionPane.showInputDialog(this, "Nombre de la ubicación:");
        if (nombre == null || nombre.isBlank()) return;
        try {
            service.crearUbicacion(nombre);
            loadUbicaciones();
        } catch (Exception e) {
            Ui.error(this, e);
        }
    }

    private void onRenombrarUbicacion() {
        int row = tblUbicaciones.getSelectedRow();
        if (row < 0) { Ui.info(this, "Seleccioná una ubicación"); return; }
        Ubicacion sel = ubicacionModel.getAt(row);
        String nombre = JOptionPane.showInputDialog(this, "Nuevo nombre:", sel.getNombre());
        if (nombre == null || nombre.isBlank()) return;
        try {
            sel.setNombre(nombre.trim());
            service.actualizarUbicacion(sel);
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
                service.bajaLogicaUbicacion(sel.getId());
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
            service.restaurarUbicacion(sel.getId());
            loadUbicaciones();
        });
    }

    private void ejecutarSobreUbicacion(java.util.function.Consumer<Ubicacion> action) {
        int row = tblUbicaciones.getSelectedRow();
        if (row < 0) { Ui.info(this, "Seleccioná una ubicación"); return; }
        Ubicacion sel = ubicacionModel.getAt(row);
        try {
            action.accept(sel);
        } catch (Exception e) {
            Ui.error(this, e);
        }
    }

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
}
