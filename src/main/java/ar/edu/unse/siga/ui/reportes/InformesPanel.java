package ar.edu.unse.siga.ui.reportes;

import ar.edu.unse.siga.domain.Categoria;
import ar.edu.unse.siga.domain.Insumo;
import ar.edu.unse.siga.domain.Tramite;
import ar.edu.unse.siga.service.InventarioService;
import ar.edu.unse.siga.service.TramiteService;
import ar.edu.unse.siga.ui.base.CardPanel;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.*;
import javax.swing.text.MaskFormatter;
import javax.swing.filechooser.FileNameExtensionFilter;
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

public class InformesPanel extends JPanel {

    // ======= Estilos =======
    private static final Color COL_BG = new Color(0xE9,0xEB,0xEF);
    private static final Color COL_TEXT_PRIMARY = new Color(0x0B,0x0B,0x0C);
    private static final Color COL_BRAND = new Color(0x2F,0x6B,0xE4);
    private static final Color COL_BRAND_SOFT = new Color(0xC7,0xD7,0xEA);
    private static final Color COL_CARD = new Color(0xF1,0xF3,0xF6);
    private static final Color COL_SHADOW = new Color(0xD1,0xD6,0xDF);

    private final InventarioService invService;
    private final TramiteService traService;

    // Métricas
    private final JLabel lblTotalInsumos = new JLabel("-");
    private final JLabel lblTotalTramites = new JLabel("-");
    private final JLabel lblPendientes = new JLabel("-");
    private final JLabel lblGastos = new JLabel("$-");
    private final JLabel lblPendientesMini = new JLabel();

    // Filtros TRÁMITES (buscador y estado)
    private final JTextField filterSearch = new JTextField(18);
    private final JComboBox<String> filterEstado =
            new JComboBox<>(new String[]{"Todos","Completado","En proceso","Pendiente","Alta"});

    // --- INVENTARIO ---
    private final JComboBox<Categoria> cbCategoria = new JComboBox<>();
    private final DateField dfDesde = new DateField();
    private final DateField dfHasta = new DateField();
    private final DefaultTableModel modelInv = new DefaultTableModel(
            new Object[]{"Código","Descripción","Estado","Fecha"}, 0
    ) { @Override public boolean isCellEditable(int r, int c) { return false; } };

    // --- TRÁMITES (informes) ---
    private final DefaultTableModel modelTra = new DefaultTableModel(
            new Object[]{"ID Trámite","Asunto","Fecha actualización","Última actualización","Descripción","Estado"}, 0
    ) { @Override public boolean isCellEditable(int r, int c) { return false; } };

    // UI de contenido por tabs
    private final CardLayout contentCards = new CardLayout();
    private final JPanel content = new JPanel(contentCards);
    private JToggleButton btnInventario;
    private JToggleButton btnTramites;

