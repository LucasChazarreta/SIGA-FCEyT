package ar.edu.unse.siga.ui.reportes;

import ar.edu.unse.siga.domain.Categoria;
import ar.edu.unse.siga.domain.Insumo;
import ar.edu.unse.siga.domain.Tramite;
import ar.edu.unse.siga.domain.Movimiento;
import ar.edu.unse.siga.service.InventarioService;
import ar.edu.unse.siga.service.TramiteService;
import ar.edu.unse.siga.ui.base.CardPanel;
import ar.edu.unse.siga.ui.base.UiBus;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.*;
import javax.swing.text.MaskFormatter;
import java.awt.*;
import java.math.BigDecimal;
import java.lang.reflect.Method;
import java.text.ParseException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.Instant;
import java.util.Date;

import java.util.List;
import java.util.*;
import java.util.stream.Collectors;
import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

// OpenPDF
import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;

/**
 * Panel de Informes con tres secciones: INVENTARIO, SOLICITUDES y MOVIMIENTOS.
 * Incluye exportación a PDF/CSV y encabezado de PDF con logo, fecha, generado
 * por y filtros.
 */
public class InformesPanel extends JPanel {

    private JPanel pnlRetiros; // campo

    // ======= Colores base =======
    private static final Color BRAND = new Color(58, 96, 224);
    private static final Color CARD_BORDER = new Color(225, 230, 246);
    private static final Color HEADER_TXT = new Color(24, 63, 150);

    private final InventarioService invService;
    private final TramiteService traService;

    // Métricas
    private final JLabel lblTotalInsumos = bigValueLabel("-");
    private final JLabel lblTotalTramites = bigValueLabel("-");

    // --- INVENTARIO (filtros) ---
    private final JComboBox<Categoria> cbCategoria = new JComboBox<>();
    private final DateField dfDesde = new DateField();
    private final DateField dfHasta = new DateField();
    private final JCheckBox chkSoloBajoMinimo = new JCheckBox("Solo bajo mínimo");
    private final DefaultTableModel modelInv = new DefaultTableModel(
            new Object[]{"Código", "Descripción", "Estado", "Fecha"}, 0
    ) {
        @Override
        public boolean isCellEditable(int r, int c) {
            return false;
        }
    };

    // --- SOLICITUDES (filtros) ---
    private final JTextField filterSearch = new JTextField(18);
    private final JComboBox<String> filterEstado
            = new JComboBox<>(new String[]{"Todos", "Completado", "En proceso", "Pendiente"});
    private final DefaultTableModel modelTra = new DefaultTableModel(
            new Object[]{"ID Solicitud", "Solicitud", "Fecha creación", "Última actualización", "Descripción", "Solicitante", "Estado"}, 0
    ) {
        @Override
        public boolean isCellEditable(int r, int c) {
            return false;
        }
    };

    // --- MOVIMIENTOS (similar a Solicitudes de UI) ---
    private final JTextField filterSearchMov = new JTextField(18);
    private final DefaultTableModel modelMov = new DefaultTableModel(
            new Object[]{"Fecha", "Tipo", "Cantidad", "Código", "Descripción", "Ubicación", "Destino", "Solicitante"}, 0
    ) {
        @Override
        public boolean isCellEditable(int r, int c) {
            return false;
        }
    };

    // UI de contenido por tabs
    private final CardLayout contentCards = new CardLayout();
    private final JPanel content = new JPanel(contentCards);
    private JToggleButton btnInventario;
    private JToggleButton btnTramites;
    private JToggleButton btnMovimientos;

    private final javax.swing.table.DefaultTableModel retirosModel
            = new javax.swing.table.DefaultTableModel(
                    new Object[]{"# Solicitud", "Fecha", "Insumo", "Cantidad"}, 0) {
        @Override
        public boolean isCellEditable(int r, int c) {
            return false;
        }
    };
    private final javax.swing.JTable tblRetiros = new javax.swing.JTable(retirosModel);

    private static String fmtFecha(Object f) {
        try {
            if (f instanceof java.time.LocalDateTime ldt) {
                return ldt.format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            }
            if (f instanceof java.time.LocalDate ld) {
                return ld.format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            }
            return "-";
        } catch (Exception e) {
            return "-";
        }
    }

    private static final DateTimeFormatter FMT_FECHA_HHMMSS = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final DateTimeFormatter FMT_FECHA_DDMM = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private static String fmtFecha(java.time.LocalDateTime dt) {
        return dt == null ? "-" : dt.format(FMT_FECHA_HHMMSS);
    }

    private static String fmtFecha(java.time.LocalDate d) {
        return d == null ? "-" : d.format(FMT_FECHA_DDMM);
    }

    public InformesPanel(InventarioService invService, TramiteService traService) {
        this.invService = invService;
        this.traService = traService;

        setLayout(new BorderLayout(0, 12));
        setOpaque(false);
        setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        // 1) CREAR pnlRetiros PRIMERO
        pnlRetiros = new JPanel(new BorderLayout());
        pnlRetiros.add(new JLabel("Salidas recientes"), BorderLayout.NORTH);
        pnlRetiros.add(new JScrollPane(tblRetiros), BorderLayout.CENTER);
        pnlRetiros.setBorder(BorderFactory.createEmptyBorder(6, 0, 0, 0));

        add(buildHeader(), BorderLayout.NORTH);
        add(buildContentScrollable(), BorderLayout.CENTER);

        // Ocultar título duplicado del shell si lo hubiera
        SwingUtilities.invokeLater(() -> {
            Window w = SwingUtilities.getWindowAncestor(InformesPanel.this);
            if (w instanceof JFrame frame) {
                Container root = frame.getContentPane();
                JLabel headerTitle = findHeaderTitleLabel(root);
                if (headerTitle != null) {
                    headerTitle.setText("");
                }
            }
        });
        this.addHierarchyListener(e -> {
            if ((e.getChangeFlags() & java.awt.event.HierarchyEvent.SHOWING_CHANGED) != 0 && isShowing()) {
                SwingUtilities.invokeLater(() -> {
                    Window w = SwingUtilities.getWindowAncestor(InformesPanel.this);
                    if (w instanceof JFrame frame) {
                        JLabel headerTitle = findHeaderTitleLabel(frame.getContentPane());
                        if (headerTitle != null) {
                            headerTitle.setText("");
                        }
                    }
                });
            }
        });

        cargarCategoriasEnCombo();
        reloadMetrics();
        runQueryInventario();
        loadTableDataTramites();
        System.out.println("[InformesPanel] tramites cargados: " + modelTra.getRowCount());
        installFiltersTramites();

        loadTableDataMovimientos();
        installFiltersMovimientos();
        loadRetirosRecientes();

        cargarCategoriasEnCombo();
        reloadMetrics();
        runQueryInventario();
        loadTableDataTramites();
        installFiltersTramites();
        loadTableDataMovimientos();
        installFiltersMovimientos();

        UiBus.on("tramite-saved", evt -> reloadAfterTramiteSaved());
    }

