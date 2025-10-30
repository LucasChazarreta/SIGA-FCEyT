package ar.edu.unse.siga.ui.tramites;

import ar.edu.unse.siga.domain.Tramite;
import ar.edu.unse.siga.service.InventarioService;
import ar.edu.unse.siga.service.TramiteService;
import ar.edu.unse.siga.ui.base.BaseCrudFrame;
import ar.edu.unse.siga.ui.base.Ui;
import ar.edu.unse.siga.ui.TramiteTableModel; // <- usamos el modelo existente
import ar.edu.unse.siga.ui.base.UiBus;
import ar.edu.unse.siga.ui.reportes.InformesPanel;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
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
    private final InventarioService invService;
    private final TramiteTableModel model = new TramiteTableModel();

    private final JTextField txtSearch = new JTextField(20);
    private final JComboBox<String> cbCampo
            = new JComboBox<>(new String[]{"ID", "Nro", "Solicitud", "Estado", "Fecha", "Solicitante"});

    private TableRowSorter<TableModel> sorter;

    public TramiteFrame(TramiteService service, InventarioService invService) {
        super("Solicitudes");
        this.service = service;
        this.invService = invService;
        table.setModel(model);

        styleActionsBar();

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
                    case "Solicitud" ->
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
        cm.getColumn(2).setPreferredWidth(260); // Solicitud
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
        // ---- Cargar
        loadData();
    }

    private void styleActionsBar() {
        btnNuevo.setText("Registrar nueva solicitud");
        btnEditar.setVisible(false);
        btnBaja.setVisible(false);

        stylePrimaryButton(btnNuevo);
        styleOutlineButton(btnRefrescar);

        JPanel top = (JPanel) ((BorderLayout) getContentPane().getLayout())
                .getLayoutComponent(BorderLayout.NORTH);
        top.removeAll();

        actionsPanel.removeAll();
        actionsPanel.setOpaque(false);
        actionsPanel.setLayout(new FlowLayout(FlowLayout.CENTER, 12, 10));
        actionsPanel.add(btnNuevo);

        JButton btnEstados = new JButton("Estados de solicitudes");
        btnEstados.addActionListener(e -> onEstado());
        styleOutlineButton(btnEstados);
        actionsPanel.add(btnEstados);

        btnRefrescar.setText("Refrescar");
        actionsPanel.add(btnRefrescar);

        top.setLayout(new BorderLayout());
        top.add(actionsPanel, BorderLayout.CENTER);
        top.revalidate();
        top.repaint();
    }

    private static void stylePrimaryButton(AbstractButton b) {
        b.setBackground(new Color(58, 96, 224));
        b.setForeground(Color.WHITE);
        b.setFont(b.getFont().deriveFont(Font.BOLD, 14f));
        b.setFocusPainted(false);
        b.setBorder(BorderFactory.createEmptyBorder(10, 16, 10, 16));
        b.putClientProperty("JButton.buttonType", "roundRect");
    }

    private static void styleOutlineButton(AbstractButton b) {
        Color brand = new Color(58, 96, 224);
        b.setBackground(Color.WHITE);
        b.setForeground(brand);
        b.setFont(b.getFont().deriveFont(Font.BOLD, 14f));
        b.setFocusPainted(false);
        b.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(brand, 1, true),
                new EmptyBorder(9, 15, 9, 15)
        ));
        b.putClientProperty("JButton.buttonType", "roundRect");
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
        Window owner = SwingUtilities.getWindowAncestor(this);

        // Usa el constructor que realmente existe (Window, TramiteService, InventarioService)
        RegistrarTramiteDialog dlg = new RegistrarTramiteDialog(owner, service, invService);
        dlg.setVisible(true);

        if (!dlg.isAccepted()) {
            return;
        }

        // 1) refresca la tabla local de Solicitudes
        loadData();

        // 2) avisa globalmente para que InformesPanel se recargue (métricas, movimientos, etc.)
        ar.edu.unse.siga.ui.base.UiBus.fire("tramite-saved");

        // 3) (opcional) refresco directo si el panel Informes está en esta misma ventana
        Window root = SwingUtilities.getWindowAncestor(this);
        ar.edu.unse.siga.ui.reportes.InformesPanel inf
                = (ar.edu.unse.siga.ui.reportes.InformesPanel) SwingUtilities.getAncestorOfClass(ar.edu.unse.siga.ui.reportes.InformesPanel.class, root);
        if (inf != null) {
            inf.reloadAfterTramiteSaved();
        }
    }

    @Override
    protected void onEditar() {
        Ui.info(this, "Edición de solicitudes no implementada (solo cambio de estado).");
    }

    @Override
    protected void onBaja() {
        Ui.info(this, "No se implementa baja física de solicitudes.");
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
                service.actualizarEstado(sel.getId(), nuevo);
                loadData();
            } catch (Exception e) {
                Ui.error(this, e);
            }
        }
    }
}
