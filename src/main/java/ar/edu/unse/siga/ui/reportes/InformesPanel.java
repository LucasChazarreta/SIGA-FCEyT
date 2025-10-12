package ar.edu.unse.siga.ui.reportes;

import ar.edu.unse.siga.domain.Categoria;
import ar.edu.unse.siga.domain.Insumo;
import ar.edu.unse.siga.domain.Tramite;
import ar.edu.unse.siga.service.InventarioService;
import ar.edu.unse.siga.service.TramiteService;
import ar.edu.unse.siga.ui.base.CardPanel;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.*;
import javax.swing.text.MaskFormatter;
import java.awt.*;
import java.lang.reflect.Method;
import java.text.ParseException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
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
 * Solo UI/estilos. La lógica de carga/filtrado/exportación permanece igual.
 */
public class InformesPanel extends JPanel {

    // ======= Colores base (coinciden con la captura) =======
    private static final Color BRAND        = new Color(58, 96, 224);
    private static final Color BRAND_SOFT   = new Color(206, 218, 255);
    private static final Color CARD_BORDER  = new Color(225, 230, 246);
    private static final Color HEADER_TXT   = new Color(24, 63, 150);
    private static final Color TITLE_TXT    = new Color(35, 55, 110);

    private final InventarioService invService;
    private final TramiteService traService;

    // Métricas
    private final JLabel lblTotalInsumos  = bigValueLabel("-");
    private final JLabel lblTotalTramites = bigValueLabel("-");

    // Filtros TRÁMITES
    private final JTextField filterSearch = new JTextField(18);
    private final JComboBox<String> filterEstado
            = new JComboBox<>(new String[]{"Todos", "Completado", "En proceso", "Pendiente", "Alta"});

    // --- INVENTARIO ---
    private final JComboBox<Categoria> cbCategoria = new JComboBox<>();
    private final DateField dfDesde = new DateField();
    private final DateField dfHasta = new DateField();
    private final DefaultTableModel modelInv = new DefaultTableModel(
            new Object[]{"Código", "Descripción", "Estado", "Fecha"}, 0
    ) { @Override public boolean isCellEditable(int r, int c) { return false; } };

    // --- TRÁMITES ---
    private final DefaultTableModel modelTra = new DefaultTableModel(
            new Object[]{"ID Trámite", "Asunto", "Fecha actualización", "Última actualización", "Descripción", "Estado"}, 0
    ) { @Override public boolean isCellEditable(int r, int c) { return false; } };

    // UI de contenido por tabs
    private final CardLayout contentCards = new CardLayout();
    private final JPanel content = new JPanel(contentCards);
    private JToggleButton btnInventario;
    private JToggleButton btnTramites;

