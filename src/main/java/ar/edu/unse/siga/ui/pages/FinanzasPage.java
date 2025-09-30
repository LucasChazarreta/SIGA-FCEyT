package ar.edu.unse.siga.ui.pages;

import ar.edu.unse.siga.ui.base.CardPanel;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;

public class FinanzasPage extends JPanel {

    private final CardLayout cardLayout = new CardLayout();
    private final JPanel cards = new JPanel(cardLayout);

    public FinanzasPage() {
        setOpaque(false);
        setLayout(new BorderLayout(20, 20));

        add(buildHeader(), BorderLayout.NORTH);
        add(buildContent(), BorderLayout.CENTER);
    }

    private JComponent buildHeader() {
        JPanel header = new JPanel(new BorderLayout(12, 12));
        header.setOpaque(false);

        JLabel title = new JLabel("Finanzas");
        title.setFont(title.getFont().deriveFont(Font.BOLD, 30f));
        title.setForeground(new Color(24, 63, 150));
        header.add(title, BorderLayout.WEST);

        JButton exportPdf = primaryButton("Exportar PDF");
        JButton exportCsv = secondaryButton("Exportar CSV");
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        actions.setOpaque(false);
        actions.add(exportPdf);
        actions.add(exportCsv);
        header.add(actions, BorderLayout.EAST);

        return header;
    }

    private JComponent buildContent() {
        JPanel wrapper = new JPanel(new BorderLayout(12, 18));
        wrapper.setOpaque(false);

        ButtonGroup tabs = new ButtonGroup();
        JToggleButton resumen = pill("Resumen");
        JToggleButton transacciones = pill("Transacciones");
        resumen.setSelected(true);
        tabs.add(resumen);
        tabs.add(transacciones);

        JPanel tabRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        tabRow.setOpaque(false);
        tabRow.add(resumen);
        tabRow.add(transacciones);

        wrapper.add(tabRow, BorderLayout.NORTH);

        cards.setOpaque(false);
        cards.add(buildResumenCard(), "resumen");
        cards.add(buildTransaccionesCard(), "transacciones");

        resumen.addActionListener(e -> cardLayout.show(cards, "resumen"));
        transacciones.addActionListener(e -> cardLayout.show(cards, "transacciones"));
        cardLayout.show(cards, "resumen");

        wrapper.add(cards, BorderLayout.CENTER);
        return wrapper;
    }

    private CardPanel buildResumenCard() {
        CardPanel card = new CardPanel();
        card.setLayout(new BorderLayout(18, 18));

        card.add(metricsRow(), BorderLayout.NORTH);

        JPanel center = new JPanel(new GridLayout(1, 2, 18, 18));
        center.setOpaque(false);
        center.add(trendCard());
        center.add(pieCard());
        card.add(center, BorderLayout.CENTER);

        card.add(activityCard(), BorderLayout.SOUTH);
        return card;
    }

    private CardPanel buildTransaccionesCard() {
        CardPanel card = new CardPanel();
        card.setLayout(new BorderLayout(18, 18));

        card.add(filterRow(), BorderLayout.NORTH);

        JTable table = new JTable(transaccionesModel());
        table.setRowHeight(28);
        table.setShowGrid(false);
        table.getColumnModel().getColumn(3).setCellRenderer(statusRenderer());
        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        card.add(scroll, BorderLayout.CENTER);

        return card;
    }

    private JPanel metricsRow() {
        JPanel metrics = new JPanel(new GridLayout(1, 4, 16, 0));
        metrics.setOpaque(false);
        metrics.add(metric("Balance general", "$15.000"));
        metrics.add(metric("Ingresos", "$25.000"));
        metrics.add(metric("Gastos", "$10.000"));
        metrics.add(metric("Presupuesto", "$50.000"));
        return metrics;
    }

