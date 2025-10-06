package ar.edu.unse.siga.ui.pages;

import ar.edu.unse.siga.domain.Insumo;
import ar.edu.unse.siga.domain.Tramite;
import ar.edu.unse.siga.domain.Usuario;
import ar.edu.unse.siga.service.InventarioService;
import ar.edu.unse.siga.service.TramiteService;
import ar.edu.unse.siga.ui.base.CardPanel;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

/**
 * Dashboard de Inicio con métricas, accesos y “Trámites recientes”.
 * - Usa InventarioService para métricas.
 * - Usa TramiteService para cargar los últimos trámites.
 * - Expone recargarTramitesRecientes() para que TramiteEntradaPage refresque el widget
 *   inmediatamente después de registrar un nuevo trámite.
 */
public class HomePage extends JPanel {

    private final Usuario usuarioActual;
    private final InventarioService inventarioService;
    private final TramiteService tramiteService;           // NUEVO
    private final Consumer<String> onNavigate;

    private final DateTimeFormatter friendlyDate =
            DateTimeFormatter.ofPattern("dd 'de' MMMM yyyy", new Locale("es", "AR"));

    // Labels de métricas (para refrescar)
    private JLabel lblTareasPend;
    private JLabel lblAprobPend;
    private JLabel lblInsumosCriticos;
    private JLabel lblBalance;

    // Contenedor dinámico de “Trámites recientes”
    private JPanel recientesList;                          // NUEVO

    private Timer autoRefreshTimer;

    // ===== Constructores de compatibilidad =====
    public HomePage() { this(null, null, null, null); }
    public HomePage(Usuario usuarioActual) { this(usuarioActual, null, null, null); }

    // ===== Constructor recomendado =====
    public HomePage(Usuario usuarioActual,
                    InventarioService inventarioService,
                    TramiteService tramiteService,
                    Consumer<String> onNavigate) {
        this.usuarioActual = usuarioActual;
        this.inventarioService = inventarioService;
        this.tramiteService = tramiteService; // NUEVO
        this.onNavigate = onNavigate;

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

        // Primer refresco
        refreshMetrics();
        recargarTramitesRecientes(); // NUEVO

        // Auto-refresco (cada 30 s)
        autoRefreshTimer = new Timer(30_000, e -> {
            refreshMetrics();
            recargarTramitesRecientes();
        });
        autoRefreshTimer.setRepeats(true);
        autoRefreshTimer.start();
    }

    /* =========================  UI: Encabezado / Métricas  ========================= */

    private JComponent buildHeroSection() {
        CardPanel card = new CardPanel();
        card.setLayout(new BorderLayout(24, 18));

        JPanel headerRow = new JPanel(new BorderLayout());
        headerRow.setOpaque(false);

        JPanel greetings = new JPanel();
        greetings.setOpaque(false);
        greetings.setLayout(new BoxLayout(greetings, BoxLayout.Y_AXIS));
        String name = (usuarioActual != null && usuarioActual.getUsuario() != null)
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
        body.add(metricsRow()); // crea las tarjetas y guarda labels
        card.add(body, BorderLayout.CENTER);

        return card;
    }

