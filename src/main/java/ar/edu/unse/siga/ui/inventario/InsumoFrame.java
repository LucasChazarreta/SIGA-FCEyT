package ar.edu.unse.siga.ui.inventario;

import ar.edu.unse.siga.domain.Insumo;
import ar.edu.unse.siga.service.InventarioService;
import ar.edu.unse.siga.ui.base.BaseCrudFrame;
import ar.edu.unse.siga.ui.base.CrudTableModel;
import ar.edu.unse.siga.ui.base.Ui;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;
import javax.swing.RowFilter;
import java.awt.*;

public class InsumoFrame extends BaseCrudFrame<Insumo> {

    private final InventarioService service;
    private final InsumoTableModel model = new InsumoTableModel();

    private final JTextField txtSearch = new JTextField(20);
    private final JComboBox<String> cbCampo = new JComboBox<>(new String[]{"Código", "Descripción", "Categoría"});
    private TableRowSorter<TableModel> sorter;

    public InsumoFrame(InventarioService service) {
        super("ABM Insumos");
        this.service = service;
        table.setModel(model);

        // ---- Ordenamiento y filtro
        sorter = new TableRowSorter<>(table.getModel());
        table.setRowSorter(sorter);

        // ---- Panel de búsqueda
        JPanel pnlSearch = new JPanel(new FlowLayout(FlowLayout.LEFT));
        pnlSearch.add(new JLabel("Buscar:"));
        pnlSearch.add(txtSearch);
        pnlSearch.add(cbCampo);
        ((JPanel) getContentPane().getComponent(0)).add(pnlSearch, BorderLayout.EAST);

        // ---- Filtro reactivo (genéricos correctos)
        txtSearch.getDocument().addDocumentListener(new DocumentListener() {
            private void filter() {
                String q = txtSearch.getText().trim().toLowerCase();
                if (q.isEmpty()) {
                    sorter.setRowFilter(null);
                    return;
                }
                int col = switch (cbCampo.getSelectedIndex()) {
                    case 0 -> 1;  // Código
                    case 1 -> 2;  // Descripción
                    default -> 3; // Categoría
                };
                sorter.setRowFilter(new RowFilter<TableModel, Integer>() {
                    @Override
                    public boolean include(Entry<? extends TableModel, ? extends Integer> entry) {
                        Object v = entry.getValue(col);
                        return v != null && v.toString().toLowerCase().contains(q);
                    }
                });
            }
            @Override public void insertUpdate(DocumentEvent e) { filter(); }
            @Override public void removeUpdate(DocumentEvent e) { filter(); }
            @Override public void changedUpdate(DocumentEvent e) { filter(); }
        });

        // ---- Zebra striping y anchos
        table.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable tbl, Object val, boolean sel, boolean foc, int row, int col) {
                Component c = super.getTableCellRendererComponent(tbl, val, sel, foc, row, col);
                if (!sel) c.setBackground(row % 2 == 0 ? new Color(250, 250, 250) : new Color(240, 240, 240));
                return c;
            }
        });
        var cm = table.getColumnModel();
        cm.getColumn(0).setPreferredWidth(50);   // ID
        cm.getColumn(1).setPreferredWidth(120);  // Código
        cm.getColumn(2).setPreferredWidth(300);  // Descripción
        cm.getColumn(3).setPreferredWidth(150);  // Categoría
        cm.getColumn(4).setPreferredWidth(100);  // Stock mín.
        cm.getColumn(5).setPreferredWidth(150);  // Ubicación
        cm.getColumn(6).setPreferredWidth(100);  // Estado

        // ---- Atajos
        var im = table.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
        var am = table.getActionMap();
        im.put(KeyStroke.getKeyStroke("control N"), "new");
        im.put(KeyStroke.getKeyStroke("control E"), "edit");
        im.put(KeyStroke.getKeyStroke("DELETE"), "del");
        im.put(KeyStroke.getKeyStroke("F5"), "refresh");
        am.put("new",     new AbstractAction(){ public void actionPerformed(java.awt.event.ActionEvent e){ onNuevo(); }});
        am.put("edit",    new AbstractAction(){ public void actionPerformed(java.awt.event.ActionEvent e){ onEditar(); }});
        am.put("del",     new AbstractAction(){ public void actionPerformed(java.awt.event.ActionEvent e){ onBaja(); }});
        am.put("refresh", new AbstractAction(){ public void actionPerformed(java.awt.event.ActionEvent e){ loadData(); }});

        // ---- Doble click = editar
        table.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent e) {
                if (e.getClickCount() == 2 && table.getSelectedRow() >= 0) onEditar();
            }
        });

        // ---- Cargar
        loadData();

        // ---- Botón Movimiento
        JButton btnMov = new JButton("Movimiento");
        ((JPanel) getContentPane().getComponent(0)).add(btnMov); // al panel norte
        btnMov.addActionListener(e -> onMovimiento());
    }

    @Override
    protected void loadData() {
        try { model.setData(service.listarTodos()); }
        catch (Exception e) { Ui.error(this, e); }
    }

    @Override
    protected void onNuevo() {
        var dlg = new InsumoFormDialog(SwingUtilities.getWindowAncestor(this), null);
        dlg.setVisible(true);
        if (dlg.isAccepted()) {
            try {
                var i = new Insumo();
                i.setCodigo(dlg.getCodigo());
                i.setDescripcion(dlg.getDescripcion());
                i.setCategoria(dlg.getCategoria());
                i.setStockMinimo(dlg.getStockMinimo());
                i.setUbicacion(dlg.getUbicacion());
                i.setEstado(dlg.getEstado());
                service.registrarInsumo(i);
                loadData();
            } catch (Exception e) { Ui.error(this, e); }
        }
    }

    @Override
    protected void onEditar() {
        int r = selectedRowOrWarn(); if (r < 0) return;
        var sel = model.getAt(r);
        var dlg = new InsumoFormDialog(SwingUtilities.getWindowAncestor(this), sel);
        dlg.setVisible(true);
        if (dlg.isAccepted()) {
            try {
                sel.setDescripcion(dlg.getDescripcion());
                sel.setCategoria(dlg.getCategoria());
                sel.setStockMinimo(dlg.getStockMinimo());
                sel.setUbicacion(dlg.getUbicacion());
                sel.setEstado(dlg.getEstado());
                service.editarInsumo(sel);
                loadData();
            } catch (Exception e) { Ui.error(this, e); }
        }
    }

    @Override
    protected void onBaja() {
        int r = selectedRowOrWarn(); if (r < 0) return;
        var sel = model.getAt(r);
        if (Ui.confirm(this, "¿Dar de baja lógica el insumo " + sel.getCodigo() + "?")) {
            try { service.bajaLogica(sel.getId()); loadData(); }
            catch (Exception e) { Ui.error(this, e); }
        }
    }

    private void onMovimiento() {
        int r = selectedRowOrWarn(); if (r < 0) return;
        var sel = model.getAt(r);
        var dlg = new MovimientoDialog(SwingUtilities.getWindowAncestor(this));
        dlg.setVisible(true);
        if (dlg.isAccepted()) {
            try {
                service.registrarMovimiento(sel.getId(), dlg.getTipo(), dlg.getCantidad(), dlg.getDestinoFuente());
                Ui.info(this, "Movimiento registrado");
                loadData();
            } catch (Exception e) { Ui.error(this, e); }
        }
    }

    static class InsumoTableModel extends CrudTableModel<Insumo> {
        InsumoTableModel() { super(new String[]{"ID", "Código", "Descripción", "Categoría", "Stock mín.", "Ubicación", "Estado"}); }
        @Override public Object getValueAt(int row, int col) {
            var i = data.get(row);
            return switch (col) {
                case 0 -> i.getId();
                case 1 -> i.getCodigo();
                case 2 -> i.getDescripcion();
                case 3 -> i.getCategoria() != null ? i.getCategoria().getNombre() : "-";
                case 4 -> i.getStockMinimo();
                case 5 -> i.getUbicacion();
                case 6 -> i.getEstado();
                default -> "";
            };
        }
    }
}