    public InformesPanel(InventarioService invService, TramiteService traService) {
        this.invService = invService;
        this.traService = traService;

        setLayout(new BorderLayout(20, 20));
        setOpaque(false);

        add(buildHeader(), BorderLayout.NORTH);
        add(buildContent(), BorderLayout.CENTER);

        // Ocultar el “Informes” chico del header global
        SwingUtilities.invokeLater(() -> {
            Window w = SwingUtilities.getWindowAncestor(InformesPanel.this);
            if (w instanceof JFrame frame) {
                Container root = frame.getContentPane();
                JLabel headerTitle = findHeaderTitleLabel(root);
                if (headerTitle != null) {
                    headerTitle.setText("");
                    Container parent = headerTitle.getParent();
                    if (parent != null) { parent.revalidate(); parent.repaint(); }
                }
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
        loadTableDataTramites();   // inicial
        installFiltersTramites();  // listeners filtros trámites
    }

    // ====== Header con export ======
    private JComponent buildHeader() {
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);

        // Centro: Título
        JLabel title = new JLabel("INFORMES", SwingConstants.CENTER);
        title.setFont(title.getFont().deriveFont(Font.BOLD, 36f));
        title.setForeground(new Color(24, 63, 150));

        JPanel center = new JPanel(new GridBagLayout());
        center.setOpaque(false);
        center.add(title);

        // Derecha: acciones
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        actions.setOpaque(false);
        JButton exportPdf = primaryButton("Exportar PDF");
        JButton exportCsv = secondaryButton("Exportar CSV");

        exportPdf.addActionListener(e -> {
            if (btnTramites != null && btnTramites.isSelected()) {
                exportTramitesToPdf();
            } else {
                exportInventarioToPdf();
            }
        });
        exportCsv.addActionListener(e -> {
            if (btnTramites != null && btnTramites.isSelected()) {
                exportTramitesToCsv();
            } else {
                exportInventarioToCsv();
            }
        });

        actions.add(exportPdf);
        actions.add(exportCsv);

        // Izquierda: espaciador
        JPanel spacer = new JPanel();
        spacer.setOpaque(false);
        spacer.setPreferredSize(actions.getPreferredSize());

        header.add(spacer, BorderLayout.WEST);
        header.add(center, BorderLayout.CENTER);
        header.add(actions, BorderLayout.EAST);
        header.setBorder(BorderFactory.createEmptyBorder(10, 20, 20, 20));
        return header;
    }

    // ====== Contenido con tabs ======
    private JComponent buildContent() {
        JPanel wrapper = new JPanel(new BorderLayout(8, 18));
        wrapper.setOpaque(false);

        ButtonGroup tabs = new ButtonGroup();
        btnInventario = pill("INVENTARIO");
        btnTramites   = pill("TRÁMITES");

        btnInventario.setSelected(true);
        tabs.add(btnInventario);
        tabs.add(btnTramites);

        JPanel tabRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 18, 6));
        tabRow.setOpaque(false);
        tabRow.add(btnInventario);
        tabRow.add(btnTramites);
        wrapper.add(tabRow, BorderLayout.NORTH);

        // Panel INVENTARIO
        JPanel invPanel = new JPanel(new BorderLayout());
        invPanel.setOpaque(false);
        invPanel.add(buildMetrics(), BorderLayout.NORTH);
        invPanel.add(buildInventarioSplit(), BorderLayout.CENTER);

        // Panel TRÁMITES
        JPanel traPanel = new JPanel(new BorderLayout(20, 24));
        traPanel.setOpaque(false);
        traPanel.add(buildTramitesFilters(), BorderLayout.NORTH);
        traPanel.add(buildTramitesTableScroll(), BorderLayout.CENTER);

        content.setOpaque(false);
        content.add(invPanel, "INV");
        content.add(traPanel, "TRA");

        wrapper.add(content, BorderLayout.CENTER);

        // Listeners tabs
        btnInventario.addActionListener(e -> contentCards.show(content, "INV"));
        btnTramites.addActionListener(e -> {
            contentCards.show(content, "TRA");
            loadTableDataTramites();
        });

        return wrapper;
    }

    // ====== INVENTARIO ======
    private JPanel buildInventarioSplit() {
        JPanel split = new JPanel(new GridBagLayout());
        split.setOpaque(false);

        GridBagConstraints gc = new GridBagConstraints();
        gc.insets = new Insets(0, 4, 0, 12);
        gc.fill = GridBagConstraints.BOTH;
        gc.weighty = 1;

        CardPanel filtros = buildFiltrosPanelInventario();
        gc.gridx = 0; gc.weightx = 0.25;
        split.add(filtros, gc);

        CardPanel tabla = buildTablaPanelInventario();
        gc.gridx = 1; gc.weightx = 0.75; gc.insets = new Insets(0, 0, 0, 0);
        split.add(tabla, gc);

        return split;
    }

