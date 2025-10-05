package ar.edu.unse.siga.ui.pages;

import ar.edu.unse.siga.domain.Usuario;
import ar.edu.unse.siga.ui.base.CardPanel;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class HomePage extends JPanel {

    private final Usuario usuarioActual;
    private final DateTimeFormatter friendlyDate = DateTimeFormatter.ofPattern("dd 'de' MMMM yyyy");

    public HomePage() {
        this(null);
    }

    public HomePage(Usuario usuarioActual) {
        this.usuarioActual = usuarioActual;
        setOpaque(false);
        setLayout(new BorderLayout());

        JPanel layout = new JPanel(new GridBagLayout());
        layout.setOpaque(false);
        layout.setBorder(new EmptyBorder(0, 8, 8, 8));

        GridBagConstraints gc = new GridBagConstraints();
        gc.gridx = 0;
        gc.weightx = 1;
        gc.fill = GridBagConstraints.HORIZONTAL;
        gc.insets = new Insets(0, 0, 18, 0);
        gc.gridy = 0;
        layout.add(buildHeroSection(), gc);

        gc.gridy = 1;
        gc.insets = new Insets(0, 0, 20, 0);
        gc.weighty = 1;
        gc.fill = GridBagConstraints.BOTH;
        layout.add(buildDashboardGrid(), gc);

        gc.gridy = 2;
        gc.insets = new Insets(0, 0, 0, 0);
        gc.weighty = 0;
        gc.fill = GridBagConstraints.HORIZONTAL;
        layout.add(buildQuickActions(), gc);

        add(layout, BorderLayout.CENTER);
    }

    private JComponent buildHeroSection() {
        CardPanel card = new CardPanel();
        card.setLayout(new BorderLayout(24, 18));

        JPanel headerRow = new JPanel(new BorderLayout());
        headerRow.setOpaque(false);

        JPanel greetings = new JPanel();
        greetings.setOpaque(false);
        greetings.setLayout(new BoxLayout(greetings, BoxLayout.Y_AXIS));
        String name = usuarioActual != null && usuarioActual.getUsuario() != null
                ? usuarioActual.getUsuario()
                : "Administrador";
        JLabel greet = new JLabel("Hola " + name + ", encantado de verte.");
        greet.setFont(new Font("Segoe UI", Font.BOLD, 24));
        greet.setForeground(new Color(29, 55, 124));
        JLabel sub = new JLabel("Panel de control general del sistema");
        sub.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        sub.setForeground(new Color(110, 126, 165));
        greetings.add(greet);
        greetings.add(Box.createVerticalStrut(4));
        greetings.add(sub);

        headerRow.add(greetings, BorderLayout.CENTER);
        headerRow.add(buildHeaderActions(), BorderLayout.EAST);
        card.add(headerRow, BorderLayout.NORTH);

        JPanel body = new JPanel();
        body.setOpaque(false);
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        body.add(alertBanner());
        body.add(Box.createVerticalStrut(18));
        body.add(metricsRow());

        card.add(body, BorderLayout.CENTER);
        return card;
    }

    private Component buildHeaderActions() {
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 0));
        actions.setOpaque(false);
        actions.add(headerChip("Perfil"));
        actions.add(headerChip("Notificaciones"));
        actions.add(headerChip("Ajustes"));
        return actions;
    }

    private JButton headerChip(String text) {
        JButton b = new JButton(text);
        b.setFocusPainted(false);
        b.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        b.setForeground(new Color(52, 83, 169));
        b.setBackground(new Color(230, 238, 255));
        b.setBorder(BorderFactory.createEmptyBorder(8, 14, 8, 14));
        b.putClientProperty("JButton.buttonType", "roundRect");
        return b;
    }

    private JComponent alertBanner() {
        JPanel banner = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                GradientPaint gp = new GradientPaint(0, 0, new Color(37, 99, 235),
                        getWidth(), getHeight(), new Color(21, 75, 204));
                g2.setPaint(gp);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 20, 20);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        banner.setOpaque(false);
        banner.setLayout(new FlowLayout(FlowLayout.LEFT, 16, 12));
        banner.setBorder(new EmptyBorder(4, 18, 4, 18));

        banner.add(alertChip("Alertas importantes"));
        banner.add(alertMessage("Nuevo protocolo de gastos"));
        banner.add(alertMessage("Mantenimiento del sistema de 11 PM a 2 AM"));
        return banner;
    }

    private JLabel alertChip(String text) {
        JLabel lbl = new JLabel(text.toUpperCase());
        lbl.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lbl.setForeground(new Color(37, 99, 235));
        lbl.setBackground(Color.WHITE);
        lbl.setOpaque(true);
        lbl.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        lbl.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(190, 210, 255)),
                new EmptyBorder(6, 12, 6, 12)));
        return lbl;
    }

    private JLabel alertMessage(String text) {
        JLabel lbl = new JLabel(text);
        lbl.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        lbl.setForeground(new Color(233, 240, 255));
        return lbl;
    }

    private JPanel metricsRow() {
        JPanel row = new JPanel(new GridLayout(1, 4, 16, 0));
        row.setOpaque(false);
        row.add(metricCard("Tareas pendientes", "7", "Nuevas tareas", new Color(58, 109, 255)));
        row.add(metricCard("Aprob. pendientes", "3", "Documentos en revisión", new Color(86, 127, 255)));
        row.add(metricCard("Insumos críticos", "12", "Requieren pedido urgente", new Color(255, 99, 99)));
        row.add(metricCard("Balance dpto", "$145.200", "Órdenes de compra", new Color(58, 182, 186)));
        return row;
    }

    private JPanel metricCard(String title, String value, String hint, Color base) {
        JPanel card = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setPaint(new GradientPaint(0, 0, base, getWidth(), getHeight(), base.darker()));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 22, 22);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        card.setOpaque(false);
        card.setLayout(new BorderLayout());
        card.setBorder(new EmptyBorder(18, 20, 18, 20));
        card.setPreferredSize(new Dimension(0, 100));

        JLabel lblTitle = new JLabel(title.toUpperCase());
        lblTitle.setForeground(new Color(217, 230, 255));
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 12));
        JLabel lblValue = new JLabel(value);
        lblValue.setForeground(Color.WHITE);
        lblValue.setFont(new Font("Segoe UI", Font.BOLD, 28));
        JLabel lblHint = new JLabel(hint);
        lblHint.setForeground(new Color(220, 231, 255));
        lblHint.setFont(new Font("Segoe UI", Font.PLAIN, 12));

        card.add(lblTitle, BorderLayout.NORTH);
        card.add(lblValue, BorderLayout.CENTER);
        card.add(lblHint, BorderLayout.SOUTH);
        return card;
    }

    private JComponent buildDashboardGrid() {
        JPanel grid = new JPanel(new GridBagLayout());
        grid.setOpaque(false);
        GridBagConstraints gc = new GridBagConstraints();
        gc.insets = new Insets(0, 12, 18, 12);
        gc.fill = GridBagConstraints.BOTH;
        gc.weightx = 0.5;
        gc.weighty = 0.5;

        gc.gridx = 0;
        gc.gridy = 0;
        grid.add(upcomingEventsCard(), gc);

        gc.gridx = 1;
        grid.add(activityCard(), gc);

        gc.gridx = 0;
        gc.gridy = 1;
        grid.add(priorityCard(), gc);

        gc.gridx = 1;
        grid.add(recentAccessCard(), gc);

        return grid;
    }

    private CardPanel upcomingEventsCard() {
        CardPanel card = new CardPanel();
        card.setLayout(new BorderLayout(0, 12));
        card.add(sectionTitle("Próximos eventos"), BorderLayout.NORTH);

        JPanel list = new JPanel();
        list.setOpaque(false);
        list.setLayout(new BoxLayout(list, BoxLayout.Y_AXIS));
        list.add(eventRow("10:00 Reunión Dpto.", "Decanato de Ciencias"));
        list.add(divider());
        list.add(eventRow("12:30 Comité Compras", "Sala 3 - Mesa de Compras"));
        list.add(divider());
        list.add(eventRow("14:00 Seguimiento Proyectos", "Sala Multimedia"));

        card.add(list, BorderLayout.CENTER);
        return card;
    }

    private CardPanel activityCard() {
        CardPanel card = new CardPanel();
        card.setLayout(new BorderLayout(0, 12));
        card.add(sectionTitle("Actividad reciente"), BorderLayout.NORTH);

        JPanel list = new JPanel();
        list.setOpaque(false);
        list.setLayout(new BoxLayout(list, BoxLayout.Y_AXIS));
        for (String[] row : new String[][]{
                {"Hace 5 min", "Juan P. aprobó Traspaso de Inventario"},
                {"Hace 15 min", "M. López registró movimiento de tóner"},
                {"Hace 30 min", "Admin derivó Expediente 123/24 a Compras"},
                {"Hace 1 h", "S. Ruiz finalizó Pedido de Suministros"}
        }) {
            list.add(activityRow(row[0], row[1]));
            list.add(divider());
        }
        list.add(activityRow("Hace 2 h", "Tesorería generó Orden de Compra 89"));

        card.add(list, BorderLayout.CENTER);
        return card;
    }

    private CardPanel priorityCard() {
        CardPanel card = new CardPanel();
        card.setLayout(new BorderLayout(0, 12));
        card.add(sectionTitle("Tareas prioritarias"), BorderLayout.NORTH);

        JPanel list = new JPanel();
        list.setOpaque(false);
        list.setLayout(new BoxLayout(list, BoxLayout.Y_AXIS));
        list.add(priorityRow("Revisar solicitud de préstamo de proyector", true));
        list.add(priorityRow("Aprobar orden #2034-09", false));
        list.add(priorityRow("Actualizar inventario laboratorio B", false));

        card.add(list, BorderLayout.CENTER);
        return card;
    }

    private CardPanel recentAccessCard() {
        CardPanel card = new CardPanel();
        card.setLayout(new BorderLayout(0, 12));
        card.add(sectionTitle("Accesos recientes"), BorderLayout.NORTH);

        JPanel list = new JPanel();
        list.setOpaque(false);
        list.setLayout(new BoxLayout(list, BoxLayout.Y_AXIS));
        for (String[] row : new String[][]{
                {"Hace 5 min", "Juan P. - Aprobó presupuesto de inventario"},
                {"Hace 9 min", "María L. - Revisó Compras"},
                {"Hace 15 min", "Sofía R. - Actualizó tramites"},
                {"Hace 20 min", "Admin - Registró nueva orden"}
        }) {
            list.add(activityRow(row[0], row[1]));
            list.add(divider());
        }
        list.add(activityRow("Hace 30 min", "Carlos D. - Revisó finanzas"));

        card.add(list, BorderLayout.CENTER);
        return card;
    }

    private JLabel sectionTitle(String text) {
        JLabel lbl = new JLabel(text);
        lbl.setFont(new Font("Segoe UI", Font.BOLD, 16));
        lbl.setForeground(new Color(35, 55, 110));
        return lbl;
    }

    private Component eventRow(String title, String subtitle) {
        JPanel row = new JPanel(new BorderLayout());
        row.setOpaque(false);
        JLabel lblTitle = new JLabel(title);
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 14));
        lblTitle.setForeground(new Color(40, 56, 120));
        JLabel lblSubtitle = new JLabel(subtitle);
        lblSubtitle.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lblSubtitle.setForeground(new Color(120, 136, 180));
        row.add(lblTitle, BorderLayout.NORTH);
        row.add(lblSubtitle, BorderLayout.SOUTH);
        return row;
    }

    private Component activityRow(String when, String description) {
        JPanel row = new JPanel(new BorderLayout());
        row.setOpaque(false);
        JLabel lblWhen = new JLabel(when);
        lblWhen.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lblWhen.setForeground(new Color(92, 109, 161));
        JLabel lblDesc = new JLabel(description);
        lblDesc.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        lblDesc.setForeground(new Color(45, 63, 120));
        row.add(lblWhen, BorderLayout.NORTH);
        row.add(lblDesc, BorderLayout.CENTER);
        return row;
    }

    private Component priorityRow(String text, boolean completed) {
        JCheckBox check = new JCheckBox(text);
        check.setOpaque(false);
        check.setSelected(completed);
        check.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        check.setForeground(new Color(45, 63, 120));
        return check;
    }

    private JSeparator divider() {
        JSeparator sep = new JSeparator();
        sep.setForeground(new Color(230, 236, 250));
        sep.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
        return sep;
    }

    private CardPanel buildQuickActions() {
        CardPanel card = new CardPanel();
        card.setLayout(new BorderLayout(0, 18));
        card.add(sectionTitle("Accesos rápidos"), BorderLayout.NORTH);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT, 16, 0));
        buttons.setOpaque(false);
        buttons.add(primaryButton("Registrar nuevo trámite"));
        buttons.add(secondaryButton("Generar orden de compra"));
        buttons.add(secondaryButton("Ver reportes"));
        card.add(buttons, BorderLayout.CENTER);

        JLabel footer = new JLabel("" + LocalDate.now().format(friendlyDate));
        footer.setForeground(new Color(125, 139, 170));
        footer.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        card.add(footer, BorderLayout.SOUTH);
        return card;
    }

    private JButton primaryButton(String text) {
        JButton b = new JButton(text);
        b.setFocusPainted(false);
        b.setFont(new Font("Segoe UI", Font.BOLD, 14));
        b.setForeground(Color.WHITE);
        b.setBackground(new Color(52, 99, 240));
        b.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));
        b.putClientProperty("JButton.buttonType", "roundRect");
        return b;
    }

    private JButton secondaryButton(String text) {
        JButton b = new JButton(text);
        b.setFocusPainted(false);
        b.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        b.setForeground(new Color(52, 99, 240));
        b.setBackground(new Color(231, 238, 255));
        b.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));
        b.putClientProperty("JButton.buttonType", "roundRect");
        return b;
    }

}
