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
import java.time.format.TextStyle;

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
import com.lowagie.text.Chunk;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Image;
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

    // Colores PDF (java.awt.Color)
    private static final Color PDF_PRIMARY = new Color(0x1E, 0x3A, 0x8A);
    private static final Color PDF_LIGHT_GRAY = new Color(0xE5, 0xE7, 0xEB);
    private static final Color PDF_BORDER = new Color(0xD1, 0xD5, 0xDB);
    private static final Color PDF_ROW_ALT = new Color(0xF5, 0xF6, 0xFA);

// Fuentes (OpenPDF acepta java.awt.Color)
    private static final com.lowagie.text.Font FONT_HEADER_INFO
            = new com.lowagie.text.Font(com.lowagie.text.Font.HELVETICA, 11, com.lowagie.text.Font.NORMAL, Color.BLACK);
    private static final com.lowagie.text.Font FONT_HEADER_TITLE
            = new com.lowagie.text.Font(com.lowagie.text.Font.HELVETICA, 14, com.lowagie.text.Font.BOLD, Color.BLACK);
    private static final com.lowagie.text.Font FONT_HEADER_DATE
            = new com.lowagie.text.Font(com.lowagie.text.Font.HELVETICA, 10, com.lowagie.text.Font.NORMAL, Color.BLACK);
    private static final com.lowagie.text.Font FONT_SECTION
            = new com.lowagie.text.Font(com.lowagie.text.Font.HELVETICA, 12, com.lowagie.text.Font.BOLD, PDF_PRIMARY);
    private static final com.lowagie.text.Font FONT_TABLE_HEADER
            = new com.lowagie.text.Font(com.lowagie.text.Font.HELVETICA, 10, com.lowagie.text.Font.BOLD, Color.WHITE);
    private static final com.lowagie.text.Font FONT_TABLE_CELL
            = new com.lowagie.text.Font(com.lowagie.text.Font.HELVETICA, 9, com.lowagie.text.Font.NORMAL, Color.BLACK);
    private static final com.lowagie.text.Font FONT_SUMMARY_LABEL
            = new com.lowagie.text.Font(com.lowagie.text.Font.HELVETICA, 10, com.lowagie.text.Font.BOLD, Color.BLACK);
    private static final com.lowagie.text.Font FONT_SUMMARY_VALUE
            = new com.lowagie.text.Font(com.lowagie.text.Font.HELVETICA, 10, com.lowagie.text.Font.NORMAL, Color.BLACK);
    private static final com.lowagie.text.Font FONT_FOOTER
            = new com.lowagie.text.Font(com.lowagie.text.Font.HELVETICA, 8, com.lowagie.text.Font.ITALIC, new Color(80, 80, 80));

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
        JPanel panel = new JPanel(new BorderLayout(12, 12));
        panel.setOpaque(true);
        panel.setBackground(Color.WHITE);

        panel.add(buildFiltrosPanelInventario(), BorderLayout.NORTH);
        panel.add(buildTablaPanelInventario(), BorderLayout.CENTER);

        return panel;
    }

    // === Panel de filtros INVENTARIO ===
    private JPanel buildFiltrosPanelInventario() {
        JPanel panel = new JPanel(new BorderLayout(18, 8));
        panel.setOpaque(false);

        JLabel title = new JLabel("FILTROS");
        title.setFont(new Font("Segoe UI", Font.BOLD, 16));
        title.setForeground(BRAND);
        panel.add(title, BorderLayout.WEST);

        JPanel fields = new JPanel(new FlowLayout(FlowLayout.LEFT, 16, 8));
        fields.setOpaque(false);

        styleFilterField(cbCategoria, 200);
        fields.add(labeledFilterField("Categoría", cbCategoria));

        JComponent desdeComp = dfDesde.getComponent();
        styleFilterField(desdeComp, 160);
        fields.add(labeledFilterField("Desde", desdeComp));

        JComponent hastaComp = dfHasta.getComponent();
        styleFilterField(hastaComp, 160);
        fields.add(labeledFilterField("Hasta", hastaComp));

        chkSoloBajoMinimo.setOpaque(false);
        chkSoloBajoMinimo.addActionListener(e -> runQueryInventario());
        fields.add(wrapCheckbox(chkSoloBajoMinimo));

        JButton apply = primaryButton("APLICAR FILTROS");
        apply.addActionListener(e -> runQueryInventario());
        fields.add(wrapButton(apply));

        panel.add(fields, BorderLayout.CENTER);
        return panel;
    }

    private JPanel labeledFilterField(String label, JComponent field) {
        JPanel wrapper = new JPanel();
        wrapper.setOpaque(false);
        wrapper.setLayout(new BoxLayout(wrapper, BoxLayout.Y_AXIS));

        JLabel lbl = new JLabel(label.toUpperCase());
        lbl.setFont(lbl.getFont().deriveFont(Font.PLAIN, 12f));
        lbl.setForeground(new Color(35, 55, 110));
        lbl.setAlignmentX(Component.LEFT_ALIGNMENT);

        field.setAlignmentX(Component.LEFT_ALIGNMENT);

        wrapper.add(lbl);
        wrapper.add(Box.createVerticalStrut(4));
        wrapper.add(field);
        return wrapper;
    }

    private JPanel wrapCheckbox(JCheckBox check) {
        JPanel wrapper = new JPanel();
        wrapper.setOpaque(false);
        wrapper.setLayout(new BoxLayout(wrapper, BoxLayout.Y_AXIS));
        wrapper.add(Box.createVerticalStrut(16));
        check.setAlignmentX(Component.LEFT_ALIGNMENT);
        wrapper.add(check);
        return wrapper;
    }

    private JPanel wrapButton(JButton button) {
        JPanel wrapper = new JPanel();
        wrapper.setOpaque(false);
        wrapper.setLayout(new BoxLayout(wrapper, BoxLayout.Y_AXIS));
        wrapper.add(Box.createVerticalStrut(16));
        button.setAlignmentX(Component.LEFT_ALIGNMENT);
        wrapper.add(button);
        return wrapper;
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
        JPanel panel = new JPanel(new BorderLayout(18, 8));
        panel.setOpaque(false);

        JLabel lbl = new JLabel("FILTRO");
        lbl.setFont(new Font("Segoe UI", Font.BOLD, 16));
        lbl.setForeground(BRAND);
        panel.add(lbl, BorderLayout.WEST);

        JPanel fields = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 8));
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
        JPanel panel = new JPanel(new BorderLayout(18, 8));
        panel.setOpaque(false);

        JLabel lbl = new JLabel("FILTRO");
        lbl.setFont(new Font("Segoe UI", Font.BOLD, 16));
        lbl.setForeground(BRAND);
        panel.add(lbl, BorderLayout.WEST);

        JPanel fields = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 8));
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
        File out = choosePdfDestination("informe-inventario.pdf");
        if (out == null) {
            return;
        }

        try {
            writePdfSafely(out, doc -> {
                addPdfHeader(doc, "Informe de Inventario");

                java.util.List<String[]> rows = new java.util.ArrayList<>();
                for (int r = 0; r < modelInv.getRowCount(); r++) {
                    rows.add(new String[]{
                        displayString(modelInv.getValueAt(r, 0)),
                        displayString(modelInv.getValueAt(r, 1)),
                        displayString(modelInv.getValueAt(r, 2)),
                        displayString(modelInv.getValueAt(r, 3))
                    });
                }

                PdfPTable table = buildStyledTable(
                        new String[]{"Código", "Descripción", "Estado", "Fecha"},
                        rows,
                        new float[]{2.2f, 4f, 2f, 2f},
                        new int[]{Element.ALIGN_LEFT, Element.ALIGN_LEFT, Element.ALIGN_CENTER, Element.ALIGN_CENTER}
                );
                doc.add(table);

                java.util.LinkedHashMap<String, String> resumen = new java.util.LinkedHashMap<>();
                resumen.put("Total de ítems registrados", String.valueOf(modelInv.getRowCount()));
                resumen.put("Stock general", formatDecimal(calcularStockGeneralInventario()));
                addSummarySection(doc, "Totales", resumen);

                addPdfFooter(doc);
            });
            JOptionPane.showMessageDialog(this, "PDF generado:\n" + out.getAbsolutePath(),
                    "Exportar PDF", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "No se pudo generar el PDF:\n" + ex.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private interface PdfBody {

        void build(Document doc) throws Exception;
    }

    private void writePdfSafely(File out, PdfBody body) throws Exception {
        // Genera el PDF en memoria primero
        java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream(32 * 1024);
        Document doc = new Document(PageSize.A4, 56f, 56f, 56f, 56f);
        PdfWriter writer = null;
        try {
            writer = PdfWriter.getInstance(doc, baos);
            // Evita que el writer cierre el stream de memoria por su cuenta
            writer.setCloseStream(false);
            doc.open();

            // Construye el contenido del PDF
            body.build(doc);

        } finally {
            if (doc.isOpen()) {
                doc.close();
            }
            if (writer != null) {
                writer.close();
            }
        }

        // Si llegamos acá, el PDF está completo en memoria. Ahora sí, guardar al disco.
        byte[] bytes = baos.toByteArray();
        if (bytes.length < 100) {
            // umbral mínimo “sanidad” (ajustable) — evita guardar PDFs vacíos
            throw new IllegalStateException("El PDF resultó vacío o incompleto.");
        }
        try (java.io.FileOutputStream fos = new java.io.FileOutputStream(out, false)) {
            fos.write(bytes);
            fos.flush();
        }
    }

    private void exportTramitesToPdf() {
        File out = choosePdfDestination("informe-solicitudes.pdf");
        if (out == null) {
            return;
        }

        try {
            writePdfSafely(out, doc -> {
                addPdfHeader(doc, "Informe de Solicitudes Registradas");

                java.util.List<String[]> rows = new java.util.ArrayList<>();
                for (int r = 0; r < modelTra.getRowCount(); r++) {
                    String nro = displayString(modelTra.getValueAt(r, 0)); // Nro Trámite
                    String solicitante = displayString(modelTra.getValueAt(r, 5)); // Solicitante
                    String destino = displayString(modelTra.getValueAt(r, 1)); // << Asegúrate que modelTra col 1 sea Destino en tu UI
                    String fecha = displayString(modelTra.getValueAt(r, 2));
                    String estado = displayString(modelTra.getValueAt(r, 6));
                    rows.add(new String[]{nro, solicitante, destino, fecha, estado});
                }

                PdfPTable table = buildStyledTable(
                        new String[]{"Nro Trámite", "Solicitante", "Destino", "Fecha", "Estado"},
                        rows,
                        new float[]{2f, 3f, 3f, 2f, 2f},
                        new int[]{Element.ALIGN_CENTER, Element.ALIGN_LEFT, Element.ALIGN_LEFT, Element.ALIGN_CENTER, Element.ALIGN_CENTER}
                );
                doc.add(table);

                java.util.LinkedHashMap<String, String> resumen = new java.util.LinkedHashMap<>();
                java.util.Map<String, Integer> estados = contarEstadosTramites();
                resumen.put("Total de solicitudes", String.valueOf(modelTra.getRowCount()));
                resumen.put("Estado NUEVO", String.valueOf(estados.getOrDefault("NUEVO", 0)));
                resumen.put("Estado APROBADO", String.valueOf(estados.getOrDefault("APROBADO", 0)));
                resumen.put("Estado RECHAZADO", String.valueOf(estados.getOrDefault("RECHAZADO", 0)));
                addSummarySection(doc, "Estadísticas", resumen);

                addPdfFooter(doc);
            });
            JOptionPane.showMessageDialog(this, "PDF generado:\n" + out.getAbsolutePath(),
                    "Exportar PDF", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "No se pudo generar el PDF:\n" + ex.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void exportMovimientosToPdf() {
        File out = choosePdfDestination("informe-movimientos.pdf");
        if (out == null) {
            return;
        }

        try {
            writePdfSafely(out, doc -> {
                addPdfHeader(doc, "Informe de Movimientos de Inventario");

                java.util.List<String[]> entradas = new java.util.ArrayList<>();
                java.util.List<String[]> salidas = new java.util.ArrayList<>();
                java.math.BigDecimal totalEntradas = java.math.BigDecimal.ZERO;
                java.math.BigDecimal totalSalidas = java.math.BigDecimal.ZERO;

                for (int r = 0; r < modelMov.getRowCount(); r++) {
                    String tipo = rawString(modelMov.getValueAt(r, 1)).toUpperCase(java.util.Locale.ROOT);
                    java.math.BigDecimal cantidad = parseCantidad(modelMov.getValueAt(r, 2));
                    String fecha = displayString(modelMov.getValueAt(r, 0));
                    String codigo = rawString(modelMov.getValueAt(r, 3));
                    String desc = rawString(modelMov.getValueAt(r, 4));
                    String insumo = (codigo.isBlank() ? desc : (codigo + " - " + desc));
                    if (insumo == null || insumo.isBlank()) {
                        insumo = "-";
                    }
                    String ubicacion = rawString(modelMov.getValueAt(r, 5));
                    String destino = rawString(modelMov.getValueAt(r, 6));
                    String origenDestino = destino.isBlank() ? (ubicacion.isBlank() ? "-" : ubicacion) : destino;

                    String[] data = new String[]{fecha, insumo, formatDecimal(cantidad), origenDestino, (tipo.isBlank() ? "-" : tipo)};

                    if ("ENTRADA".equalsIgnoreCase(tipo)) {
                        entradas.add(data);
                        totalEntradas = totalEntradas.add(cantidad);
                    } else if ("SALIDA".equalsIgnoreCase(tipo)) {
                        salidas.add(data);
                        totalSalidas = totalSalidas.add(cantidad);
                    }
                }

                String[] headers = new String[]{"Fecha", "Insumo", "Cantidad", "Origen/Destino", "Tipo"};
                float[] widths = new float[]{2f, 4f, 1.8f, 3f, 1.6f};
                int[] aligns = new int[]{Element.ALIGN_CENTER, Element.ALIGN_LEFT, Element.ALIGN_RIGHT, Element.ALIGN_LEFT, Element.ALIGN_CENTER};

                if (!entradas.isEmpty()) {
                    Paragraph sec = new Paragraph("Movimientos de Entrada", FONT_SECTION);
                    sec.setSpacingBefore(8f);
                    sec.setSpacingAfter(4f);
                    doc.add(sec);
                    doc.add(buildStyledTable(headers, entradas, widths, aligns));
                }

                if (!salidas.isEmpty()) {
                    Paragraph sec = new Paragraph("Movimientos de Salida", FONT_SECTION);
                    sec.setSpacingBefore(12f);
                    sec.setSpacingAfter(4f);
                    doc.add(sec);
                    doc.add(buildStyledTable(headers, salidas, widths, aligns));
                }

                if (entradas.isEmpty() && salidas.isEmpty()) {
                    Paragraph sinDatos = new Paragraph("No se registran movimientos para el período seleccionado.", FONT_TABLE_CELL);
                    sinDatos.setAlignment(Element.ALIGN_LEFT);
                    doc.add(sinDatos);
                }

                java.util.LinkedHashMap<String, String> resumen = new java.util.LinkedHashMap<>();
                resumen.put("Total de entradas", formatDecimal(totalEntradas));
                resumen.put("Total de salidas", formatDecimal(totalSalidas));
                addSummarySection(doc, "Totales", resumen);

                addPdfFooter(doc);
            });
            JOptionPane.showMessageDialog(this, "PDF generado:\n" + out.getAbsolutePath(),
                    "Exportar PDF", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "No se pudo generar el PDF:\n" + ex.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private File choosePdfDestination(String defaultName) {
        JFileChooser fc = new JFileChooser();
        fc.setDialogTitle("Guardar informe en PDF");
        fc.setFileFilter(new FileNameExtensionFilter("Archivo PDF (*.pdf)", "pdf"));
        fc.setSelectedFile(new File(defaultName));
        int opt = fc.showSaveDialog(this);
        if (opt != JFileChooser.APPROVE_OPTION) {
            return null;
        }

        File selected = fc.getSelectedFile();
        if (!selected.getName().toLowerCase(Locale.ROOT).endsWith(".pdf")) {
            selected = new File(selected.getParentFile(), selected.getName() + ".pdf");
        }
        return selected;
    }

    private void addPdfHeader(Document doc, String titulo) throws Exception {
        PdfPTable header = new PdfPTable(new float[]{1f, 4f});
        header.setWidthPercentage(100);
        header.getDefaultCell().setBorder(PdfPCell.NO_BORDER);

        PdfPCell logoCell = new PdfPCell();
        logoCell.setBorder(PdfPCell.NO_BORDER);
        try {
            Image logo = Image.getInstance(getClass().getResource("/branding/logo_sigav2.png"));
            logo.scaleToFit(60, 60);
            logoCell.addElement(logo);
        } catch (Exception e) {
            Paragraph fallback = new Paragraph("SIGA", FONT_HEADER_TITLE);
            fallback.setAlignment(Element.ALIGN_LEFT);
            logoCell.addElement(fallback);
        }
        header.addCell(logoCell);

        PdfPCell infoCell = new PdfPCell();
        infoCell.setBorder(PdfPCell.NO_BORDER);
        Paragraph sistema = new Paragraph(
                "SIGA – Sistema de Gestión Administrativa\n"
                + "Facultad de Ciencias Exactas y Tecnologías\n"
                + "Universidad Nacional de Santiago del Estero",
                FONT_HEADER_INFO
        );
        sistema.setAlignment(Element.ALIGN_LEFT);
        infoCell.addElement(sistema);

        Paragraph tituloParrafo = new Paragraph(titulo.toUpperCase(Locale.ROOT), FONT_HEADER_TITLE);
        tituloParrafo.setSpacingBefore(6f);
        tituloParrafo.setSpacingAfter(2f);
        infoCell.addElement(tituloParrafo);

        header.addCell(infoCell);
        doc.add(header);

        Paragraph fecha = new Paragraph(formatFechaEmision(), FONT_HEADER_DATE);
        fecha.setAlignment(Element.ALIGN_RIGHT);
        doc.add(fecha);
        doc.add(Chunk.NEWLINE);
    }

    private void addPdfFooter(Document doc) throws DocumentException {
        doc.add(Chunk.NEWLINE);
        Paragraph footer = new Paragraph(
                "SIGA – Sistema de Gestión Administrativa FCEyT\n"
                + "Universidad Nacional de Santiago del Estero – 2025\n"
                + "\"Generado automáticamente. Documento sin valor legal.\"",
                FONT_FOOTER
        );
        footer.setAlignment(Element.ALIGN_CENTER);
        doc.add(footer);
    }

    private PdfPTable buildStyledTable(String[] headers, List<String[]> rows,
            float[] widths, int[] alignments) throws DocumentException {
        PdfPTable table = new PdfPTable(headers.length);
        table.setWidthPercentage(100);
        if (widths != null) {
            table.setWidths(widths);
        }
        table.setSpacingBefore(8f);
        table.setSpacingAfter(6f);

        for (String h : headers) {
            PdfPCell cell = new PdfPCell(new Phrase(h, FONT_TABLE_HEADER));
            cell.setHorizontalAlignment(Element.ALIGN_CENTER);
            cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
            cell.setPadding(7f);
            cell.setBackgroundColor(PDF_PRIMARY);
            cell.setBorderColor(PDF_BORDER);
            cell.setBorderWidth(0.6f);
            table.addCell(cell);
        }

        for (int r = 0; r < rows.size(); r++) {
            String[] row = rows.get(r);
            Color bg = (r % 2 == 0) ? Color.WHITE : PDF_ROW_ALT;
            for (int c = 0; c < headers.length; c++) {
                String value = (row != null && c < row.length) ? row[c] : "-";
                if (value == null || value.isBlank()) {
                    value = "-";
                }
                PdfPCell cell = new PdfPCell(new Phrase(value, FONT_TABLE_CELL));
                cell.setHorizontalAlignment((alignments != null && c < alignments.length) ? alignments[c] : Element.ALIGN_LEFT);
                cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
                cell.setPadding(6f);
                cell.setBorderColor(PDF_BORDER);
                cell.setBorderWidth(0.5f);
                cell.setBackgroundColor(bg);
                table.addCell(cell);
            }
        }
        return table;
    }

    private void addSummarySection(Document doc, String titulo, LinkedHashMap<String, String> valores)
            throws DocumentException {
        if (valores == null || valores.isEmpty()) {
            return;
        }
        Paragraph section = new Paragraph(titulo.toUpperCase(Locale.ROOT), FONT_SECTION);
        section.setSpacingBefore(14f);
        section.setSpacingAfter(6f);
        doc.add(section);

        PdfPTable resumen = new PdfPTable(new float[]{3f, 2f});
        resumen.setWidthPercentage(60);
        resumen.setHorizontalAlignment(Element.ALIGN_LEFT);

        for (Map.Entry<String, String> entry : valores.entrySet()) {
            PdfPCell label = new PdfPCell(new Phrase(entry.getKey(), FONT_SUMMARY_LABEL));
            label.setBackgroundColor(PDF_LIGHT_GRAY);
            label.setBorderColor(PDF_BORDER);
            label.setBorderWidth(0.5f);
            label.setPadding(6f);
            resumen.addCell(label);

            PdfPCell value = new PdfPCell(new Phrase(entry.getValue(), FONT_SUMMARY_VALUE));
            value.setBorderColor(PDF_BORDER);
            value.setBorderWidth(0.5f);
            value.setPadding(6f);
            resumen.addCell(value);
        }

        doc.add(resumen);
    }

    private String formatFechaEmision() {
        LocalDate hoy = LocalDate.now();
        Locale locale = new Locale("es", "AR");
        String mes = hoy.getMonth().getDisplayName(TextStyle.FULL, locale);
        if (!mes.isEmpty()) {
            mes = mes.substring(0, 1).toUpperCase(locale) + mes.substring(1);
        }
        return String.format(locale, "Santiago del Estero, %d de %s de %d",
                hoy.getDayOfMonth(), mes, hoy.getYear());
    }

    private String rawString(Object value) {
        if (value == null) {
            return "";
        }
        return value.toString().trim();
    }

    private String displayString(Object value) {
        String s = rawString(value);
        return s.isEmpty() ? "-" : s;
    }

    private BigDecimal calcularStockGeneralInventario() {
        BigDecimal total = BigDecimal.ZERO;
        for (int r = 0; r < modelInv.getRowCount(); r++) {
            String codigo = rawString(modelInv.getValueAt(r, 0));
            if (codigo.isEmpty()) {
                continue;
            }
            try {
                Optional<Insumo> ins = invService.buscarPorCodigo(codigo);
                if (ins.isPresent() && ins.get().getId() != null) {
                    BigDecimal stock = invService.stockActualExacto(ins.get().getId());
                    if (stock != null) {
                        total = total.add(stock);
                    }
                }
            } catch (Exception ignore) {
            }
        }
        return total;
    }

    private Map<String, Integer> contarEstadosTramites() {
        Map<String, Integer> counts = new LinkedHashMap<>();
        counts.put("NUEVO", 0);
        counts.put("APROBADO", 0);
        counts.put("RECHAZADO", 0);

        for (int r = 0; r < modelTra.getRowCount(); r++) {
            String estado = rawString(modelTra.getValueAt(r, 6)).toUpperCase(Locale.ROOT);
            if (estado.contains("NUEV")) {
                counts.computeIfPresent("NUEVO", (k, v) -> v + 1);
            } else if (estado.contains("APR")) {
                counts.computeIfPresent("APROBADO", (k, v) -> v + 1);
            } else if (estado.contains("RECH")) {
                counts.computeIfPresent("RECHAZADO", (k, v) -> v + 1);
            }
        }
        return counts;
    }

    private BigDecimal parseCantidad(Object value) {
        if (value == null) {
            return BigDecimal.ZERO;
        }
        if (value instanceof BigDecimal bd) {
            return bd;
        }
        if (value instanceof Number n) {
            return BigDecimal.valueOf(n.doubleValue());
        }
        String s = rawString(value);
        if (s.isEmpty()) {
            return BigDecimal.ZERO;
        }
        s = s.replace(" ", "");
        if (s.contains(",")) {
            if (s.lastIndexOf(',') > s.lastIndexOf('.')) {
                s = s.replace(".", "");
                s = s.replace(',', '.');
            } else {
                s = s.replace(",", "");
            }
        }
        try {
            return new BigDecimal(s);
        } catch (Exception e) {
            return BigDecimal.ZERO;
        }
    }

    private String formatDecimal(BigDecimal value) {
        if (value == null) {
            return "0";
        }
        BigDecimal normalized = value.stripTrailingZeros();
        if (normalized.compareTo(BigDecimal.ZERO) == 0) {
            return "0";
        }
        String str = normalized.toPlainString();
        return str.replace('.', ',');
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
