package ar.edu.unse.siga.ui.pages;

import ar.edu.unse.siga.domain.Insumo;
import ar.edu.unse.siga.service.InventarioService;
import ar.edu.unse.siga.ui.base.CardPanel;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.List;

/**
 * Visual panel inspired by the "Registrar movimiento" mockup.  It does not
 * execute movements directly but provides a polished UI scaffolding where the
 * behaviour can be wired later on.
 */
public class InventoryMovementsPage extends JPanel {

    private final InventarioService service;
    private final DefaultListModel<Insumo> listModel = new DefaultListModel<>();
    private final JList<Insumo> lstInsumos = new JList<>(listModel);
    private final JLabel lblSeleccion = new JLabel("Seleccioná un insumo");

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

        // Column left (selector + detalles)
        var left = new JPanel();
        left.setOpaque(false);
        left.setLayout(new BoxLayout(left, BoxLayout.Y_AXIS));
        left.add(selectorCard());
        left.add(Box.createVerticalStrut(12));
        left.add(detallesCard());

        gc.gridx = 0;
        gc.gridy = 0;
        body.add(left, gc);

        // Column right (resumen + historial)
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

        JPanel searchPanel = new JPanel(new BorderLayout(8, 0));
        searchPanel.setOpaque(false);
        var txtSearch = new JTextField();
        txtSearch.putClientProperty("JTextField.placeholderText", "Buscar por código/nombre");
        JButton btnSearch = new JButton("Buscar");
        btnSearch.setFocusPainted(false);
        btnSearch.addActionListener(e -> loadInsumos(txtSearch.getText()));
        searchPanel.add(txtSearch, BorderLayout.CENTER);
        searchPanel.add(btnSearch, BorderLayout.EAST);
        north.add(searchPanel);
        card.add(north, BorderLayout.NORTH);

        lstInsumos.setVisibleRowCount(8);
        lstInsumos.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        lstInsumos.setCellRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                Component c = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof Insumo insumo) {
                    setText("" + insumo.getCodigo() + " · " + insumo.getDescripcion());
                }
                return c;
            }
        });
        lstInsumos.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                Insumo selected = lstInsumos.getSelectedValue();
                if (selected != null) {
                    lblSeleccion.setText("" + selected.getDescripcion() + "  |  Stock: "
                            + (selected.getStockMinimo() == null ? "-" : selected.getStockMinimo()));
                }
            }
        });

        card.add(new JScrollPane(lstInsumos), BorderLayout.CENTER);
        return card;
    }

    private CardPanel detallesCard() {
        CardPanel card = new CardPanel();
        card.setLayout(new BorderLayout(12, 12));
        card.add(sectionTitle("Detalles del movimiento"), BorderLayout.NORTH);

        JPanel form = new JPanel(new GridLayout(0, 2, 12, 12));
        form.setOpaque(false);
        form.add(labelField("Cantidad", spinner(0, 0, 10_000)));
        form.add(labelField("Destino / Responsable", textField("Ej: Oficina 3")));
        form.add(labelField("Motivo", textField("Descripción corta")));
        form.add(labelField("Método / Uso", textField("Entrega / Devolución")));
        form.add(labelField("Fecha", textField("dd/mm/aaaa")));
        card.add(form, BorderLayout.CENTER);

        JButton btn = new JButton("Registrar salida");
        btn.setPreferredSize(new Dimension(180, 38));
        btn.setBackground(new Color(58, 96, 224));
        btn.setForeground(Color.WHITE);
        btn.setFocusPainted(false);
        JPanel south = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        south.setOpaque(false);
        south.add(btn);
        card.add(south, BorderLayout.SOUTH);

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

        String[] sample = {
                "22-09-2024 · x10 Papel A4 · Destino: Mesa de Entradas",
                "18-09-2024 · x5 Toner HP 410X · Destino: Rectorado",
                "16-09-2024 · x12 Lapiceras Gel · Destino: Laboratorio"
        };
        JList<String> lst = new JList<>(sample);
        lst.setOpaque(false);
        lst.setBorder(new EmptyBorder(6, 6, 6, 6));
        card.add(new JScrollPane(lst), BorderLayout.CENTER);

        return card;
    }

    private JLabel sectionTitle(String text) {
        JLabel lbl = new JLabel(text.toUpperCase());
        lbl.setFont(lbl.getFont().deriveFont(Font.BOLD, 14f));
        lbl.setForeground(new Color(70, 96, 180));
        return lbl;
    }

    private JPanel labelField(String label, JComponent field) {
        JPanel p = new JPanel(new BorderLayout(4, 4));
        p.setOpaque(false);
        JLabel l = new JLabel(label);
        l.setFont(l.getFont().deriveFont(Font.PLAIN, 12f));
        p.add(l, BorderLayout.NORTH);
        p.add(field, BorderLayout.CENTER);
        return p;
    }

    private JComponent textField(String placeholder) {
        JTextField t = new JTextField();
        t.putClientProperty("JTextField.placeholderText", placeholder);
        t.putClientProperty("JComponent.roundRect", true);
        return t;
    }

    private JComponent spinner(int value, int min, int max) {
        JSpinner sp = new JSpinner(new SpinnerNumberModel(value, min, max, 1));
        JSpinner.NumberEditor ed = new JSpinner.NumberEditor(sp, "0");
        sp.setEditor(ed);
        sp.putClientProperty("JComponent.roundRect", true);
        return sp;
    }

    private void loadInsumos(String filtro) {
        listModel.clear();
        try {
            List<Insumo> data = service.listarTodos();
            filtro = filtro == null ? "" : filtro.trim().toLowerCase();
            for (Insumo i : data) {
                if (filtro.isEmpty() ||
                        (i.getCodigo() != null && i.getCodigo().toLowerCase().contains(filtro)) ||
                        (i.getDescripcion() != null && i.getDescripcion().toLowerCase().contains(filtro))) {
                    listModel.addElement(i);
                }
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}