    private CardPanel metric(String title, String value) {
        CardPanel card = new CardPanel();
        card.setLayout(new BorderLayout(8, 8));
        JLabel lblTitle = new JLabel(title.toUpperCase());
        lblTitle.setFont(lblTitle.getFont().deriveFont(Font.BOLD, 12f));
        lblTitle.setForeground(new Color(91, 122, 211));
        JLabel lblValue = new JLabel(value);
        lblValue.setFont(lblValue.getFont().deriveFont(Font.BOLD, 24f));
        lblValue.setForeground(new Color(32, 45, 94));
        card.add(lblTitle, BorderLayout.NORTH);
        card.add(lblValue, BorderLayout.CENTER);
        return card;
    }

    private CardPanel trendCard() {
        CardPanel card = new CardPanel();
        card.setLayout(new BorderLayout(10, 10));
        JLabel title = new JLabel("Tendencia de ingresos vs gastos".toUpperCase());
        title.setForeground(new Color(73, 103, 204));
        title.setFont(title.getFont().deriveFont(Font.BOLD, 14f));
        card.add(title, BorderLayout.NORTH);
        card.add(new TrendChartPanel(), BorderLayout.CENTER);
        return card;
    }

    private CardPanel pieCard() {
        CardPanel card = new CardPanel();
        card.setLayout(new BorderLayout(10, 10));
        JLabel title = new JLabel("Gastos por categoría".toUpperCase());
        title.setForeground(new Color(73, 103, 204));
        title.setFont(title.getFont().deriveFont(Font.BOLD, 14f));
        card.add(title, BorderLayout.NORTH);
        card.add(new PieChartPanel(), BorderLayout.CENTER);
        return card;
    }

    private CardPanel activityCard() {
        CardPanel card = new CardPanel();
        card.setLayout(new BorderLayout(10, 10));
        JLabel title = new JLabel("Actividad reciente".toUpperCase());
        title.setForeground(new Color(73, 103, 204));
        title.setFont(title.getFont().deriveFont(Font.BOLD, 14f));
        card.add(title, BorderLayout.NORTH);

        String[] cols = {"Transacción", "Estado", "Monto"};
        Object[][] rows = {
                {"Transacción 002", "Completado", "$5.000"},
                {"Transacción 003", "En proceso", "$2.100"},
                {"Transacción 004", "Pendiente", "$1.200"}
        };
        JTable table = new JTable(rows, cols);
        table.setRowHeight(26);
        table.setShowGrid(false);
        table.getColumnModel().getColumn(1).setCellRenderer(statusRenderer());
        card.add(new JScrollPane(table), BorderLayout.CENTER);
        return card;
    }

    private JPanel filterRow() {
        JPanel row = new JPanel(new GridLayout(1, 3, 14, 0));
        row.setOpaque(false);
        row.add(filterField("Buscar", new JTextField()));
        row.add(filterField("Categoría", new JComboBox<>(new String[]{"Todas", "Ingresos", "Gastos"})));
        JButton btn = primaryButton("Registrar nueva transacción");
        JPanel wrap = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        wrap.setOpaque(false);
        wrap.add(btn);
        row.add(wrap);
        return row;
    }

    private DefaultTableModel transaccionesModel() {
        String[] cols = {"Fecha", "Tipo", "Categoría", "Estado", "Monto"};
        Object[][] rows = {
                {"15-03-2025", "Matrículas", "Ingreso", "Completado", "$15.000"},
                {"27-06-2025", "Suministros", "Gasto", "Pendiente", "$5.500"},
                {"18-09-2025", "Pago Semestre", "Ingreso", "Completado", "$10.000"},
                {"10-10-2025", "Suministros", "Gasto", "En proceso", "$3.200"},
                {"14-09-2025", "Pago Servicios", "Gasto", "En proceso", "$1.500"}
        };
        return new DefaultTableModel(rows, cols) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
    }

    private JPanel filterField(String label, JComponent component) {
        JPanel p = new JPanel(new BorderLayout(4, 4));
        p.setOpaque(false);
        JLabel lbl = new JLabel(label.toUpperCase());
        lbl.setFont(lbl.getFont().deriveFont(Font.PLAIN, 12f));
        p.add(lbl, BorderLayout.NORTH);
        p.add(component, BorderLayout.CENTER);
        return p;
    }

