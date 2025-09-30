package ar.edu.unse.siga.ui.reportes;

import ar.edu.unse.siga.domain.Insumo;
import ar.edu.unse.siga.service.InventarioService;
import ar.edu.unse.siga.service.TramiteService;
import ar.edu.unse.siga.ui.base.CardPanel;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

public class InformesPanel extends JPanel {

    private final InventarioService invService;
    private final TramiteService traService;

    private final JLabel lblTotalInsumos = new JLabel("-");
    private final JLabel lblTotalTramites = new JLabel("-");
    private final JLabel lblPendientes = new JLabel("-");
    private final JLabel lblGastos = new JLabel("$-");

    private final JComboBox<String> cbCategoria = new JComboBox<>(new String[]{
            "Todas", "Oficina", "Bienes", "Insumo"
    });
    private final JTextField tfDesde = new JTextField(10);
    private final JTextField tfHasta = new JTextField(10);

    private final DefaultTableModel model = new DefaultTableModel(
            new Object[]{"Código","Descripción","Estado","Fecha"}, 0
    );

    public InformesPanel(InventarioService invService, TramiteService traService) {
        this.invService = invService;
        this.traService = traService;

        setLayout(new BorderLayout(20, 20));
        setOpaque(false);

        add(buildHeader(), BorderLayout.NORTH);
        add(buildMetrics(), BorderLayout.BEFORE_FIRST_LINE);
        add(buildContent(), BorderLayout.CENTER);

        reloadMetrics();
        runQuery();
    }

    private JComponent buildHeader() {
        JPanel header = new JPanel(new BorderLayout(12, 12));
        header.setOpaque(false);

        JLabel title = new JLabel("Informes");
        title.setFont(title.getFont().deriveFont(Font.BOLD, 30f));
        title.setForeground(new Color(24, 63, 150));
        header.add(title, BorderLayout.WEST);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        actions.setOpaque(false);
        JButton exportPdf = primaryButton("Exportar PDF");
        JButton exportCsv = secondaryButton("Exportar CSV");
        actions.add(exportPdf);
        actions.add(exportCsv);
        header.add(actions, BorderLayout.EAST);

        return header;
    }

    private JComponent buildMetrics() {
        JPanel panel = new JPanel(new GridLayout(1, 4, 16, 0));
        panel.setOpaque(false);
        panel.setBorder(BorderFactory.createEmptyBorder(0, 4, 0, 4));
        panel.add(metricCard("Total insumos", lblTotalInsumos));
        panel.add(metricCard("Trámites", lblTotalTramites));
        panel.add(metricCard("Tareas pendientes", lblPendientes));
        panel.add(metricCard("Gastos mensuales", lblGastos));
        return panel;
    }

    private CardPanel metricCard(String title, JLabel value) {
        CardPanel card = new CardPanel();
        card.setLayout(new BorderLayout(6, 6));
        JLabel lblTitle = new JLabel(title.toUpperCase());
        lblTitle.setForeground(new Color(91, 122, 211));
        lblTitle.setFont(lblTitle.getFont().deriveFont(Font.BOLD, 12f));
        value.setFont(value.getFont().deriveFont(Font.BOLD, 26f));
        value.setForeground(new Color(35, 48, 98));
        card.add(lblTitle, BorderLayout.NORTH);
        card.add(value, BorderLayout.CENTER);
        return card;
    }

    private JComponent buildContent() {
        JPanel wrapper = new JPanel(new BorderLayout(18, 18));
        wrapper.setOpaque(false);

        ButtonGroup tabs = new ButtonGroup();
        JToggleButton btnInventario = pill("Inventario");
        JToggleButton btnTramites = pill("Trámites");
        btnInventario.setSelected(true);
        tabs.add(btnInventario);
        tabs.add(btnTramites);

        JPanel tabRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        tabRow.setOpaque(false);
        tabRow.add(btnInventario);
        tabRow.add(btnTramites);

        wrapper.add(tabRow, BorderLayout.NORTH);

        JPanel split = new JPanel(new GridBagLayout());
        split.setOpaque(false);

        GridBagConstraints gc = new GridBagConstraints();
        gc.insets = new Insets(0, 0, 0, 18);
        gc.fill = GridBagConstraints.BOTH;
        gc.weighty = 1;

        CardPanel filtros = buildFiltrosPanel();
        gc.gridx = 0;
        gc.weightx = 0.25;
        split.add(filtros, gc);

        CardPanel tabla = buildTablaPanel();
        gc.gridx = 1;
        gc.weightx = 0.75;
        gc.insets = new Insets(0, 0, 0, 0);
        split.add(tabla, gc);

        wrapper.add(split, BorderLayout.CENTER);

        return wrapper;
    }

