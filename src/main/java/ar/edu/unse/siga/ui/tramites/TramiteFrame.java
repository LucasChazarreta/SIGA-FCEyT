package ar.edu.unse.siga.ui.tramites;

import ar.edu.unse.siga.domain.Tramite;
import ar.edu.unse.siga.service.TramiteService;
import ar.edu.unse.siga.ui.base.BaseCrudFrame;
import ar.edu.unse.siga.ui.base.Ui;
import ar.edu.unse.siga.ui.TramiteTableModel; // <- usamos el modelo existente

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;
import javax.swing.RowFilter;
import java.awt.*;
import java.util.List;

public class TramiteFrame extends BaseCrudFrame<Tramite> {

    private final TramiteService service;
    private final TramiteTableModel model = new TramiteTableModel();

    private final JTextField txtSearch = new JTextField(20);
    private final JComboBox<String> cbCampo
            = new JComboBox<>(new String[]{"ID", "Nro", "Asunto", "Estado", "Fecha", "Solicitante"});

    private TableRowSorter<TableModel> sorter;

    public TramiteFrame(TramiteService service) {
        super("ABM Trámites");
        this.service = service;
        table.setModel(model);

        // ---- Ordenamiento y filtro
        sorter = new TableRowSorter<>(table.getModel());
        table.setRowSorter(sorter);

        // ---- Panel de búsqueda (una sola vez)
        JPanel pnlSearch = new JPanel(new FlowLayout(FlowLayout.LEFT));
        pnlSearch.add(new JLabel("Buscar:"));
        pnlSearch.add(txtSearch);
        pnlSearch.add(cbCampo);
        ((JPanel) getContentPane().getComponent(0)).add(pnlSearch, BorderLayout.EAST);

        // ---- Filtro reactivo
        txtSearch.getDocument().addDocumentListener(new DocumentListener() {
            private void filter() {
                String q = txtSearch.getText().trim().toLowerCase();
                if (q.isEmpty()) {
                    sorter.setRowFilter(null);
                    return;
                }
                int col = switch (cbCampo.getSelectedItem().toString()) {
                    case "ID" ->
                        0;
                    case "Nro" ->
                        1;
                    case "Asunto" ->
                        2;
                    case "Estado" ->
                        3;
                    case "Fecha" ->
                        4;
                    case "Solicitante" ->
                        5;
                    default ->
                        1;
                };

                sorter.setRowFilter(new RowFilter<TableModel, Integer>() {
                    @Override
                    public boolean include(Entry<? extends TableModel, ? extends Integer> entry) {
                        Object v = entry.getValue(col);
                        return v != null && v.toString().toLowerCase().contains(q);
                    }
                });
            }

            @Override
            public void insertUpdate(DocumentEvent e) {
                filter();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                filter();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                filter();
            }
        });

        // ---- Zebra striping y anchos
        table.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable tbl, Object val, boolean sel, boolean foc, int row, int col) {
                Component c = super.getTableCellRendererComponent(tbl, val, sel, foc, row, col);
                if (!sel) {
                    c.setBackground(row % 2 == 0 ? new Color(250, 250, 250) : new Color(240, 240, 240));
                }
                return c;
            }
        });
        var cm = table.getColumnModel();
        cm.getColumn(0).setPreferredWidth(120); // ID
        cm.getColumn(1).setPreferredWidth(160); // Nro
        cm.getColumn(2).setPreferredWidth(260); // Asunto
        cm.getColumn(3).setPreferredWidth(160); // Estado
        cm.getColumn(4).setPreferredWidth(180); // Fecha
        cm.getColumn(5).setPreferredWidth(220); // Solicitante

        // ---- Atajos
        var im = table.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
        var am = table.getActionMap();
        im.put(KeyStroke.getKeyStroke("control N"), "new");
        im.put(KeyStroke.getKeyStroke("control E"), "edit");
        im.put(KeyStroke.getKeyStroke("DELETE"), "del");
        im.put(KeyStroke.getKeyStroke("F5"), "refresh");
        am.put("new", new AbstractAction() {
            public void actionPerformed(java.awt.event.ActionEvent e) {
                onNuevo();
            }
        });
        am.put("edit", new AbstractAction() {
            public void actionPerformed(java.awt.event.ActionEvent e) {
                onEditar();
            }
        });
        am.put("del", new AbstractAction() {
            public void actionPerformed(java.awt.event.ActionEvent e) {
                onBaja();
            }
        });
        am.put("refresh", new AbstractAction() {
            public void actionPerformed(java.awt.event.ActionEvent e) {
                loadData();
            }
        });

        // ---- Doble click = editar
        table.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent e) {
                if (e.getClickCount() == 2 && table.getSelectedRow() >= 0) {
                    onEditar();
                }
            }
        });

        // ---- Botón Cambiar estado
        JButton btnEstado = new JButton("Cambiar estado");
        ((JPanel) getContentPane().getComponent(0)).add(btnEstado);
        btnEstado.addActionListener(e -> onEstado());

        // ---- Cargar
        loadData();
    }

    @Override
    protected void loadData() {
        try {
            List<Tramite> todos = service.listarTodos();
            model.setData(todos);
        } catch (Exception e) {
            Ui.error(this, e);
        }
    }

    @Override
    protected void onNuevo() {
        var dlg = new TramiteFormDialog(SwingUtilities.getWindowAncestor(this));
        dlg.setVisible(true);
        if (dlg.isAccepted()) {
            try {
                // ⚠️ ahora pasamos los 5 parámetros que requiere el Service
                service.registrarTramite(
                        dlg.getNro(),
                        dlg.getAsunto(),
                        dlg.getSolicitante(),
                        dlg.getDescripcion(),
                        dlg.getDestino()
                );
                loadData();
            } catch (Exception e) {
                Ui.error(this, e);
            }
        }
    }

    @Override
    protected void onEditar() {
        Ui.info(this, "Edición de campos no implementada (solo cambio de estado).");
    }

    @Override
    protected void onBaja() {
        Ui.info(this, "No se implementa baja física de trámites.");
    }

    private void onEstado() {
        int r = selectedRowOrWarn();
        if (r < 0) {
            return;
        }
        // El modelo expone getAt(r) en BaseCrudFrame? Si no, convertimos por view->model:
        int modelRow = table.convertRowIndexToModel(r);
        Tramite sel = service.listarTodos().get(modelRow); // fallback seguro

        String nuevo = (String) JOptionPane.showInputDialog(this,
                "Nuevo estado:", "Cambiar estado",
                JOptionPane.PLAIN_MESSAGE, null,
                new String[]{"PENDIENTE", "EN_PROCESO", "COMPLETADO", "CERRADO"},
                sel.getEstado());
        if (nuevo != null) {
            try {
                service.cambiarEstado(sel.getId(), nuevo);
                loadData();
            } catch (Exception e) {
                Ui.error(this, e);
            }
        }
    }
}