    private CardPanel buildFiltrosPanelInventario() {
        CardPanel card = new CardPanel();
        card.setBorder(BorderFactory.createEmptyBorder(12, 8, 12, 12));
        card.setLayout(new BorderLayout(10, 10));

        JLabel title = new JLabel("FILTROS");
        title.setFont(title.getFont().deriveFont(Font.BOLD, 16f));
        title.setForeground(new Color(73, 103, 204));
        card.add(title, BorderLayout.NORTH);

        JPanel fields = new JPanel();
        fields.setOpaque(false);
        fields.setLayout(new BoxLayout(fields, BoxLayout.Y_AXIS));

        int formWidth = 180;
        Dimension fieldSize = new Dimension(formWidth, 34);

        cbCategoria.setPreferredSize(fieldSize);
        cbCategoria.setMaximumSize(fieldSize);
        cbCategoria.setAlignmentX(Component.LEFT_ALIGNMENT);

        dfDesde.getComponent().setPreferredSize(fieldSize);
        dfDesde.getComponent().setMaximumSize(fieldSize);
        dfDesde.getComponent().setAlignmentX(Component.LEFT_ALIGNMENT);

        dfHasta.getComponent().setPreferredSize(fieldSize);
        dfHasta.getComponent().setMaximumSize(fieldSize);
        dfHasta.getComponent().setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel pCat   = filterField("Categoría", cbCategoria);
        JPanel pDesde = filterField("Desde", dfDesde.getComponent());
        JPanel pHasta = filterField("Hasta", dfHasta.getComponent());

        Dimension panelFieldSize = new Dimension(formWidth, pCat.getPreferredSize().height);
        for (JPanel p : new JPanel[]{pCat, pDesde, pHasta}) {
            p.setAlignmentX(Component.LEFT_ALIGNMENT);
            p.setMaximumSize(new Dimension(formWidth, Integer.MAX_VALUE));
            p.setPreferredSize(panelFieldSize);
        }

        fields.add(pCat);
        fields.add(Box.createVerticalStrut(16));
        fields.add(pDesde);
        fields.add(Box.createVerticalStrut(16));
        fields.add(pHasta);
        fields.add(Box.createVerticalStrut(22));

        JButton apply = primaryButton("APLICAR FILTROS");
        apply.addActionListener(e -> runQueryInventario());
        apply.setAlignmentX(Component.LEFT_ALIGNMENT);
        apply.setPreferredSize(new Dimension(formWidth, apply.getPreferredSize().height));
        apply.setMaximumSize(new Dimension(formWidth, apply.getPreferredSize().height));
        fields.add(apply);

        fields.setPreferredSize(new Dimension(formWidth, fields.getPreferredSize().height));
        fields.setMaximumSize(new Dimension(formWidth, Integer.MAX_VALUE));

        JPanel centerWrapper = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 80));
        centerWrapper.setOpaque(false);
        centerWrapper.add(fields);
        card.add(centerWrapper, BorderLayout.CENTER);

        JPanel tags = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 10));
        tags.setOpaque(false);
        tags.add(tag("Insumos"));
        tags.add(tag("Oficina"));
        tags.add(tag("Bienes"));
        card.add(tags, BorderLayout.SOUTH);

        return card;
    }

    private CardPanel buildTablaPanelInventario() {
        CardPanel card = new CardPanel();
        card.setLayout(new BorderLayout(10, 10));

        JLabel title = new JLabel("RESULTADOS");
        title.setFont(title.getFont().deriveFont(Font.BOLD, 18f));
        title.setForeground(new Color(73, 103, 204));
        card.add(title, BorderLayout.NORTH);

        JTable table = new JTable(modelInv);
        table.setRowHeight(38);
        table.setIntercellSpacing(new Dimension(0, 6));
        table.setShowGrid(true);
        table.setGridColor(new Color(232, 232, 232));
        table.setFillsViewportHeight(true);
        table.getColumnModel().getColumn(2).setCellRenderer(statusRenderer());
        table.setDefaultEditor(Object.class, null);

        JTableHeader header = table.getTableHeader();
        header.setReorderingAllowed(false);
        header.setDefaultRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object v, boolean s, boolean f, int r, int c) {
                JLabel lbl = (JLabel) super.getTableCellRendererComponent(t, v, s, f, r, c);
                lbl.setHorizontalAlignment(SwingConstants.CENTER);
                lbl.setBackground(new Color(230, 230, 230));
                lbl.setOpaque(true);
                lbl.setFont(lbl.getFont().deriveFont(Font.BOLD));
                lbl.setBorder(UIManager.getBorder("TableHeader.cellBorder"));
                return lbl;
            }
        });

        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(SwingConstants.CENTER);
        for (int i = 0; i < table.getColumnCount(); i++) {
            if (i != 2) table.getColumnModel().getColumn(i).setCellRenderer(centerRenderer);
        }

        JScrollPane scroll = new JScrollPane(table);
        card.setBorder(BorderFactory.createCompoundBorder(
                new javax.swing.border.LineBorder(new Color(232, 232, 232), 1, true),
                BorderFactory.createEmptyBorder(6, 6, 6, 6)
        ));
        card.add(scroll, BorderLayout.CENTER);
        return card;
    }

    private DefaultTableCellRenderer statusRenderer() {
        return new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object v, boolean s, boolean f, int r, int c) {
                Component comp = super.getTableCellRendererComponent(t, v, s, f, r, c);
                setHorizontalAlignment(CENTER);
                if (v != null) {
                    String text = v.toString();
                    setText(text.toUpperCase());
                    setOpaque(true);
                    setBorder(BorderFactory.createEmptyBorder(4, 12, 4, 12));
                    switch (text.toLowerCase(Locale.ROOT)) {
                        case "activo"    -> setBackground(new Color(212, 235, 216));
                        case "pendiente" -> setBackground(new Color(255, 239, 200));
                        default          -> setBackground(new Color(220, 228, 255));
                    }
                }
                return comp;
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
        b.setBackground(COL_BRAND);
        b.setForeground(Color.WHITE);
        b.setFont(b.getFont().deriveFont(Font.BOLD, 14f));
        b.setFocusPainted(false);
        b.setBorder(BorderFactory.createEmptyBorder(12, 18, 12, 18));
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
        JToggleButton t = new JToggleButton(text);
        t.setFocusPainted(false);
        t.setOpaque(true);
        t.setBackground(Color.WHITE);
        t.setForeground(COL_BRAND);
        t.setFont(t.getFont().deriveFont(Font.BOLD, 16f));
        t.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(COL_BRAND, 2, true),
                BorderFactory.createEmptyBorder(12, 22, 12, 22)
        ));
        t.addChangeListener(e -> {
            if (t.isSelected()) {
                t.setBackground(COL_BRAND);
                t.setForeground(Color.WHITE);
            } else {
                t.setBackground(Color.WHITE);
                t.setForeground(COL_BRAND);
            }
        });
        return t;
    }

    // ==== Métricas ====
    private JComponent buildMetrics() {
        JPanel row = new JPanel(new GridLayout(1, 3, 24, 0));
        row.setOpaque(false);
        row.setBorder(BorderFactory.createEmptyBorder(10, 40, 40, 40));

        CardPanel c1 = metricCard("TOTAL INSUMOS", lblTotalInsumos, COL_BRAND_SOFT, COL_TEXT_PRIMARY, false);
        c1.setBorder(BorderFactory.createEmptyBorder(14, 20, 14, 20));

        CardPanel c2 = metricCard("TRÁMITES", lblTotalTramites, COL_CARD, COL_TEXT_PRIMARY, false);
        c2.setBorder(BorderFactory.createEmptyBorder(14, 20, 14, 20));

        CardPanel c3 = metricCard("GASTOS MENSUALES", lblGastos, COL_BRAND, Color.WHITE, true);
        c3.setBorder(BorderFactory.createEmptyBorder(14, 20, 14, 20));

        row.add(c1); row.add(c2); row.add(c3);
        return row;
    }

    private CardPanel metricCard(String title, JLabel value, Color bg, Color text, boolean strong) {
        CardPanel card = new CardPanel() {
            @Override protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                int w = getWidth(), h = getHeight();
                Color start = new Color(180, 205, 255);
                Color end   = new Color(110, 150, 240);
                GradientPaint gp = new GradientPaint(0, 0, start, w, 0, end);
                g2.setPaint(gp);
                g2.fillRoundRect(0, 0, w, h, 20, 20);
                g2.dispose();
            }
        };
        card.setLayout(new BorderLayout());
        card.setOpaque(false);
        card.setBorder(BorderFactory.createEmptyBorder(14, 20, 14, 20));

        JLabel lblTitle = new JLabel(title);
        lblTitle.setForeground(new Color(30, 50, 100));
        lblTitle.setFont(lblTitle.getFont().deriveFont(Font.BOLD, 14f));
        lblTitle.setBorder(BorderFactory.createEmptyBorder(0, 0, 8, 0));

        value.setForeground(new Color(20, 40, 90));
        value.setFont(value.getFont().deriveFont(Font.BOLD, strong ? 36f : 28f));

        JPanel center = new JPanel(new BorderLayout());
        center.setOpaque(false);
        center.add(value, BorderLayout.CENTER);

        card.add(lblTitle, BorderLayout.NORTH);
        card.add(center, BorderLayout.CENTER);

        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createEmptyBorder(10, 10, 10, 10),
                BorderFactory.createLineBorder(new Color(0, 0, 0, 30), 1, true)
        ));

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
            public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                          boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                setText(value == null ? "Todas" : ((Categoria) value).getNombre());
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
            long pend = tramites.stream()
                    .filter(t -> "PENDIENTE".equalsIgnoreCase(String.valueOf(t.getEstado())))
                    .count();
            lblPendientes.setText(String.valueOf(pend));
            lblPendientesMini.setText(" " + lblPendientes.getText() + " PENDIENTES");
        } catch (Exception e) {
            lblTotalTramites.setText("-"); lblPendientes.setText("-");
        }

        try { lblGastos.setText("+$0"); }
        catch (Exception e) { lblGastos.setText("$-"); }
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
                modelInv.addRow(new Object[]{ i.getCodigo(), i.getDescripcion(), i.getEstado(), fecha });
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
            if (d2 != null && (fa == null || fa.isAfter(d2)))  return false;
            return true;
        }).collect(Collectors.toList());
    }

    // ====== TRÁMITES (Informes) ======
    private Component buildTramitesFilters() {
        JPanel panel = new JPanel(new BorderLayout(18, 0));
        panel.setOpaque(false);

        JLabel lbl = new JLabel("FILTRO");
        lbl.setFont(new Font("Segoe UI", Font.BOLD, 16));
        lbl.setForeground(new Color(45, 66, 132));
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
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                if (getRowCount() == 0) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                    String msg = "Sin resultados";
                    g2.setFont(getFont().deriveFont(Font.PLAIN, 14f));
                    g2.setColor(new Color(120, 130, 150));
                    FontMetrics fm = g2.getFontMetrics();
                    int x = (getWidth() - fm.stringWidth(msg)) / 2;
                    int y = getHeight() / 2;
                    g2.drawString(msg, x, y);
                    g2.dispose();
                }
            }
        };
        table.setRowHeight(44);
        table.setShowHorizontalLines(false);
        table.setShowVerticalLines(false);
        table.setIntercellSpacing(new Dimension(0, 0));
        table.setFillsViewportHeight(true);
        table.setSelectionBackground(new Color(226, 233, 255));
        table.setSelectionForeground(new Color(32, 48, 105));
        table.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        table.setAutoCreateRowSorter(true);

        JTableHeader header = table.getTableHeader();
        header.setPreferredSize(new Dimension(header.getPreferredSize().width, 46));
        header.setDefaultRenderer(new TableHeaderRenderer());

        table.getColumnModel().getColumn(5).setCellRenderer(new BadgeRenderer(BadgeRenderer.Type.STATUS));

        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.getViewport().setBackground(Color.WHITE);
        return scroll;
    }

    private void styleFilterField(JComponent c, int w) {
        Dimension d = new Dimension(w, 32);
        c.setPreferredSize(d);
        c.setMinimumSize(d);
    }

    /** Carga de datos para TRÁMITES (sin filtro de categoría). */
    private void loadTableDataTramites() {
        modelTra.setRowCount(0);
        try {
            String search = filterSearch.getText().trim().toLowerCase(Locale.ROOT);
            String estadoFiltro = (String) filterEstado.getSelectedItem();

            List<Tramite> tramites = traService.listarTodos();
            if (tramites == null) return;

            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

            for (Tramite t : tramites) {
                // búsqueda: nro + asunto + descripción
                if (!search.isEmpty()) {
                    String texto = (String.valueOf(t.getAsunto()) + " " + String.valueOf(t.getNro()) + " " +
                                   (t.getDescripcion() != null ? t.getDescripcion() : ""))
                                   .toLowerCase(Locale.ROOT);
                    if (!texto.contains(search)) continue;
                }

                String estado = estadoFriendly(t.getEstado());
                if (!"Todos".equals(estadoFiltro) && !estado.equalsIgnoreCase(estadoFiltro)) continue;

                String actualizacion = t.getFecha() == null ? "-" : t.getFecha().format(fmt);
                String ultima        = t.getFecha() == null ? "-" : t.getFecha().plusDays(1).format(fmt); // placeholder si no tenés campo real
                String descripcion   = extraerDescripcionTramite(t);

                modelTra.addRow(new Object[]{
                    t.getNro(),
                    t.getAsunto(),
                    actualizacion,
                    ultima,
                    descripcion,
                    estado
                });
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    /** Conecta los filtros (buscador y estado) para TRÁMITES. */
    private void installFiltersTramites() {
        if (filterSearch != null && filterSearch.getDocument() != null) {
            filterSearch.getDocument().addDocumentListener(new SimpleDocumentListener() {
                @Override public void update() { loadTableDataTramites(); }
            });
        }
        if (filterEstado != null) {
            filterEstado.addActionListener(e -> loadTableDataTramites());
        }
    }

    /** Devuelve la descripción REAL del trámite (si no hay, "-"). */
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
        } catch (Throwable ignore) {}

        String v = tryGetter(t,
                "getDescripción","getDetalle","getDetalles",
                "getObservacion","getObservación","getObservaciones",
                "getNota","getNotas",
                "getComentario","getComentarios",
                "getMotivo","getResumen",
                "getInfo","getInformacion","getInformación"
        );
        if (v != null && !v.isBlank()) return v.trim();

        try {
            for (Method m : t.getClass().getMethods()) {
                if (m.getParameterCount() == 0 &&
                    m.getName().startsWith("get") &&
                    m.getReturnType() == String.class) {
                    String name = m.getName().toLowerCase(Locale.ROOT);
                    if (name.contains("desc") || name.contains("detalle") ||
                        name.contains("observ") || name.contains("nota") ||
                        name.contains("coment") || name.contains("motivo")) {
                        Object val = m.invoke(t);
                        if (val != null) {
                            String s = val.toString().trim();
                            if (!s.isEmpty()) return s;
                        }
                    }
                }
            }
        } catch (Throwable ignore) {}

        try {
            for (java.lang.reflect.Field f : t.getClass().getDeclaredFields()) {
                String n = f.getName().toLowerCase(Locale.ROOT);
                if (n.contains("desc") || n.contains("detalle") ||
                    n.contains("observ") || n.contains("nota") ||
                    n.contains("coment") || n.contains("motivo")) {
                    f.setAccessible(true);
                    Object val = f.get(t);
                    if (val != null) {
                        String s = val.toString().trim();
                        if (!s.isEmpty()) return s;
                    }
                }
            }
        } catch (Throwable ignore) {}

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
            } catch (Throwable ignore) {
            }
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

    private String prioridadDesdeEstado(String estado) {
        if (estado == null) return "Media";
        String e = estado.trim().toLowerCase(Locale.ROOT);
        if (e.contains("alta")) return "Alta";
        if (e.contains("pend")) return "Media";
        return "Baja";
    }

    // Encabezado y badges
    static class TableHeaderRenderer extends DefaultTableCellRenderer {
        TableHeaderRenderer() {
            setHorizontalAlignment(LEFT);
            setFont(new Font("Segoe UI", Font.BOLD, 13));
            setForeground(new Color(70, 88, 140));
            setBackground(new Color(238, 242, 255));
            setBorder(new EmptyBorder(12, 14, 12, 14));
        }
        @Override public Component getTableCellRendererComponent(JTable t, Object v, boolean s, boolean f, int r, int c) {
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
                if (normalized.contains("complet"))     base = new Color(28, 184, 113);
                else if (normalized.contains("proceso")) base = new Color(58, 96, 224);
                else if (normalized.contains("alta"))    base = new Color(220, 84, 84);
                else                                     base = new Color(180, 180, 180);
            }

            if (s) {
                setForeground(Color.WHITE);
                setBackground(base.darker());
            } else {
                setForeground(Color.WHITE);
                setBackground(base);
            }
            return this;
        }
    }

    // ====== Export ======
    private void exportInventarioToPdf() {
        exportModelToPdf("INFORME DE INVENTARIO", new String[]{"Código","Descripción","Estado","Fecha"}, modelInv);
    }
    private void exportTramitesToPdf() {
        exportModelToPdf("INFORME DE TRÁMITES",
                new String[]{"ID Trámite","Asunto","Fecha actualización","Última actualización","Descripción","Estado"},
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

            com.lowagie.text.Font fTitle = new com.lowagie.text.Font(com.lowagie.text.Font.HELVETICA, 18, com.lowagie.text.Font.BOLD, new Color(24,63,150));
            Paragraph title = new Paragraph(titulo, fTitle);
            title.setAlignment(Element.ALIGN_CENTER);
            title.setSpacingAfter(8f);
            doc.add(title);

            PdfPTable table = new PdfPTable(headers.length);
            table.setWidthPercentage(100);
            float[] w = new float[headers.length];
            Arrays.fill(w, 1f);
            table.setWidths(w);

            com.lowagie.text.Font fHeader = new com.lowagie.text.Font(com.lowagie.text.Font.HELVETICA, 11, com.lowagie.text.Font.BOLD, Color.WHITE);
            for (String h : headers) {
                PdfPCell cell = new PdfPCell(new Phrase(h, fHeader));
                cell.setHorizontalAlignment(Element.ALIGN_CENTER);
                cell.setBackgroundColor(new Color(47,107,228));
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

    /** Busca un JLabel "Informes" que esté en la barra superior (no en el menú lateral) */
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

    // Listener simple para JTextField
    private static abstract class SimpleDocumentListener implements javax.swing.event.DocumentListener {
        public abstract void update();
        @Override public void insertUpdate(javax.swing.event.DocumentEvent e) { update(); }
        @Override public void removeUpdate(javax.swing.event.DocumentEvent e) { update(); }
        @Override public void changedUpdate(javax.swing.event.DocumentEvent e) { update(); }
    }
}
