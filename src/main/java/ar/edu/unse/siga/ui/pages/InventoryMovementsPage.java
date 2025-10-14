package ar.edu.unse.siga.ui.pages;

import ar.edu.unse.siga.domain.Insumo;
import ar.edu.unse.siga.domain.Movimiento;
import ar.edu.unse.siga.service.InventarioService;
import ar.edu.unse.siga.service.InventarioService.StockCheckResult;
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

    private final JTextArea taSeleccion = new JTextArea();
    private Color resumenColorNormal;
    private final JButton btnEntrada = new JButton("Registrar ENTRADA");
    private final JButton btnSalida = new JButton("Registrar SALIDA");

    private final DefaultListModel<String> historialModel = new DefaultListModel<>();
    private final JList<String> lstHistorial = new JList<>(historialModel);
    
    public void refreshInsumos() { /* repoblar la lista desde DAO/Service */ }


    public InventoryMovementsPage(InventarioService service) {
        this.service = service;
        setOpaque(false);
        setLayout(new BorderLayout(16, 16));

        add(buildHeader(), BorderLayout.NORTH);
        add(buildContent(), BorderLayout.CENTER);

        loadInsumos("");
        setActionsEnabled(false);
    }

    private JComponent buildHeader() {
        var header = new JPanel(new BorderLayout());
        header.setOpaque(false);

        var title = new JLabel("Registrar movimiento");
        title.setFont(title.getFont().deriveFont(Font.BOLD, 26f));
        title.setForeground(new Color(28, 66, 148));
        header.add(title, BorderLayout.WEST);

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

        var left = new JPanel();
        left.setOpaque(false);
        left.setLayout(new BoxLayout(left, BoxLayout.Y_AXIS));
        left.add(selectorCard());

        gc.gridx = 0; gc.gridy = 0;
        body.add(left, gc);

        var right = new JPanel();
        right.setOpaque(false);
        right.setLayout(new BoxLayout(right, BoxLayout.Y_AXIS));
        right.add(resumenCard());
        right.add(Box.createVerticalStrut(12));
        right.add(historialCard());

        gc.gridx = 1; gc.gridy = 0;
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

        JPanel searchPanel = new JPanel(new BorderLayout(8, 0));
        searchPanel.setOpaque(false);
        var txtSearch = new JTextField();
        txtSearch.putClientProperty("JTextField.placeholderText", "Buscar por código/nombre");
        txtSearch.addActionListener(e -> loadInsumos(txtSearch.getText()));

        JButton btnSearch = new JButton("Buscar");
        btnSearch.setFocusPainted(false);
        btnSearch.addActionListener(e -> loadInsumos(txtSearch.getText()));

        var rightBtns = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        rightBtns.setOpaque(false);
        rightBtns.add(btnSearch);

        searchPanel.add(txtSearch, BorderLayout.CENTER);
        searchPanel.add(rightBtns, BorderLayout.EAST);
        north.add(searchPanel);

        card.add(north, BorderLayout.NORTH);

        lstInsumos.setVisibleRowCount(12);
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
                    refreshSelectedSummary();
                    refreshHistorial(selected.getId());
                    setActionsEnabled(true);
                } else {
                    setActionsEnabled(false);
                    taSeleccion.setText("Seleccioná un insumo");
                    historialModel.clear();
                    if (resumenColorNormal != null) taSeleccion.setForeground(resumenColorNormal);
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

        taSeleccion.setOpaque(false);
        taSeleccion.setWrapStyleWord(true);
        taSeleccion.setLineWrap(true);
        taSeleccion.setEditable(false);
        taSeleccion.setFont(taSeleccion.getFont().deriveFont(Font.BOLD, 14f));
        taSeleccion.setBorder(new EmptyBorder(8, 8, 8, 8));

        var scroll = new JScrollPane(taSeleccion);
        scroll.setBorder(null);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.setPreferredSize(new Dimension(10, 90));
        card.add(scroll, BorderLayout.CENTER);

        resumenColorNormal = taSeleccion.getForeground();

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        actions.setOpaque(false);

        stylePrimary(btnEntrada);
        stylePrimary(btnSalida);

        btnEntrada.addActionListener(e -> onRegistrarMovimiento("ENTRADA"));
        btnSalida.addActionListener(e -> onRegistrarMovimiento("SALIDA"));

        actions.add(btnEntrada);
        actions.add(btnSalida);
        card.add(actions, BorderLayout.SOUTH);

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

    private void stylePrimary(JButton b) {
        b.setFocusPainted(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    }

    private void setActionsEnabled(boolean enabled) {
        btnEntrada.setEnabled(enabled);
        btnSalida.setEnabled(enabled);
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

    private void onRegistrarMovimiento(String tipoInicial) {
        Insumo sel = lstInsumos.getSelectedValue();
        if (sel == null) {
            JOptionPane.showMessageDialog(this, "Seleccioná un insumo primero.", "Atención", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int stock = 0;
        try {
            StockCheckResult res = service.stockActual(sel.getId());
            stock = (res != null && res.stockActual != null) ? res.stockActual : 0;   // ← usar lo devuelto
        } catch (Exception ignored) {}

        var win = SwingUtilities.getWindowAncestor(this);
        String ctx = "Insumo: " + sel.getCodigo() + " · " + sel.getDescripcion()
                + "  |  Stock actual: " + stock;

        MovimientoDialog dlg = new MovimientoDialog(win, ctx, tipoInicial, null, 1);
        dlg.setVisible(true);
        if (!dlg.isAccepted()) return;

        String tipo = dlg.getTipo();
        int cantidad = dlg.getCantidad();
        String destino = dlg.getDestinoFuente();

        if (cantidad <= 0) {
            JOptionPane.showMessageDialog(this, "La cantidad debe ser mayor a 0.", "Atención", JOptionPane.WARNING_MESSAGE);
            return;
        }

        // Pre-chequeo SALIDA
        try {
            StockCheckResult res = service.stockActual(sel.getId());
            stock = (res != null && res.stockActual != null) ? res.stockActual : 0;   // ← actualizar base para validar
        } catch (Exception ignored) {}
        if ("SALIDA".equalsIgnoreCase(tipo) && cantidad > stock) {
            JOptionPane.showMessageDialog(this,
                    String.format("No hay suficiente stock para realizar esta salida.\n\nCantidad disponible: %d\nCantidad solicitada: %d\n\nPodés registrar una cantidad menor o agregar más stock con una ENTRADA.",
                            stock, cantidad),
                    "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        try {
            // *** REGISTRAR MOVIMIENTO EN BD ***
            service.registrarMovimiento(sel.getId(), tipo, cantidad, destino);

            // Consultar stock actualizado y notificar
            StockCheckResult res = service.stockActual(sel.getId());

            JOptionPane.showMessageDialog(this,
                    String.format("%s registrada.\nStock actual: %d",
                            tipo, (res.stockActual == null ? 0 : res.stockActual)),
                    "OK", JOptionPane.INFORMATION_MESSAGE);

            if (res.bajoMinimo) {
                JOptionPane.showMessageDialog(this,
                        String.format("⚠ Stock por debajo del mínimo.\nActual: %d  |  Mínimo: %s",
                                (res.stockActual == null ? 0 : res.stockActual),
                                res.stockMinimo == null ? "-" : res.stockMinimo),
                        "Alerta de reposición", JOptionPane.WARNING_MESSAGE);
            }

            refreshHistorial(sel.getId());
            refreshSelectedSummary();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Ocurrió un error al registrar el movimiento.",
                    "Error", JOptionPane.ERROR_MESSAGE);
            ex.printStackTrace();
        }
    }

    private void refreshHistorial(Long insumoId) {
        historialModel.clear();
        if (insumoId == null) return;
        try {
            List<Movimiento> movs = service.ultimosMovimientos(insumoId, 20);
            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm");
            for (Movimiento m : movs) {
                String fecha = (m.getFecha() != null ? m.getFecha().format(fmt) : "");
                String linea = String.format("%s · x%d %s · Destino: %s",
                        fecha, m.getCantidad(), m.getTipo(),
                        (m.getDestinoFuente() == null || m.getDestinoFuente().isBlank()) ? "-" : m.getDestinoFuente());
                historialModel.addElement(linea);
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "No se pudieron cargar los movimientos.", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void refreshSelectedSummary() {
        Insumo sel = lstInsumos.getSelectedValue();
        if (sel == null) {
            taSeleccion.setText("Seleccioná un insumo");
            if (resumenColorNormal != null) taSeleccion.setForeground(resumenColorNormal);
            return;
        }
        int stockAct = 0;
        try {
            StockCheckResult res = service.stockActual(sel.getId());
            stockAct = (res != null && res.stockActual != null) ? res.stockActual : 0; // ← usar lo devuelto
        } catch (Exception ignored) {}

        String cod = sel.getCodigo() == null ? "-" : sel.getCodigo();
        String desc = sel.getDescripcion() == null ? "-" : sel.getDescripcion();
        String min = sel.getStockMinimo() == null ? "-" : String.valueOf(sel.getStockMinimo());

        taSeleccion.setText(String.format("#%d · %s · %s%nStock mín.: %s  |  Stock actual: %d",
                sel.getId(), cod, desc, min, stockAct));
        taSeleccion.setCaretPosition(0);
        taSeleccion.setToolTipText(desc.length() > 40 ? desc : null);

        boolean bajoMinimo = (sel.getStockMinimo() != null) && (stockAct < sel.getStockMinimo());
        if (bajoMinimo) {
            taSeleccion.setForeground(new Color(176, 0, 32));
        } else if (resumenColorNormal != null) {
            taSeleccion.setForeground(resumenColorNormal);
        }
    }
}