    private Component buildHeaderActions() {
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 0));
        actions.setOpaque(false);

        JButton perfil = headerChip("Perfil");
        perfil.addActionListener(e -> navigateOrInfo("perfil"));

        JButton notif = headerChip("Notificaciones");
        notif.addActionListener(e -> JOptionPane.showMessageDialog(
                this, "Módulo de notificaciones en construcción.", "Info", JOptionPane.INFORMATION_MESSAGE));

        JButton ajustes = headerChip("Ajustes");
        ajustes.addActionListener(e -> navigateOrInfo("ajustes"));

        actions.add(perfil);
        actions.add(notif);
        actions.add(ajustes);
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

        JLabel chip = alertChip("Alertas importantes");
        chip.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        chip.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override public void mouseClicked(java.awt.event.MouseEvent e) { navigateOrInfo("informes"); }
        });

        banner.add(chip);
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

        row.add(metricCard("Tareas pendientes", lblTareasPend = new JLabel("0"),
                "Tareas por revisar", new Color(58, 109, 255)));
        row.add(metricCard("Aprob. pendientes", lblAprobPend = new JLabel("3"),
                "Órdenes de compra", new Color(86, 127, 255)));
        row.add(metricCard("Insumos críticos", lblInsumosCriticos = new JLabel("0"),
                "Requieren pedido urgente", new Color(255, 99, 99)));
        row.add(metricCard("Balance dpto", lblBalance = new JLabel("$145.200"),
                "Órdenes de compra", new Color(58, 182, 186)));
        return row;
    }

    private JPanel metricCard(String title, JLabel valueLabel, String hint, Color base) {
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

        valueLabel.setForeground(Color.WHITE);
        valueLabel.setFont(new Font("Segoe UI", Font.BOLD, 28));

        JLabel lblHint = new JLabel(hint);
        lblHint.setForeground(new Color(220, 231, 255));
        lblHint.setFont(new Font("Segoe UI", Font.PLAIN, 12));

        card.add(lblTitle, BorderLayout.NORTH);
        card.add(valueLabel, BorderLayout.CENTER);
        card.add(lblHint, BorderLayout.SOUTH);

        card.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        card.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override public void mouseClicked(java.awt.event.MouseEvent e) {
                switch (title.toLowerCase()) {
                    case "insumos críticos" -> navigateOrInfo("inventario");
                    case "aprob. pendientes" -> navigateOrInfo("tramites");
                    case "tareas pendientes" -> navigateOrInfo("tramites");
                    case "balance dpto"      -> navigateOrInfo("finanzas");
                }
            }
        });
        return card;
    }

    /* =========================  UI: Grilla central  ========================= */

    private JComponent buildDashboardGrid() {
        JPanel grid = new JPanel(new GridBagLayout());
        grid.setOpaque(false);
        GridBagConstraints gc = new GridBagConstraints();
        gc.insets = new Insets(0, 12, 18, 12);
        gc.fill = GridBagConstraints.BOTH;
        gc.weightx = 0.5;
        gc.weighty = 0.5;

        gc.gridx = 0; gc.gridy = 0; grid.add(upcomingEventsCard(), gc);
        gc.gridx = 1; gc.gridy = 0; grid.add(activityCard(), gc);
        gc.gridx = 0; gc.gridy = 1; grid.add(priorityCard(), gc);
        gc.gridx = 1; gc.gridy = 1; grid.add(recentTramitesCard(), gc);   // NUEVO: widget dinámico

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
        list.add(eventRow("Mañana: Cierre de mes", "Finanzas"));
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
                {"Hace 5 min", "Juan P. solicitó informe de inventario"},
                {"Hace 9 min", "Tito    P. solicitó informe de trámites"},
                {"Ayer", "Agus T. aprobó préstamo de proyector"},
                {"Ayer", "Tito P. aprobó préstamo de proyector"},}) {
            list.add(activityRow(row[0], row[1]));
            list.add(divider());
        }

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
        list.add(priorityRow("Aprobar orden #2034-08", false));
        list.add(priorityRow("Aprobar orden #2034-09", false));

        card.add(list, BorderLayout.CENTER);
        return card;
    }

    /* ====================  “Trámites recientes” dinámico  ==================== */

    private CardPanel recentTramitesCard() {
        CardPanel card = new CardPanel();
        card.setLayout(new BorderLayout(0, 12));
        card.add(sectionTitle("Trámites recientes"), BorderLayout.NORTH);

        recientesList = new JPanel();
        recientesList.setOpaque(false);
        recientesList.setLayout(new BoxLayout(recientesList, BoxLayout.Y_AXIS));
        recientesList.setBorder(new EmptyBorder(10, 10, 10, 10)); 


        JButton link = subtleLink("Ver historial completo");
        link.addActionListener(e -> navigateOrInfo("tramites"));

        // inicial: el contenido real lo carga recargarTramitesRecientes()
        recientesList.add(Box.createVerticalStrut(12));
        recientesList.add(link);

        card.add(recientesList, BorderLayout.CENTER);
        return card;
    }

    /** Público: refresca el listado de recientes desde TramiteService. */
    public void recargarTramitesRecientes() {
        if (recientesList == null) return;

        // preservar el link final
        Component last = recientesList.getComponent(recientesList.getComponentCount() - 1);
        recientesList.removeAll();

        try {
            List<Tramite> ult = (tramiteService != null)
                    ? tramiteService.tramitesRecientes(3)
                    : java.util.List.of();

            for (Tramite t : ult) {
                String asunto = "Asunto: " + (t.getAsunto() == null ? "-" : t.getAsunto());
                String estado = "Estado: " + (t.getEstado() == null ? "-" : t.getEstado().toUpperCase(Locale.ROOT));

                String badgeTxt = estadoFriendly(t.getEstado());
                Color badgeColor = switch (badgeTxt.toLowerCase(Locale.ROOT)) {
                    case "completado" -> new Color(73, 198, 154);
                    case "en proceso" -> new Color(86, 127, 255);
                    default -> new Color(255, 170, 70); // pendiente
                };

                recientesList.add(recentItem(asunto, estado, badgeTxt, badgeColor));
                recientesList.add(Box.createVerticalStrut(6));
            }
        } catch (Exception ignored) {
            // si falla, lo dejamos vacío y no rompemos la pantalla
        }

        recientesList.add(Box.createVerticalStrut(12));
        recientesList.add(last);
        recientesList.revalidate();
        recientesList.repaint();
    }

    /* =========================  Resto de helpers/estilos  ========================= */

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

        JButton btnTramite = primaryButton("Registrar nuevo trámite");
        btnTramite.addActionListener(e -> navigateOrInfo("tramites"));

        JButton btnOC = secondaryButton("Generar orden de compra");
        btnOC.addActionListener(e -> navigateOrInfo("finanzas"));

        JButton btnReportes = secondaryButton("Ver reportes");
        btnReportes.addActionListener(e -> navigateOrInfo("reportes"));

        buttons.add(btnTramite);
        buttons.add(btnOC);
        buttons.add(btnReportes);
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

    /* =========================  Lógica dinámica ========================= */