    public InformesPanel(InventarioService invService, TramiteService traService) {
        this.invService = invService;
        this.traService = traService;

        setLayout(new BorderLayout(0, 12));
        setOpaque(false);
        setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        add(buildHeader(), BorderLayout.NORTH);
        add(buildContentScrollable(), BorderLayout.CENTER);

        // Ocultar título duplicado del shell si lo hubiera
        SwingUtilities.invokeLater(() -> {
            Window w = SwingUtilities.getWindowAncestor(InformesPanel.this);
            if (w instanceof JFrame frame) {
                Container root = frame.getContentPane();
                JLabel headerTitle = findHeaderTitleLabel(root);
                if (headerTitle != null) headerTitle.setText("");
            }
        });
        this.addHierarchyListener(e -> {
            if ((e.getChangeFlags() & java.awt.event.HierarchyEvent.SHOWING_CHANGED) != 0 && isShowing()) {
                SwingUtilities.invokeLater(() -> {
                    Window w = SwingUtilities.getWindowAncestor(InformesPanel.this);
                    if (w instanceof JFrame frame) {
                        JLabel headerTitle = findHeaderTitleLabel(frame.getContentPane());
                        if (headerTitle != null) headerTitle.setText("");
                    }
                });
            }
        });

        cargarCategoriasEnCombo();
        reloadMetrics();
        runQueryInventario();
        loadTableDataTramites();
        installFiltersTramites();
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
            if (btnTramites != null && btnTramites.isSelected()) exportTramitesToPdf();
            else exportInventarioToPdf();
        });
        exportCsv.addActionListener(e -> {
            if (btnTramites != null && btnTramites.isSelected()) exportTramitesToCsv();
            else exportInventarioToCsv();
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
        btnTramites   = pill("TRÁMITES");
        btnInventario.setSelected(true);
        tabs.add(btnInventario);
        tabs.add(btnTramites);

        JPanel tabRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 18, 6));
        tabRow.setOpaque(true);
        tabRow.setBackground(Color.WHITE);
        tabRow.add(btnInventario);
        tabRow.add(btnTramites);
        wrapper.add(tabRow, BorderLayout.NORTH);

        // INVENTARIO
        JPanel invPanel = new JPanel(new BorderLayout());
        invPanel.setOpaque(true);
        invPanel.setBackground(Color.WHITE);
        invPanel.add(buildMetrics(), BorderLayout.NORTH);
        invPanel.add(buildInventarioSplit(), BorderLayout.CENTER);

        // TRÁMITES
        JPanel traPanel = new JPanel(new BorderLayout(12, 12));
        traPanel.setOpaque(true);
        traPanel.setBackground(Color.WHITE);
        traPanel.add(buildTramitesFilters(), BorderLayout.NORTH);
        traPanel.add(buildTramitesTableScroll(), BorderLayout.CENTER);

        // Contenedor central
        content.setOpaque(true);
        content.setBackground(Color.WHITE);
        content.add(invPanel, "INV");
        content.add(traPanel, "TRA");

        wrapper.add(content, BorderLayout.CENTER);

        btnInventario.addActionListener(e -> contentCards.show(content, "INV"));
        btnTramites.addActionListener(e -> {
            contentCards.show(content, "TRA");
            loadTableDataTramites();
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
// --- INVENTARIO: división filtros/tabla ---
// === INVENTARIO: división filtros / tabla (simple, sin scroll a la izquierda)
// === INVENTARIO: división filtros / tabla (bajo filtros y quito tags)
private JPanel buildInventarioSplit() {
    JPanel split = new JPanel(new GridBagLayout());
    split.setOpaque(true);
    split.setBackground(Color.WHITE);

    GridBagConstraints gc = new GridBagConstraints();
    gc.gridy = 0;
    gc.fill = GridBagConstraints.BOTH;
    gc.weighty = 1.0;

    // IZQ: filtros (ya SIN CardPanel)
    JPanel filtros = buildFiltrosPanelInventario();  // <-- ahora devuelve JPanel
    JPanel holder = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
    holder.setOpaque(false);
    holder.add(filtros);

    gc.gridx = 0;
    gc.weightx = 0.33;
    gc.insets  = new Insets(16, 0, 0, 12);
    split.add(holder, gc);

    // DER: tabla
    CardPanel tabla = buildTablaPanelInventario();
    tabla.setOpaque(true);
    tabla.setBackground(Color.WHITE);

    gc.gridx = 1;
    gc.weightx = 0.67;
    gc.insets  = new Insets(0, 0, 0, 0);
    split.add(tabla, gc);

    return split;
}




// === Panel de filtros limpio, sin fondo ni sombra ===
// === Panel de filtros limpio (sin CardPanel, sin fondo) ===
private JPanel buildFiltrosPanelInventario() {
    JPanel card = new JPanel(new BorderLayout());
    card.setOpaque(false);                       // sin fondo
    card.setBorder(BorderFactory.createEmptyBorder());

    // Título
    JLabel title = new JLabel("FILTROS");
    title.setFont(title.getFont().deriveFont(Font.BOLD, 18f));
    title.setForeground(new Color(73, 103, 204));
    JPanel titleRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
    titleRow.setBorder(BorderFactory.createEmptyBorder(120, 0, 0, 0)); // margen arriba de 10 px

    titleRow.setOpaque(false);
    titleRow.add(title);
    card.add(titleRow, BorderLayout.NORTH);

    // Campos (alineados a la IZQUIERDA del campo, como pediste)
    JPanel fields = new JPanel();
    fields.setOpaque(false);
    fields.setLayout(new BoxLayout(fields, BoxLayout.Y_AXIS));

    Dimension fieldSize = new Dimension(260, 34);
    cbCategoria.setPreferredSize(fieldSize);
    cbCategoria.setMaximumSize(fieldSize);

    JComponent desdeComp = dfDesde.getComponent();
    desdeComp.setPreferredSize(fieldSize);
    desdeComp.setMaximumSize(fieldSize);
    // asegura que la fecha se vea completa
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
    fields.add(Box.createVerticalStrut(28));

    // Botón centrado
    JButton apply = primaryButton("APLICAR FILTROS");
    apply.addActionListener(e -> runQueryInventario());
    JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
    btnRow.setOpaque(false);
    btnRow.add(apply);
    fields.add(btnRow);

    // Centrado vertical “suave”
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
    field.setBorder(BorderFactory.createLineBorder(new Color(225, 230, 246))); // borde limpio

    p.add(lbl);
    p.add(Box.createVerticalStrut(4));
    p.add(field);
    return p;
}


// 🔹 Etiqueta + campo alineados a la izquierda
// Reemplazá por completo tu método filterField actual por este:
private JPanel filterField(String label, JComponent field) {
    JPanel panel = new JPanel();
    panel.setOpaque(false);
    panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

    JLabel lbl = new JLabel(label.toUpperCase());
    lbl.setFont(lbl.getFont().deriveFont(Font.PLAIN, 12f));
    lbl.setForeground(new Color(35, 55, 110));
    lbl.setAlignmentX(Component.LEFT_ALIGNMENT);

    // ancho cómodo para que la fecha no se corte
    Dimension fieldSize = new Dimension(260, 34);
    field.setPreferredSize(fieldSize);
    field.setMaximumSize(fieldSize);
    field.setMinimumSize(fieldSize);
    field.setAlignmentX(Component.LEFT_ALIGNMENT);

    // borde simple y limpio
    field.setBorder(BorderFactory.createLineBorder(new Color(225, 230, 246)));

    panel.add(lbl);
    panel.add(Box.createVerticalStrut(4));
    panel.add(field);
    return panel;
}





// Helper: centra un label + campo
private JPanel centerField(String label, JComponent field) {
    JPanel panel = new JPanel();
    panel.setOpaque(false);
    panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

    JLabel lbl = new JLabel(label.toUpperCase(), SwingConstants.CENTER);
    lbl.setFont(lbl.getFont().deriveFont(Font.PLAIN, 12f));
    lbl.setForeground(new Color(35, 55, 110));
    lbl.setAlignmentX(Component.CENTER_ALIGNMENT);
    field.setAlignmentX(Component.CENTER_ALIGNMENT);

    panel.add(lbl);
    panel.add(Box.createVerticalStrut(4));
    panel.add(field);
    return panel;
}



private CardPanel buildTablaPanelInventario() {
    CardPanel card = new CardPanel();
    card.setLayout(new BorderLayout(10, 10));

    JLabel title = new JLabel("RESULTADOS");
    title.setFont(title.getFont().deriveFont(Font.BOLD, 18f));
    title.setForeground(BRAND);
    card.add(title, BorderLayout.NORTH);

    JTable table = new JTable(modelInv) {
        @Override public boolean getScrollableTracksViewportWidth() {
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

    // Header azul con texto blanco
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

    // 1) Centrar TODO el contenido por defecto (zebra suave)
    table.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
        @Override
        public Component getTableCellRendererComponent(JTable t, Object v, boolean s, boolean f, int r, int c) {
            Component comp = super.getTableCellRendererComponent(t, v, s, f, r, c);
            setHorizontalAlignment(SwingConstants.CENTER);
            if (!s) comp.setBackground((r % 2 == 0) ? new Color(250, 252, 255) : Color.WHITE);
            return comp;
        }
    });

    // 2) Renderer ESPECIAL para "Estado": banda completa verde/roja y texto centrado
    int estadoCol = table.getColumnModel().getColumnIndex("Estado");
    table.getColumnModel().getColumn(estadoCol).setCellRenderer(new DefaultTableCellRenderer() {
        // Colores estilo captura
        private final Color ACTIVE_BG = new Color(0xCFE8D1); // verde suave
        private final Color ACTIVE_FG = new Color(0x0B4D2B); // verde oscuro
        private final Color INACT_BG  = new Color(0xF3D0D0); // rojo suave
        private final Color INACT_FG  = new Color(0x7A1212); // rojo oscuro
        private final Color OTHER_BG  = new Color(0xE6E9F5); // fallback
        private final Color OTHER_FG  = new Color(0x344054);

        @Override
        public Component getTableCellRendererComponent(JTable t, Object v, boolean s, boolean f, int r, int c) {
            JLabel lbl = (JLabel) super.getTableCellRendererComponent(t, v, s, f, r, c);

            String texto = (v == null ? "-" : v.toString().trim()).toUpperCase(Locale.ROOT);
            lbl.setText(texto);
            lbl.setHorizontalAlignment(SwingConstants.CENTER);
            lbl.setFont(lbl.getFont().deriveFont(Font.PLAIN));
            lbl.setOpaque(true);

            // Padding vertical para que se vea como una "banda"
            lbl.setBorder(new EmptyBorder(8, 0, 8, 0));

            // Colores según estado (sin perder selección)
            if (texto.equals("ACTIVO")) {
                lbl.setBackground(s ? ACTIVE_BG.darker() : ACTIVE_BG);
                lbl.setForeground(ACTIVE_FG);
            } else if (texto.equals("INACTIVO")) {
                lbl.setBackground(s ? INACT_BG.darker() : INACT_BG);
                lbl.setForeground(INACT_FG);
            } else {
                lbl.setBackground(s ? OTHER_BG.darker() : OTHER_BG);
                lbl.setForeground(OTHER_FG);
            }
            return lbl;
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



    private DefaultTableCellRenderer statusRenderer() {
        return new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object v, boolean s, boolean f, int r, int c) {
                super.getTableCellRendererComponent(t, v, s, f, r, c);
                setHorizontalAlignment(CENTER);
                setOpaque(true);
                setBorder(new EmptyBorder(4, 12, 4, 12));
                String text = v == null ? "-" : v.toString();
                setText(text.toUpperCase());
                // Colores: ACTIVO=verde, INACTIVO=azul suave, PENDIENTE=amarillo
                Color base;
                switch (text.toLowerCase(Locale.ROOT)) {
                    case "activo"    -> base = new Color(190, 225, 200);
                    case "pendiente" -> base = new Color(255, 239, 200);
                    default          -> base = new Color(208, 220, 255);
                }
                setBackground(s ? base.darker() : base);
                return this;
            }
        };
    }


    // ===== Botones con estilo =====
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

    private JLabel tag(String text) {
        JLabel l = new JLabel(text);
        l.setOpaque(true);
        l.setBackground(new Color(232, 238, 255));
        l.setForeground(new Color(65, 90, 181));
        l.setBorder(BorderFactory.createEmptyBorder(4, 12, 4, 12));
        return l;
    }

private JToggleButton pill(String text) {
    final Color blue = new Color(0x0B, 0x2F, 0xB5); // azul fuerte

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

    // no seleccionado
    t.setBackground(Color.WHITE);
    t.setForeground(blue);
    t.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(blue, 1, true),
            BorderFactory.createEmptyBorder(10, 20, 10, 20)
    ));

    // texto blanco cuando está seleccionado
    t.addChangeListener(e -> t.setForeground(t.isSelected() ? Color.WHITE : blue));

    // evita estilos especiales del LAF
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

    // ==== Métricas (SOLO 2 tarjetas centradas) ====
    private JComponent buildMetrics() {
        JPanel row = new JPanel(new FlowLayout(FlowLayout.CENTER, 16, 0));
        row.setOpaque(false);
        row.setBorder(BorderFactory.createEmptyBorder(6, 6, 12, 6));

        CardPanel c1 = metricCardGradient("TOTAL INSUMOS", lblTotalInsumos,
                new Color(70, 120, 255), new Color(110, 150, 255));
        CardPanel c2 = metricCardGradient("TRÁMITES", lblTotalTramites,
                new Color(70, 120, 255), new Color(140, 170, 255));

        // tamaño agradable para centrado visual
        c1.setPreferredSize(new Dimension(420, 110));
        c2.setPreferredSize(new Dimension(420, 110));

        row.add(c1);
        row.add(c2);
        return row;
    }

private CardPanel metricCardGradient(String title, JLabel value, Color c1, Color c2) {
    CardPanel card = new CardPanel() {
        @Override protected void paintComponent(Graphics g) {
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

    // Título (por ejemplo "TOTAL INSUMOS")
    JLabel lblTitle = new JLabel(title.toUpperCase());
    lblTitle.setForeground(new Color(230, 240, 255));
    lblTitle.setFont(lblTitle.getFont().deriveFont(Font.BOLD, 13f));

    // Valor principal (más grande)
    value.setForeground(Color.WHITE);
    value.setFont(value.getFont().deriveFont(Font.BOLD, 36f)); // tamaño aumentado
    value.setHorizontalAlignment(SwingConstants.LEFT);

    // Agregar solo título y valor (sin texto adicional debajo)
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
            for (Categoria c : categorias) model.addElement(c);
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
        try { lblTotalInsumos.setText(String.valueOf(invService.listarTodos().size())); }
        catch (Exception e) { lblTotalInsumos.setText("-"); }

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
                if (!cat.equalsIgnoreCase(i.getCategoria().getNombre())) return false;
            }
            LocalDate fa = i.getFechaAlta();
            if (fa == null && i.getCreatedAt() != null) {
                fa = java.time.ZonedDateTime.ofInstant(i.getCreatedAt(), java.time.ZoneId.systemDefault()).toLocalDate();
            }
            if (d1 != null && (fa == null || fa.isBefore(d1))) return false;
            if (d2 != null && (fa == null || fa.isAfter(d2))) return false;
            return true;
        }).collect(Collectors.toList());
    }

    // ====== TRÁMITES ======
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

    // Anchos de columnas
    TableColumnModel tcm = table.getColumnModel();
    if (tcm.getColumnCount() >= 6) {
        tcm.getColumn(0).setPreferredWidth(120);
        tcm.getColumn(1).setPreferredWidth(240);
        tcm.getColumn(2).setPreferredWidth(160);
        tcm.getColumn(3).setPreferredWidth(170);
        tcm.getColumn(4).setPreferredWidth(300);
        tcm.getColumn(5).setPreferredWidth(130);
    }

    JTableHeader header = table.getTableHeader();
    header.setPreferredSize(new Dimension(header.getPreferredSize().width, 46));
    header.setDefaultRenderer(new TableHeaderRenderer());

    table.getColumnModel().getColumn(5).setCellRenderer(new BadgeRenderer(BadgeRenderer.Type.STATUS));

    // Zebra para el resto
    DefaultTableCellRenderer zebra = new DefaultTableCellRenderer() {
        @Override
        public Component getTableCellRendererComponent(JTable t, Object v, boolean s, boolean f, int r, int c) {
            Component comp = super.getTableCellRendererComponent(t, v, s, f, r, c);
            setHorizontalAlignment(CENTER);
            if (!s) comp.setBackground((r % 2 == 0) ? new Color(250, 252, 255) : Color.WHITE);
            return comp;
        }
    };
    for (int i = 0; i < 5; i++) table.getColumnModel().getColumn(i).setCellRenderer(zebra);

    JScrollPane scroll = new JScrollPane(table,
            ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
            ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
    beautifyScroll(scroll);

    scroll.setBorder(BorderFactory.createEmptyBorder());
    scroll.getViewport().setBackground(Color.WHITE);

    // 🎨 Scrollbar moderno y azul
    scroll.getVerticalScrollBar().setUI(new javax.swing.plaf.basic.BasicScrollBarUI() {
    // Colores propios
    private final Color TRACK = new Color(240, 245, 255);
    private final Color THUMB = new Color(58, 96, 224);
    private final Color THUMB_HOVER = new Color(40, 70, 190);

    @Override
    protected JButton createDecreaseButton(int orientation) {
        return createZeroButton();
    }

    @Override
    protected JButton createIncreaseButton(int orientation) {
        return createZeroButton();
    }

    private JButton createZeroButton() {
        JButton b = new JButton();
        b.setPreferredSize(new Dimension(0, 0));
        b.setMinimumSize(new Dimension(0, 0));
        b.setMaximumSize(new Dimension(0, 0));
        return b;
    }

    @Override
    protected void paintThumb(Graphics g, JComponent c, Rectangle thumbBounds) {
        if (!scrollbar.isEnabled() || thumbBounds.width > thumbBounds.height) return; // solo vertical
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setPaint(isThumbRollover() ? THUMB_HOVER : THUMB);
        g2.fillRoundRect(thumbBounds.x + 2, thumbBounds.y + 2,
                         thumbBounds.width - 4, thumbBounds.height - 4, 10, 10);
        g2.dispose();
    }

    @Override
    protected void paintTrack(Graphics g, JComponent c, Rectangle trackBounds) {
        g.setColor(TRACK);
        g.fillRect(trackBounds.x, trackBounds.y, trackBounds.width, trackBounds.height);
    }
});

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

            List<Tramite> tramites = traService.listarTodos();
            if (tramites == null) return;

            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

            for (Tramite t : tramites) {
                if (!search.isEmpty()) {
                    String texto = (String.valueOf(t.getAsunto()) + " " + String.valueOf(t.getNro()) + " " +
                            (t.getDescripcion() != null ? t.getDescripcion() : "")).toLowerCase(Locale.ROOT);
                    if (!texto.contains(search)) continue;
                }

                String estado = estadoFriendly(t.getEstado());
                if (!"Todos".equals(estadoFiltro) && !estado.equalsIgnoreCase(estadoFiltro)) continue;

                String actualizacion = t.getFecha() == null ? "-" : t.getFecha().format(fmt);
                String ultima = t.getFecha() == null ? "-" : t.getFecha().plusDays(1).format(fmt); // placeholder
                String descripcion = extraerDescripcionTramite(t);

                modelTra.addRow(new Object[]{t.getNro(), t.getAsunto(), actualizacion, ultima, descripcion, estado});
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void installFiltersTramites() {
        if (filterSearch.getDocument() != null) {
            filterSearch.getDocument().addDocumentListener(new SimpleDocumentListener() {
                @Override public void update() { loadTableDataTramites(); }
            });
        }
        filterEstado.addActionListener(e -> loadTableDataTramites());
    }

    private String extraerDescripcionTramite(Object t) {
        if (t == null) return "-";
        try {
            Method m = t.getClass().getMethod("getDescripcion");
            Object val = m.invoke(t);
            if (val != null) {
                String s = val.toString().trim();
                if (!s.isEmpty()) return s;
            }
        } catch (NoSuchMethodException ignore) {
        } catch (Throwable ignore) { }

        String v = tryGetter(t, "getDescripción", "getDetalle", "getDetalles", "getObservacion", "getObservación",
                "getObservaciones", "getNota", "getNotas", "getComentario", "getComentarios",
                "getMotivo", "getResumen", "getInfo", "getInformacion", "getInformación");
        if (v != null && !v.isBlank()) return v.trim();

        try {
            for (Method m : t.getClass().getMethods()) {
                if (m.getParameterCount() == 0 && m.getName().startsWith("get") && m.getReturnType() == String.class) {
                    String name = m.getName().toLowerCase(Locale.ROOT);
                    if (name.contains("desc") || name.contains("detalle") || name.contains("observ") ||
                            name.contains("nota") || name.contains("coment") || name.contains("motivo")) {
                        Object val = m.invoke(t);
                        if (val != null) {
                            String s = val.toString().trim();
                            if (!s.isEmpty()) return s;
                        }
                    }
                }
            }
        } catch (Throwable ignore) { }

        try {
            for (java.lang.reflect.Field f : t.getClass().getDeclaredFields()) {
                String n = f.getName().toLowerCase(Locale.ROOT);
                if (n.contains("desc") || n.contains("detalle") || n.contains("observ") ||
                        n.contains("nota") || n.contains("coment") || n.contains("motivo")) {
                    f.setAccessible(true);
                    Object val = f.get(t);
                    if (val != null) {
                        String s = val.toString().trim();
                        if (!s.isEmpty()) return s;
                    }
                }
            }
        } catch (Throwable ignore) { }
        return "-";
    }

    private static String tryGetter(Object obj, String... getters) {
        for (String g : getters) {
            try {
                Method m = obj.getClass().getMethod(g);
                Object val = m.invoke(obj);
                if (val != null) {
                    String s = val.toString().trim();
                    if (!s.isEmpty()) return s;
                }
            } catch (NoSuchMethodException ignore) {
            } catch (Throwable ignore) { }
        }
        return null;
    }

    private String estadoFriendly(String estado) {
        if (estado == null) return "Pendiente";
        String e = estado.trim().toLowerCase(Locale.ROOT);
        if (e.contains("comp")) return "Completado";
        if (e.contains("proc")) return "En proceso";
        if (e.contains("pend")) return "Pendiente";
        if (e.contains("alta")) return "Alta";
        return "Pendiente";
    }

    // Encabezado y badges (TRÁMITES)
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
        enum Type { PRIORITY, STATUS }
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
                if (normalized.contains("alta"))       base = new Color(255, 120, 102);
                else if (normalized.contains("media")) base = new Color(255, 188, 75);
                else                                   base = new Color(140, 198, 62);
            } else {
                if (normalized.contains("complet"))      base = new Color(28, 184, 113);
                else if (normalized.contains("proceso")) base = new Color(58, 96, 224);
                else if (normalized.contains("alta"))    base = new Color(220, 84, 84);
                else                                     base = new Color(180, 180, 180);
            }
            setForeground(Color.WHITE);
            setBackground(s ? base.darker() : base);
            return this;
        }
    }

    // ====== Export ======
    private void exportInventarioToPdf() {
        exportModelToPdf("INFORME DE INVENTARIO", new String[]{"Código", "Descripción", "Estado", "Fecha"}, modelInv);
    }
    private void exportTramitesToPdf() {
        exportModelToPdf("INFORME DE TRÁMITES",
                new String[]{"ID Trámite", "Asunto", "Fecha actualización", "Última actualización", "Descripción", "Estado"},
                modelTra);
    }
    private void exportModelToPdf(String titulo, String[] headers, DefaultTableModel model) {
        JFileChooser fc = new JFileChooser();
        fc.setDialogTitle("Guardar informe en PDF");
        fc.setFileFilter(new FileNameExtensionFilter("Archivo PDF (*.pdf)", "pdf"));
        fc.setSelectedFile(new File(titulo.toLowerCase().replace(" ", "-") + ".pdf"));
        int opt = fc.showSaveDialog(this);
        if (opt != JFileChooser.APPROVE_OPTION) return;

        File out = fc.getSelectedFile();
        if (!out.getName().toLowerCase().endsWith(".pdf")) out = new File(out.getParentFile(), out.getName() + ".pdf");

        Document doc = new Document(PageSize.A4.rotate(), 36, 36, 36, 36);
        try (FileOutputStream fos = new FileOutputStream(out)) {
            PdfWriter.getInstance(doc, fos);
            doc.open();

            com.lowagie.text.Font fTitle = new com.lowagie.text.Font(com.lowagie.text.Font.HELVETICA, 18, com.lowagie.text.Font.BOLD, HEADER_TXT);
            Paragraph title = new Paragraph(titulo, fTitle);
            title.setAlignment(Element.ALIGN_CENTER);
            title.setSpacingAfter(8f);
            doc.add(title);

            PdfPTable table = new PdfPTable(headers.length);
            table.setWidthPercentage(100);
            float[] w = new float[headers.length]; Arrays.fill(w, 1f); table.setWidths(w);

            com.lowagie.text.Font fHeader = new com.lowagie.text.Font(com.lowagie.text.Font.HELVETICA, 11, com.lowagie.text.Font.BOLD, Color.WHITE);
            for (String h : headers) {
                PdfPCell cell = new PdfPCell(new Phrase(h, fHeader));
                cell.setHorizontalAlignment(Element.ALIGN_CENTER);
                cell.setBackgroundColor(BRAND);
                cell.setPadding(6f);
                table.addCell(cell);
            }

            com.lowagie.text.Font fCell = new com.lowagie.text.Font(com.lowagie.text.Font.HELVETICA, 10, com.lowagie.text.Font.NORMAL, Color.BLACK);
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
            doc.close();
            JOptionPane.showMessageDialog(this, "PDF generado:\n" + out.getAbsolutePath(), "Exportar PDF", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception ex) {
            try { doc.close(); } catch (Exception ignore) {}
            JOptionPane.showMessageDialog(this, "No se pudo generar el PDF:\n" + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void exportInventarioToCsv() { exportModelToCsv(modelInv, ','); }
    private void exportTramitesToCsv()   { exportModelToCsv(modelTra, ','); }

    private void exportModelToCsv(DefaultTableModel model, char sep) {
        JFileChooser fc = new JFileChooser();
        fc.setDialogTitle("Guardar informe en CSV");
        fc.setFileFilter(new FileNameExtensionFilter("Archivo CSV (*.csv)", "csv"));
        fc.setSelectedFile(new File("informe.csv"));
        int opt = fc.showSaveDialog(this);
        if (opt != JFileChooser.APPROVE_OPTION) return;

        File out = fc.getSelectedFile();
        if (!out.getName().toLowerCase().endsWith(".csv")) out = new File(out.getParentFile(), out.getName() + ".csv");

        Charset enc = StandardCharsets.UTF_8;
        try (PrintWriter pw = new PrintWriter(out, enc)) {
            for (int c = 0; c < model.getColumnCount(); c++) {
                if (c > 0) pw.print(sep);
                pw.print(csvEscape(model.getColumnName(c)));
            }
            pw.println();

            for (int r = 0; r < model.getRowCount(); r++) {
                for (int c = 0; c < model.getColumnCount(); c++) {
                    if (c > 0) pw.print(sep);
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
                } catch (Throwable ignore) { return null; }
            } else {
                String txt = ((JFormattedTextField) comp).getText();
                if (txt == null) return null;
                txt = txt.trim();
                if (txt.isEmpty() || txt.contains("_")) return null;
                try { return LocalDate.parse(txt, fmt); } catch (Exception e) { return null; }
            }
        }
        JComponent getComponent() { return comp; }
    }

    /** Busca un JLabel "Informes" en la barra superior del shell. */
    private static JLabel findHeaderTitleLabel(Container root) {
        for (Component c : root.getComponents()) {
            if (c instanceof JLabel l && "Informes".equals(l.getText())) {
                Point p = SwingUtilities.convertPoint(l.getParent(), l.getLocation(), root);
                if (p.y < 120) return l;
            }
            if (c instanceof Container ct) {
                JLabel r = findHeaderTitleLabel(ct);
                if (r != null) return r;
            }
        }
        return null;
    }

    private static abstract class SimpleDocumentListener implements javax.swing.event.DocumentListener {
        public abstract void update();
        @Override public void insertUpdate(javax.swing.event.DocumentEvent e) { update(); }
        @Override public void removeUpdate(javax.swing.event.DocumentEvent e) { update(); }
        @Override public void changedUpdate(javax.swing.event.DocumentEvent e) { update(); }
    }
    
    /** Aplica un ScrollBarUI moderno y azul (vertical y horizontal) al JScrollPane dado. */
private static void beautifyScroll(JScrollPane sp) {
    java.util.function.Supplier<javax.swing.plaf.basic.BasicScrollBarUI> uiFactory = () ->
        new javax.swing.plaf.basic.BasicScrollBarUI() {
            private final Color TRACK = new Color(240, 245, 255);
            private final Color THUMB = new Color(58, 96, 224);
            private final Color THUMB_HOVER = new Color(40, 70, 190);

            @Override protected JButton createDecreaseButton(int orientation) { return zeroButton(); }
            @Override protected JButton createIncreaseButton(int orientation) { return zeroButton(); }
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

            @Override protected void paintTrack(Graphics g, JComponent c, Rectangle r) {
                g.setColor(TRACK);
                g.fillRect(r.x, r.y, r.width, r.height);
            }

            @Override protected void paintThumb(Graphics g, JComponent c, Rectangle r) {
                if (!scrollbar.isEnabled() || r.width <= 0 || r.height <= 0) return;
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                boolean hover = isThumbRollover();
                g2.setColor(hover ? THUMB_HOVER : THUMB);
                int arc = 10;
                // padding para que no “pegue” a los bordes
                g2.fillRoundRect(r.x + 2, r.y + 2, r.width - 4, r.height - 4, arc, arc);
                g2.dispose();
            }
        };

    sp.getVerticalScrollBar().setUI(uiFactory.get());
    sp.getHorizontalScrollBar().setUI(uiFactory.get());
    sp.getVerticalScrollBar().setPreferredSize(new Dimension(10, 0));
    sp.getHorizontalScrollBar().setPreferredSize(new Dimension(0, 10));
}

}