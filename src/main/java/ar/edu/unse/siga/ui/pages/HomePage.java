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
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.math.BigDecimal;

/**
 * Dashboard de Inicio con métricas y actividad reciente dinámica.
 * - Usa InventarioService para métricas en tiempo real.
 * - Usa TramiteService para cargar las últimas solicitudes registradas.
 * - Expone recargarTramitesRecientes() para que TramiteEntradaPage actualice la actividad
 *   inmediatamente después de registrar una nueva solicitud.
 */
public class HomePage extends JPanel {

    private final Usuario usuarioActual;
    private final InventarioService inventarioService;
    private final TramiteService tramiteService;
    public interface HomeNavigation {
        void navigateTo(String key);
        void openInventarioBajoMinimo();
        void openSolicitudesNuevas();
        void openMovimientosHoy();
    }

    private final HomeNavigation navigation;

    // Labels de métricas (para refrescar)
    private JLabel lblInsumosCriticos;
    private JLabel lblSolicitudesNuevas;
    private JLabel lblSalidasHoy;
    private JLabel lblAlertStock;

    // Contenedor dinámico de la tarjeta de actividad
    private JPanel actividadList;

    private Timer autoRefreshTimer;

    // ===== Constructores de compatibilidad =====
    public HomePage() { this(null, null, null, null); }
    public HomePage(Usuario usuarioActual) { this(usuarioActual, null, null, null); }

