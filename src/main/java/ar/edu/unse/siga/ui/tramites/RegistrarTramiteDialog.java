package ar.edu.unse.siga.ui.tramites;

import ar.edu.unse.siga.domain.Insumo;
import ar.edu.unse.siga.domain.Ubicacion;
import ar.edu.unse.siga.service.InventarioService;
import ar.edu.unse.siga.service.TramiteService;
import ar.edu.unse.siga.ui.base.Ui;

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
import java.util.Comparator;
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
        this.insumosDisponibles = new ArrayList<>(inventarioService.listarTodos());
        this.insumosDisponibles.removeIf(i -> i == null || (i.getEstado() != null
                && i.getEstado().equalsIgnoreCase("INACTIVO")));
        this.insumosDisponibles.sort(Comparator.comparing(this::labelForInsumo,
                String.CASE_INSENSITIVE_ORDER));
        this.ready = true;

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

        // Editor para seleccionar o escribir el nombre
        table.getColumnModel().getColumn(0).setCellEditor(new NombreCellEditor());
        // Editor de cantidad
        table.getColumnModel().getColumn(2).setCellEditor(new CantidadCellEditor());

        add(new JScrollPane(table), BorderLayout.CENTER);

        JPanel south = new JPanel(new BorderLayout());
        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT));
        left.add(lblTotales);
        south.add(left, BorderLayout.WEST);

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton btnAgregar = new JButton("Agregar renglón");
        JButton btnPedidoEspecial = new JButton("Agregar pedido especial");
        JButton btnQuitar = new JButton("Quitar renglón");
        right.add(btnAgregar);
        right.add(btnPedidoEspecial);
        right.add(btnQuitar);
        right.add(btnRegistrar);
        south.add(right, BorderLayout.EAST);
        add(south, BorderLayout.SOUTH);

        btnAgregar.addActionListener(e -> {
            model.addEmptyRow();
            actualizarTotales();
        });
        btnPedidoEspecial.addActionListener(e -> onAgregarPedidoEspecial());
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
            TramiteService.RegistroTramite resultado = tramiteService.registrarNuevoTramite(
                    solicitud,
                    solicitante.isEmpty() ? null : solicitante,
                    descripcion.isEmpty() ? null : descripcion,
                    destino,
                    lineas
            );

            StringBuilder info = new StringBuilder();
            info.append("Solicitudes registradas (base ")
                    .append(resultado.getNumeroBase())
                    .append("):\n");
            resultado.getTramites().forEach(t -> info.append("• #")
                    .append(t.getNro())
                    .append(" · ")
                    .append(t.getEstado())
                    .append('\n'));

            Ui.info(this, info.toString());
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

    private void onAgregarPedidoEspecial() {
        String nombre = JOptionPane.showInputDialog(this,
                "Detalle del pedido especial:",
                "Agregar pedido especial",
                JOptionPane.PLAIN_MESSAGE);
        if (nombre == null) {
            return;
        }
        String cleaned = nombre.trim();
        if (cleaned.isEmpty()) {
            Ui.warn(this, "Ingresá un nombre para el pedido especial.");
            return;
        }
        model.addPedidoEspecial(cleaned);
        actualizarTotales();
    }

    private String labelForInsumo(Insumo insumo) {
        if (insumo == null) {
            return "";
        }
        String codigo = insumo.getCodigo() == null ? "" : insumo.getCodigo().trim();
        String desc = insumo.getDescripcion() == null ? "" : insumo.getDescripcion().trim();
        if (!codigo.isEmpty() && !desc.isEmpty()) {
            return codigo + " - " + desc;
        }
        if (!codigo.isEmpty()) {
            return codigo;
        }
        return desc;
    }

    private void loadDestinosPredefinidos() {
        cboDestino.removeAllItems();
        if (inventarioService != null) {
            List<Ubicacion> ubicaciones = inventarioService.listarUbicaciones();
            ubicaciones.stream()
                    .map(Ubicacion::getNombre)
                    .filter(n -> n != null && !n.isBlank())
                    .distinct()
                    .forEach(cboDestino::addItem);
        }
        if (cboDestino.getItemCount() == 0) {
            cboDestino.addItem("Secretaría FCEyT");
            cboDestino.addItem("Compras");
            cboDestino.addItem("Finanzas");
            cboDestino.addItem("Mantenimiento");
        }
        cboDestino.setSelectedIndex(0);
    }

    private int stockDisponible(Insumo insumo) {
        if (insumo == null || insumo.getId() == null || inventarioService == null) return 0;
        BigDecimal stock = inventarioService.stockActualExacto(insumo.getId());
        if (stock == null) return 0;
        return stock.setScale(0, RoundingMode.DOWN).intValue();
    }

    // ======= Tabla de líneas =======
    private class LineaTableModel extends AbstractTableModel {
        private final String[] cols = {"Nombre", "Stock disponible", "Cantidad"};
        private final List<Fila> filas = new ArrayList<>();

        void addEmptyRow() {
            filas.add(new Fila(null, null, 1));
            fireTableRowsInserted(filas.size() - 1, filas.size() - 1);
        }

        void addPedidoEspecial(String descripcion) {
            filas.add(new Fila(null, descripcion, 1));
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
                if (f.esPedidoEspecial()) {
                    if (f.especial == null || f.especial.isBlank()) {
                        return false;
                    }
                } else {
                    if (f.insumo == null) {
                        return false;
                    }
                }
                if (f.cantidad <= 0) {
                    return false;
                }
            }
            return true;
        }

        List<TramiteService.LineaTramite> toLineas() {
            List<TramiteService.LineaTramite> list = new ArrayList<>();
            for (Fila f : filas) {
                if (f.cantidad <= 0) {
                    continue;
                }
                if (f.esPedidoEspecial()) {
                    list.add(TramiteService.LineaTramite.pedidoEspecial(f.especial, f.cantidad));
                } else if (f.insumo != null) {
                    list.add(TramiteService.LineaTramite.deInsumo(f.insumo.getId(), labelForInsumo(f.insumo), f.cantidad));
                }
            }
            return list;
        }

        int totalUnidades() {
            int sum = 0;
            for (Fila f : filas) sum += Math.max(0, f.cantidad);
            return sum;
        }

        boolean isPedidoEspecial(int row) {
            if (row < 0 || row >= filas.size()) return false;
            return filas.get(row).esPedidoEspecial();
        }

        String getEspecialDescripcion(int row) {
            if (row < 0 || row >= filas.size()) return "";
            String texto = filas.get(row).especial;
            return texto == null ? "" : texto;
        }

        Insumo getInsumo(int row) {
            if (row < 0 || row >= filas.size()) return null;
            return filas.get(row).insumo;
        }

        @Override public int getRowCount() { return filas.size(); }
        @Override public int getColumnCount() { return cols.length; }
        @Override public String getColumnName(int column) { return cols[column]; }
        @Override public Class<?> getColumnClass(int columnIndex) {
            return switch (columnIndex) {
                case 2 -> Integer.class;
                default -> String.class;
            };
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            Fila f = filas.get(rowIndex);
            return switch (columnIndex) {
                case 0 -> f.esPedidoEspecial() ? f.especial : labelForInsumo(f.insumo);
                case 1 -> f.esPedidoEspecial() ? "-" : String.valueOf(stockDisponible(f.insumo));
                case 2 -> f.cantidad;
                default -> null;
            };
        }

        @Override
        public boolean isCellEditable(int rowIndex, int columnIndex) {
            return columnIndex != 1;
        }

        @Override
        public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
            Fila f = filas.get(rowIndex);
            if (columnIndex == 0) {
                if (aValue instanceof Insumo) {
                    Insumo nuevo = (Insumo) aValue;
                    for (int i = 0; i < filas.size(); i++) {
                        if (i != rowIndex && filas.get(i).insumo != null
                                && filas.get(i).insumo.getId().equals(nuevo.getId())) {
                            Ui.warn(RegistrarTramiteDialog.this, "Ese insumo ya fue agregado.");
                            fireTableRowsUpdated(rowIndex, rowIndex);
                            return;
                        }
                    }
                    f.insumo = nuevo;
                    f.especial = null;
                    if (f.cantidad <= 0) f.cantidad = 1;
                    fireTableRowsUpdated(rowIndex, rowIndex);
                } else if (aValue != null) {
                    String texto = String.valueOf(aValue).trim();
                    if (texto.isEmpty()) {
                        Ui.warn(RegistrarTramiteDialog.this, "Ingresá un nombre válido.");
                        return;
                    }
                    f.especial = texto;
                    f.insumo = null;
                    if (f.cantidad <= 0) f.cantidad = 1;
                    fireTableRowsUpdated(rowIndex, rowIndex);
                }
            } else if (columnIndex == 2) {
                try {
                    int val = Integer.parseInt(String.valueOf(aValue));
                    if (val <= 0) {
                        Ui.warn(RegistrarTramiteDialog.this, "Cantidad inválida.");
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
            String especial;
            int cantidad;
            Fila(Insumo i, String especial, int c) { this.insumo = i; this.especial = especial; this.cantidad = c; }
            boolean esPedidoEspecial() { return especial != null && !especial.isBlank(); }
        }
    }

    // ======= Editores =======
    private class NombreCellEditor extends AbstractCellEditor implements TableCellEditor {
        private final JComboBox<Insumo> combo = new JComboBox<>();
        private final JTextField textField = new JTextField();
        private boolean modoEspecial = false;

        public NombreCellEditor() {
            for (Insumo i : insumosDisponibles) combo.addItem(i);
            combo.setRenderer(new DefaultListCellRenderer() {
                @Override
                public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                    Component c = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                    if (value instanceof Insumo) {
                        setText(labelForInsumo((Insumo) value));
                    }
                    return c;
                }
            });
            textField.setBorder(BorderFactory.createEmptyBorder(0, 6, 0, 6));
        }

        @Override public Object getCellEditorValue() {
            if (modoEspecial) {
                return textField.getText();
            }
            return combo.getSelectedItem();
        }

        @Override public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
            modoEspecial = model.isPedidoEspecial(row);
            if (modoEspecial) {
                textField.setText(model.getEspecialDescripcion(row));
                SwingUtilities.invokeLater(textField::selectAll);
                return textField;
            }
            Insumo actual = model.getInsumo(row);
            if (actual != null) {
                combo.setSelectedItem(actual);
            }
            return combo;
        }
    }

    private class CantidadCellEditor extends AbstractCellEditor implements TableCellEditor {
        private final JTextField field = new JTextField();

        private CantidadCellEditor() {
            field.setHorizontalAlignment(JTextField.RIGHT);
            field.setBorder(BorderFactory.createEmptyBorder(0, 6, 0, 6));
            field.setForeground(Color.BLACK);
            field.setBackground(Color.WHITE);
            field.setCaretColor(Color.BLACK);
        }

        @Override public Object getCellEditorValue() { return field.getText(); }

        @Override public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
            field.setText(value == null ? "1" : String.valueOf(value));
            SwingUtilities.invokeLater(field::selectAll);
            return field;
        }
    }
}