/** Refresca las métricas del dashboard. */
public void refreshMetrics() {
    // Mock inicial
    lblTareasPend.setText("7");
    lblAprobPend.setText("3");

    // Insumos críticos (stockActual < stockMinimo)
    if (inventarioService != null) {
        try {
            List<Insumo> insumos = inventarioService.listarTodos();
            int criticos = 0;
            for (Insumo i : insumos) {
                Integer min = i.getStockMinimo();
                if (min == null) continue;

                int actual;
                try {
                    actual = inventarioService.stockActual(i.getId() != null ? i.getId().longValue() : 0L);
                } catch (Exception ex) {
                    actual = 0;
                }
                if (actual < min) criticos++;
            }
            lblInsumosCriticos.setText(String.valueOf(criticos)); // <— OJO: 'criticos' sin cortes
        } catch (Exception ex) {
            // si falla, conservamos el último valor visible
        }
    }

    // Balance dpto (placeholder)
    lblBalance.setText("$145.200");
}


    /** Navega usando el callback, o informa si no está configurado. */
    private void navigateOrInfo(String pageKey) {
        if (onNavigate != null) onNavigate.accept(pageKey);
        else JOptionPane.showMessageDialog(this,
                "Navegación a: " + pageKey + " (configurá onNavigate en ShellFrame).",
                "Navegación", JOptionPane.INFORMATION_MESSAGE);
    }

    /** Llamar cuando cierres la ventana para detener el timer. */
    public void dispose() {
        if (autoRefreshTimer != null) autoRefreshTimer.stop();
    }

    /* =========================  Sub-helpers visuales  ========================= */

    private JLabel sideTitle(String text) {
        JLabel lbl = new JLabel(text);
        lbl.setFont(new Font("Segoe UI", Font.BOLD, 14));
        lbl.setForeground(new Color(54, 92, 190));
        return lbl;
    }

    private Component recentItem(String title, String subtitle, String badge, Color badgeColor) {
        JPanel row = new JPanel(new BorderLayout());
        row.setOpaque(false);
        JLabel lblTitle = new JLabel(title);
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 13));
        lblTitle.setForeground(new Color(43, 57, 120));
        JLabel lblSubtitle = new JLabel(subtitle);
        lblSubtitle.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lblSubtitle.setForeground(new Color(118, 133, 182));

        JPanel left = new JPanel();
        left.setOpaque(false);
        left.setLayout(new BoxLayout(left, BoxLayout.Y_AXIS));
        left.add(lblTitle);
        left.add(Box.createVerticalStrut(4));
        left.add(lblSubtitle);

        JLabel badgeLabel = new JLabel(badge);
        badgeLabel.setOpaque(true);
        badgeLabel.setBackground(badgeColor);
        badgeLabel.setForeground(Color.WHITE);
        badgeLabel.setFont(new Font("Segoe UI", Font.BOLD, 11));
        badgeLabel.setBorder(new EmptyBorder(4, 12, 4, 12));
        badgeLabel.setHorizontalAlignment(SwingConstants.CENTER);
        badgeLabel.setAlignmentY(Component.TOP_ALIGNMENT);

        row.add(left, BorderLayout.CENTER);
        row.add(badgeLabel, BorderLayout.EAST);
        row.setBorder(new EmptyBorder(4, 0, 4, 0));
        return row;
    }
    
    private String estadoFriendly(String estado) {
    if (estado == null) return "Pendiente";
    switch (estado.toUpperCase(java.util.Locale.ROOT)) {
        case "COMPLETADO":
        case "CERRADO":
            return "Completado";
        case "EN_PROCESO":
            return "En proceso";
        case "ALTA":
            return "Alta";
        case "PENDIENTE":
            return "Pendiente";
        default:
            return capitalize(estado);
    }
}

private String capitalize(String t) {
    if (t == null || t.isEmpty()) return "";
    return t.substring(0, 1).toUpperCase(java.util.Locale.ROOT)
            + t.substring(1).toLowerCase(java.util.Locale.ROOT);
}

private JButton subtleLink(String text) {
    JButton b = new JButton(text);
    b.setContentAreaFilled(false);
    b.setBorderPainted(false);
    b.setFocusPainted(false);
    b.setHorizontalAlignment(SwingConstants.LEFT);
    b.setForeground(new Color(68, 105, 220));
    b.setFont(new Font("Segoe UI", Font.BOLD, 12));
    return b;
}


}
