package ar.edu.unse.siga.ui.tramites;

import ar.edu.unse.siga.domain.Insumo;
import ar.edu.unse.siga.service.TramiteService;
import ar.edu.unse.siga.ui.base.Ui;

import javax.swing.*;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableCellEditor;
import java.awt.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

public class RegistrarTramiteDialog extends JDialog {

    private final TramiteService tramiteService;
    private final JTable table = new JTable();
    private final LineaTableModel model = new LineaTableModel();
    private final JButton btnGuardar = new JButton("Guardar");
    private final JLabel lblTotales = new JLabel("0 ítems · 0 unidades");
    private boolean accepted = false;
    private final List<Insumo> insumosDisponibles;
    private final boolean ready;

    public RegistrarTramiteDialog(Window owner, TramiteService tramiteService) {
        super(owner, "Registrar trámite de salida", ModalityType.APPLICATION_MODAL);
        this.tramiteService = tramiteService;
        this.insumosDisponibles = new ArrayList<>(tramiteService.insumosConStockDisponible());
        this.ready = !insumosDisponibles.isEmpty();

        if (!ready) {
            Ui.warn(owner, "No hay insumos con stock disponible.");
            accepted = false;
            dispose();
            return;
        }

        setLayout(new BorderLayout(10, 10));
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

        model.addEmptyRow();
        updateState();

        setPreferredSize(new Dimension(720, 400));
        pack();
        setLocationRelativeTo(owner);
    }

    private void updateState() {
        lblTotales.setText(model.totales());
        btnGuardar.setEnabled(model.isValidRows());
    }

    private void onGuardar() {
        if (!model.isValidRows()) {
            Ui.warn(this, "Completá correctamente todas las filas.");
            return;
        }
        List<TramiteService.LineaTramite> lineas = model.toLineas();
        try {
            Long id = tramiteService.registrarNuevoTramite(lineas);
            Ui.info(this, "Trámite registrado. ID: " + id);
            accepted = true;
            dispose();
        } catch (IllegalStateException ex) {
            Ui.warn(this, ex.getMessage());
        } catch (Exception ex) {
            Ui.error(this, ex);
        }
    }

    public boolean isAccepted() {
        return accepted;
    }

    public boolean isReady() {
        return ready;
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
