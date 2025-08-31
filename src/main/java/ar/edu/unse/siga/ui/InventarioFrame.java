/* esto es la ventana principal 
Esta clase:
Carga insumos en una tabla.
Tiene botones Nuevo, Editar, Baja y Refrescar.
Pide las categorías para el formulario (consulta rápida a la BD).
*/
package ar.edu.unse.siga.ui;

import ar.edu.unse.siga.domain.Categoria;
import ar.edu.unse.siga.domain.Insumo;
import ar.edu.unse.siga.persistence.dao.InsumoDao;
import ar.edu.unse.siga.service.InventarioService;

import javax.swing.*;
import java.awt.*;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

import static ar.edu.unse.siga.persistence.DataSourceFactory.getConnection;

public class InventarioFrame extends JFrame {
    private final InventarioService service;
    private final InsTableModel tableModel = new InsTableModel();
    private final JTable table = new JTable(tableModel);

    public InventarioFrame(InventarioService service) {
        super("SIGA - Inventario");
        this.service = service;
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLayout(new BorderLayout(10,10));

        // Tabla
        add(new JScrollPane(table), BorderLayout.CENTER);

        // Botones
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton btnNuevo = new JButton("Nuevo");
        JButton btnEditar = new JButton("Editar");
        JButton btnBaja = new JButton("Baja lógica");
        JButton btnRefresh = new JButton("Refrescar");
        actions.add(btnNuevo);
        actions.add(btnEditar);
        actions.add(btnBaja);
        actions.add(btnRefresh);
        add(actions, BorderLayout.NORTH);

        // Acciones
        btnRefresh.addActionListener(e -> loadData());

        btnNuevo.addActionListener(e -> {
            var categorias = loadCategorias();
            var dialog = new InsFormDialog(this, "Nuevo Insumo", categorias, null);
            dialog.setVisible(true);
            if (dialog.isAccepted()) {
                try {
                    Insumo i = new Insumo();
                    i.setCodigo(dialog.getCodigo());
                    i.setDescripcion(dialog.getDescripcion());
                    i.setCategoria(dialog.getCategoria());
                    i.setStockMinimo(dialog.getStockMinimo());
                    i.setUbicacion(dialog.getUbicacion());
                    i.setEstado(dialog.getEstado());
                    service.registrarInsumo(i);
                    loadData();
                } catch (Exception ex) {
                    showError(ex);
                }
            }
        });

        btnEditar.addActionListener(e -> {
            int row = table.getSelectedRow();
            if (row < 0) { JOptionPane.showMessageDialog(this, "Seleccioná un insumo"); return; }
            Insumo sel = tableModel.getAt(row);

            var categorias = loadCategorias();
            var dialog = new InsFormDialog(this, "Editar Insumo", categorias, sel);
            dialog.setVisible(true);
            if (dialog.isAccepted()) {
                try {
                    sel.setDescripcion(dialog.getDescripcion());
                    sel.setCategoria(dialog.getCategoria());
                    sel.setStockMinimo(dialog.getStockMinimo());
                    sel.setUbicacion(dialog.getUbicacion());
                    sel.setEstado(dialog.getEstado());
                    service.editarInsumo(sel);
                    loadData();
                } catch (Exception ex) {
                    showError(ex);
                }
            }
        });

        btnBaja.addActionListener(e -> {
            int row = table.getSelectedRow();
            if (row < 0) { JOptionPane.showMessageDialog(this, "Seleccioná un insumo"); return; }
            Insumo sel = tableModel.getAt(row);
            int r = JOptionPane.showConfirmDialog(this, "¿Dar de baja lógica el insumo " + sel.getCodigo() + "?", "Confirmar", JOptionPane.YES_NO_OPTION);
            if (r == JOptionPane.YES_OPTION) {
                try {
                    service.bajaLogica(sel.getId());
                    loadData();
                } catch (Exception ex) {
                    showError(ex);
                }
            }
        });

        // Inicial
        setSize(900, 500);
        setLocationRelativeTo(null);
        loadData();
    }

    private void loadData() {
        try {
            var list = service.listarTodos();
            tableModel.setData(list);
        } catch (Exception e) {
            showError(e);
        }
    }

    private List<Categoria> loadCategorias() {
        List<Categoria> list = new ArrayList<>();
        String sql = "SELECT id, nombre FROM categoria ORDER BY nombre";
        try (Connection cn = getConnection();
             PreparedStatement ps = cn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(new Categoria(rs.getInt("id"), rs.getString("nombre")));
            }
        } catch (Exception e) {
            showError(e);
        }
        return list;
    }

    private void showError(Exception e) {
        e.printStackTrace();
        JOptionPane.showMessageDialog(this, e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
    }
}

// Nota: para simplificar, aqui el frame consulta categorías directo con getConnection(). 
// para que sea mas puro, podríamos crear CategoriaDao y pedirlas al service. Lo podemos refactorizar luego.