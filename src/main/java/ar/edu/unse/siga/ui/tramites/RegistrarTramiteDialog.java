package ar.edu.unse.siga.ui.tramites;

import ar.edu.unse.siga.domain.Insumo;
import ar.edu.unse.siga.service.InventarioService;
import ar.edu.unse.siga.ui.base.Ui;
import ar.edu.unse.siga.service.TramiteService;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableCellEditor;
import java.awt.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class RegistrarTramiteDialog extends JDialog {

    private final InventarioService inventarioService;
    private final JTable table = new JTable();
    private final LineaTableModel model = new LineaTableModel();
    private final JButton btnGuardar = new JButton("Registrar");
    private final JLabel lblTotales = new JLabel("0 ítems · 0 unidades");
    private boolean accepted = false;
    private final JTextField txtSolicitud = new JTextField(30);
    private final JTextField txtSolicitante = new JTextField(25);
    private final JTextArea txtDescripcion = new JTextArea(4, 25);
    private final JTextField txtDestino = new JTextField(25);
    private final JLabel lblNumero = new JLabel();
    private final List<Insumo> insumosDisponibles;
    private final boolean ready;

    private String solicitud;
    private String solicitante;
    private String descripcion;
    private String destino;
    private List<TramiteService.LineaTramite> lineasSeleccionadas = Collections.emptyList();

    public RegistrarTramiteDialog(Window owner, InventarioService inventarioService) {
        super(owner, "Registrar solicitud", ModalityType.APPLICATION_MODAL);
        this.inventarioService = inventarioService;
        this.insumosDisponibles = new ArrayList<>(inventarioService.insumosConStockDisponible());
        this.ready = !insumosDisponibles.isEmpty();

        if (!ready) {
            Ui.warn(owner, "No hay insumos con stock disponible.");
            accepted = false;
            dispose();
            return;
        }

        setLayout(new BorderLayout(10, 10));
        txtDescripcion.setLineWrap(true);
        txtDescripcion.setWrapStyleWord(true);
        lblNumero.setText("Se generará al registrar");
        lblNumero.setFont(lblNumero.getFont().deriveFont(Font.BOLD));

        add(buildFormPanel(), BorderLayout.NORTH);

        table.setModel(model);
        table.setRowHeight(26);
        table.putClientProperty("terminateEditOnFocusLost", Boolean.TRUE);

        table.getColumnModel().getColumn(0).setCellEditor(new InsumoCellEditor());
        table.getColumnModel().getColumn(2).setCellEditor(new CantidadCellEditor());

        add(new JScrollPane(table), BorderLayout.CENTER);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton btnAgregar = new JButton("Agregar fila");
        JButton btnQuitar = new JButton("Quitar fila");
        actions.add(btnAgregar);
        actions.add(btnQuitar);
        actions.add(lblTotales);

        JPanel bottom = new JPanel(new BorderLayout());
        bottom.add(actions, BorderLayout.WEST);
        JPanel right = Ui.flowRight(new JButton("Cancelar"), btnGuardar);
        bottom.add(right, BorderLayout.EAST);
        add(bottom, BorderLayout.SOUTH);

        ((JButton) right.getComponent(0)).addActionListener(e -> dispose());
        btnGuardar.addActionListener(e -> onGuardar());
        btnAgregar.addActionListener(e -> model.addEmptyRow());
        btnQuitar.addActionListener(e -> model.removeSelected(table.getSelectedRow()));

        model.addTableModelListener(new TableModelListener() {
            @Override
            public void tableChanged(TableModelEvent e) {
                updateState();
            }
        });

        txtSolicitud.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            @Override
            public void insertUpdate(javax.swing.event.DocumentEvent e) { updateState(); }

            @Override
            public void removeUpdate(javax.swing.event.DocumentEvent e) { updateState(); }

            @Override
            public void changedUpdate(javax.swing.event.DocumentEvent e) { updateState(); }
        });

        model.addEmptyRow();
        updateState();

        setPreferredSize(new Dimension(720, 400));
        pack();
        setLocationRelativeTo(owner);
    }

    private JPanel buildFormPanel() {
        JPanel form = new JPanel(new GridBagLayout());
        form.setBorder(new EmptyBorder(8, 8, 8, 8));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 4, 4, 4);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.gridx = 0;
        gbc.gridy = 0;

        form.add(new JLabel("N° automático:"), gbc);
        gbc.gridx = 1;
        gbc.weightx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        form.add(lblNumero, gbc);

        gbc.gridy++;
        gbc.gridx = 0;
        gbc.weightx = 0;
        gbc.fill = GridBagConstraints.NONE;
        form.add(new JLabel("Solicitud:"), gbc);
        gbc.gridx = 1;
        gbc.weightx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        form.add(txtSolicitud, gbc);

        gbc.gridy++;
        gbc.gridx = 0;
        gbc.weightx = 0;
        gbc.fill = GridBagConstraints.NONE;
        form.add(new JLabel("Solicitante:"), gbc);
        gbc.gridx = 1;
        gbc.weightx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        form.add(txtSolicitante, gbc);

        gbc.gridy++;
        gbc.gridx = 0;
        gbc.weightx = 0;
        gbc.fill = GridBagConstraints.NONE;
        form.add(new JLabel("Descripción:"), gbc);
        gbc.gridx = 1;
        gbc.weightx = 1;
        gbc.fill = GridBagConstraints.BOTH;
        JScrollPane descripcionScroll = new JScrollPane(txtDescripcion);
        descripcionScroll.setPreferredSize(new Dimension(0, 90));
        form.add(descripcionScroll, gbc);

        gbc.gridy++;
        gbc.gridx = 0;
        gbc.weightx = 0;
        gbc.fill = GridBagConstraints.NONE;
        form.add(new JLabel("Destino:"), gbc);
        gbc.gridx = 1;
        gbc.weightx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        form.add(txtDestino, gbc);

        return form;
    }

    private void updateState() {
        lblTotales.setText(model.totales());
        boolean hasSolicitud = txtSolicitud.getText() != null && !txtSolicitud.getText().trim().isEmpty();
        btnGuardar.setEnabled(hasSolicitud && model.isValidRows());
    }

    private void onGuardar() {
        String solicitud = txtSolicitud.getText() != null ? txtSolicitud.getText().trim() : "";
        if (solicitud.isEmpty()) {
            Ui.warn(this, "La solicitud es obligatoria.");
            return;
        }
        if (!model.isValidRows()) {
            Ui.warn(this, "Completá correctamente todas las filas.");
            return;
        }
        List<TramiteService.LineaTramite> lineas = model.toLineas();
        if (lineas.isEmpty()) {
            Ui.warn(this, "Agregá al menos un insumo.");
            return;
        }
        String solicitante = txtSolicitante.getText() != null ? txtSolicitante.getText().trim() : "";
        String descripcion = txtDescripcion.getText() != null ? txtDescripcion.getText().trim() : "";
        String destino = txtDestino.getText() != null ? txtDestino.getText().trim() : "";
        this.solicitud = solicitud;
        this.solicitante = solicitante.isEmpty() ? null : solicitante;
        this.descripcion = descripcion.isEmpty() ? null : descripcion;
        this.destino = destino.isEmpty() ? null : destino;
        this.lineasSeleccionadas = java.util.Collections.unmodifiableList(new ArrayList<>(lineas));
        accepted = true;
        dispose();
    }

    public boolean isAccepted() {
        return accepted;
    }

    public boolean isReady() {
        return ready;
    }

    public String getSolicitud() {
        return solicitud;
    }

    public String getSolicitante() {
        return solicitante;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public String getDestino() {
        return destino;
    }

    public List<TramiteService.LineaTramite> getLineas() {
        return lineasSeleccionadas;
    }

    private int stockDisponible(Insumo insumo) {
        if (insumo == null) return 0;
        BigDecimal stock = insumo.getStockActual();
        if (stock == null) return 0;
        return stock.setScale(0, RoundingMode.FLOOR).intValue();
    }

    private class LineaTableModel extends AbstractTableModel {
        private final String[] cols = {"Insumo", "Stock disponible", "Cantidad"};
        private final List<Fila> filas = new ArrayList<>();

        void addEmptyRow() {
            Insumo insumo = insumosDisponibles.stream()
                    .filter(i -> filas.stream().noneMatch(f -> f.insumo != null && f.insumo.getId().equals(i.getId())))
                    .findFirst()
                    .orElse(null);
            if (insumo == null) {
                Ui.warn(RegistrarTramiteDialog.this, "Ya agregaste todos los insumos disponibles.");
                return;
            }
            filas.add(new Fila(insumo, 1));
            fireTableRowsInserted(filas.size() - 1, filas.size() - 1);
        }

        void removeSelected(int row) {
            if (row >= 0 && row < filas.size()) {
                filas.remove(row);
                fireTableRowsDeleted(row, row);
            }
        }

        boolean isValidRows() {
            if (filas.isEmpty()) return false;
            for (Fila f : filas) {
                if (f.insumo == null) return false;
                int max = stockDisponible(f.insumo);
                if (f.cantidad <= 0 || f.cantidad > max) return false;
            }
            return true;
        }

        String totales() {
            int items = filas.size();
            int unidades = filas.stream().mapToInt(f -> f.cantidad).sum();
            return String.format("%d ítems · %d unidades", items, unidades);
        }

        List<TramiteService.LineaTramite> toLineas() {
            List<TramiteService.LineaTramite> list = new ArrayList<>();
            for (Fila f : filas) {
                list.add(new TramiteService.LineaTramite(f.insumo.getId(), f.cantidad));
            }
            return list;
        }

        @Override
        public int getRowCount() { return filas.size(); }

        @Override
        public int getColumnCount() { return cols.length; }

        @Override
        public String getColumnName(int column) { return cols[column]; }

        @Override
        public Class<?> getColumnClass(int columnIndex) {
            return switch (columnIndex) {
                case 0 -> Insumo.class;
                case 1, 2 -> Integer.class;
                default -> Object.class;
            };
        }

        @Override
        public boolean isCellEditable(int rowIndex, int columnIndex) {
            return columnIndex != 1;
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            Fila f = filas.get(rowIndex);
            return switch (columnIndex) {
                case 0 -> f.insumo;
                case 1 -> stockDisponible(f.insumo);
                case 2 -> f.cantidad;
                default -> null;
            };
        }

        @Override
        public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
            Fila f = filas.get(rowIndex);
            if (columnIndex == 0 && aValue instanceof Insumo nuevo) {
                for (int i = 0; i < filas.size(); i++) {
                    if (i != rowIndex && filas.get(i).insumo != null
                            && filas.get(i).insumo.getId().equals(nuevo.getId())) {
                        Ui.warn(RegistrarTramiteDialog.this, "Ese insumo ya fue agregado.");
                        return;
                    }
                }
                f.insumo = nuevo;
                int max = stockDisponible(nuevo);
                if (f.cantidad > max) {
                    f.cantidad = Math.max(1, max);
                }
            } else if (columnIndex == 2 && aValue instanceof Number n) {
                f.cantidad = n.intValue();
            }
            fireTableRowsUpdated(rowIndex, rowIndex);
        }
    }

    private static class Fila {
        Insumo insumo;
        int cantidad;

        Fila(Insumo insumo, int cantidad) {
            this.insumo = insumo;
            this.cantidad = cantidad;
        }
    }

    private class InsumoCellEditor extends AbstractCellEditor implements TableCellEditor {
        private final JComboBox<Insumo> combo;

        InsumoCellEditor() {
            combo = new JComboBox<>(insumosDisponibles.toArray(new Insumo[0]));
            combo.setRenderer(new DefaultListCellRenderer() {
                @Override
                public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                    Component c = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                    if (value instanceof Insumo ins) {
                        String nombre = ins.getDescripcion() != null ? ins.getDescripcion() : ins.getCodigo();
                        int stock = stockDisponible(ins);
                        setText(String.format("%s (stock: %d)", nombre, stock));
                    }
                    return c;
                }
            });
        }

        @Override
        public Object getCellEditorValue() {
            return combo.getSelectedItem();
        }

        @Override
        public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
            combo.setSelectedItem(value);
            return combo;
        }
    }

    private class CantidadCellEditor extends AbstractCellEditor implements TableCellEditor {
        private final JSpinner spinner = new JSpinner();

        CantidadCellEditor() {
            spinner.setModel(new SpinnerNumberModel(1, 1, Integer.MAX_VALUE, 1));
        }

        @Override
        public Object getCellEditorValue() {
            return spinner.getValue();
        }

        @Override
        public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
            Insumo ins = (Insumo) table.getValueAt(row, 0);
            int max = Math.max(1, stockDisponible(ins));
            SpinnerNumberModel model = (SpinnerNumberModel) spinner.getModel();
            model.setMaximum(max);
            model.setMinimum(1);
            model.setValue(Math.min(value instanceof Number n ? n.intValue() : 1, max));
            return spinner;
        }
    }
}