    // ===== Constructor recomendado =====
    public HomePage(Usuario usuarioActual,
                    InventarioService inventarioService,
                    TramiteService tramiteService,
                    HomeNavigation navigation) {
        this.usuarioActual = usuarioActual;
        this.inventarioService = inventarioService;
        this.tramiteService = tramiteService;
        this.navigation = navigation;

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

        add(layout, BorderLayout.CENTER);

        // Primer refresco
        refreshMetrics();
        refrescarActividadReciente();

        // Auto-refresco (cada 30 s)
        autoRefreshTimer = new Timer(30_000, e -> {
            refreshMetrics();
            refrescarActividadReciente();
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
        lblAlertStock = alertMessage("-");
        banner.add(lblAlertStock);
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
        JPanel row = new JPanel(new GridLayout(1, 3, 16, 0));
        row.setOpaque(false);

        row.add(metricCard("Insumos bajo mínimo", lblInsumosCriticos = new JLabel("0"),
                "Stock por reponer", new Color(255, 99, 99), this::openInventarioBajoMinimo));
        row.add(metricCard("Solicitudes nuevas", lblSolicitudesNuevas = new JLabel("0"),
                "Ingresadas recientemente", new Color(86, 127, 255), this::openSolicitudesNuevas));
        row.add(metricCard("Últimas salidas (hoy)", lblSalidasHoy = new JLabel("0"),
                "Movimientos registrados hoy", new Color(58, 182, 186), this::openMovimientosHoy));
        return row;
    }

    private JPanel metricCard(String title, JLabel valueLabel, String hint, Color base, Runnable onClick) {
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
                if (onClick != null) onClick.run();
            }
        });
        return card;
    }

    /* =========================  UI: Grilla central  ========================= */

    private JComponent buildDashboardGrid() {
        JPanel grid = new JPanel(new GridBagLayout());
        grid.setOpaque(false);

        GridBagConstraints gc = new GridBagConstraints();
        gc.gridx = 0;
        gc.gridy = 0;
        gc.insets = new Insets(0, 12, 18, 12);
        gc.fill = GridBagConstraints.BOTH;
        gc.weightx = 1;
        gc.weighty = 1;

        grid.add(activityCard(), gc);
        return grid;
    }

    private CardPanel activityCard() {
        CardPanel card = new CardPanel();
        card.setLayout(new BorderLayout(0, 12));
        card.add(sectionTitle("Actividad reciente"), BorderLayout.NORTH);

        actividadList = new JPanel();
        actividadList.setOpaque(false);
        actividadList.setLayout(new BoxLayout(actividadList, BoxLayout.Y_AXIS));

        card.add(actividadList, BorderLayout.CENTER);
        return card;
    }

    /** Público: refresca la tarjeta de actividad desde TramiteService. */
    public void recargarTramitesRecientes() {
        refrescarActividadReciente();
    }

    /* =========================  Resto de helpers/estilos  ========================= */

    private JLabel sectionTitle(String text) {
        JLabel lbl = new JLabel(text);
        lbl.setFont(new Font("Segoe UI", Font.BOLD, 16));
        lbl.setForeground(new Color(35, 55, 110));
        return lbl;
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

    private JSeparator divider() {
        JSeparator sep = new JSeparator();
        sep.setForeground(new Color(230, 236, 250));
        sep.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
        return sep;
    }

    /* =========================  Lógica dinámica ========================= */

    /** Refresca las métricas del dashboard. */
    public void refreshMetrics() {
        int criticos = 0;
        int tramitesNuevos = 0;
        int salidasHoy = 0;

        if (tramiteService != null) {
            try {
                List<Tramite> tramites = tramiteService.listarTodos();
                tramitesNuevos = (int) tramites.stream()
                        .filter(t -> {
                            String estado = t.getEstado();
                            return estado != null && "NUEVO".equalsIgnoreCase(estado.trim());
                        })
                        .count();
            } catch (Exception ignored) {
            }
        }

        if (inventarioService != null) {
            try {
                List<Insumo> insumos = inventarioService.listarTodos();
                for (Insumo i : insumos) {
                    Integer min = i.getStockMinimo();
                    Long id = i.getId();
                    BigDecimal actual = BigDecimal.ZERO;
                    if (id != null) {
                        try {
                            actual = inventarioService.stockActualExacto(id);
                        } catch (Exception ex) {
                            actual = BigDecimal.ZERO;
                        }
                    }
                    if (min != null && actual.compareTo(BigDecimal.valueOf(min)) < 0) {
                        criticos++;
                    }
                }
                salidasHoy = inventarioService.movimientosPorFechaYTipo(java.time.LocalDate.now(),
                        java.time.LocalDate.now(), "SALIDA").size();
            } catch (Exception ignored) {
                // dejamos los valores previos si algo falla
            }
        }

        lblInsumosCriticos.setText(String.valueOf(criticos));
        lblSolicitudesNuevas.setText(String.valueOf(tramitesNuevos));
        lblSalidasHoy.setText(String.valueOf(salidasHoy));
        updateAlertStock(criticos);
    }

    private void updateAlertStock(int criticos) {
        if (lblAlertStock != null) {
            lblAlertStock.setText("Insumos con stock mínimo: " + criticos);
        }
    }

    private void refrescarActividadReciente() {
        if (actividadList == null) return;

        Runnable task = () -> {
            actividadList.removeAll();

            List<Tramite> ultimos = List.of();
            if (tramiteService != null) {
                try {
                    ultimos = tramiteService.tramitesRecientes(5);
                } catch (Exception ignored) {
                    ultimos = List.of();
                }
            }

            if (ultimos.isEmpty()) {
                JLabel empty = new JLabel("Sin actividad reciente");
                empty.setFont(new Font("Segoe UI", Font.ITALIC, 13));
                empty.setForeground(new Color(120, 136, 180));
                actividadList.add(empty);
            } else {
                for (int i = 0; i < ultimos.size(); i++) {
                    Tramite t = ultimos.get(i);
                    String when = tiempoRelativo(t.getFecha());
                    String desc = descripcionActividad(t);
                    actividadList.add(activityRow(when, desc));
                    if (i < ultimos.size() - 1) {
                        actividadList.add(divider());
                    }
                }
            }

            actividadList.revalidate();
            actividadList.repaint();
        };

        if (SwingUtilities.isEventDispatchThread()) {
            task.run();
        } else {
            SwingUtilities.invokeLater(task);
        }
    }


    /** Navega usando el callback, o informa si no está configurado. */
    private void openInventarioBajoMinimo() {
        if (navigation != null) navigation.openInventarioBajoMinimo();
        else navigateOrInfo("inventario");
    }

    private void openSolicitudesNuevas() {
        if (navigation != null) navigation.openSolicitudesNuevas();
        else navigateOrInfo("tramites");
    }

    private void openMovimientosHoy() {
        if (navigation != null) navigation.openMovimientosHoy();
        else navigateOrInfo("movimientos");
    }

    private void navigateOrInfo(String pageKey) {
        if (navigation != null) navigation.navigateTo(pageKey);
        else JOptionPane.showMessageDialog(this,
                "Navegación a: " + pageKey + " (configurá onNavigate en ShellFrame).",
                "Navegación", JOptionPane.INFORMATION_MESSAGE);
    }

    /** Llamar cuando cierres la ventana para detener el timer. */
    public void dispose() {
        if (autoRefreshTimer != null) autoRefreshTimer.stop();
    }

    /* =========================  Sub-helpers visuales  ========================= */

    private String estadoFriendly(String estado) {
        if (estado == null) return "Pendiente";
        switch (estado.toUpperCase(Locale.ROOT)) {
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
        return t.substring(0, 1).toUpperCase(Locale.ROOT)
                + t.substring(1).toLowerCase(Locale.ROOT);
    }

    private String tiempoRelativo(LocalDateTime fecha) {
        if (fecha == null) return "-";
        Duration dur = Duration.between(fecha, LocalDateTime.now());
        long minutos = dur.toMinutes();
        if (minutos < 1) return "Hace instantes";
        if (minutos < 60) return "Hace " + minutos + " min";
        long horas = dur.toHours();
        if (horas < 24) return "Hace " + horas + " h";
        long dias = dur.toDays();
        if (dias == 1) return "Ayer";
        if (dias < 7) return "Hace " + dias + " días";
        return fecha.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
    }

    private String descripcionActividad(Tramite t) {
        String nro = (t.getNro() == null || t.getNro().isBlank()) ? "-" : t.getNro();
        String asunto = (t.getAsunto() == null || t.getAsunto().isBlank()) ? "Solicitud sin asunto" : t.getAsunto();
        String estado = estadoFriendly(t.getEstado());
        String solicitante = (t.getSolicitante() == null || t.getSolicitante().isBlank())
                ? ""
                : " · " + t.getSolicitante();
        return "#" + nro + " - " + asunto + " (" + estado + ")" + solicitante;
    }
}