    private DefaultTableCellRenderer statusRenderer() {
        return new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                setHorizontalAlignment(CENTER);
                if (value != null) {
                    String status = value.toString();
                    setText(status.toUpperCase());
                    setOpaque(true);
                    setBorder(BorderFactory.createEmptyBorder(4, 12, 4, 12));
                    switch (status.toLowerCase()) {
                        case "completado" -> setBackground(new Color(212, 235, 216));
                        case "en proceso" -> setBackground(new Color(255, 239, 200));
                        case "pendiente" -> setBackground(new Color(255, 220, 220));
                        default -> setBackground(new Color(220, 228, 255));
                    }
                }
                return c;
            }
        };
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

    /** Simple painted trend chart using sample data */
    static class TrendChartPanel extends JPanel {
        private final int[] ingresos = {10, 15, 18, 21, 25, 30};
        private final int[] gastos = {8, 11, 14, 13, 16, 18};

        TrendChartPanel() {
            setOpaque(false);
            setPreferredSize(new Dimension(320, 220));
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int w = getWidth() - 60;
            int h = getHeight() - 40;
            int originX = 40;
            int originY = getHeight() - 30;

            g2.setColor(new Color(200, 210, 235));
            g2.drawLine(originX, originY, originX + w, originY);
            g2.drawLine(originX, originY, originX, originY - h);

            drawSeries(g2, ingresos, originX, originY, w, h, new Color(72, 115, 255));
            drawSeries(g2, gastos, originX, originY, w, h, new Color(255, 149, 128));

            g2.dispose();
        }

        private void drawSeries(Graphics2D g2, int[] data, int ox, int oy, int w, int h, Color color) {
            int n = data.length;
            int step = w / (n - 1);
            int max = 0;
            for (int v : data) max = Math.max(max, v);
            max = Math.max(max, 1);

            g2.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), 60));
            Polygon area = new Polygon();
            area.addPoint(ox, oy);
            for (int i = 0; i < n; i++) {
                int x = ox + i * step;
                int y = oy - (int) (h * (data[i] / (double) max));
                area.addPoint(x, y);
            }
            area.addPoint(ox + (n - 1) * step, oy);
            g2.fill(area);

            g2.setStroke(new BasicStroke(2f));
            g2.setColor(color.darker());
            for (int i = 0; i < n - 1; i++) {
                int x1 = ox + i * step;
                int y1 = oy - (int) (h * (data[i] / (double) max));
                int x2 = ox + (i + 1) * step;
                int y2 = oy - (int) (h * (data[i + 1] / (double) max));
                g2.drawLine(x1, y1, x2, y2);
                g2.fillOval(x1 - 3, y1 - 3, 6, 6);
                if (i == n - 2) {
                    g2.fillOval(x2 - 3, y2 - 3, 6, 6);
                }
            }
        }
    }

    static class PieChartPanel extends JPanel {
        private final double[] values = {45, 25, 20, 10};
        private final Color[] colors = {
                new Color(72, 115, 255),
                new Color(117, 212, 173),
                new Color(255, 183, 77),
                new Color(255, 149, 128)
        };

        PieChartPanel() {
            setOpaque(false);
            setPreferredSize(new Dimension(280, 220));
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int size = Math.min(getWidth(), getHeight()) - 40;
            int x = (getWidth() - size) / 2;
            int y = (getHeight() - size) / 2;

            double sum = 0;
            for (double v : values) sum += v;

            double angle = 0;
            for (int i = 0; i < values.length; i++) {
                double extent = values[i] / sum * 360;
                g2.setColor(colors[i]);
                g2.fillArc(x, y, size, size, (int) angle, (int) Math.round(extent));
                angle += extent;
            }

            g2.dispose();
        }
    }
}

