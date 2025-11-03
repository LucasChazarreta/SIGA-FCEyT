package ar.edu.unse.siga.ui.tramites;

import ar.edu.unse.siga.domain.Insumo;
import ar.edu.unse.siga.service.InventarioService;
import ar.edu.unse.siga.service.TramiteService;
import ar.edu.unse.siga.ui.base.Ui;
import ar.edu.unse.siga.persistence.DataSourceFactory;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellEditor;
import java.awt.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

public class RegistrarTramiteDialog extends JDialog {

    private final TramiteService tramiteService;
    private final InventarioService inventarioService;

    private final JTable table = new JTable();
    private final LineaTableModel model = new LineaTableModel();
    private final JButton btnRegistrar = new JButton("Registrar");
    private final JLabel lblTotales = new JLabel("0 ítems · 0 unidades");

    private boolean accepted = false;
    private final JTextField txtSolicitud = new JTextField(30);
    private final JTextField txtSolicitante = new JTextField(25);
    private final JTextArea txtDescripcion = new JTextArea(4, 25);
    private final JComboBox<String> cboDestino = new JComboBox<>();
    private final JLabel lblNumero = new JLabel();

    private final List<Insumo> insumosDisponibles;
    private final boolean ready;

    public RegistrarTramiteDialog(Window owner, TramiteService tramiteService, InventarioService inventarioService) {
        super(owner, "Registrar solicitud", ModalityType.APPLICATION_MODAL);
        this.tramiteService = tramiteService;
        this.inventarioService = inventarioService;
        this.insumosDisponibles = new ArrayList<>(inventarioService.insumosConStockDisponible());
        this.ready = !insumosDisponibles.isEmpty();

        if (!ready) {
            Ui.warn(this, "No hay insumos con stock disponible para registrar.");
        }

        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setLayout(new BorderLayout(10, 10));
        ((JComponent) getContentPane()).setBorder(new EmptyBorder(10, 10, 10, 10));

        txtDescripcion.setLineWrap(true);
        txtDescripcion.setWrapStyleWord(true);
        lblNumero.setText("Se generará al guardar");
        lblNumero.setFont(lblNumero.getFont().deriveFont(Font.BOLD));

        // Cargar destinos desde BD (movimientos SALIDA)
        loadDestinosPredefinidos();

        add(buildFormPanel(), BorderLayout.NORTH);

        table.setModel(model);
        table.setRowHeight(26);
        table.putClientProperty("terminateEditOnFocusLost", Boolean.TRUE);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
        table.setFillsViewportHeight(true);

        // Editor para seleccionar Insumo
        table.getColumnModel().getColumn(0).setCellEditor(new InsumoCellEditor());
        // Editor de cantidad
        table.getColumnModel().getColumn(2).setCellEditor(new CantidadCellEditor());

        // Renderer que muestra "codigo - descripcion" para Insumo
        table.setDefaultRenderer(Insumo.class, new DefaultTableCellRenderer() {
            @Override public void setValue(Object value) {
                if (value instanceof Insumo) {
                    Insumo i = (Insumo) value;
                    String codigo = i.getCodigo() == null ? "" : i.getCodigo().trim();
                    String desc   = i.getDescripcion() == null ? "" : i.getDescripcion().trim();
                    String label;
                    if (!codigo.isEmpty() && !desc.isEmpty())      label = codigo + " - " + desc;
                    else if (!codigo.isEmpty())                    label = codigo;
                    else if (!desc.isEmpty())                      label = desc;
                    else                                           label = "";
                    setText(label);
                } else {
                    setText("");
                }
            }
        });

        add(new JScrollPane(table), BorderLayout.CENTER);

        JPanel south = new JPanel(new BorderLayout());
        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT));
        left.add(lblTotales);
        south.add(left, BorderLayout.WEST);

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton btnAgregar = new JButton("Agregar renglón");
        JButton btnQuitar = new JButton("Quitar renglón");
        right.add(btnAgregar);
        right.add(btnQuitar);
        right.add(btnRegistrar);
        south.add(right, BorderLayout.EAST);
        add(south, BorderLayout.SOUTH);

        btnAgregar.addActionListener(e -> {
            model.addEmptyRow();
            actualizarTotales();
        });
        btnQuitar.addActionListener(e -> {
            int row = table.getSelectedRow();
            if (row >= 0) {
                model.removeSelected(row);
                actualizarTotales();
            }
        });
        btnRegistrar.addActionListener(e -> onGuardar());

        model.addTableModelListener(new TableModelListener() {
            @Override public void tableChanged(TableModelEvent e) { actualizarTotales(); }
        });

        pack();
        setLocationRelativeTo(owner);
        setMinimumSize(new Dimension(780, 520));
    }

    public boolean isAccepted() { return accepted; }
    public boolean isReady() { return ready; }

    private JPanel buildFormPanel() {
        JPanel form = new JPanel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(4,4,4,4);
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 0;

        int r = 0;

        // Nro (solo informativo)
        c.gridx = 0; c.gridy = r; form.add(new JLabel("Nro:"), c);
        c.gridx = 1; c.gridy = r; c.gridwidth = 3; c.weightx = 1; form.add(lblNumero, c);
        c.gridwidth = 1; c.weightx = 0; r++;

        c.gridx = 0; c.gridy = r; form.add(new JLabel("Solicitud:"), c);
        c.gridx = 1; c.gridy = r; c.gridwidth = 3; c.weightx = 1; form.add(txtSolicitud, c);
        c.gridwidth = 1; c.weightx = 0; r++;

        c.gridx = 0; c.gridy = r; form.add(new JLabel("Solicitante:"), c);
        c.gridx = 1; c.gridy = r; c.gridwidth = 1; c.weightx = 1; form.add(txtSolicitante, c);
        c.gridx = 2; c.gridy = r; form.add(new JLabel("Destino:"), c);
        c.gridx = 3; c.gridy = r; form.add(cboDestino, c);
        r++;

        c.gridx = 0; c.gridy = r; c.anchor = GridBagConstraints.NORTHWEST; form.add(new JLabel("Descripción:"), c);
        c.gridx = 1; c.gridy = r; c.gridwidth = 3; c.weightx = 1; form.add(new JScrollPane(txtDescripcion), c);
        c.gridwidth = 1; c.weightx = 0; r++;

        return form;
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
        String destino = (String) cboDestino.getSelectedItem();
        if (destino == null || destino.trim().isEmpty()) {
            Ui.warn(this, "Seleccioná un destino.");
            return;
        }

        try {
            Long id = tramiteService.registrarNuevoTramite(
                    solicitud,
                    solicitante.isEmpty() ? null : solicitante,
                    descripcion.isEmpty() ? null : descripcion,
                    destino,
                    lineas
            );
            Ui.info(this, "Solicitud registrada. ID: " + id);
            accepted = true;
            dispose();
        } catch (IllegalStateException ex) {
            Ui.warn(this, ex.getMessage());
        } catch (Exception ex) {
            Ui.error(this, ex);
        }
    }

    // ========= helpers =========
    private void actualizarTotales() {
        int filas = model.getRowCount();
        int unidades = model.totalUnidades();
        lblTotales.setText(filas + " ítems · " + unidades + " unidades");
    }

    private void loadDestinosPredefinidos() {
        // Busca destinos existentes en 'movimiento.destino_fuente' para SALIDA
        LinkedHashSet<String> set = new LinkedHashSet<>();
        final String sql = """
            SELECT DISTINCT destino_fuente
            FROM movimiento
            WHERE tipo='SALIDA' AND destino_fuente IS NOT NULL AND TRIM(destino_fuente) <> ''
            ORDER BY destino_fuente
            """;
        try (Connection cn = DataSourceFactory.getConnection();
             PreparedStatement ps = cn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                String d = rs.getString(1);
                if (d != null && !d.isBlank()) set.add(d.trim());
            }
        } catch (Exception ignored) {
            // si falla, seguimos con defaults
        }
        if (set.isEmpty()) {
            set.add("Secretaría FCEyT");
            set.add("Compras");
            set.add("Finanzas");
            set.add("Mantenimiento");
        }
        cboDestino.removeAllItems();
        for (String d : set) cboDestino.addItem(d);
        cboDestino.setSelectedIndex(0);
    }

    private int stockDisponible(Insumo insumo) {
        if (insumo == null) return 0;
        BigDecimal stock = insumo.getStockActual();
        if (stock == null) return 0;
        return stock.setScale(0, RoundingMode.FLOOR).intValue();
    }

    // ======= Tabla de líneas =======
    private class LineaTableModel extends AbstractTableModel {
        private final String[] cols = {"Insumo", "Stock disponible", "Cantidad"};
        private final List<Fila> filas = new ArrayList<>();

        void addEmptyRow() {
            if (filas.size() >= insumosDisponibles.size()) {
                Ui.warn(RegistrarTramiteDialog.this, "Ya agregaste todos los insumos disponibles.");
                return;
            }
            filas.add(new Fila(null, 1));
            fireTableRowsInserted(filas.size() - 1, filas.size() - 1);
        }

        void removeSelected(int row) {
            if (row >= 0 && row < filas.size()) {
                filas.remove(row);
                fireTableRowsDeleted(row, row);
            }
        }

        boolean isValidRows() {
            for (Fila f : filas) {
                if (f.insumo == null) return false;
                if (f.cantidad <= 0) return false;
                if (f.cantidad > stockDisponible(f.insumo)) return false;
            }
            return true;
        }

        List<TramiteService.LineaTramite> toLineas() {
            List<TramiteService.LineaTramite> list = new ArrayList<>();
            for (Fila f : filas) {
                if (f.insumo != null && f.cantidad > 0) {
                    list.add(new TramiteService.LineaTramite(f.insumo.getId(), f.cantidad));
                }
            }
            return list;
        }

        int totalUnidades() {
            int sum = 0;
            for (Fila f : filas) sum += Math.max(0, f.cantidad);
            return sum;
        }

        @Override public int getRowCount() { return filas.size(); }
        @Override public int getColumnCount() { return cols.length; }
        @Override public String getColumnName(int column) { return cols[column]; }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            Fila f = filas.get(rowIndex);
            switch (columnIndex) {
                case 0: return f.insumo;
                case 1: return f.insumo == null ? 0 : stockDisponible(f.insumo);
                case 2: return f.cantidad;
            }
            return null;
        }

        @Override
        public boolean isCellEditable(int rowIndex, int columnIndex) { return columnIndex != 1; }

        @Override
        public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
            Fila f = filas.get(rowIndex);
            if (columnIndex == 0 && aValue instanceof Insumo) {
                Insumo nuevo = (Insumo) aValue;
                // evitar duplicados
                for (int i = 0; i < filas.size(); i++) {
                    if (i != rowIndex && filas.get(i).insumo != null
                            && filas.get(i).insumo.getId().equals(nuevo.getId())) {
                        Ui.warn(RegistrarTramiteDialog.this, "Ese insumo ya fue agregado.");
                        fireTableRowsUpdated(rowIndex, rowIndex);
                        return;
                    }
                }
                int max = stockDisponible(nuevo);
                if (max <= 0) {
                    Ui.warn(RegistrarTramiteDialog.this, "El insumo seleccionado no tiene stock disponible.");
                    return;
                }
                f.insumo = nuevo;
                if (f.cantidad <= 0) f.cantidad = 1;
                fireTableRowsUpdated(rowIndex, rowIndex);
            } else if (columnIndex == 2) {
                try {
                    int val = Integer.parseInt(String.valueOf(aValue));
                    if (val <= 0) {
                        Ui.warn(RegistrarTramiteDialog.this, "Cantidad inválida.");
                        return;
                    }
                    if (f.insumo != null && val > stockDisponible(f.insumo)) {
                        Ui.warn(RegistrarTramiteDialog.this, "No hay stock suficiente.");
                        return;
                    }
                    f.cantidad = val;
                    fireTableRowsUpdated(rowIndex, rowIndex);
                } catch (NumberFormatException ex) {
                    Ui.warn(RegistrarTramiteDialog.this, "Cantidad inválida.");
                }
            }
        }

        class Fila {
            Insumo insumo;
            int cantidad;
            Fila(Insumo i, int c) { this.insumo = i; this.cantidad = c; }
        }
    }

    // ======= Editores =======
    private class InsumoCellEditor extends AbstractCellEditor implements TableCellEditor {
        private final JComboBox<Insumo> combo = new JComboBox<>();
        public InsumoCellEditor() {
            for (Insumo i : insumosDisponibles) combo.addItem(i);
            // Renderer del combo: "codigo - descripcion"
            combo.setRenderer(new DefaultListCellRenderer() {
                @Override
                public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                    Component c = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                    if (value instanceof Insumo) {
                        Insumo i = (Insumo) value;
                        String codigo = i.getCodigo() == null ? "" : i.getCodigo().trim();
                        String desc   = i.getDescripcion() == null ? "" : i.getDescripcion().trim();
                        String label = (!codigo.isEmpty() && !desc.isEmpty()) ? (codigo + " - " + desc)
                                     : (!codigo.isEmpty() ? codigo : desc);
                        setText(label);
                    }
                    return c;
                }
            });
        }
        @Override public Object getCellEditorValue() { return combo.getSelectedItem(); }
        @Override public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
            if (value instanceof Insumo) combo.setSelectedItem(value);
            return combo;
        }
    }

    private class CantidadCellEditor extends AbstractCellEditor implements TableCellEditor {
        private final JTextField field = new JTextField();
        @Override public Object getCellEditorValue() { return field.getText(); }
        @Override public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
            field.setText(value == null ? "1" : String.valueOf(value));
            return field;
        }
    }
}
