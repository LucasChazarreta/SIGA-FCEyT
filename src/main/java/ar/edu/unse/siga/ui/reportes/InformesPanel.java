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

public class InformesPanel extends JPanel {

    // ======= Estilos =======
    private static final Color COL_BG = new Color(0xE9, 0xEB, 0xEF);
    private static final Color COL_TEXT_PRIMARY = new Color(0x0B, 0x0B, 0x0C);
    private static final Color COL_BRAND = new Color(0x2F, 0x6B, 0xE4);
    private static final Color COL_BRAND_SOFT = new Color(0xC7, 0xD7, 0xEA);
    private static final Color COL_CARD = new Color(0xF1, 0xF3, 0xF6);

    private final InventarioService invService;
    private final TramiteService traService;

    // Métricas
    private final JLabel lblTotalInsumos = new JLabel("-");
    private final JLabel lblTotalTramites = new JLabel("-");
    private final JLabel lblPendientes = new JLabel("-");
    private final JLabel lblGastos = new JLabel("$-");
    private final JLabel lblPendientesMini = new JLabel();

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
    ) {
        @Override
        public boolean isCellEditable(int r, int c) {
            return false;
        }
    };

    // --- TRÁMITES ---
    private final DefaultTableModel modelTra = new DefaultTableModel(
            new Object[]{"ID Trámite", "Asunto", "Fecha actualización", "Última actualización", "Descripción", "Estado"}, 0
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

    public InformesPanel(InventarioService invService, TramiteService traService) {
        this.invService = invService;
        this.traService = traService;

        setLayout(new BorderLayout(0, 12));
        setOpaque(false);
        setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        add(buildHeader(), BorderLayout.NORTH);
        add(buildContentScrollable(), BorderLayout.CENTER);

        // Ocultar título duplicado en el header global (si existe)
        SwingUtilities.invokeLater(() -> {
            Window w = SwingUtilities.getWindowAncestor(InformesPanel.this);
            if (w instanceof JFrame frame) {
                Container root = frame.getContentPane();
                JLabel headerTitle = findHeaderTitleLabel(root);
                if (headerTitle != null) {
                    headerTitle.setText("");
                    headerTitle.getParent().revalidate();
                    headerTitle.getParent().repaint();
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
        installFiltersTramites();
    }

    // ====== Header con export ======
    private JComponent buildHeader() {
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);

        JLabel title = new JLabel("INFORMES", SwingConstants.CENTER);
        title.setFont(title.getFont().deriveFont(Font.BOLD, 34f));
        title.setForeground(new Color(24, 63, 150));

        JPanel center = new JPanel(new GridBagLayout());
        center.setOpaque(false);
        center.add(title);

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
        wrapper.setOpaque(false);

        ButtonGroup tabs = new ButtonGroup();
        btnInventario = pill("INVENTARIO");
        btnTramites = pill("TRÁMITES");
        btnInventario.setSelected(true);
        tabs.add(btnInventario);
        tabs.add(btnTramites);

        JPanel tabRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 18, 6));
        tabRow.setOpaque(false);
        tabRow.add(btnInventario);
        tabRow.add(btnTramites);
        wrapper.add(tabRow, BorderLayout.NORTH);

        // INVENTARIO
        JPanel invPanel = new JPanel(new BorderLayout());
        invPanel.setOpaque(false);
        invPanel.add(buildMetrics(), BorderLayout.NORTH);
        invPanel.add(buildInventarioSplit(), BorderLayout.CENTER);

        // TRÁMITES
        JPanel traPanel = new JPanel(new BorderLayout(12, 12));
        traPanel.setOpaque(false);
        traPanel.add(buildTramitesFilters(), BorderLayout.NORTH);
        traPanel.add(buildTramitesTableScroll(), BorderLayout.CENTER);

        content.setOpaque(false);
        content.add(invPanel, "INV");
        content.add(traPanel, "TRA");

        wrapper.add(content, BorderLayout.CENTER);

        btnInventario.addActionListener(e -> contentCards.show(content, "INV"));
        btnTramites.addActionListener(e -> {
            contentCards.show(content, "TRA");
            loadTableDataTramites();
        });

        // 🔑 Scroll general para todo el contenido (si la ventana se achica)
        JScrollPane scroller = new JScrollPane(wrapper,
                ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scroller.setBorder(BorderFactory.createEmptyBorder());
        scroller.getVerticalScrollBar().setUnitIncrement(18);
        return scroller;
    }

    // ====== INVENTARIO ======
    private JPanel buildInventarioSplit() {
        JPanel split = new JPanel(new GridBagLayout());
        split.setOpaque(false);

        GridBagConstraints gc = new GridBagConstraints();
        gc.insets = new Insets(0, 0, 0, 12);
        gc.fill = GridBagConstraints.BOTH;
        gc.weighty = 1;

        // Lado filtros con SU PROPIO scroll (para que "APLICAR FILTROS" siempre sea accesible)
        CardPanel filtrosCard = buildFiltrosPanelInventario();
        JScrollPane filtrosScroll = new JScrollPane(filtrosCard,
                ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        filtrosScroll.setBorder(BorderFactory.createEmptyBorder());
        filtrosScroll.setMinimumSize(new Dimension(260, 180));
        filtrosScroll.setPreferredSize(new Dimension(300, 300));
        gc.gridx = 0;
        gc.weightx = 0.32;
        split.add(filtrosScroll, gc);

        // Lado tabla
        CardPanel tabla = buildTablaPanelInventario();
        gc.gridx = 1;
        gc.weightx = 0.68;
        gc.insets = new Insets(0, 0, 0, 0);
        split.add(tabla, gc);

        return split;
    }

    private CardPanel buildFiltrosPanelInventario() {
        CardPanel card = new CardPanel();
        card.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        card.setLayout(new BorderLayout(10, 10));

        JLabel title = new JLabel("FILTROS");
        title.setFont(title.getFont().deriveFont(Font.BOLD, 16f));
        title.setForeground(new Color(73, 103, 204));
        card.add(title, BorderLayout.NORTH);

        JPanel fields = new JPanel();
        fields.setOpaque(false);
        fields.setLayout(new BoxLayout(fields, BoxLayout.Y_AXIS));

        Dimension fieldSize = new Dimension(220, 34);

        cbCategoria.setAlignmentX(Component.LEFT_ALIGNMENT);
        cbCategoria.setMaximumSize(fieldSize);
        cbCategoria.setPreferredSize(fieldSize);

        dfDesde.getComponent().setAlignmentX(Component.LEFT_ALIGNMENT);
        dfDesde.getComponent().setMaximumSize(fieldSize);
        dfDesde.getComponent().setPreferredSize(fieldSize);

        dfHasta.getComponent().setAlignmentX(Component.LEFT_ALIGNMENT);
        dfHasta.getComponent().setMaximumSize(fieldSize);
        dfHasta.getComponent().setPreferredSize(fieldSize);

        fields.add(filterField("Categoría", cbCategoria));
        fields.add(Box.createVerticalStrut(12));
        fields.add(filterField("Desde", dfDesde.getComponent()));
        fields.add(Box.createVerticalStrut(12));
        fields.add(filterField("Hasta", dfHasta.getComponent()));
        fields.add(Box.createVerticalStrut(16));

        JButton apply = primaryButton("APLICAR FILTROS");
        apply.addActionListener(e -> runQueryInventario());
        apply.setAlignmentX(Component.LEFT_ALIGNMENT);
        fields.add(apply);

        fields.add(Box.createVerticalStrut(16));

        JPanel tags = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        tags.setOpaque(false);
        tags.add(tag("Insumos"));
        tags.add(tag("Oficina"));
        tags.add(tag("Bienes"));
        tags.setAlignmentX(Component.LEFT_ALIGNMENT);
        fields.add(tags);

        // glue para que el scroll funcione bien cuando hay espacio
        fields.add(Box.createVerticalGlue());

        card.add(fields, BorderLayout.CENTER);
        return card;
    }

    private CardPanel buildTablaPanelInventario() {
        CardPanel card = new CardPanel();
        card.setLayout(new BorderLayout(10, 10));

        JLabel title = new JLabel("RESULTADOS");
        title.setFont(title.getFont().deriveFont(Font.BOLD, 18f));
        title.setForeground(new Color(73, 103, 204));
        card.add(title, BorderLayout.NORTH);

        JTable table = new JTable(modelInv) {
            @Override
            public boolean getScrollableTracksViewportWidth() {
                // permite scroll horizontal si columnas exceden el viewport
                return getParent() instanceof JViewport
                        && getPreferredSize().width < getParent().getWidth();
            }
        };
        table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF); // 🔑 habilita scroll horizontal
        table.setRowHeight(38);
        table.setIntercellSpacing(new Dimension(0, 6));
        table.setShowGrid(true);
        table.setGridColor(new Color(232, 232, 232));
        table.setFillsViewportHeight(true);
        table.setDefaultEditor(Object.class, null);

        // Anchos razonables
        TableColumnModel tcm = table.getColumnModel();
        if (tcm.getColumnCount() >= 4) {
            tcm.getColumn(0).setPreferredWidth(180);
            tcm.getColumn(1).setPreferredWidth(280);
            tcm.getColumn(2).setPreferredWidth(120);
            tcm.getColumn(3).setPreferredWidth(120);
        }

        // Renderers
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

        DefaultTableCellRenderer center = new DefaultTableCellRenderer();
        center.setHorizontalAlignment(SwingConstants.CENTER);
        for (int i = 0; i < table.getColumnCount(); i++) {
            table.getColumnModel().getColumn(i).setCellRenderer(i == 2 ? statusRenderer() : center);
        }

        JScrollPane scroll = new JScrollPane(table,
                ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scroll.setBorder(BorderFactory.createCompoundBorder(
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
                        case "activo" ->
                            setBackground(new Color(212, 235, 216));
                        case "pendiente" ->
                            setBackground(new Color(255, 239, 200));
                        default ->
                            setBackground(new Color(220, 228, 255));
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
        b.setBorder(BorderFactory.createEmptyBorder(10, 16, 10, 16));
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
                BorderFactory.createEmptyBorder(10, 18, 10, 18)
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
        JPanel row = new JPanel(new GridLayout(1, 3, 16, 0));
        row.setOpaque(false);
        row.setBorder(BorderFactory.createEmptyBorder(6, 6, 12, 6));

        CardPanel c1 = metricCard("TOTAL INSUMOS", lblTotalInsumos, COL_BRAND_SOFT, COL_TEXT_PRIMARY, false);
        CardPanel c2 = metricCard("TRÁMITES", lblTotalTramites, COL_CARD, COL_TEXT_PRIMARY, false);
        CardPanel c3 = metricCard("GASTOS MENSUALES", lblGastos, COL_BRAND, Color.WHITE, true);

        row.add(c1);
        row.add(c2);
        row.add(c3);
        return row;
    }

    private CardPanel metricCard(String title, JLabel value, Color bg, Color text, boolean strong) {
        CardPanel card = new CardPanel();
        card.setLayout(new BorderLayout());
        card.setOpaque(false);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createEmptyBorder(8, 8, 8, 8),
                BorderFactory.createLineBorder(new Color(0, 0, 0, 30), 1, true)
        ));

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
            public Component getListCellRendererComponent(JList<?> list,
                    Object value,
                    int index,
                    boolean isSelected,
                    boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                String texto = (value instanceof Categoria c) ? c.getNombre() : "Todas";
                setText(texto);
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
            long pend = tramites.stream().filter(t -> "PENDIENTE".equalsIgnoreCase(String.valueOf(t.getEstado()))).count();
            lblPendientes.setText(String.valueOf(pend));
            lblPendientesMini.setText(" " + lblPendientes.getText() + " PENDIENTES");
        } catch (Exception e) {
            lblTotalTramites.setText("-");
            lblPendientes.setText("-");
        }

        lblGastos.setText("+$0"); // placeholder
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

    // ====== TRÁMITES ======
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
            public boolean getScrollableTracksViewportWidth() {
                return getParent() instanceof JViewport
                        && getPreferredSize().width < getParent().getWidth();
            }
        };
        table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF); // 🔑 scroll horizontal si hace falta
        table.setRowHeight(44);
        table.setShowHorizontalLines(false);
        table.setShowVerticalLines(false);
        table.setIntercellSpacing(new Dimension(0, 0));
        table.setFillsViewportHeight(true);
        table.setSelectionBackground(new Color(226, 233, 255));
        table.setSelectionForeground(new Color(32, 48, 105));
        table.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        table.setAutoCreateRowSorter(true);

        // anchos base
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

        JScrollPane scroll = new JScrollPane(table,
                ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.getViewport().setBackground(Color.WHITE);
        return scroll;
    }

    private void styleFilterField(JComponent c, int w) {
        Dimension d = new Dimension(w, 32);
        c.setPreferredSize(d);
        c.setMinimumSize(d);
    }

    private void loadTableDataTramites() {
        modelTra.setRowCount(0);
        try {
            String search = filterSearch.getText().trim().toLowerCase(Locale.ROOT);
            String estadoFiltro = (String) filterEstado.getSelectedItem();

            List<Tramite> tramites = traService.listarTodos();
            if (tramites == null) {
                return;
            }

            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

            for (Tramite t : tramites) {
                if (!search.isEmpty()) {
                    String texto = (String.valueOf(t.getAsunto()) + " " + String.valueOf(t.getNro()) + " "
                            + (t.getDescripcion() != null ? t.getDescripcion() : ""))
                            .toLowerCase(Locale.ROOT);
                    if (!texto.contains(search)) {
                        continue;
                    }
                }

                String estado = estadoFriendly(t.getEstado());
                if (!"Todos".equals(estadoFiltro) && !estado.equalsIgnoreCase(estadoFiltro)) {
                    continue;
                }

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
                @Override
                public void update() {
                    loadTableDataTramites();
                }
            });
        }
        filterEstado.addActionListener(e -> loadTableDataTramites());
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

        String v = tryGetter(t, "getDescripción", "getDetalle", "getDetalles", "getObservacion", "getObservación", "getObservaciones",
                "getNota", "getNotas", "getComentario", "getComentarios", "getMotivo", "getResumen", "getInfo", "getInformacion", "getInformación");
        if (v != null && !v.isBlank()) {
            return v.trim();
        }

        try {
            for (Method m : t.getClass().getMethods()) {
                if (m.getParameterCount() == 0 && m.getName().startsWith("get") && m.getReturnType() == String.class) {
                    String name = m.getName().toLowerCase(Locale.ROOT);
                    if (name.contains("desc") || name.contains("detalle") || name.contains("observ") || name.contains("nota")
                            || name.contains("coment") || name.contains("motivo")) {
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
                if (n.contains("desc") || n.contains("detalle") || n.contains("observ") || n.contains("nota")
                        || n.contains("coment") || n.contains("motivo")) {
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

    // Encabezado y badges
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

            com.lowagie.text.Font fTitle = new com.lowagie.text.Font(com.lowagie.text.Font.HELVETICA, 18, com.lowagie.text.Font.BOLD, new Color(24, 63, 150));
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
                cell.setBackgroundColor(new Color(47, 107, 228));
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
            try {
                doc.close();
            } catch (Exception ignore) {
            }
            JOptionPane.showMessageDialog(this, "No se pudo generar el PDF:\n" + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void exportInventarioToCsv() {
        exportModelToCsv(modelInv, ',');
    }

    private void exportTramitesToCsv() {
        exportModelToCsv(modelTra, ',');
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
     * Busca un JLabel "Informes" que esté en la barra superior
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
}