    private void loadRetirosRecientes() {
        retirosModel.setRowCount(0);
        try {
            // Tomo SALIDAS de los últimos 7 días (ajustá si querés)
            java.time.LocalDate hoy = java.time.LocalDate.now();
            java.time.LocalDate desde = hoy.minusDays(7);
            java.time.LocalDate hasta = hoy;

            java.time.format.DateTimeFormatter fmt
                    = java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

            // Reuso lo que ya usás en Movimientos:
            java.util.List<ar.edu.unse.siga.domain.Movimiento> movs
                    = invService.movimientosPorFechaYTipo(desde, hasta, "SALIDA");

            for (ar.edu.unse.siga.domain.Movimiento m : movs) {
                String fecha = (m.getFecha() != null) ? m.getFecha().format(fmt) : "-";
                String insumo = (m.getInsumo() != null && m.getInsumo().getDescripcion() != null)
                        ? m.getInsumo().getDescripcion() : "-";
                String cantidad = (m.getCantidad() == null) ? "0"
                        : m.getCantidad().stripTrailingZeros().toPlainString();

                // #Solicitud: si tu Movimiento expone el id/nro de la solicitud, usalo;
                // si no, mostramos "-" hasta que lo agreguemos al modelo:
                String nroTramite = "-";
                try {
                    // Opción A (si existe getTramiteId):
                    var mt = m.getClass().getMethod("getTramiteId");
                    Object val = mt.invoke(m);
                    if (val != null) {
                        nroTramite = String.valueOf(val);
                    }
                } catch (Throwable ignore) {
                    try {
                        // Opción B (si existe getTramite()->getId()):
                        var mt = m.getClass().getMethod("getTramite");
                        Object tram = mt.invoke(m);
                        if (tram != null) {
                            var mid = tram.getClass().getMethod("getId");
                            Object id = mid.invoke(tram);
                            if (id != null) {
                                nroTramite = String.valueOf(id);
                            }
                        }
                    } catch (Throwable ignore2) {
                    }
                }

                retirosModel.addRow(new Object[]{nroTramite, fecha, insumo, cantidad});
            }

        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this,
                    "Error cargando Salidas recientes:\n" + ex.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    /*public void reloadAfterTramiteSaved() {
        // Si tenés métricas/tablitas ya implementadas, llamá a sus loaders aquí:
        // loadKpis(); loadRetirosRecientes(); loadStocks(); etc.
        // Al menos, recargá la sección que esperás ver actualizada:
        loadRetirosRecientes(); // ejemplo
        revalidate();
        repaint();
    }*/
    public void mostrarInventarioBajoMinimo() {
        if (btnInventario != null) {
            btnInventario.setSelected(true);
            contentCards.show(content, "INV");
        }
        chkSoloBajoMinimo.setSelected(true);
        runQueryInventario();
    }

    public void mostrarMovimientosSalidasHoy() {
        if (btnMovimientos != null) {
            btnMovimientos.setSelected(true);
            contentCards.show(content, "MOV");
        }
        cbTipoMov.setSelectedItem("Salida");
        cbPeriodoMov.setSelectedItem("Hoy");
        txtFiltroMov.setText("");
        loadTableDataMovimientos();
    }

    // ====== Header (título centrado + export) ======
    private JComponent buildHeader() {
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);

        JLabel title = new JLabel("INFORMES", SwingConstants.CENTER);
        title.setFont(title.getFont().deriveFont(Font.BOLD, 34f));
        title.setForeground(HEADER_TXT);

        JPanel center = new JPanel(new GridBagLayout());
        center.setOpaque(false);
        center.add(title);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        actions.setOpaque(false);
        JButton exportPdf = primaryButton("Exportar PDF");
        JButton exportCsv = outlineButton("Exportar CSV");

        exportPdf.addActionListener(e -> {
            if (btnTramites != null && btnTramites.isSelected()) {
                exportTramitesToPdf();
            } else if (btnMovimientos != null && btnMovimientos.isSelected()) {
                exportMovimientosToPdf();
            } else {
                exportInventarioToPdf();
            }
        });
        exportCsv.addActionListener(e -> {
            if (btnTramites != null && btnTramites.isSelected()) {
                exportTramitesToCsv();
            } else if (btnMovimientos != null && btnMovimientos.isSelected()) {
                exportMovimientosToCsv();
            } else {
                exportInventarioToCsv();
            }
        });

        actions.add(exportPdf);
        actions.add(exportCsv);

        // separador izquierdo para centrar visualmente
        JPanel spacer = new JPanel();
        spacer.setOpaque(false);
        spacer.setPreferredSize(actions.getPreferredSize());

        header.add(spacer, BorderLayout.WEST);
        header.add(center, BorderLayout.CENTER);
        header.add(actions, BorderLayout.EAST);
        header.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));
        return header;
    }

    // ====== Contenido con scroll general ======
    private JComponent buildContentScrollable() {
        JPanel wrapper = new JPanel(new BorderLayout(8, 18));
        wrapper.setOpaque(true);
        wrapper.setBackground(Color.WHITE);

        // Tabs tipo "pill"
        ButtonGroup tabs = new ButtonGroup();
        btnInventario = pill("INVENTARIO");
        btnTramites = pill("SOLICITUDES");
        btnMovimientos = pill("MOVIMIENTOS");
        btnInventario.setSelected(true);
        tabs.add(btnInventario);
        tabs.add(btnTramites);
        tabs.add(btnMovimientos);

        JPanel tabRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 18, 6));
        tabRow.setOpaque(true);
        tabRow.setBackground(Color.WHITE);
        tabRow.add(btnInventario);
        tabRow.add(btnTramites);
        tabRow.add(btnMovimientos);
        wrapper.add(tabRow, BorderLayout.NORTH);

        // INVENTARIO
        JPanel invPanel = new JPanel(new BorderLayout());
        invPanel.setOpaque(true);
        invPanel.setBackground(Color.WHITE);
        invPanel.add(buildMetrics(), BorderLayout.NORTH);
        invPanel.add(buildInventarioSplit(), BorderLayout.CENTER);

        // SOLICITUDES
        JPanel traPanel = new JPanel(new BorderLayout(12, 12));
        traPanel.setOpaque(true);
        traPanel.setBackground(Color.WHITE);
        traPanel.add(buildTramitesFilters(), BorderLayout.NORTH);
        traPanel.add(buildTramitesTableScroll(), BorderLayout.CENTER);

        // MOVIMIENTOS (misma estética que SOLICITUDES)
        JPanel movPanel = new JPanel(new BorderLayout(12, 12));
        movPanel.setOpaque(true);
        movPanel.setBackground(Color.WHITE);
        movPanel.add(buildMovimientosFilters(), BorderLayout.NORTH);
        movPanel.add(buildMovimientosTableScroll(), BorderLayout.CENTER);
        movPanel.add(pnlRetiros, BorderLayout.SOUTH);

        // Contenedor central
        content.setOpaque(true);
        content.setBackground(Color.WHITE);
        content.add(invPanel, "INV");
        content.add(traPanel, "TRA");
        content.add(movPanel, "MOV");

        wrapper.add(content, BorderLayout.CENTER);

        btnInventario.addActionListener(e -> contentCards.show(content, "INV"));
        btnTramites.addActionListener(e -> {
            contentCards.show(content, "TRA");
            loadTableDataTramites();
        });
        btnMovimientos.addActionListener(e -> {
            contentCards.show(content, "MOV");
            loadTableDataMovimientos();
        });

        // Fondo blanco para todo el panel
        setOpaque(true);
        setBackground(Color.WHITE);

        // Scroll general
        JScrollPane scroller = new JScrollPane(
                wrapper,
                ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER
        );
        scroller.setBorder(BorderFactory.createEmptyBorder());
        scroller.getViewport().setBackground(Color.WHITE);
        scroller.setBackground(Color.WHITE);
        scroller.getVerticalScrollBar().setUnitIncrement(18);

        return scroller;
    }

    // ====== INVENTARIO: división filtros/tabla ======
    private JPanel buildInventarioSplit() {
        JPanel split = new JPanel(new GridBagLayout());
        split.setOpaque(true);
        split.setBackground(Color.WHITE);

        GridBagConstraints gc = new GridBagConstraints();
        gc.gridy = 0;
        gc.fill = GridBagConstraints.BOTH;
        gc.weighty = 1.0;

        // IZQ: filtros
        JPanel filtros = buildFiltrosPanelInventario();
        JPanel holder = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        holder.setOpaque(false);
        holder.add(filtros);

        gc.gridx = 0;
        gc.weightx = 0.33;
        gc.insets = new Insets(16, 0, 0, 12);
        split.add(holder, gc);

        // DER: tabla
        CardPanel tabla = buildTablaPanelInventario();
        tabla.setOpaque(true);
        tabla.setBackground(Color.WHITE);

        gc.gridx = 1;
        gc.weightx = 0.67;
        gc.insets = new Insets(0, 0, 0, 0);
        split.add(tabla, gc);

        return split;
    }

    // === Panel de filtros INVENTARIO ===
    private JPanel buildFiltrosPanelInventario() {
        JPanel card = new JPanel(new BorderLayout());
        card.setOpaque(false);
        card.setBorder(BorderFactory.createEmptyBorder());

        // Título
        JLabel title = new JLabel("FILTROS");
        title.setFont(title.getFont().deriveFont(Font.BOLD, 18f));
        title.setForeground(new Color(73, 103, 204));
        JPanel titleRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        titleRow.setBorder(BorderFactory.createEmptyBorder(120, 0, 0, 0));
        titleRow.setOpaque(false);
        titleRow.add(title);
        card.add(titleRow, BorderLayout.NORTH);

        // Campos
        JPanel fields = new JPanel();
        fields.setOpaque(false);
        fields.setLayout(new BoxLayout(fields, BoxLayout.Y_AXIS));

        Dimension fieldSize = new Dimension(260, 34);
        cbCategoria.setPreferredSize(fieldSize);
        cbCategoria.setMaximumSize(fieldSize);

        JComponent desdeComp = dfDesde.getComponent();
        desdeComp.setPreferredSize(fieldSize);
        desdeComp.setMaximumSize(fieldSize);
        desdeComp.setMinimumSize(fieldSize);

        JComponent hastaComp = dfHasta.getComponent();
        hastaComp.setPreferredSize(fieldSize);
        hastaComp.setMaximumSize(fieldSize);
        hastaComp.setMinimumSize(fieldSize);

        fields.add(Box.createVerticalStrut(40));
        fields.add(leftField("Categoría", cbCategoria));
        fields.add(Box.createVerticalStrut(18));
        fields.add(leftField("Desde", desdeComp));
        fields.add(Box.createVerticalStrut(18));
        fields.add(leftField("Hasta", hastaComp));
        chkSoloBajoMinimo.setOpaque(false);
        chkSoloBajoMinimo.addActionListener(e -> runQueryInventario());
        fields.add(Box.createVerticalStrut(18));
        fields.add(chkSoloBajoMinimo);
        fields.add(Box.createVerticalStrut(28));

        // Botón
        JButton apply = primaryButton("APLICAR FILTROS");
        apply.addActionListener(e -> runQueryInventario());
        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        btnRow.setOpaque(false);
        btnRow.add(apply);
        fields.add(btnRow);

        // Centrado vertical
        JPanel center = new JPanel();
        center.setOpaque(false);
        center.setLayout(new BoxLayout(center, BoxLayout.Y_AXIS));
        center.add(Box.createVerticalGlue());
        center.add(fields);
        center.add(Box.createVerticalGlue());

        card.add(center, BorderLayout.CENTER);
        return card;
    }

    // Etiqueta a la izquierda + campo
    private JPanel leftField(String label, JComponent field) {
        JPanel p = new JPanel();
        p.setOpaque(false);
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));

        JLabel lbl = new JLabel(label.toUpperCase());
        lbl.setFont(lbl.getFont().deriveFont(Font.PLAIN, 12f));
        lbl.setForeground(new Color(35, 55, 110));
        lbl.setAlignmentX(Component.LEFT_ALIGNMENT);

        field.setAlignmentX(Component.LEFT_ALIGNMENT);
        field.setBorder(BorderFactory.createLineBorder(new Color(225, 230, 246)));

        p.add(lbl);
        p.add(Box.createVerticalStrut(4));
        p.add(field);
        return p;
    }

    private CardPanel buildTablaPanelInventario() {
        CardPanel card = new CardPanel();
        card.setLayout(new BorderLayout(10, 10));

        JLabel title = new JLabel("RESULTADOS");
        title.setFont(title.getFont().deriveFont(Font.BOLD, 18f));
        title.setForeground(BRAND);
        card.add(title, BorderLayout.NORTH);

        JTable table = new JTable(modelInv) {
            @Override
            public boolean getScrollableTracksViewportWidth() {
                return getParent() instanceof JViewport
                        && getPreferredSize().width < getParent().getWidth();
            }
        };
        table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        table.setRowHeight(40);
        table.setIntercellSpacing(new Dimension(0, 6));
        table.setShowGrid(true);
        table.setGridColor(new Color(232, 232, 232));
        table.setFillsViewportHeight(true);
        table.setDefaultEditor(Object.class, null);
        table.setFont(new Font("Segoe UI", Font.PLAIN, 13));

        // Anchos
        TableColumnModel tcm = table.getColumnModel();
        if (tcm.getColumnCount() >= 4) {
            tcm.getColumn(0).setPreferredWidth(200);
            tcm.getColumn(1).setPreferredWidth(330);
            tcm.getColumn(2).setPreferredWidth(140);
            tcm.getColumn(3).setPreferredWidth(140);
        }

        // Header
        JTableHeader header = table.getTableHeader();
        header.setPreferredSize(new Dimension(header.getPreferredSize().width, 42));
        header.setReorderingAllowed(false);
        header.setDefaultRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object v, boolean s, boolean f, int r, int c) {
                JLabel lbl = (JLabel) super.getTableCellRendererComponent(t, v, s, f, r, c);
                lbl.setHorizontalAlignment(SwingConstants.CENTER);
                lbl.setForeground(Color.WHITE);
                lbl.setBackground(new Color(58, 96, 224));
                lbl.setFont(lbl.getFont().deriveFont(Font.BOLD, 13f));
                lbl.setBorder(new EmptyBorder(10, 6, 10, 6));
                return lbl;
            }
        });

        // Contenido centrado + zebra
        table.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object v, boolean s, boolean f, int r, int c) {
                Component comp = super.getTableCellRendererComponent(t, v, s, f, r, c);
                setHorizontalAlignment(SwingConstants.CENTER);
                if (!s) {
                    comp.setBackground((r % 2 == 0) ? new Color(250, 252, 255) : Color.WHITE);
                }
                return comp;
            }
        });

        JScrollPane scroll = new JScrollPane(table,
                ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        beautifyScroll(scroll);
        scroll.setBorder(BorderFactory.createCompoundBorder(
                new javax.swing.border.LineBorder(new Color(232, 232, 232), 1, true),
                BorderFactory.createEmptyBorder(6, 6, 6, 6)
        ));
        scroll.getViewport().setBackground(Color.WHITE);

        card.add(scroll, BorderLayout.CENTER);
        return card;
    }

    // ==== Métricas (2 tarjetas centradas) ====
    private JComponent buildMetrics() {
        JPanel row = new JPanel(new FlowLayout(FlowLayout.CENTER, 16, 0));
        row.setOpaque(false);
        row.setBorder(BorderFactory.createEmptyBorder(6, 6, 12, 6));

        CardPanel c1 = metricCardGradient("TOTAL INSUMOS", lblTotalInsumos,
                new Color(70, 120, 255), new Color(110, 150, 255));
        CardPanel c2 = metricCardGradient("SOLICITUDES", lblTotalTramites,
                new Color(70, 120, 255), new Color(140, 170, 255));

        c1.setPreferredSize(new Dimension(420, 110));
        c2.setPreferredSize(new Dimension(420, 110));

        row.add(c1);
        row.add(c2);
        return row;
    }

    private CardPanel metricCardGradient(String title, JLabel value, Color c1, Color c2) {
        CardPanel card = new CardPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                GradientPaint gp = new GradientPaint(0, 0, c1, getWidth(), getHeight(), c2);
                g2.setPaint(gp);
                g2.fillRoundRect(4, 4, getWidth() - 8, getHeight() - 8, 18, 18);
                g2.dispose();
            }
        };

        card.setLayout(new BorderLayout());
        card.setOpaque(false);
        card.setBorder(new EmptyBorder(20, 24, 20, 24));

        JLabel lblTitle = new JLabel(title.toUpperCase());
        lblTitle.setForeground(new Color(230, 240, 255));
        lblTitle.setFont(lblTitle.getFont().deriveFont(Font.BOLD, 13f));

        value.setForeground(Color.WHITE);
        value.setFont(value.getFont().deriveFont(Font.BOLD, 36f));
        value.setHorizontalAlignment(SwingConstants.LEFT);

        card.add(lblTitle, BorderLayout.NORTH);
        card.add(value, BorderLayout.CENTER);

        return card;
    }

    // ==== Carga de datos Inventario ====
    private void cargarCategoriasEnCombo() {
        DefaultComboBoxModel<Categoria> model = new DefaultComboBoxModel<>();
        model.addElement(null); // "Todas"

        try {
            var categorias = invService.listarCategorias();
            for (Categoria c : categorias) {
                model.addElement(c);
            }
        } catch (Exception e) {
            System.err.println("No se pudieron cargar categorías: " + e.getMessage());
        }
        cbCategoria.setModel(model);

        cbCategoria.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                setText(value instanceof Categoria c ? c.getNombre() : "Todas");
                return this;
            }
        });
    }

    private void reloadMetrics() {
        try {
            lblTotalInsumos.setText(String.valueOf(invService.listarTodos().size()));
        } catch (Exception e) {
            lblTotalInsumos.setText("-");
        }

        try {
            var tramites = traService.listarTodos();
            lblTotalTramites.setText(String.valueOf(tramites.size()));
        } catch (Exception e) {
            lblTotalTramites.setText("-");
        }
    }

    private void runQueryInventario() {
        modelInv.setRowCount(0);
        try {
            Categoria sel = (Categoria) cbCategoria.getSelectedItem();
            String cat = (sel == null ? null : sel.getNombre());
            LocalDate d1 = dfDesde.getDate();
            LocalDate d2 = dfHasta.getDate();
            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy");

            List<Insumo> data = filtrarLocal(invService.listarTodos(), cat, d1, d2);
            if (chkSoloBajoMinimo.isSelected()) {
                data = data.stream()
                        .filter(i -> {
                            Integer minimo = i.getStockMinimo();
                            if (minimo == null || i.getId() == null) {
                                return false;
                            }
                            try {
                                BigDecimal stock = invService.stockActualExacto(i.getId());
                                return stock.compareTo(BigDecimal.valueOf(minimo)) < 0;
                            } catch (Exception ex) {
                                return false;
                            }
                        })
                        .collect(Collectors.toList());
            }
            for (Insumo i : data) {
                LocalDate fa = i.getFechaAlta();
                if (fa == null && i.getCreatedAt() != null) {
                    fa = java.time.ZonedDateTime.ofInstant(i.getCreatedAt(), java.time.ZoneId.systemDefault()).toLocalDate();
                }
                String fecha = (fa != null) ? fa.format(fmt) : "-";
                modelInv.addRow(new Object[]{i.getCodigo(), i.getDescripcion(), i.getEstado(), fecha});
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private static List<Insumo> filtrarLocal(List<Insumo> base, String cat, LocalDate d1, LocalDate d2) {
        return base.stream().filter(i -> {
            if (cat != null && i.getCategoria() != null) {
                if (!cat.equalsIgnoreCase(i.getCategoria().getNombre())) {
                    return false;
                }
            }
            LocalDate fa = i.getFechaAlta();
            if (fa == null && i.getCreatedAt() != null) {
                fa = java.time.ZonedDateTime.ofInstant(i.getCreatedAt(), java.time.ZoneId.systemDefault()).toLocalDate();
            }
            if (d1 != null && (fa == null || fa.isBefore(d1))) {
                return false;
            }
            if (d2 != null && (fa == null || fa.isAfter(d2))) {
                return false;
            }
            return true;
        }).collect(Collectors.toList());
    }

    // ====== SOLICITUDES ======
    private Component buildTramitesFilters() {
        JPanel panel = new JPanel(new BorderLayout(18, 0));
        panel.setOpaque(false);

        JLabel lbl = new JLabel("FILTRO");
        lbl.setFont(new Font("Segoe UI", Font.BOLD, 16));
        lbl.setForeground(BRAND);
        panel.add(lbl, BorderLayout.WEST);

        JPanel fields = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        fields.setOpaque(false);

        styleFilterField(filterSearch, 220);
        filterSearch.putClientProperty("JTextField.placeholderText", "Buscar");
        fields.add(filterSearch);

        styleFilterField(filterEstado, 160);
        fields.add(filterEstado);

        panel.add(fields, BorderLayout.CENTER);
        return panel;
    }

    private JScrollPane buildTramitesTableScroll() {
        JTable table = new JTable(modelTra) {
            @Override
            public boolean getScrollableTracksViewportWidth() {
                return getParent() instanceof JViewport
                        && getPreferredSize().width < getParent().getWidth();
            }
        };
        table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        table.setRowHeight(44);
        table.setShowHorizontalLines(false);
        table.setShowVerticalLines(false);
        table.setIntercellSpacing(new Dimension(0, 0));
        table.setFillsViewportHeight(true);
        table.setSelectionBackground(new Color(226, 233, 255));
        table.setSelectionForeground(new Color(32, 48, 105));
        table.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        table.setAutoCreateRowSorter(true);

        // Anchos
        TableColumnModel tcm = table.getColumnModel();
        if (tcm.getColumnCount() >= 8) {
            tcm.getColumn(0).setPreferredWidth(120);
            tcm.getColumn(1).setPreferredWidth(240);
            tcm.getColumn(2).setPreferredWidth(160);
            tcm.getColumn(3).setPreferredWidth(170);
            tcm.getColumn(4).setPreferredWidth(300);
            tcm.getColumn(5).setPreferredWidth(180);
            tcm.getColumn(6).setPreferredWidth(130);
            tcm.getColumn(7).setPreferredWidth(180);
        }

        JTableHeader header = table.getTableHeader();
        header.setPreferredSize(new Dimension(header.getPreferredSize().width, 46));
        header.setDefaultRenderer(new TableHeaderRenderer());

        table.getColumnModel().getColumn(6).setCellRenderer(new BadgeRenderer(BadgeRenderer.Type.STATUS));

        // Zebra para el resto
        DefaultTableCellRenderer zebra = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object v, boolean s, boolean f, int r, int c) {
                Component comp = super.getTableCellRendererComponent(t, v, s, f, r, c);
                setHorizontalAlignment(CENTER);
                if (!s) {
                    comp.setBackground((r % 2 == 0) ? new Color(250, 252, 255) : Color.WHITE);
                }
                return comp;
            }
        };
        for (int i = 0; i < 5; i++) {
            table.getColumnModel().getColumn(i).setCellRenderer(zebra);
        }

        JScrollPane scroll = new JScrollPane(table,
                ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        beautifyScroll(scroll);

        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.getViewport().setBackground(Color.WHITE);
        scroll.getVerticalScrollBar().setPreferredSize(new Dimension(10, 0));
        scroll.setBackground(Color.WHITE);

        return scroll;
    }

    private void styleFilterField(JComponent c, int w) {
        Dimension d = new Dimension(w, 32);
        c.setPreferredSize(d);
        c.setMinimumSize(d);
        c.setBorder(BorderFactory.createLineBorder(CARD_BORDER));
    }

    private void loadTableDataTramites() {
        modelTra.setRowCount(0);
        try {
            String search = filterSearch.getText().trim().toLowerCase(Locale.ROOT);
            String estadoFiltro = (String) filterEstado.getSelectedItem();
            if (estadoFiltro == null) {
                estadoFiltro = "Todos";
            }

            List<Tramite> tramites = traService.listarTodos();
            if (tramites == null) {
                return;
            }

            //DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            for (Tramite t : tramites) {
                if (!search.isEmpty()) {
                    String texto = (String.valueOf(t.getAsunto()) + " " + String.valueOf(t.getNro()) + " "
                            + (t.getDescripcion() != null ? t.getDescripcion() : "")).toLowerCase(Locale.ROOT);
                    if (!texto.contains(search)) {
                        continue;
                    }
                }

                String estado = estadoFriendly(t.getEstado());
                if (!"Todos".equals(estadoFiltro) && !estado.equalsIgnoreCase(estadoFiltro)) {
                    continue;
                }

                //String actualizacion = t.getFecha() == null ? "-" : t.getFecha().format(fmt);
                //String ultima = t.getFecha() == null ? "-" : t.getFecha().plusDays(1).format(fmt); // placeholder
                String actualizacion = fmtFecha(t.getFecha());
                String ultima = actualizacion; // o calculá algo real si tenés "updatedAt"

                String descripcion = extraerDescripcionTramite(t);
                String solicitante = (t.getSolicitante() == null || t.getSolicitante().isBlank()) ? "-" : t.getSolicitante();

                modelTra.addRow(new Object[]{t.getNro(), t.getAsunto(), actualizacion, ultima, descripcion, solicitante, estado});
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void installFiltersTramites() {
        if (filterSearch.getDocument() != null) {
            filterSearch.getDocument().addDocumentListener(new SimpleDocumentListener() {
                @Override
                public void update() {
                    loadTableDataTramites();
                }
            });
        }
        filterEstado.addActionListener(e -> loadTableDataTramites());
        filterEstado.setSelectedItem("Todos");
    }

    private String extraerDescripcionTramite(Object t) {
        if (t == null) {
            return "-";
        }
        try {
            Method m = t.getClass().getMethod("getDescripcion");
            Object val = m.invoke(t);
            if (val != null) {
                String s = val.toString().trim();
                if (!s.isEmpty()) {
                    return s;
                }
            }
        } catch (NoSuchMethodException ignore) {
        } catch (Throwable ignore) {
        }

        String v = tryGetter(t, "getDescripción", "getDetalle", "getDetalles", "getObservacion", "getObservación",
                "getObservaciones", "getNota", "getNotas", "getComentario", "getComentarios",
                "getMotivo", "getResumen", "getInfo", "getInformacion", "getInformación");
        if (v != null && !v.isBlank()) {
            return v.trim();
        }

        try {
            for (Method m : t.getClass().getMethods()) {
                if (m.getParameterCount() == 0 && m.getName().startsWith("get") && m.getReturnType() == String.class) {
                    String name = m.getName().toLowerCase(Locale.ROOT);
                    if (name.contains("desc") || name.contains("detalle") || name.contains("observ")
                            || name.contains("nota") || name.contains("coment") || name.contains("motivo")) {
                        Object val = m.invoke(t);
                        if (val != null) {
                            String s = val.toString().trim();
                            if (!s.isEmpty()) {
                                return s;
                            }
                        }
                    }
                }
            }
        } catch (Throwable ignore) {
        }

        try {
            for (java.lang.reflect.Field f : t.getClass().getDeclaredFields()) {
                String n = f.getName().toLowerCase(Locale.ROOT);
                if (n.contains("desc") || n.contains("detalle") || n.contains("observ")
                        || n.contains("nota") || n.contains("coment") || n.contains("motivo")) {
                    f.setAccessible(true);
                    Object val = f.get(t);
                    if (val != null) {
                        String s = val.toString().trim();
                        if (!s.isEmpty()) {
                            return s;
                        }
                    }
                }
            }
        } catch (Throwable ignore) {
        }
        return "-";
    }

    private static String tryGetter(Object obj, String... getters) {
        for (String g : getters) {
            try {
                Method m = obj.getClass().getMethod(g);
                Object val = m.invoke(obj);
                if (val != null) {
                    String s = val.toString().trim();
                    if (!s.isEmpty()) {
                        return s;
                    }
                }
            } catch (NoSuchMethodException ignore) {
            } catch (Throwable ignore) {
            }
        }
        return null;
    }

    private String estadoFriendly(String estado) {
        if (estado == null) {
            return "Pendiente";
        }
        String e = estado.trim().toLowerCase(Locale.ROOT);
        if (e.contains("comp")) {
            return "Completado";
        }
        if (e.contains("proc")) {
            return "En proceso";
        }
        if (e.contains("pend")) {
            return "Pendiente";
        }
        if (e.contains("alta")) {
            return "Alta";
        }
        return "Pendiente";
    }

    // ====== MOVIMIENTOS ======
    // === MOVIMIENTOS: filtros ===
    private final JTextField filterMovSearch = new JTextField(18);
    private final JComboBox<String> filterMovTipo
            = new JComboBox<>(new String[]{"Todos", "Entrada", "Salida"});
    private final JComboBox<String> cbPeriodoMov
            = new JComboBox<>(new String[]{"Todos", "Hoy", "Últimos 7 días"});

    // == MOVIMIENTOS: fila de filtros (usa SIEMPRE los campos txtFiltroMov y cbTipoMov) ==
    private JComponent buildMovimientosFilters() {
        JPanel panel = new JPanel(new BorderLayout(18, 0));
        panel.setOpaque(false);

        JLabel lbl = new JLabel("FILTRO");
        lbl.setFont(new Font("Segoe UI", Font.BOLD, 16));
        lbl.setForeground(BRAND);
        panel.add(lbl, BorderLayout.WEST);

        JPanel fields = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        fields.setOpaque(false);

        // Usa los CAMPOS, NO crear variables locales nuevas:
        styleFilterField(txtFiltroMov, 220);
        txtFiltroMov.putClientProperty("JTextField.placeholderText", "Buscar");
        fields.add(txtFiltroMov);

        styleFilterField(cbTipoMov, 140); // {Todos, Entrada, Salida}
        fields.add(cbTipoMov);

        styleFilterField(cbPeriodoMov, 140);
        fields.add(cbPeriodoMov);

        // Listeners para refrescar:
        if (txtFiltroMov.getDocument() != null) {
            txtFiltroMov.getDocument().addDocumentListener(new SimpleDocumentListener() {
                @Override
                public void update() {
                    loadTableDataMovimientos();
                }
            });
        }
        cbTipoMov.addActionListener(e -> loadTableDataMovimientos());
        cbPeriodoMov.addActionListener(e -> loadTableDataMovimientos());

        panel.add(fields, BorderLayout.CENTER);
        return panel;
    }

    private JScrollPane buildMovimientosTableScroll() {
        JTable table = new JTable(modelMov) {
            @Override
            public boolean getScrollableTracksViewportWidth() {
                return getParent() instanceof JViewport
                        && getPreferredSize().width < getParent().getWidth();
            }
        };
        table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        table.setRowHeight(44);
        table.setShowHorizontalLines(false);
        table.setShowVerticalLines(false);
        table.setIntercellSpacing(new Dimension(0, 0));
        table.setFillsViewportHeight(true);
        table.setSelectionBackground(new Color(226, 233, 255));
        table.setSelectionForeground(new Color(32, 48, 105));
        table.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        table.setAutoCreateRowSorter(true);

        // Anchos
        TableColumnModel tcm = table.getColumnModel();
        if (tcm.getColumnCount() >= 8) {
            tcm.getColumn(0).setPreferredWidth(160); // Fecha
            tcm.getColumn(1).setPreferredWidth(90);  // Tipo
            tcm.getColumn(2).setPreferredWidth(110); // Cantidad
            tcm.getColumn(3).setPreferredWidth(140); // Código
            tcm.getColumn(4).setPreferredWidth(220); // Descripción
            tcm.getColumn(5).setPreferredWidth(160); // Ubicación
            tcm.getColumn(6).setPreferredWidth(200); // Destino
            tcm.getColumn(7).setPreferredWidth(180); // Solicitante
        }

        JTableHeader header = table.getTableHeader();
        header.setPreferredSize(new Dimension(header.getPreferredSize().width, 46));
        header.setDefaultRenderer(new TableHeaderRenderer());

        // Zebra
        DefaultTableCellRenderer zebra = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object v, boolean s, boolean f, int r, int c) {
                Component comp = super.getTableCellRendererComponent(t, v, s, f, r, c);
                setHorizontalAlignment(CENTER);
                if (!s) {
                    comp.setBackground((r % 2 == 0) ? new Color(250, 252, 255) : Color.WHITE);
                }
                return comp;
            }
        };
        for (int i = 0; i < 7; i++) {
            table.getColumnModel().getColumn(i).setCellRenderer(zebra);
        }

        JScrollPane scroll = new JScrollPane(table,
                ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        beautifyScroll(scroll);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.getViewport().setBackground(Color.WHITE);
        scroll.getVerticalScrollBar().setPreferredSize(new Dimension(10, 0));
        scroll.setBackground(Color.WHITE);

        return scroll;
    }

    private void loadTableDataMovimientos() {
        modelMov.setRowCount(0);
        try {
            String search = (txtFiltroMov != null && txtFiltroMov.getText() != null)
                    ? txtFiltroMov.getText().trim().toLowerCase()
                    : "";

            String tipoSel = (cbTipoMov != null && cbTipoMov.getSelectedItem() != null)
                    ? cbTipoMov.getSelectedItem().toString().trim()
                    : "Todos";

            String periodoSel = (cbPeriodoMov != null && cbPeriodoMov.getSelectedItem() != null)
                    ? cbPeriodoMov.getSelectedItem().toString().trim()
                    : "Todos";

            String tipoFiltro = switch (tipoSel.toUpperCase()) {
                case "ENTRADA" ->
                    "ENTRADA";
                case "SALIDA" ->
                    "SALIDA";
                default ->
                    null;
            };

            java.time.LocalDate hoy = java.time.LocalDate.now();
            java.time.LocalDate desde = null;
            java.time.LocalDate hasta = null;
            switch (periodoSel.toUpperCase()) {
                case "HOY" -> {
                    desde = hoy;
                    hasta = hoy;
                }
                case "ÚLTIMOS 7 DÍAS" -> {
                    hasta = hoy;
                    desde = hoy.minusDays(6);
                }
            }

            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

            List<Movimiento> movimientos = invService.movimientosPorFechaYTipo(desde, hasta, tipoFiltro);
            for (Movimiento m : movimientos) {
                String codigo = (m.getInsumo() != null && m.getInsumo().getCodigo() != null)
                        ? m.getInsumo().getCodigo() : "-";
                String desc = (m.getInsumo() != null && m.getInsumo().getDescripcion() != null)
                        ? m.getInsumo().getDescripcion() : "-";
                String ubic = (m.getInsumo() != null && m.getInsumo().getUbicacion() != null
                        && !m.getInsumo().getUbicacion().isBlank())
                        ? m.getInsumo().getUbicacion() : "-";

                String solicitante = (m.getSolicitante() == null || m.getSolicitante().isBlank())
                        ? "-" : m.getSolicitante();

                if (!search.isEmpty()) {
                    String src = (codigo + " " + desc + " " + ubic
                            + " " + (m.getDestinoFuente() == null ? "" : m.getDestinoFuente())
                            + " " + solicitante)
                            .toLowerCase();
                    if (!src.contains(search)) {
                        continue;
                    }
                }

                String fecha = m.getFecha() != null ? m.getFecha().format(fmt) : "-";
                String cantidad = formatCantidad(m.getCantidad());
                String destino = (m.getDestinoFuente() == null || m.getDestinoFuente().isBlank())
                        ? "-" : m.getDestinoFuente();

                modelMov.addRow(new Object[]{fecha, m.getTipo(), cantidad, codigo, desc, ubic, destino, solicitante});
            }

        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this,
                    "Error cargando informe de movimientos:\n" + ex.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private String formatCantidad(java.math.BigDecimal valor) {
        if (valor == null) {
            return "0";
        }
        return valor.stripTrailingZeros().toPlainString();
    }

    // --- MOVIMIENTOS (filtros) ---
    private final JTextField txtFiltroMov = new JTextField(18);
    private final JComboBox<String> cbTipoMov
            = new JComboBox<>(new String[]{"Todos", "Entrada", "Salida"});

    private void installFiltersMovimientos() {
        if (filterSearchMov.getDocument() != null) {
            filterSearchMov.getDocument().addDocumentListener(new SimpleDocumentListener() {
                @Override
                public void update() {
                    loadTableDataMovimientos();
                }
            });
        }
    }

    // ===== Encabezado y badges (TRÁMITES y MOVIMIENTOS headers) =====
    static class TableHeaderRenderer extends DefaultTableCellRenderer {

        TableHeaderRenderer() {
            setHorizontalAlignment(LEFT);
            setFont(new Font("Segoe UI", Font.BOLD, 13));
            setForeground(new Color(70, 88, 140));
            setBackground(new Color(238, 242, 255));
            setBorder(new EmptyBorder(12, 14, 12, 14));
        }

        @Override
        public Component getTableCellRendererComponent(JTable t, Object v, boolean s, boolean f, int r, int c) {
            super.getTableCellRendererComponent(t, v, s, f, r, c);
            setText(v == null ? "" : v.toString().toUpperCase(Locale.ROOT));
            return this;
        }
    }

    static class BadgeRenderer extends DefaultTableCellRenderer {

        enum Type {
            PRIORITY, STATUS
        }
        private final Type type;

        BadgeRenderer(Type type) {
            this.type = type;
            setHorizontalAlignment(CENTER);
            setFont(new Font("Segoe UI", Font.BOLD, 12));
        }

        @Override
        public Component getTableCellRendererComponent(JTable t, Object v, boolean s, boolean f, int r, int c) {
            super.getTableCellRendererComponent(t, v, s, f, r, c);
            String text = v == null ? "-" : v.toString();
            setText(text);
            setOpaque(true);
            setBorder(new EmptyBorder(6, 12, 6, 12));
            Color base;
            String normalized = text.toLowerCase(Locale.ROOT);
            if (type == Type.PRIORITY) {
                if (normalized.contains("alta")) {
                    base = new Color(255, 120, 102);
                } else if (normalized.contains("media")) {
                    base = new Color(255, 188, 75);
                } else {
                    base = new Color(140, 198, 62);
                }
            } else {
                if (normalized.contains("complet")) {
                    base = new Color(28, 184, 113);
                } else if (normalized.contains("proceso")) {
                    base = new Color(58, 96, 224);
                } else if (normalized.contains("alta")) {
                    base = new Color(220, 84, 84);
                } else {
                    base = new Color(180, 180, 180);
                }
            }
            setForeground(Color.WHITE);
            setBackground(s ? base.darker() : base);
            return this;
        }
    }

    // ====== Filtros para PDF ======
    private String getFiltroCategoria() {
        try {
            Object sel = cbCategoria.getSelectedItem();
            if (sel == null) {
                return null;
            }
            int idx = cbCategoria.getSelectedIndex();
            if (idx == 0) {
                return null; // "Todas"
            }
            if (sel instanceof ar.edu.unse.siga.domain.Categoria c) {
                String nombre = c.getNombre();
                return (nombre == null || nombre.isBlank()) ? null : nombre;
            }
            String s = sel.toString();
            return (s == null || s.isBlank() || s.equalsIgnoreCase("todas")) ? null : s;
        } catch (Throwable t) {
            return null;
        }
    }

    private String getFiltroFechaDesde() {
        LocalDate d = dfDesde.getDate();
        return (d == null) ? null : d.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
    }

    private String getFiltroFechaHasta() {
        LocalDate d = dfHasta.getDate();
        return (d == null) ? null : d.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
    }

    // ====== Export ======
    private void exportInventarioToPdf() {
        String categoria = getFiltroCategoria();
        String fDesde = getFiltroFechaDesde();
        String fHasta = getFiltroFechaHasta();

        exportModelToPdf(
                "INFORME DE INVENTARIO",
                new String[]{"Código", "Descripción", "Estado", "Fecha"},
                modelInv,
                categoria, fDesde, fHasta
        );
    }

    private void exportTramitesToPdf() {
        String estadoFiltro = (String) filterEstado.getSelectedItem();
        if (estadoFiltro != null && estadoFiltro.equalsIgnoreCase("Todos")) {
            estadoFiltro = null;
        }
        // Ajuste de headers para que coincida con modelTra (7 columnas, incluida "Solicitante")
        exportModelToPdf(
                "INFORME DE SOLICITUDES",
                new String[]{"ID Solicitud", "Solicitud", "Fecha creación", "Última actualización", "Descripción", "Solicitante", "Estado"},
                modelTra,
                estadoFiltro,
                null,
                null
        );
    }

    private void exportMovimientosToPdf() {
        String filtroTipo = "Todos";
        try {
            if (cbTipoMov != null && cbTipoMov.getSelectedItem() != null) {
                filtroTipo = cbTipoMov.getSelectedItem().toString();
            }
        } catch (Exception ignore) {
        }

        exportModelToPdf(
                "INFORME DE MOVIMIENTOS",
                new String[]{"Fecha", "Tipo", "Cantidad", "Código", "Descripción", "Ubicación", "Destino", "Solicitante"},
                modelMov,
                filtroTipo,
                null,
                null
        );
    }

    private void exportInventarioToCsv() {
        exportModelToCsv(modelInv, ',');
    }

    private void exportTramitesToCsv() {
        exportModelToCsv(modelTra, ',');
    }

    private void exportMovimientosToCsv() {
        exportModelToCsv(modelMov, ',');
    }

    // ===== Compat: firma anterior =====
    private void exportModelToPdf(String titulo, String[] headers, DefaultTableModel model) {
        exportModelToPdf(titulo, headers, model, null, null, null);
    }

    // ===== Core PDF con encabezado (logo, fecha, generado por, filtros opcionales)
    private void exportModelToPdf(String titulo, String[] headers, DefaultTableModel model,
            String categoriaFiltro, String fechaDesdeFiltro, String fechaHastaFiltro) {
        JFileChooser fc = new JFileChooser();
        fc.setDialogTitle("Guardar informe en PDF");
        fc.setFileFilter(new FileNameExtensionFilter("Archivo PDF (*.pdf)", "pdf"));
        fc.setSelectedFile(new File(titulo.toLowerCase().replace(" ", "-") + ".pdf"));
        int opt = fc.showSaveDialog(this);
        if (opt != JFileChooser.APPROVE_OPTION) {
            return;
        }

        File out = fc.getSelectedFile();
        if (!out.getName().toLowerCase().endsWith(".pdf")) {
            out = new File(out.getParentFile(), out.getName() + ".pdf");
        }

        Document doc = new Document(PageSize.A4.rotate(), 36, 36, 36, 36);
        try (FileOutputStream fos = new FileOutputStream(out)) {
            PdfWriter.getInstance(doc, fos);
            doc.open();

            PdfPTable head = new PdfPTable(2);
            head.setWidthPercentage(100);
            head.setWidths(new float[]{1f, 1f});
            head.getDefaultCell().setBorder(PdfPCell.NO_BORDER);

            PdfPCell left = new PdfPCell();
            left.setBorder(PdfPCell.NO_BORDER);
            try {
                com.lowagie.text.Image logo = com.lowagie.text.Image.getInstance("ui/icons/logo-fceyt.png");
                logo.scaleToFit(90, 90);
                logo.setAlignment(com.lowagie.text.Image.ALIGN_LEFT);
                left.addElement(logo);
            } catch (Exception ignore) {
                com.lowagie.text.Font fOrg = new com.lowagie.text.Font(
                        com.lowagie.text.Font.HELVETICA, 12, com.lowagie.text.Font.BOLD, Color.BLACK);
                left.addElement(new Paragraph("SIGA-FCEyT", fOrg));
            }
            head.addCell(left);

            com.lowagie.text.Font fFecha = new com.lowagie.text.Font(
                    com.lowagie.text.Font.HELVETICA, 10, com.lowagie.text.Font.NORMAL, Color.BLACK);
            String fechaHoy = new java.text.SimpleDateFormat("dd/MM/yyyy").format(new java.util.Date());
            Paragraph pFecha = new Paragraph("Fecha: " + fechaHoy, fFecha);
            pFecha.setAlignment(Element.ALIGN_RIGHT);

            PdfPCell right = new PdfPCell();
            right.setBorder(PdfPCell.NO_BORDER);
            right.addElement(pFecha);
            head.addCell(right);

            doc.add(head);

            String generadoPor = "Administrador";
            com.lowagie.text.Font fGen = new com.lowagie.text.Font(
                    com.lowagie.text.Font.HELVETICA, 10, com.lowagie.text.Font.NORMAL, Color.BLACK);
            Paragraph pGen = new Paragraph("Generado por: " + generadoPor, fGen);
            pGen.setSpacingBefore(4f);
            pGen.setSpacingAfter(2f);
            doc.add(pGen);

            StringBuilder filtros = new StringBuilder();
            if (categoriaFiltro != null && !categoriaFiltro.isBlank()) {
                String etiqueta;
                if ("INFORME DE SOLICITUDES".equalsIgnoreCase(titulo)) {
                    etiqueta = "Estado";
                } else if ("INFORME DE MOVIMIENTOS".equalsIgnoreCase(titulo)) {
                    etiqueta = "Movimiento";
                } else {
                    etiqueta = "Categoría";
                }
                filtros.append(etiqueta).append(": ").append(categoriaFiltro);
            }
            if ((fechaDesdeFiltro != null && !fechaDesdeFiltro.isBlank())
                    || (fechaHastaFiltro != null && !fechaHastaFiltro.isBlank())) {
                if (filtros.length() > 0) {
                    filtros.append("    |    ");
                }
                filtros.append("Rango: ");
                filtros.append(fechaDesdeFiltro != null && !fechaDesdeFiltro.isBlank() ? fechaDesdeFiltro : "—");
                filtros.append(" – ");
                filtros.append(fechaHastaFiltro != null && !fechaHastaFiltro.isBlank() ? fechaHastaFiltro : "—");
            }
            if (filtros.length() > 0) {
                com.lowagie.text.Font fFilt = new com.lowagie.text.Font(
                        com.lowagie.text.Font.HELVETICA, 9, com.lowagie.text.Font.ITALIC, Color.BLACK);
                Paragraph pFilt = new Paragraph(filtros.toString(), fFilt);
                pFilt.setSpacingAfter(8f);
                doc.add(pFilt);
            }

            com.lowagie.text.Font fTitle = new com.lowagie.text.Font(
                    com.lowagie.text.Font.HELVETICA, 18, com.lowagie.text.Font.BOLD, HEADER_TXT);
            Paragraph title = new Paragraph(titulo, fTitle);
            title.setAlignment(Element.ALIGN_CENTER);
            title.setSpacingAfter(8f);
            doc.add(title);

            PdfPTable table = new PdfPTable(headers.length);
            table.setWidthPercentage(100);
            float[] w = new float[headers.length];
            Arrays.fill(w, 1f);
            table.setWidths(w);

            com.lowagie.text.Font fHeader = new com.lowagie.text.Font(
                    com.lowagie.text.Font.HELVETICA, 11, com.lowagie.text.Font.BOLD, Color.WHITE);
            for (String h : headers) {
                PdfPCell cell = new PdfPCell(new Phrase(h, fHeader));
                cell.setHorizontalAlignment(Element.ALIGN_CENTER);
                cell.setBackgroundColor(BRAND);
                cell.setPadding(6f);
                table.addCell(cell);
            }

            com.lowagie.text.Font fCell = new com.lowagie.text.Font(
                    com.lowagie.text.Font.HELVETICA, 10, com.lowagie.text.Font.NORMAL, Color.BLACK);
            for (int r = 0; r < model.getRowCount(); r++) {
                for (int c = 0; c < model.getColumnCount(); c++) {
                    Object v = model.getValueAt(r, c);
                    String txt = (v == null) ? "" : v.toString();
                    PdfPCell cell = new PdfPCell(new Phrase(txt, fCell));
                    cell.setHorizontalAlignment(c == 1 ? Element.ALIGN_LEFT : Element.ALIGN_CENTER);
                    cell.setPadding(5f);
                    table.addCell(cell);
                }
            }

            doc.add(table);

            if ("INFORME DE MOVIMIENTOS".equalsIgnoreCase(titulo)) {
                com.lowagie.text.Font fSub = new com.lowagie.text.Font(
                        com.lowagie.text.Font.HELVETICA, 13, com.lowagie.text.Font.BOLD, HEADER_TXT);

                com.lowagie.text.Font fCell2 = new com.lowagie.text.Font(
                        com.lowagie.text.Font.HELVETICA, 10, com.lowagie.text.Font.NORMAL, Color.BLACK);

                Paragraph sep = new Paragraph("\nTRAZABILIDAD DE MOVIMIENTOS", fSub);
                sep.setSpacingBefore(10f);
                sep.setSpacingAfter(6f);
                doc.add(sep);

                Map<String, Insumo> byCodigo = new HashMap<>();
                try {
                    for (Insumo ins : invService.listarTodos()) {
                        if (ins.getCodigo() != null) {
                            byCodigo.put(ins.getCodigo(), ins);
                        }
                    }
                } catch (Exception ignore) {
                }

                DateTimeFormatter fmtMov = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

                for (int r = 0; r < model.getRowCount(); r++) {
                    String codigo = Objects.toString(model.getValueAt(r, 0), "");
                    String descripcion = Objects.toString(model.getValueAt(r, 1), "");
                    Insumo ins = byCodigo.get(codigo);
                    if (ins == null) {
                        continue;
                    }

                    List<Movimiento> movs;
                    try {
                        movs = invService.ultimosMovimientos(ins.getId(), 200);
                    } catch (Exception ex) {
                        continue;
                    }
                    if (movs == null || movs.isEmpty()) {
                        continue;
                    }

                    Paragraph pIns = new Paragraph(
                            String.format("%s - %s", codigo, descripcion),
                            new com.lowagie.text.Font(com.lowagie.text.Font.HELVETICA, 11, com.lowagie.text.Font.BOLD, Color.BLACK)
                    );
                    pIns.setSpacingBefore(6f);
                    pIns.setSpacingAfter(2f);
                    doc.add(pIns);

                    PdfPTable tMov = new PdfPTable(5);
                    tMov.setWidthPercentage(100);
                    tMov.setWidths(new float[]{1.2f, 0.8f, 0.6f, 1.2f, 1.2f});

                    for (String h : List.of("Fecha", "Tipo", "Cantidad", "Destino", "Solicitante")) {
                        PdfPCell hc = new PdfPCell(new Phrase(h, fHeader));
                        hc.setHorizontalAlignment(Element.ALIGN_CENTER);
                        hc.setBackgroundColor(BRAND);
                        hc.setPadding(5f);
                        tMov.addCell(hc);
                    }

                    for (Movimiento m : movs) {
                        String f = (m.getFecha() == null) ? "" : m.getFecha().format(fmtMov);
                        String tipo = Objects.toString(m.getTipo(), "");
                        String cant = String.valueOf(m.getCantidad() == null ? 0 : m.getCantidad());
                        String dest = (m.getDestinoFuente() == null || m.getDestinoFuente().isBlank()) ? "-" : m.getDestinoFuente();
                        String sol = (m.getSolicitante() == null || m.getSolicitante().isBlank()) ? "-" : m.getSolicitante();

                        PdfPCell c1 = new PdfPCell(new Phrase(f, fCell2));
                        c1.setHorizontalAlignment(Element.ALIGN_CENTER);
                        c1.setPadding(4f);
                        tMov.addCell(c1);

                        PdfPCell c2 = new PdfPCell(new Phrase(tipo, fCell2));
                        c2.setHorizontalAlignment(Element.ALIGN_CENTER);
                        c2.setPadding(4f);
                        tMov.addCell(c2);

                        PdfPCell c3 = new PdfPCell(new Phrase(cant, fCell2));
                        c3.setHorizontalAlignment(Element.ALIGN_CENTER);
                        c3.setPadding(4f);
                        tMov.addCell(c3);

                        PdfPCell c4 = new PdfPCell(new Phrase(dest, fCell2));
                        c4.setHorizontalAlignment(Element.ALIGN_LEFT);
                        c4.setPadding(4f);
                        tMov.addCell(c4);

                        PdfPCell c5 = new PdfPCell(new Phrase(sol, fCell2));
                        c5.setHorizontalAlignment(Element.ALIGN_LEFT);
                        c5.setPadding(4f);
                        tMov.addCell(c5);
                    }

                    doc.add(tMov);
                }
            }

            doc.close();
            JOptionPane.showMessageDialog(this, "PDF generado:\n" + out.getAbsolutePath(),
                    "Exportar PDF", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception ex) {
            try {
                doc.close();
            } catch (Exception ignore) {
            }
            JOptionPane.showMessageDialog(this, "No se pudo generar el PDF:\n" + ex.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void exportModelToCsv(DefaultTableModel model, char sep) {
        JFileChooser fc = new JFileChooser();
        fc.setDialogTitle("Guardar informe en CSV");
        fc.setFileFilter(new FileNameExtensionFilter("Archivo CSV (*.csv)", "csv"));
        fc.setSelectedFile(new File("informe.csv"));
        int opt = fc.showSaveDialog(this);
        if (opt != JFileChooser.APPROVE_OPTION) {
            return;
        }

        File out = fc.getSelectedFile();
        if (!out.getName().toLowerCase().endsWith(".csv")) {
            out = new File(out.getParentFile(), out.getName() + ".csv");
        }

        Charset enc = StandardCharsets.UTF_8;
        try (PrintWriter pw = new PrintWriter(out, enc)) {
            for (int c = 0; c < model.getColumnCount(); c++) {
                if (c > 0) {
                    pw.print(sep);
                }
                pw.print(csvEscape(model.getColumnName(c)));
            }
            pw.println();

            for (int r = 0; r < model.getRowCount(); r++) {
                for (int c = 0; c < model.getColumnCount(); c++) {
                    if (c > 0) {
                        pw.print(sep);
                    }
                    Object v = model.getValueAt(r, c);
                    pw.print(csvEscape(v == null ? "" : String.valueOf(v)));
                }
                pw.println();
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "No se pudo generar el CSV:\n" + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        JOptionPane.showMessageDialog(this, "CSV generado:\n" + out.getAbsolutePath(), "Exportar CSV", JOptionPane.INFORMATION_MESSAGE);
    }

    private static String csvEscape(String s) {
        boolean needQuotes = s.contains(",") || s.contains(";") || s.contains("\"") || s.contains("\n") || s.contains("\r");
        String x = s.replace("\"", "\"\"");
        return needQuotes ? "\"" + x + "\"" : x;
    }

    // ====== Utilidades ======
    private static class DateField {

        private final JComponent comp;
        private final boolean usesFlatPicker;
        private final DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy");

        DateField() {
            JComponent c;
            boolean ok = false;
            try {
                Class<?> cls = Class.forName("com.formdev.flatlaf.extras.components.FlatDatePicker");
                c = (JComponent) cls.getDeclaredConstructor().newInstance();
                Method setFormatString = cls.getMethod("setFormatString", String.class);
                Method setPlaceholderText = cls.getMethod("setPlaceholderText", String.class);
                Method setEditable = cls.getMethod("setEditable", boolean.class);
                setFormatString.invoke(c, "dd/MM/yyyy");
                setPlaceholderText.invoke(c, "dd/mm/aaaa");
                setEditable.invoke(c, false);
                ok = true;
            } catch (Throwable ignore) {
                c = createMasked();
            }
            this.comp = c;
            this.usesFlatPicker = ok;
            Dimension size = new Dimension(120, 30);
            comp.setPreferredSize(size);
            comp.setMaximumSize(size);
        }

        private static JFormattedTextField createMasked() {
            try {
                MaskFormatter mf = new MaskFormatter("##/##/####");
                mf.setPlaceholderCharacter('_');
                JFormattedTextField tf = new JFormattedTextField(mf);
                tf.putClientProperty("JComponent.roundRect", true);
                tf.putClientProperty("JTextField.placeholderText", "dd/mm/aaaa");
                return tf;
            } catch (ParseException e) {
                return new JFormattedTextField();
            }
        }

        LocalDate getDate() {
            if (usesFlatPicker) {
                try {
                    Method getDate = comp.getClass().getMethod("getDate");
                    Object val = getDate.invoke(comp);
                    return (val instanceof LocalDate) ? (LocalDate) val : null;
                } catch (Throwable ignore) {
                    return null;
                }
            } else {
                String txt = ((JFormattedTextField) comp).getText();
                if (txt == null) {
                    return null;
                }
                txt = txt.trim();
                if (txt.isEmpty() || txt.contains("_")) {
                    return null;
                }
                try {
                    return LocalDate.parse(txt, fmt);
                } catch (Exception e) {
                    return null;
                }
            }
        }

        JComponent getComponent() {
            return comp;
        }
    }

    /**
     * Busca un JLabel "Informes" en la barra superior del shell.
     */
    private static JLabel findHeaderTitleLabel(Container root) {
        for (Component c : root.getComponents()) {
            if (c instanceof JLabel l && "Informes".equals(l.getText())) {
                Point p = SwingUtilities.convertPoint(l.getParent(), l.getLocation(), root);
                if (p.y < 120) {
                    return l;
                }
            }
            if (c instanceof Container ct) {
                JLabel r = findHeaderTitleLabel(ct);
                if (r != null) {
                    return r;
                }
            }
        }
        return null;
    }

    private static abstract class SimpleDocumentListener implements javax.swing.event.DocumentListener {

        public abstract void update();

        @Override
        public void insertUpdate(javax.swing.event.DocumentEvent e) {
            update();
        }

        @Override
        public void removeUpdate(javax.swing.event.DocumentEvent e) {
            update();
        }

        @Override
        public void changedUpdate(javax.swing.event.DocumentEvent e) {
            update();
        }
    }

    /**
     * ScrollBarUI moderno para JScrollPane dado (vertical y horizontal).
     */
    private static void beautifyScroll(JScrollPane sp) {
        java.util.function.Supplier<javax.swing.plaf.basic.BasicScrollBarUI> uiFactory = ()
                -> new javax.swing.plaf.basic.BasicScrollBarUI() {
            private final Color TRACK = new Color(240, 245, 255);
            private final Color THUMB = new Color(58, 96, 224);
            private final Color THUMB_HOVER = new Color(40, 70, 190);

            @Override
            protected JButton createDecreaseButton(int orientation) {
                return zeroButton();
            }

            @Override
            protected JButton createIncreaseButton(int orientation) {
                return zeroButton();
            }

            private JButton zeroButton() {
                JButton b = new JButton();
                b.setPreferredSize(new Dimension(0, 0));
                b.setMinimumSize(new Dimension(0, 0));
                b.setMaximumSize(new Dimension(0, 0));
                b.setFocusable(false);
                b.setBorderPainted(false);
                b.setContentAreaFilled(false);
                return b;
            }

            @Override
            protected void paintTrack(Graphics g, JComponent c, Rectangle r) {
                g.setColor(TRACK);
                g.fillRect(r.x, r.y, r.width, r.height);
            }

            @Override
            protected void paintThumb(Graphics g, JComponent c, Rectangle r) {
                if (!scrollbar.isEnabled() || r.width <= 0 || r.height <= 0) {
                    return;
                }
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                boolean hover = isThumbRollover();
                g2.setColor(hover ? THUMB_HOVER : THUMB);
                int arc = 10;
                g2.fillRoundRect(r.x + 2, r.y + 2, r.width - 4, r.height - 4, arc, arc);
                g2.dispose();
            }
        };

        sp.getVerticalScrollBar().setUI(uiFactory.get());
        sp.getHorizontalScrollBar().setUI(uiFactory.get());
        sp.getVerticalScrollBar().setPreferredSize(new Dimension(10, 0));
        sp.getHorizontalScrollBar().setPreferredSize(new Dimension(0, 10));
    }

    private static JButton primaryButton(String text) {
        JButton b = new JButton(text);
        b.setBackground(BRAND);
        b.setForeground(Color.WHITE);
        b.setFont(b.getFont().deriveFont(Font.BOLD, 14f));
        b.setFocusPainted(false);
        b.setBorder(BorderFactory.createEmptyBorder(10, 16, 10, 16));
        b.putClientProperty("JButton.buttonType", "roundRect");
        return b;
    }

    private static JButton outlineButton(String text) {
        JButton b = new JButton(text);
        b.setBackground(Color.WHITE);
        b.setForeground(BRAND);
        b.setFont(b.getFont().deriveFont(Font.BOLD, 14f));
        b.setFocusPainted(false);
        b.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BRAND, 1, true),
                new EmptyBorder(9, 15, 9, 15)
        ));
        b.putClientProperty("JButton.buttonType", "roundRect");
        return b;
    }

    private JToggleButton pill(String text) {
        final Color blue = new Color(0x0B, 0x2F, 0xB5);

        JToggleButton t = new JToggleButton(text) {
            @Override
            protected void paintComponent(Graphics g) {
                if (isSelected()) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g2.setColor(blue);
                    g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                    g2.dispose();
                }
                super.paintComponent(g);
            }
        };

        t.setFocusPainted(false);
        t.setOpaque(false);
        t.setContentAreaFilled(false);
        t.setFont(t.getFont().deriveFont(Font.BOLD, 16f));
        t.setBackground(Color.WHITE);
        t.setForeground(blue);
        t.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(blue, 1, true),
                new EmptyBorder(10, 20, 10, 20)
        ));
        t.addChangeListener(e -> t.setForeground(t.isSelected() ? Color.WHITE : blue));
        t.putClientProperty("JButton.buttonType", "square");

        return t;
    }

    private static JLabel bigValueLabel(String txt) {
        JLabel l = new JLabel(txt);
        l.setFont(l.getFont().deriveFont(Font.BOLD, 28f));
        l.setForeground(Color.WHITE);
        l.setHorizontalAlignment(SwingConstants.LEFT);
        return l;
    }

    // =======================
    // MÉTODOS NUEVOS PÚBLICOS
    // =======================
    /**
     * Llamar esto APENAS termina OK el guardado del trámite.
     */
    public void reloadAfterTramiteSaved() {
        SwingUtilities.invokeLater(() -> {
            try {
                reloadMetrics();
                loadTableDataTramites();
                loadTableDataMovimientos();
            } catch (Exception ignore) {
            }
        });
    }

    /**
     * Útil si sólo querés refrescar la grilla de Solicitudes.
     */
    public void reloadTramitesUI() {
        SwingUtilities.invokeLater(this::loadTableDataTramites);
    }

    /**
     * Útil si sólo querés refrescar la grilla de Movimientos.
     */
    public void reloadMovimientosUI() {
        SwingUtilities.invokeLater(this::loadTableDataMovimientos);
    }
}
