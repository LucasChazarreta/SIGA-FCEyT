package ar.edu.unse.siga.ui.pages;

import ar.edu.unse.siga.common.CurrentSession;
import ar.edu.unse.siga.domain.Categoria;
import ar.edu.unse.siga.common.RoleName;
import ar.edu.unse.siga.domain.Insumo;
import ar.edu.unse.siga.domain.Ubicacion;
import ar.edu.unse.siga.domain.Usuario;
import ar.edu.unse.siga.service.InventarioService;
import ar.edu.unse.siga.ui.base.CardPanel;
import ar.edu.unse.siga.ui.base.Ui;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.*;
import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public class InventoryPage extends JPanel {

    private static final Dimension BUTTON_SIZE = new Dimension(180, 42);

    private final InventarioService service;
    private final CardLayout cardLayout = new CardLayout();
    private final JPanel cards = new JPanel(cardLayout);
    private JToggleButton tabRegistrar;
    private JToggleButton tabModificar;
    private JToggleButton tabEliminar;

    private final RegistroPanel registroPanel;
    private final ModificarPanel modificarPanel;
    private final EliminarPanel eliminarPanel;

    public InventoryPage(InventarioService service) {
        this.service = service;

        // Inicializar los subpaneles *después* de tener el service listo
        this.registroPanel = new RegistroPanel();
        this.modificarPanel = new ModificarPanel();
        this.eliminarPanel = new EliminarPanel();

        setOpaque(false);
        setLayout(new BorderLayout(16, 16));
        add(buildHeader(), BorderLayout.NORTH);
        add(buildContent(), BorderLayout.CENTER);
    }

    private JComponent buildHeader() {
        JPanel header = new JPanel(new BorderLayout(12, 12));
        header.setOpaque(false);

        JLabel title = new JLabel("Gestión de inventario");
        title.setFont(title.getFont().deriveFont(Font.BOLD, 28f));
        title.setForeground(new Color(24, 63, 150));
        header.add(title, BorderLayout.WEST);

        return header;
    }

    private JComponent buildContent() {
        JPanel wrapper = new JPanel(new BorderLayout(16, 24));
        wrapper.setOpaque(false);

        ButtonGroup group = new ButtonGroup();
        JPanel tabs = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        tabs.setOpaque(false);

        tabRegistrar = pillButton("Cargar");
        tabModificar = pillButton("Modificar");
        tabEliminar = pillButton("Eliminar");

        tabRegistrar.setSelected(true);
        group.add(tabRegistrar);
        group.add(tabModificar);
        group.add(tabEliminar);

        tabs.add(tabRegistrar);
        tabs.add(tabModificar);
        tabs.add(tabEliminar);

        wrapper.add(tabs, BorderLayout.NORTH);

        cards.setOpaque(false);
        cards.add(registroPanel, "reg");
        cards.add(modificarPanel, "mod");
        cards.add(eliminarPanel, "del");
        wrapper.add(cards, BorderLayout.CENTER);

        tabRegistrar.addActionListener(e -> {
            registroPanel.refreshCombos();
            cardLayout.show(cards, "reg");
        });
        tabModificar.addActionListener(e -> {
            modificarPanel.refreshData();
            cardLayout.show(cards, "mod");
        });
        tabEliminar.addActionListener(e -> {
            eliminarPanel.refreshData();
            cardLayout.show(cards, "del");
        });

        cardLayout.show(cards, "reg");
        return wrapper;
    }

    public void mostrarInsumosBajoMinimo() {
        SwingUtilities.invokeLater(() -> {
            if (tabModificar != null) {
                tabModificar.setSelected(true);
            }
            cardLayout.show(cards, "mod");
            modificarPanel.mostrarSoloBajoMinimo();
        });
    }

    public void refreshAll() {
        registroPanel.refreshCombos();
        modificarPanel.refreshData();
        eliminarPanel.refreshData();
    }

    private JToggleButton pillButton(String text) {
        JToggleButton b = new JToggleButton(text.toUpperCase());
        b.setFocusPainted(false);
        b.setFont(b.getFont().deriveFont(Font.BOLD, 12f));
        b.setOpaque(true);
        b.setBackground(Color.WHITE);
        b.setForeground(new Color(66, 100, 189));
        b.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(211, 222, 255)),
                BorderFactory.createEmptyBorder(10, 22, 10, 22)
        ));
        b.putClientProperty("JComponent.minimumWidth", 140);
        b.putClientProperty("JComponent.minimumHeight", 38);
        b.addChangeListener(e -> {
            if (b.isSelected()) {
                b.setBackground(new Color(58, 96, 224));
                b.setForeground(Color.WHITE);
            } else {
                b.setBackground(Color.WHITE);
                b.setForeground(new Color(66, 100, 189));
            }
        });
        return b;
    }

    private List<Categoria> categorias() {
        return service.listarCategorias();
    }

    private List<String> ubicaciones() {
        return service.listarUbicaciones().stream()
                .map(Ubicacion::getNombre)
                .filter(Objects::nonNull)
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .toList();
    }

    private boolean isAdmin() {
        return RoleName.isAdmin(CurrentSession.getUser());
    }

    private List<Insumo> buscarInsumos(String query) {
        String q = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
        return service.listarTodos().stream()
                .filter(i -> {
                    if (q.isEmpty()) {
                        return true;
                    }
                    String cod = i.getCodigo() == null ? "" : i.getCodigo().toLowerCase(Locale.ROOT);
                    String desc = i.getDescripcion() == null ? "" : i.getDescripcion().toLowerCase(Locale.ROOT);
                    return cod.contains(q) || desc.contains(q);
                })
                .sorted(Comparator.comparing(i -> i.getCodigo() == null ? "" : i.getCodigo()))
                .toList();
    }

    private void stylePrimaryButton(JButton b) {
        b.setPreferredSize(BUTTON_SIZE);
        b.setBackground(new Color(58, 96, 224));
        b.setForeground(Color.WHITE);
        b.setFocusPainted(false);
    }

    private class RegistroPanel extends CardPanel {

        private final JTextField txtCodigo = new JTextField();
        private final JTextField txtDescripcion = new JTextField();
        private final JComboBox<Categoria> cbCategoria = new JComboBox<>();
        private final JComboBox<String> cbUbicacion = new JComboBox<>();
        private final JComboBox<String> cbTipo = new JComboBox<>(new String[]{"INSUMO", "BIEN"});
        private final JSpinner spStockMin = new JSpinner(new SpinnerNumberModel(0, 0, 999999, 1));
        private final JButton btnGuardar = new JButton("Guardar");

        RegistroPanel() {
            setLayout(new BorderLayout(12, 16));
            setBorder(new EmptyBorder(12, 12, 12, 12));

            JLabel title = new JLabel("Registro de inventario");
            title.setFont(title.getFont().deriveFont(Font.BOLD, 16f));
            title.setForeground(new Color(70, 96, 180));
            add(title, BorderLayout.NORTH);

            JPanel form = new JPanel(new GridLayout(0, 2, 18, 18));
            form.setOpaque(false);

            txtCodigo.putClientProperty("JTextField.placeholderText", "Ej: INS001");
            txtDescripcion.putClientProperty("JTextField.placeholderText", "Descripción...");
            cbUbicacion.putClientProperty("JComponent.roundRect", true);
            cbCategoria.putClientProperty("JComponent.roundRect", true);
            cbTipo.putClientProperty("JComponent.roundRect", true);
            spStockMin.setEditor(new JSpinner.NumberEditor(spStockMin, "#"));

            form.add(labeled("CÓDIGO", txtCodigo));
            form.add(labeled("DESCRIPCIÓN", txtDescripcion));
            form.add(labeled("CATEGORÍA", cbCategoria));
            form.add(labeled("UBICACIÓN", cbUbicacion));
            form.add(labeled("TIPO", cbTipo));
            form.add(labeled("STOCK MÍNIMO", spStockMin));
            add(form, BorderLayout.CENTER);

            stylePrimaryButton(btnGuardar);
            btnGuardar.addActionListener(e -> guardar());

            JPanel south = new JPanel(new FlowLayout(FlowLayout.RIGHT));
            south.setOpaque(false);
            south.add(btnGuardar);
            add(south, BorderLayout.SOUTH);

            refreshCombos();
        }

        void refreshCombos() {
            cbCategoria.removeAllItems();
            for (Categoria c : categorias()) {
                cbCategoria.addItem(c);
            }

            cbUbicacion.removeAllItems();
            for (String nombre : ubicaciones()) {
                cbUbicacion.addItem(nombre);
            }
            if (cbUbicacion.getItemCount() == 0) {
                cbUbicacion.addItem("-");
            }
            cbUbicacion.setSelectedIndex(0);
        }

        private void guardar() {
            try {
                String codigo = txtCodigo.getText().trim();
                String desc = txtDescripcion.getText().trim();
                if (codigo.isEmpty() || desc.isEmpty()) {
                    Ui.warn(this, "Código y descripción son obligatorios.");
                    return;
                }

                Categoria categoria = (Categoria) cbCategoria.getSelectedItem();
                if (categoria == null) {
                    Ui.warn(this, "Seleccioná una categoría.");
                    return;
                }

                Object v = spStockMin.getValue();
                if (!(v instanceof Number)) {
                    Ui.warn(this, "El stock mínimo debe ser numérico.");
                    return;
                }
                int stockMin = ((Number) v).intValue();

                Insumo ins = new Insumo();
                ins.setCodigo(codigo);
                ins.setDescripcion(desc);
                ins.setCategoria(categoria);
                ins.setUbicacion((String) cbUbicacion.getSelectedItem());
                ins.setStockMinimo(stockMin);
                ins.setEstado("ACTIVO");
                ins.setTipo((String) cbTipo.getSelectedItem());

                Long id = service.registrarInsumo(ins);
                Ui.info(this, "Insumo guardado. ID = " + id);

                txtCodigo.setText("");
                txtDescripcion.setText("");
                spStockMin.setValue(0);
                cbTipo.setSelectedIndex(0);
                refreshCombos();
            } catch (IllegalArgumentException ex) {
                Ui.warn(this, ex.getMessage());
            } catch (Exception ex) {
                Ui.error(this, ex);
            }
        }
    }

    private class ModificarPanel extends CardPanel {

        private final JTextField txtBuscar = new JTextField(18);
        private final DefaultListModel<Insumo> listModel = new DefaultListModel<>();
        private final JList<Insumo> lstResultados = new JList<>(listModel);

        private final JTextField txtCodigo = new JTextField();
        private final JTextField txtDescripcion = new JTextField();
        private final JComboBox<Categoria> cbCategoria = new JComboBox<>();
        private final JComboBox<String> cbUbicacion = new JComboBox<>();
        private final JComboBox<String> cbTipo = new JComboBox<>(new String[]{"INSUMO", "BIEN"});
        private final JSpinner spStockMin = new JSpinner(new SpinnerNumberModel(0, 0, 999999, 1));
        private final JComboBox<String> cbEstado = new JComboBox<>(new String[]{"ACTIVO", "INACTIVO"});
        private final JButton btnGuardar = new JButton("Guardar cambios");

        private Insumo seleccionado;

        ModificarPanel() {
            setLayout(new BorderLayout(12, 16));
            setBorder(new EmptyBorder(12, 12, 12, 12));

            JLabel title = new JLabel("Modificar insumo existente");
            title.setFont(title.getFont().deriveFont(Font.BOLD, 16f));
            title.setForeground(new Color(70, 96, 180));
            add(title, BorderLayout.NORTH);

            JPanel content = new JPanel(new BorderLayout(12, 12));
            content.setOpaque(false);
            add(content, BorderLayout.CENTER);

            JPanel search = new JPanel(new BorderLayout(8, 0));
            search.setOpaque(false);
            txtBuscar.putClientProperty("JTextField.placeholderText", "Código o descripción");
            JButton btnBuscar = new JButton("Buscar");
            btnBuscar.addActionListener(e -> cargarResultados());
            search.add(txtBuscar, BorderLayout.CENTER);
            search.add(btnBuscar, BorderLayout.EAST);
            content.add(search, BorderLayout.NORTH);

            lstResultados.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
            lstResultados.setVisibleRowCount(8);
            lstResultados.setCellRenderer(new InsumoRenderer());
            lstResultados.addListSelectionListener(new ListSelectionListener() {
                @Override
                public void valueChanged(ListSelectionEvent e) {
                    if (!e.getValueIsAdjusting()) {
                        seleccionado = lstResultados.getSelectedValue();
                        cargarSeleccionado();
                    }
                }
            });

            JScrollPane scroll = new JScrollPane(lstResultados);
            scroll.setPreferredSize(new Dimension(260, 180));
            content.add(scroll, BorderLayout.WEST);

            JPanel form = new JPanel(new GridLayout(0, 2, 18, 18));
            form.setOpaque(false);
            txtCodigo.setEditable(false);
            spStockMin.setEditor(new JSpinner.NumberEditor(spStockMin, "#"));
            cbCategoria.putClientProperty("JComponent.roundRect", true);
            cbUbicacion.putClientProperty("JComponent.roundRect", true);
            cbTipo.putClientProperty("JComponent.roundRect", true);
            cbEstado.putClientProperty("JComponent.roundRect", true);

            form.add(labeled("CÓDIGO", txtCodigo));
            form.add(labeled("DESCRIPCIÓN", txtDescripcion));
            form.add(labeled("CATEGORÍA", cbCategoria));
            form.add(labeled("UBICACIÓN", cbUbicacion));
            form.add(labeled("TIPO", cbTipo));
            form.add(labeled("STOCK MÍNIMO", spStockMin));
            form.add(labeled("ESTADO", cbEstado));
            content.add(form, BorderLayout.CENTER);

            stylePrimaryButton(btnGuardar);
            btnGuardar.addActionListener(e -> guardar());
            JPanel south = new JPanel(new FlowLayout(FlowLayout.RIGHT));
            south.setOpaque(false);
            south.add(btnGuardar);
            add(south, BorderLayout.SOUTH);

            refreshData();
        }

        void refreshData() {
            cargarResultados();
            cbCategoria.removeAllItems();
            for (Categoria c : categorias()) {
                cbCategoria.addItem(c);
            }

            cbUbicacion.removeAllItems();
            for (String nombre : ubicaciones()) {
                cbUbicacion.addItem(nombre);
            }
            if (cbUbicacion.getItemCount() == 0) {
                cbUbicacion.addItem("-");
            }

            cbEstado.setEnabled(isAdmin());
        }

        void mostrarSoloBajoMinimo() {
            refreshData();
            txtBuscar.setText("");
            listModel.clear();
            List<Insumo> insumos = service.listarTodos();
            for (Insumo i : insumos) {
                if (i.getId() == null) {
                    continue;
                }
                Integer minimo = i.getStockMinimo();
                if (minimo == null) {
                    continue;
                }
                BigDecimal actual;
                try {
                    actual = service.stockActualExacto(i.getId());
                } catch (Exception ex) {
                    actual = BigDecimal.ZERO;
                }
                if (actual.compareTo(BigDecimal.valueOf(minimo)) < 0) {
                    listModel.addElement(i);
                }
            }
            if (listModel.isEmpty()) {
                limpiarFormulario();
            } else {
                lstResultados.setSelectedIndex(0);
            }
        }

        private void cargarResultados() {
            listModel.clear();
            for (Insumo i : buscarInsumos(txtBuscar.getText())) {
                listModel.addElement(i);
            }
            seleccionado = null;
            limpiarFormulario();
        }

        private void limpiarFormulario() {
            txtCodigo.setText("");
            txtDescripcion.setText("");
            if (cbCategoria.getItemCount() > 0) {
                cbCategoria.setSelectedIndex(0);
            }
            if (cbUbicacion.getItemCount() > 0) {
                cbUbicacion.setSelectedIndex(0);
            }
            cbTipo.setSelectedIndex(0);
            spStockMin.setValue(0);
            cbEstado.setSelectedIndex(0);
        }

        private void cargarSeleccionado() {
            if (seleccionado == null) {
                limpiarFormulario();
                return;
            }
            txtCodigo.setText(seleccionado.getCodigo());
            txtDescripcion.setText(seleccionado.getDescripcion() == null ? "" : seleccionado.getDescripcion());

            if (seleccionado.getCategoria() != null) {
                for (int i = 0; i < cbCategoria.getItemCount(); i++) {
                    if (Objects.equals(cbCategoria.getItemAt(i).getId(), seleccionado.getCategoria().getId())) {
                        cbCategoria.setSelectedIndex(i);
                        break;
                    }
                }
            }

            if (seleccionado.getUbicacion() != null) {
                String ubic = seleccionado.getUbicacion();
                ComboBoxModel<String> model = cbUbicacion.getModel();
                boolean found = false;
                for (int i = 0; i < model.getSize(); i++) {
                    if (ubic.equalsIgnoreCase(model.getElementAt(i))) {
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    cbUbicacion.addItem(ubic);
                }
                cbUbicacion.setSelectedItem(ubic);
            }
            cbTipo.setSelectedItem(seleccionado.getTipo() == null ? "INSUMO" : seleccionado.getTipo());

            Integer sm = seleccionado.getStockMinimo();
            spStockMin.setValue(sm == null ? 0 : sm);
            cbEstado.setSelectedItem(seleccionado.getEstado() == null ? "ACTIVO" : seleccionado.getEstado());
        }

        private void guardar() {
            if (seleccionado == null) {
                Ui.warn(this, "Primero seleccioná un insumo de la lista.");
                return;
            }
            try {
                seleccionado.setDescripcion(txtDescripcion.getText().trim());
                seleccionado.setCategoria((Categoria) cbCategoria.getSelectedItem());
                seleccionado.setUbicacion((String) cbUbicacion.getSelectedItem());
                seleccionado.setTipo((String) cbTipo.getSelectedItem());

                spStockMin.commitEdit();
                Object v = spStockMin.getValue();
                if (!(v instanceof Number)) {
                    Ui.warn(this, "El stock mínimo debe ser numérico.");
                    return;
                }
                seleccionado.setStockMinimo(((Number) v).intValue());

                if (isAdmin()) {
                    seleccionado.setEstado((String) cbEstado.getSelectedItem());
                }

                service.editarInsumo(seleccionado);
                Ui.info(this, "Cambios guardados correctamente.");
                cargarResultados();
            } catch (IllegalStateException ex) {
                Ui.warn(this, ex.getMessage());
            } catch (IllegalArgumentException ex) {
                Ui.warn(this, ex.getMessage());
            } catch (Exception ex) {
                Ui.error(this, ex);
            }
        }
    }

    private class EliminarPanel extends CardPanel {

        private final JTextField txtBuscar = new JTextField(18);
        private final DefaultListModel<Insumo> listModel = new DefaultListModel<>();
        private final JList<Insumo> lstResultados = new JList<>(listModel);

        private final JTextField txtCodigo = new JTextField();
        private final JTextField txtDescripcion = new JTextField();
        private final JTextField txtCategoria = new JTextField();
        private final JTextField txtUbicacion = new JTextField();
        private final JTextField txtTipo = new JTextField();
        private final JTextField txtEstado = new JTextField();
        private final JLabel lblStock = new JLabel(" ");
        private final JButton btnEliminar = new JButton("Eliminar");

        private Insumo seleccionado;
        private BigDecimal stockActual = BigDecimal.ZERO;

        EliminarPanel() {
            setLayout(new BorderLayout(12, 16));
            setBorder(new EmptyBorder(12, 12, 12, 12));

            JLabel title = new JLabel("Baja lógica de insumo");
            title.setFont(title.getFont().deriveFont(Font.BOLD, 16f));
            title.setForeground(new Color(180, 70, 70));
            add(title, BorderLayout.NORTH);

            JPanel content = new JPanel(new BorderLayout(12, 12));
            content.setOpaque(false);
            add(content, BorderLayout.CENTER);

            JPanel search = new JPanel(new BorderLayout(8, 0));
            search.setOpaque(false);
            txtBuscar.putClientProperty("JTextField.placeholderText", "Código o descripción");
            JButton btnBuscar = new JButton("Buscar");
            btnBuscar.addActionListener(e -> cargarResultados());
            search.add(txtBuscar, BorderLayout.CENTER);
            search.add(btnBuscar, BorderLayout.EAST);
            content.add(search, BorderLayout.NORTH);

            lstResultados.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
            lstResultados.setVisibleRowCount(8);
            lstResultados.setCellRenderer(new InsumoRenderer());
            lstResultados.addListSelectionListener(e -> {
                if (!e.getValueIsAdjusting()) {
                    seleccionado = lstResultados.getSelectedValue();
                    cargarSeleccionado();
                }
            });
            content.add(new JScrollPane(lstResultados), BorderLayout.WEST);

            JPanel form = new JPanel(new GridLayout(0, 2, 18, 12));
            form.setOpaque(false);
            for (JTextField tf : new JTextField[]{txtCodigo, txtDescripcion, txtCategoria, txtUbicacion, txtTipo, txtEstado}) {
                tf.setEditable(false);
            }
            form.add(labeled("CÓDIGO", txtCodigo));
            form.add(labeled("DESCRIPCIÓN", txtDescripcion));
            form.add(labeled("CATEGORÍA", txtCategoria));
            form.add(labeled("UBICACIÓN", txtUbicacion));
            form.add(labeled("TIPO", txtTipo));
            form.add(labeled("ESTADO", txtEstado));
            content.add(form, BorderLayout.CENTER);

            lblStock.setForeground(new Color(200, 90, 90));
            lblStock.setFont(lblStock.getFont().deriveFont(Font.BOLD, 12f));

            stylePrimaryButton(btnEliminar);
            btnEliminar.setBackground(new Color(200, 60, 60));
            btnEliminar.addActionListener(e -> eliminar());
            btnEliminar.setEnabled(false);

            JPanel south = new JPanel(new BorderLayout());
            south.setOpaque(false);
            south.add(lblStock, BorderLayout.WEST);
            JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.RIGHT));
            btnRow.setOpaque(false);
            btnRow.add(btnEliminar);
            south.add(btnRow, BorderLayout.EAST);
            add(south, BorderLayout.SOUTH);

            refreshData();
        }

        void refreshData() {
            cargarResultados();
        }

        private void cargarResultados() {
            listModel.clear();
            for (Insumo i : buscarInsumos(txtBuscar.getText())) {
                listModel.addElement(i);
            }
            seleccionado = null;
            limpiarFormulario();
        }

        private void limpiarFormulario() {
            for (JTextField tf : new JTextField[]{txtCodigo, txtDescripcion, txtCategoria, txtUbicacion, txtTipo, txtEstado}) {
                tf.setText("");
            }
            lblStock.setText(" ");
            btnEliminar.setEnabled(false);
        }

        private void cargarSeleccionado() {
            if (seleccionado == null) {
                limpiarFormulario();
                return;
            }
            txtCodigo.setText(seleccionado.getCodigo());
            txtDescripcion.setText(seleccionado.getDescripcion() == null ? "" : seleccionado.getDescripcion());
            txtCategoria.setText(seleccionado.getCategoria() != null ? seleccionado.getCategoria().getNombre() : "");
            txtUbicacion.setText(seleccionado.getUbicacion() == null ? "" : seleccionado.getUbicacion());
            txtTipo.setText(seleccionado.getTipo() == null ? "INSUMO" : seleccionado.getTipo());
            txtEstado.setText(seleccionado.getEstado() == null ? "ACTIVO" : seleccionado.getEstado());

            stockActual = service.stockActualExacto(seleccionado.getId());
            if (stockActual.compareTo(BigDecimal.ZERO) > 0) {
                lblStock.setText("Stock: " + stockActual.stripTrailingZeros().toPlainString() + " (no eliminable)");
                btnEliminar.setEnabled(false);
            } else {
                lblStock.setText("Stock: 0");
                btnEliminar.setEnabled(true);
            }
        }

        private void eliminar() {
            if (seleccionado == null) {
                Ui.warn(this, "Seleccioná un insumo.");
                return;
            }
            if (!Ui.confirm(this, "¿Confirmás la baja lógica de " + seleccionado.getCodigo() + "?")) {
                return;
            }
            try {
                service.bajaLogica(seleccionado.getId());
                Ui.info(this, "El insumo fue dado de baja.");
                cargarResultados();
            } catch (IllegalStateException ex) {
                Ui.warn(this, ex.getMessage());
            } catch (Exception ex) {
                Ui.error(this, ex);
            }
        }
    }

    private JPanel labeled(String titulo, JComponent field) {
        JPanel p = new JPanel(new BorderLayout(6, 6));
        p.setOpaque(false);
        JLabel lbl = new JLabel(titulo);
        lbl.setFont(lbl.getFont().deriveFont(Font.PLAIN, 12f));
        p.add(lbl, BorderLayout.NORTH);
        field.setPreferredSize(new Dimension(220, 28));
        p.add(field, BorderLayout.CENTER);
        return p;
    }

    private static class InsumoRenderer extends DefaultListCellRenderer {

        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
            super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            if (value instanceof Insumo ins) {
                String cod = ins.getCodigo() == null ? "-" : ins.getCodigo();
                String desc = ins.getDescripcion() == null ? "" : ins.getDescripcion();
                String tipo = ins.getTipo() == null ? "INSUMO" : ins.getTipo();
                setText(cod + " · " + desc + " · " + tipo);
            }
            return this;
        }
    }
}
