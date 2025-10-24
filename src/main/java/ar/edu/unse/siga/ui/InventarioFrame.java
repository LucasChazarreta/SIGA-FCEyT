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

import ar.edu.unse.siga.common.CsvExporter;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.Date;

import ar.edu.unse.siga.ui.inventario.MovimientoDialog;

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

        JSpinner spDesde = new JSpinner(new SpinnerDateModel(new Date(), null, null, java.util.Calendar.DAY_OF_MONTH));
        JSpinner spHasta = new JSpinner(new SpinnerDateModel(new Date(), null, null, java.util.Calendar.DAY_OF_MONTH));
        JPanel dates = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        spDesde.setEditor(new JSpinner.DateEditor(spDesde, "yyyy-MM-dd"));
        spHasta.setEditor(new JSpinner.DateEditor(spHasta, "yyyy-MM-dd"));
        dates.add(new JLabel("Desde:")); dates.add(spDesde);
        dates.add(new JLabel("Hasta:")); dates.add(spHasta);
        add(dates, BorderLayout.SOUTH);
        
        // Botones
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton btnNuevo = new JButton("Nuevo");
        JButton btnEditar = new JButton("Editar");
        JButton btnBaja = new JButton("Baja lógica");
        JButton btnRefresh = new JButton("Refrescar");
        JButton btnMov = new JButton("Movimiento");
        JButton btnCsv = new JButton("Exportar CSV");
        
        actions.add(btnCsv);
        actions.add(btnMov);
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
        
        // Acción del botón Movimiento:
        btnMov.addActionListener(e -> {
            int row = table.getSelectedRow();
            if (row < 0) { JOptionPane.showMessageDialog(this, "Seleccioná un insumo"); return; }
            var sel = tableModel.getAt(row);

            String[] opciones = {"ENTRADA", "SALIDA"};
            String tipo = (String) JOptionPane.showInputDialog(
                    this,
                    "Seleccioná el tipo de movimiento",
                    "Movimiento",
                    JOptionPane.QUESTION_MESSAGE,
                    null,
                    opciones,
                    "SALIDA");
            if (tipo == null) return;

            java.math.BigDecimal stockActual = java.math.BigDecimal.ZERO;
            try {
                var res = service.stockActual(sel.getId());
                if (res != null) {
                    stockActual = res.getStockActualDecimal();
                }
            } catch (Exception ignored) {}

            String contexto = String.format("%s · %s  |  Stock actual: %s",
                    sel.getCodigo(),
                    sel.getDescripcion() == null ? "" : sel.getDescripcion(),
                    stockActual.stripTrailingZeros().toPlainString());

            boolean allowDecimal = sel.getTipo() == null || !"BIEN".equalsIgnoreCase(sel.getTipo());
            java.util.List<String> ubicaciones = service.listarUbicaciones().stream()
                    .map(u -> u.getNombre())
                    .filter(s -> s != null && !s.isBlank())
                    .collect(java.util.stream.Collectors.toList());

            var dlg = new MovimientoDialog(this, contexto, tipo, allowDecimal, ubicaciones);
            dlg.setVisible(true);
            if (dlg.isAccepted()) {
                try {
                    service.registrarMovimiento(sel.getId(), dlg.getTipo(), dlg.getCantidad(), dlg.getDestinoFuente(), dlg.getSolicitante());
                    loadData();
                    JOptionPane.showMessageDialog(this, "Movimiento registrado");
                } catch (Exception ex) {
                    showError(ex);
                }
            }
        });

        btnCsv.addActionListener(e -> {
            try {
                Date d1 = (Date) spDesde.getValue();
                Date d2 = (Date) spHasta.getValue();
                var sdf = new SimpleDateFormat("yyyy-MM-dd");
                String f1 = sdf.format(d1) + " 00:00:00";
                String f2 = sdf.format(d2) + " 23:59:59";

                String sql = """
                    SELECT m.id, i.codigo, i.descripcion, m.tipo, m.cantidad, m.destino_fuente, m.solicitante, m.fecha
                    FROM movimiento m JOIN insumo i ON i.id = m.insumo_id
                    WHERE m.fecha BETWEEN ? AND ?
                    ORDER BY m.fecha DESC
                """;

                java.util.List<String[]> rows = new java.util.ArrayList<>();
                rows.add(new String[]{"ID","CODIGO","DESCRIPCION","TIPO","CANTIDAD","DESTINO","SOLICITANTE","FECHA"});

                try (var cn = getConnection(); var ps = cn.prepareStatement(sql)) {
                    ps.setString(1, f1);
                    ps.setString(2, f2);
                    try (var rs = ps.executeQuery()) {
                        while (rs.next()) {
                            rows.add(new String[]{
                                String.valueOf(rs.getLong("id")),
                                rs.getString("codigo"),
                                rs.getString("descripcion"),
                                rs.getString("tipo"),
                                String.valueOf(rs.getInt("cantidad")),
                                rs.getString("destino_fuente"),
                                rs.getString("solicitante"),
                                String.valueOf(rs.getTimestamp("fecha"))
                            });
                        }
                    }
                }

                var chooser = new javax.swing.JFileChooser();
                chooser.setDialogTitle("Guardar reporte CSV");
                chooser.setSelectedFile(new java.io.File("reporte_movimientos.csv"));
                int resp = chooser.showSaveDialog(this);
                if (resp == javax.swing.JFileChooser.APPROVE_OPTION) {
                    Path path = chooser.getSelectedFile().toPath();
                    CsvExporter.write(path, rows);
                    JOptionPane.showMessageDialog(this, "CSV exportado en:\n" + path);
                }
            } catch (Exception ex) {
                showError(ex);
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
        String sql = "SELECT id, nombre FROM categoria WHERE activo = 1 ORDER BY nombre";
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