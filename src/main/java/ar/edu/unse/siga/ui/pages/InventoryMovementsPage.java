package ar.edu.unse.siga.ui.pages;

import ar.edu.unse.siga.domain.Insumo;
import ar.edu.unse.siga.domain.Movimiento;
import ar.edu.unse.siga.service.InventarioService;
import ar.edu.unse.siga.ui.base.CardPanel;
import ar.edu.unse.siga.ui.inventario.MovimientoDialog;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class InventoryMovementsPage extends JPanel {

    private final InventarioService service;
    private final DefaultListModel<Insumo> listModel = new DefaultListModel<>();
    private final JList<Insumo> lstInsumos = new JList<>(listModel);
    private final JLabel lblSeleccion = new JLabel("Seleccioná un insumo");

    // historial real
    private final DefaultListModel<String> historialModel = new DefaultListModel<>();
    private final JList<String> lstHistorial = new JList<>(historialModel);

    public InventoryMovementsPage(InventarioService service) {
        this.service = service;
        setOpaque(false);
        setLayout(new BorderLayout(16, 16));

        add(buildHeader(), BorderLayout.NORTH);
        add(buildContent(), BorderLayout.CENTER);

        loadInsumos("");
    }

    private JComponent buildHeader() {
        var header = new JPanel(new BorderLayout());
        header.setOpaque(false);

        var title = new JLabel("Registrar movimiento");
        title.setFont(title.getFont().deriveFont(Font.BOLD, 26f));
        title.setForeground(new Color(28, 66, 148));
        header.add(title, BorderLayout.WEST);

        var btnNuevo = new JButton("Nuevo movimiento…");
        btnNuevo.addActionListener(e -> onNuevoMovimiento());
        var right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        right.setOpaque(false);
        right.add(btnNuevo);
        header.add(right, BorderLayout.EAST);

        return header;
    }

    private JComponent buildContent() {
        CardPanel container = new CardPanel();
        container.setLayout(new BorderLayout(20, 20));

        var body = new JPanel(new GridBagLayout());
        body.setOpaque(false);
        container.add(body, BorderLayout.CENTER);

        GridBagConstraints gc = new GridBagConstraints();
        gc.insets = new Insets(12, 12, 12, 12);
        gc.fill = GridBagConstraints.BOTH;
        gc.weightx = 0.5;
        gc.weighty = 1;

        // izquierda: selector
        var left = new JPanel();
        left.setOpaque(false);
        left.setLayout(new BoxLayout(left, BoxLayout.Y_AXIS));
        left.add(selectorCard());

        gc.gridx = 0;
        gc.gridy = 0;
        body.add(left, gc);

        // derecha: resumen + historial real
        var right = new JPanel();
        right.setOpaque(false);
        right.setLayout(new BoxLayout(right, BoxLayout.Y_AXIS));
        right.add(resumenCard());
        right.add(Box.createVerticalStrut(12));
        right.add(historialCard());

        gc.gridx = 1;
        gc.gridy = 0;
        body.add(right, gc);

        return container;
    }

    private CardPanel selectorCard() {
        CardPanel card = new CardPanel();
        card.setLayout(new BorderLayout(10, 10));

        JPanel north = new JPanel();
        north.setOpaque(false);
        north.setLayout(new BoxLayout(north, BoxLayout.Y_AXIS));
        north.add(sectionTitle("Seleccionar insumo"));
        north.add(Box.createVerticalStrut(8));

        // --- búsqueda con botón limpiar ---
        JPanel searchPanel = new JPanel(new BorderLayout(8, 0));
        searchPanel.setOpaque(false);

        var txtSearch = new JTextField();
        txtSearch.putClientProperty("JTextField.placeholderText", "Buscar por código/nombre");

        JButton btnSearch = new JButton("Buscar");
        btnSearch.setFocusPainted(false);
        btnSearch.addActionListener(e -> loadInsumos(txtSearch.getText()));

        JButton btnClear = new JButton("Limpiar");
        btnClear.setFocusPainted(false);
        btnClear.addActionListener(e -> {
            txtSearch.setText("");
            loadInsumos("");
        });

        var rightBtns = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        rightBtns.setOpaque(false);
        rightBtns.add(btnSearch);
        rightBtns.add(btnClear);

        searchPanel.add(txtSearch, BorderLayout.CENTER);
        searchPanel.add(rightBtns, BorderLayout.EAST);
        north.add(searchPanel);

        card.add(north, BorderLayout.NORTH);

        // --- lista de insumos ---
        lstInsumos.setVisibleRowCount(10);
        lstInsumos.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        lstInsumos.setCellRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                    boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof Insumo ins) {
                    String cod = ins.getCodigo() == null ? "-" : ins.getCodigo();
                    String desc = ins.getDescripcion() == null ? "-" : ins.getDescripcion();
                    setText("#" + ins.getId() + " · " + cod + " · " + desc);
                }
                return this;
            }
        });

        lstInsumos.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                Insumo selected = lstInsumos.getSelectedValue();
                if (selected != null) {
                    String desc = selected.getDescripcion() == null ? "-" : selected.getDescripcion();
                    String min = selected.getStockMinimo() == null ? "-" : String.valueOf(selected.getStockMinimo());
                    lblSeleccion.setText("#" + selected.getId() + " · " + desc + "  |  Stock mín.: " + min);
                    refreshHistorial(selected.getId());
                } else {
                    lblSeleccion.setText("Seleccioná un insumo");
                    historialModel.clear();
                }
            }
        });

        card.add(new JScrollPane(lstInsumos), BorderLayout.CENTER);
        return card;
    }

    private CardPanel resumenCard() {
        CardPanel card = new CardPanel();
        card.setLayout(new BorderLayout(10, 10));
        card.add(sectionTitle("Insumo seleccionado"), BorderLayout.NORTH);

        lblSeleccion.setFont(lblSeleccion.getFont().deriveFont(Font.BOLD, 14f));
        lblSeleccion.setBorder(new EmptyBorder(8, 8, 8, 8));
        card.add(lblSeleccion, BorderLayout.CENTER);

        return card;
    }

    private CardPanel historialCard() {
        CardPanel card = new CardPanel();
        card.setLayout(new BorderLayout(10, 10));
        card.add(sectionTitle("Últimos movimientos"), BorderLayout.NORTH);

        lstHistorial.setOpaque(false);
        lstHistorial.setBorder(new EmptyBorder(6, 6, 6, 6));
        card.add(new JScrollPane(lstHistorial), BorderLayout.CENTER);

        return card;
    }

    private JLabel sectionTitle(String text) {
        JLabel lbl = new JLabel(text.toUpperCase());
        lbl.setFont(lbl.getFont().deriveFont(Font.BOLD, 14f));
        lbl.setForeground(new Color(70, 96, 180));
        return lbl;
    }

    private void loadInsumos(String filtro) {
        listModel.clear();
        try {
            List<Insumo> data = service.listarTodos();
            filtro = filtro == null ? "" : filtro.trim().toLowerCase();
            for (Insumo i : data) {
                if (filtro.isEmpty()
                        || (i.getCodigo() != null && i.getCodigo().toLowerCase().contains(filtro))
                        || (i.getDescripcion() != null && i.getDescripcion().toLowerCase().contains(filtro))) {
                    listModel.addElement(i);
                }
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void onNuevoMovimiento() {
        Insumo sel = lstInsumos.getSelectedValue();
        if (sel == null) {
            JOptionPane.showMessageDialog(this, "Seleccioná un insumo primero.", "Atención", JOptionPane.WARNING_MESSAGE);
            return;
        }

        var win = SwingUtilities.getWindowAncestor(this);
        String ctx = "Insumo: " + sel.getCodigo() + " · " + sel.getDescripcion();
        MovimientoDialog dlg = new MovimientoDialog(win, ctx, "SALIDA", null, 1);
        dlg.setVisible(true);
        if (!dlg.isAccepted()) {
            return;
        }

        String tipo = dlg.getTipo();
        int cantidad = dlg.getCantidad();
        String destino = dlg.getDestinoFuente();

        if (cantidad <= 0) {
            JOptionPane.showMessageDialog(this, "La cantidad debe ser mayor a 0.", "Atención", JOptionPane.WARNING_MESSAGE);
            return;
        }

        try {
            Long id = service.registrarMovimiento(sel.getId(), tipo, cantidad, destino);
            JOptionPane.showMessageDialog(this, tipo + " registrada (movimiento #" + id + ").",
                    "OK", JOptionPane.INFORMATION_MESSAGE);
            refreshHistorial(sel.getId()); // << refrescamos al finalizar
        } catch (IllegalArgumentException | IllegalStateException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Ocurrió un error al registrar el movimiento.",
                    "Error", JOptionPane.ERROR_MESSAGE);
            ex.printStackTrace();
        }
    }

    private void refreshHistorial(Long insumoId) {
        historialModel.clear();
        if (insumoId == null) {
            return;
        }
        try {
            List<Movimiento> movs = service.ultimosMovimientos(insumoId, 20);
            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm");
            for (Movimiento m : movs) {
                String fecha = (m.getFecha() != null ? m.getFecha().format(fmt) : "");
                String linea = String.format(
                        "%s · x%d %s · Destino: %s",
                        fecha, m.getCantidad(), m.getTipo(), (m.getDestinoFuente() == null ? "-" : m.getDestinoFuente())
                );
                historialModel.addElement(linea);
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "No se pudieron cargar los movimientos.", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}