    private CardPanel buildFiltrosPanel() {
        CardPanel card = new CardPanel();
        card.setLayout(new BorderLayout(10, 10));

        JLabel title = new JLabel("Filtros".toUpperCase());
        title.setFont(title.getFont().deriveFont(Font.BOLD, 14f));
        title.setForeground(new Color(73, 103, 204));
        card.add(title, BorderLayout.NORTH);

        JPanel fields = new JPanel();
        fields.setOpaque(false);
        fields.setLayout(new BoxLayout(fields, BoxLayout.Y_AXIS));

        fields.add(filterField("Categoría", cbCategoria));
        fields.add(Box.createVerticalStrut(12));
        tfDesde.putClientProperty("JTextField.placeholderText", "dd/mm/aaaa");
        tfHasta.putClientProperty("JTextField.placeholderText", "dd/mm/aaaa");
        fields.add(filterField("Desde", tfDesde));
        fields.add(Box.createVerticalStrut(12));
        fields.add(filterField("Hasta", tfHasta));

        JButton apply = primaryButton("Aplicar filtros");
        apply.addActionListener(e -> runQuery());
        apply.setAlignmentX(Component.CENTER_ALIGNMENT);
        fields.add(Box.createVerticalStrut(18));
        fields.add(apply);

        card.add(fields, BorderLayout.CENTER);

        JPanel tags = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 6));
        tags.setOpaque(false);
        tags.add(tag("Insumos"));
        tags.add(tag("Oficina"));
        tags.add(tag("Bienes"));
        card.add(tags, BorderLayout.SOUTH);

        return card;
    }

    private CardPanel buildTablaPanel() {
        CardPanel card = new CardPanel();
        card.setLayout(new BorderLayout(10, 10));

        JLabel title = new JLabel("Resultados".toUpperCase());
        title.setFont(title.getFont().deriveFont(Font.BOLD, 14f));
        title.setForeground(new Color(73, 103, 204));
        card.add(title, BorderLayout.NORTH);

        JTable table = new JTable(model);
        table.setRowHeight(28);
        table.setShowGrid(false);
        table.setFillsViewportHeight(true);
        table.getColumnModel().getColumn(2).setCellRenderer(statusRenderer());
        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        card.add(scroll, BorderLayout.CENTER);

        return card;
    }

    private DefaultTableCellRenderer statusRenderer() {
        return new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                setHorizontalAlignment(CENTER);
                if (value != null) {
                    String text = value.toString();
                    setText(text.toUpperCase());
                    setOpaque(true);
                    setBorder(BorderFactory.createEmptyBorder(4, 12, 4, 12));
                    switch (text.toLowerCase()) {
                        case "activo" -> setBackground(new Color(212, 235, 216));
                        case "pendiente" -> setBackground(new Color(255, 239, 200));
                        default -> setBackground(new Color(220, 228, 255));
                    }
                }
                return c;
            }
        };
    }

    private JPanel filterField(String label, JComponent component) {
        JPanel p = new JPanel();
        p.setOpaque(false);
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        JLabel lbl = new JLabel(label.toUpperCase());
        lbl.setFont(lbl.getFont().deriveFont(Font.PLAIN, 12f));
        lbl.setAlignmentX(Component.LEFT_ALIGNMENT);
        component.setAlignmentX(Component.LEFT_ALIGNMENT);
        p.add(lbl);
        p.add(Box.createVerticalStrut(4));
        p.add(component);
        return p;
    }

    private JButton primaryButton(String text) {
        JButton b = new JButton(text);
        b.setBackground(new Color(58, 96, 224));
        b.setForeground(Color.WHITE);
        b.setFocusPainted(false);
        b.putClientProperty("JButton.buttonType", "roundRect");
        return b;
    }

    private JButton secondaryButton(String text) {
        JButton b = new JButton(text);
        b.setFocusPainted(false);
        b.putClientProperty("JButton.buttonType", "roundRect");
        return b;
    }

    private JLabel tag(String text) {
        JLabel l = new JLabel(text);
        l.setOpaque(true);
        l.setBackground(new Color(232, 238, 255));
        l.setForeground(new Color(65, 90, 181));
        l.setBorder(BorderFactory.createEmptyBorder(4, 12, 4, 12));
        return l;
    }

    private JToggleButton pill(String text) {
        JToggleButton t = new JToggleButton(text.toUpperCase());
        t.setFocusPainted(false);
        t.setBackground(Color.WHITE);
        t.setForeground(new Color(66, 100, 189));
        t.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(205, 218, 255)),
                BorderFactory.createEmptyBorder(8, 18, 8, 18)
        ));
        t.addChangeListener(e -> {
            if (t.isSelected()) {
                t.setBackground(new Color(58, 96, 224));
                t.setForeground(Color.WHITE);
            } else {
                t.setBackground(Color.WHITE);
                t.setForeground(new Color(66, 100, 189));
            }
        });
        return t;
    }

    // ==== reemplazos sin depender de nuevos métodos del service ====

    private void reloadMetrics() {
        try {
            // total insumos por listarTodos()
            lblTotalInsumos.setText(String.valueOf(invService.listarTodos().size()));
        } catch (Exception e) { lblTotalInsumos.setText("-"); }

        try {
            var tramites = traService.listarTodos();
            lblTotalTramites.setText(String.valueOf(tramites.size()));
            long pend = tramites.stream()
                    .filter(t -> "PENDIENTE".equalsIgnoreCase(String.valueOf(t.getEstado())))
                    .count();
            lblPendientes.setText(String.valueOf(pend));
        } catch (Exception e) { lblTotalTramites.setText("-"); lblPendientes.setText("-"); }

        try {
            // si no tenés movimientos aún, mostramos 0
            lblGastos.setText("+$0");
        } catch (Exception e) { lblGastos.setText("$-"); }
    }

    private void runQuery() {
        model.setRowCount(0);
        try {
            String cat = cbCategoria.getSelectedIndex() == 0 ? null : cbCategoria.getSelectedItem().toString();
            LocalDate d1 = parse(tfDesde.getText());
            LocalDate d2 = parse(tfHasta.getText());

            List<Insumo> data = filtrarLocal(invService.listarTodos(), cat, d1, d2);
            for (var i : data) {
                // muchos dominios no tienen fechaAlta; mostramos "-"
                model.addRow(new Object[]{ i.getCodigo(), i.getDescripcion(), i.getEstado(), "-" });
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private static List<Insumo> filtrarLocal(List<Insumo> base, String cat, LocalDate d1, LocalDate d2) {
        return base.stream().filter(i -> {
            boolean ok = true;
            if (cat != null && i.getCategoria() != null) {
                ok &= cat.equalsIgnoreCase(i.getCategoria().getNombre());
            }
            // si luego agregamos fechaAlta en Insumo, filtramos por d1/d2 aquí
            return ok;
        }).collect(Collectors.toList());
    }

    private static LocalDate parse(String ddmmyyyy) {
        if (ddmmyyyy == null) return null;
        ddmmyyyy = ddmmyyyy.trim();
        if (ddmmyyyy.isEmpty()) return null;
        try {
            String[] p = ddmmyyyy.split("[/\\-]");
            int d = Integer.parseInt(p[0]);
            int m = Integer.parseInt(p[1]);
            int y = Integer.parseInt(p[2]);
            return LocalDate.of(y, m, d);
        } catch (Exception e) {
            throw new IllegalArgumentException("Fecha inválida: use dd/mm/aaaa");
        }
    }
}
