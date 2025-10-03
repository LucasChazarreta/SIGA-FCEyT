package ar.edu.unse.siga.ui.reportes;

import ar.edu.unse.siga.domain.Categoria;
import ar.edu.unse.siga.domain.Insumo;
import ar.edu.unse.siga.service.InventarioService;
import ar.edu.unse.siga.service.TramiteService;
import ar.edu.unse.siga.ui.base.CardPanel;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import javax.swing.text.MaskFormatter;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.lang.reflect.Method;
import java.text.ParseException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

import java.io.File;
import java.io.PrintWriter;
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
import java.io.FileOutputStream;

public class InformesPanel extends JPanel {

    // ======= Estilos Figma =======
    private static final Color COL_BG = new Color(0xE9,0xEB,0xEF);
    private static final Color COL_TEXT_PRIMARY = new Color(0x0B,0x0B,0x0C);
    private static final Color COL_BRAND = new Color(0x2F,0x6B,0xE4);
    private static final Color COL_BRAND_SOFT = new Color(0xC7,0xD7,0xEA);
    private static final Color COL_CARD = new Color(0xF1,0xF3,0xF6);
    private static final Color COL_SHADOW = new Color(0xD1,0xD6,0xDF);

    private final InventarioService invService;
    private final TramiteService traService;

    private final JLabel lblTotalInsumos = new JLabel("-");
    private final JLabel lblTotalTramites = new JLabel("-");
    private final JLabel lblPendientes = new JLabel("-");
    private final JLabel lblGastos = new JLabel("$-");
    private final JLabel lblPendientesMini = new JLabel();

    private final JComboBox<Categoria> cbCategoria = new JComboBox<>();

    // Date fields
    private final DateField dfDesde = new DateField();
    private final DateField dfHasta = new DateField();

    private final DefaultTableModel model = new DefaultTableModel(
            new Object[]{"Código","Descripción","Estado","Fecha"}, 0
    ) {
        @Override public boolean isCellEditable(int row, int column) { return false; }
    };

    public InformesPanel(InventarioService invService, TramiteService traService) {
        this.invService = invService;
        this.traService = traService;

        setLayout(new BorderLayout(20, 20));
        setOpaque(false);

        add(buildHeader(), BorderLayout.NORTH);
        add(buildContent(), BorderLayout.CENTER);

        // Ocultar “Informes” del header global
        SwingUtilities.invokeLater(() -> {
            Window w = SwingUtilities.getWindowAncestor(InformesPanel.this);
            if (w instanceof JFrame frame) {
                Container root = frame.getContentPane();
                JLabel headerTitle = findHeaderTitleLabel(root);
                if (headerTitle != null) {
                    headerTitle.setText("");
                    Container parent = headerTitle.getParent();
                    if (parent != null) {
                        parent.revalidate();
                        parent.repaint();
                    }
                }
            }
        });

        // Re-aplicar cuando se muestre de nuevo
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
        runQuery();
    }

    /** Wrapper de fecha: FlatDatePicker si está disponible; si no, máscara. */
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
                try {
                    return LocalDate.parse(txt, fmt);
                } catch (Exception e) {
                    return null;
                }
            }
        }

        JComponent getComponent() { return comp; }
    }

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

    private JComponent buildHeader() {
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);

        JLabel title = new JLabel("INFORMES", SwingConstants.CENTER);
        title.setFont(title.getFont().deriveFont(Font.BOLD, 36f));
        title.setForeground(new Color(24, 63, 150));
        header.add(title, BorderLayout.CENTER);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        actions.setOpaque(false);
        JButton exportPdf = primaryButton("Exportar PDF");
        JButton exportCsv = secondaryButton("Exportar CSV");

        // Listener para exportar a PDF y CSV
        exportPdf.addActionListener(e -> exportToPdf());
        exportCsv.addActionListener(e -> exportToCsv());

        actions.add(exportPdf);
        actions.add(exportCsv);
        header.add(actions, BorderLayout.EAST);

        header.setBorder(BorderFactory.createEmptyBorder(10, 20, 20, 20));
        return header;
    }

    private JComponent buildMetrics() {
        JPanel row = new JPanel(new GridLayout(1, 3, 24, 0));
        row.setOpaque(false);
        row.setBorder(BorderFactory.createEmptyBorder(10, 40, 20, 40));

        CardPanel c1 = metricCard("TOTAL INSUMOS", lblTotalInsumos, COL_BRAND_SOFT, COL_TEXT_PRIMARY, false);
        CardPanel c2 = metricCard("TRÁMITES", lblTotalTramites, COL_CARD, COL_TEXT_PRIMARY, false);

        lblPendientesMini.setText(" " + lblPendientes.getText() + " PENDIENTES");
        lblPendientesMini.setForeground(COL_BRAND);
        lblPendientesMini.setFont(lblPendientesMini.getFont().deriveFont(Font.BOLD, 16f));
        JPanel c2inner = (JPanel) c2.getComponent(1);
        c2inner.add(lblPendientesMini, BorderLayout.SOUTH);

        CardPanel c3 = metricCard("GASTOS MENSUALES", lblGastos, COL_BRAND, Color.WHITE, true);

        row.add(c1);
        row.add(c2);
        row.add(c3);
        return row;
    }

    private CardPanel metricCard(String title, JLabel value, Color bg, Color text, boolean strong) {
        CardPanel card = new CardPanel();
        card.setLayout(new BorderLayout());
        card.setOpaque(true);
        card.setBackground(bg);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createEmptyBorder(14, 20, 14, 20),
                BorderFactory.createMatteBorder(0, 0, 3, 0, COL_SHADOW)
        ));

        JLabel lblTitle = new JLabel(title);
        lblTitle.setForeground(text.equals(Color.WHITE) ? Color.WHITE : new Color(0x44,0x52,0x63));
        lblTitle.setFont(lblTitle.getFont().deriveFont(Font.BOLD, 14f));
        lblTitle.setBorder(BorderFactory.createEmptyBorder(0, 0, 8, 0));

        value.setForeground(text);
        value.setFont(value.getFont().deriveFont(Font.BOLD, strong ? 36f : 28f));

        JPanel center = new JPanel(new BorderLayout());
        center.setOpaque(false);
        center.add(value, BorderLayout.CENTER);

        card.add(lblTitle, BorderLayout.NORTH);
        card.add(center, BorderLayout.CENTER);
        return card;
    }

    private JComponent buildContent() {
        JPanel wrapper = new JPanel(new BorderLayout(8, 18));
        wrapper.setOpaque(false);

        ButtonGroup tabs = new ButtonGroup();
        JToggleButton btnInventario = pill("INVENTARIO");
        JToggleButton btnTramites = pill("TRÁMITES");
        btnInventario.setSelected(true);
        tabs.add(btnInventario);
        tabs.add(btnTramites);

        JPanel tabRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 18, 6));
        tabRow.setOpaque(false);
        tabRow.add(btnInventario);
        tabRow.add(btnTramites);
        wrapper.add(tabRow, BorderLayout.NORTH);

        JPanel split = new JPanel(new GridBagLayout());
        split.setOpaque(false);

        GridBagConstraints gc = new GridBagConstraints();
        gc.insets = new Insets(0, 4, 0, 12);
        gc.fill = GridBagConstraints.BOTH;
        gc.weighty = 1;

        CardPanel filtros = buildFiltrosPanel();
        gc.gridx = 0; gc.weightx = 0.25;
        split.add(filtros, gc);

        CardPanel tabla = buildTablaPanel();
        gc.gridx = 1; gc.weightx = 0.75; gc.insets = new Insets(0, 0, 0, 0);
        split.add(tabla, gc);

        JPanel center = new JPanel(new BorderLayout());
        center.setOpaque(false);
        center.add(buildMetrics(), BorderLayout.NORTH);
        center.add(split, BorderLayout.CENTER);

        wrapper.add(center, BorderLayout.CENTER);
        return wrapper;
    }

    private CardPanel buildFiltrosPanel() {
        CardPanel card = new CardPanel();
        card.setBorder(BorderFactory.createEmptyBorder(12, 8, 12, 12));
        card.setLayout(new BorderLayout(10, 10));

        JLabel title = new JLabel("FILTROS");
        title.setFont(title.getFont().deriveFont(Font.BOLD, 14f));
        title.setForeground(new Color(73, 103, 204));
        card.add(title, BorderLayout.NORTH);

        JPanel fields = new JPanel();
        fields.setOpaque(false);
        fields.setLayout(new BoxLayout(fields, BoxLayout.Y_AXIS));

        Dimension comboSize = new Dimension(220, 32);
        cbCategoria.setPreferredSize(comboSize);
        cbCategoria.setMaximumSize(comboSize);
        cbCategoria.setAlignmentX(Component.LEFT_ALIGNMENT);
        fields.add(filterField("Categoría", cbCategoria));
        fields.add(Box.createVerticalStrut(12));

        fields.add(filterField("Desde", dfDesde.getComponent()));
        fields.add(Box.createVerticalStrut(12));
        fields.add(filterField("Hasta", dfHasta.getComponent()));

        JButton apply = primaryButton("APLICAR FILTROS");
        apply.addActionListener(e -> runQuery());
        apply.setAlignmentX(Component.CENTER_ALIGNMENT);
        fields.add(Box.createVerticalStrut(18));
        fields.add(apply);

        int formWidth = 260;
        fields.setPreferredSize(new Dimension(formWidth, fields.getPreferredSize().height));
        fields.setMaximumSize(new Dimension(formWidth, Integer.MAX_VALUE));

        JPanel centerWrapper = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        centerWrapper.setOpaque(false);
        centerWrapper.add(fields);
        card.add(centerWrapper, BorderLayout.CENTER);

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

        JLabel title = new JLabel("RESULTADOS");
        title.setFont(title.getFont().deriveFont(Font.BOLD, 18f));
        title.setForeground(new Color(73, 103, 204));
        card.add(title, BorderLayout.NORTH);

        JTable table = new JTable(model);
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
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                                                           boolean hasFocus, int row, int column) {
                JLabel lbl = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
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
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                                                           boolean hasFocus, int row, int column) {
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
        b.setBackground(COL_BRAND);
        b.setForeground(Color.WHITE);
        b.setFont(b.getFont().deriveFont(Font.BOLD, 14f));
        b.setFocusPainted(false);
        b.setBorder(BorderFactory.createEmptyBorder(12, 18, 12, 18));
        return b;
    }

    private void stylizeSmall(JButton b) {
        b.setFont(b.getFont().deriveFont(Font.BOLD, 12f));
        b.setBorder(BorderFactory.createEmptyBorder(10, 14, 10, 14));
        b.setBackground(COL_BRAND);
        b.setForeground(Color.WHITE);
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

    // ==== Métricas y consulta ====

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
        } catch (Exception e) { lblTotalTramites.setText("-"); lblPendientes.setText("-"); }

        try { lblGastos.setText("+$0"); }
        catch (Exception e) { lblGastos.setText("$-"); }
    }

    private void runQuery() {
        model.setRowCount(0);
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
                    fa = java.time.ZonedDateTime.ofInstant(
                            i.getCreatedAt(), java.time.ZoneId.systemDefault()
                    ).toLocalDate();
                }
                String fecha = (fa != null) ? fa.format(fmt) : "-";
                model.addRow(new Object[]{ i.getCodigo(), i.getDescripcion(), i.getEstado(), fecha });
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
                fa = java.time.ZonedDateTime.ofInstant(
                        i.getCreatedAt(), java.time.ZoneId.systemDefault()
                ).toLocalDate();
            }
            if (d1 != null && (fa == null || fa.isBefore(d1))) return false;
            if (d2 != null && (fa == null || fa.isAfter(d2)))  return false;

            return true;
        }).collect(Collectors.toList());
    }

    /** Exporta la tabla a PDF (OpenPDF). */
    private void exportToPdf() {
        // Elegir archivo destino
        JFileChooser fc = new JFileChooser();
        fc.setDialogTitle("Guardar informe en PDF");
        fc.setFileFilter(new FileNameExtensionFilter("Archivo PDF (*.pdf)", "pdf"));
        fc.setSelectedFile(new java.io.File("informe-inventario.pdf"));
        int opt = fc.showSaveDialog(this);
        if (opt != JFileChooser.APPROVE_OPTION) return;

        java.io.File out = fc.getSelectedFile();
        if (!out.getName().toLowerCase().endsWith(".pdf")) {
            out = new java.io.File(out.getParentFile(), out.getName() + ".pdf");
        }

        var doc = new Document(PageSize.A4.rotate(), 36, 36, 36, 36); // apaisado
        try (FileOutputStream fos = new FileOutputStream(out)) {
            PdfWriter.getInstance(doc, fos);
            doc.open();

            // Título
            com.lowagie.text.Font fTitle = new com.lowagie.text.Font(com.lowagie.text.Font.HELVETICA, 18, com.lowagie.text.Font.BOLD, new Color(24,63,150));
            Paragraph title = new Paragraph("INFORME DE INVENTARIO", fTitle);
            title.setAlignment(Element.ALIGN_CENTER);
            title.setSpacingAfter(8f);
            doc.add(title);

            // Subtítulo con filtros
            String cat = (cbCategoria.getSelectedItem() instanceof Categoria c) ? c.getNombre() : "Todas";
            String d1 = (dfDesde.getDate() != null) ? dfDesde.getDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) : "–";
            String d2 = (dfHasta.getDate() != null) ? dfHasta.getDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) : "–";
            com.lowagie.text.Font fSub = new com.lowagie.text.Font(com.lowagie.text.Font.HELVETICA, 10, com.lowagie.text.Font.NORMAL, Color.DARK_GRAY);
            Paragraph info = new Paragraph("Categoría: " + cat + "   |   Desde: " + d1 + "   |   Hasta: " + d2, fSub);
            info.setSpacingAfter(12f);
            doc.add(info);

            // Tabla con los datos del JTable
            PdfPTable table = new PdfPTable(4);
            table.setWidthPercentage(100);
            table.setWidths(new float[]{20f, 55f, 25f, 20f});

            // Encabezados
            com.lowagie.text.Font fHeader = new com.lowagie.text.Font(com.lowagie.text.Font.HELVETICA, 11, com.lowagie.text.Font.BOLD, Color.WHITE);
            String[] headers = {"Código","Descripción","Estado","Fecha"};
            for (String h : headers) {
                PdfPCell cell = new PdfPCell(new Phrase(h, fHeader));
                cell.setHorizontalAlignment(Element.ALIGN_CENTER);
                cell.setBackgroundColor(new Color(47,107,228)); // azul
                cell.setPadding(6f);
                table.addCell(cell);
            }

            // Filas
            com.lowagie.text.Font fCell = new com.lowagie.text.Font(com.lowagie.text.Font.HELVETICA, 10, com.lowagie.text.Font.NORMAL, Color.BLACK);
            int rows = model.getRowCount();
            for (int r = 0; r < rows; r++) {
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

            JOptionPane.showMessageDialog(this, "PDF generado:\n" + out.getAbsolutePath(),
                    "Exportar PDF", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception ex) {
            doc.close();
            JOptionPane.showMessageDialog(this, "No se pudo generar el PDF:\n" + ex.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    /** Exporta la tabla a CSV (UTF-8, con encabezados). */
private void exportToCsv() {
    JFileChooser fc = new JFileChooser();
    fc.setDialogTitle("Guardar informe en CSV");
    fc.setFileFilter(new FileNameExtensionFilter("Archivo CSV (*.csv)", "csv"));
    fc.setSelectedFile(new File("informe-inventario.csv"));
    int opt = fc.showSaveDialog(this);
    if (opt != JFileChooser.APPROVE_OPTION) return;

    File out = fc.getSelectedFile();
    if (!out.getName().toLowerCase().endsWith(".csv")) {
        out = new File(out.getParentFile(), out.getName() + ".csv");
    }

    try (PrintWriter pw = new PrintWriter(out, java.nio.charset.Charset.forName("Windows-1252")))  {
        // Encabezados
        for (int c = 0; c < model.getColumnCount(); c++) {
            if (c > 0) pw.print(";"); // separador coma
            pw.print(csvEscape(model.getColumnName(c)));
        }
        pw.println();

        // Filas
        for (int r = 0; r < model.getRowCount(); r++) {
            for (int c = 0; c < model.getColumnCount(); c++) {
                if (c > 0) pw.print(",");
                Object v = model.getValueAt(r, c);
                pw.print(csvEscape(v == null ? "" : String.valueOf(v)));
            }
            pw.println();
        }
    } catch (Exception ex) {
        JOptionPane.showMessageDialog(this, "No se pudo generar el CSV:\n" + ex.getMessage(),
                "Error", JOptionPane.ERROR_MESSAGE);
        return;
    }

    JOptionPane.showMessageDialog(this, "CSV generado:\n" + out.getAbsolutePath(),
            "Exportar CSV", JOptionPane.INFORMATION_MESSAGE);
}

/** Escapa un valor según CSV (RFC 4180): comillas dobles y valores con coma/nueva línea. */
private static String csvEscape(String s) {
    boolean needQuotes = s.contains(",") || s.contains("\"") || s.contains("\n") || s.contains("\r");
    String x = s.replace("\"", "\"\"");
    return needQuotes ? "\"" + x + "\"" : x;
}


    /** Busca un JLabel "Informes" que esté en la barra superior (no en el menú lateral) */
    private static JLabel findHeaderTitleLabel(Container root) {
        for (Component c : root.getComponents()) {
            if (c instanceof JLabel l && "Informes".equals(l.getText())) {
                Point p = SwingUtilities.convertPoint(l.getParent(), l.getLocation(), root);
                if (p.y < 120) { // está arriba (header)
                    return l;
                }
            }
            if (c instanceof Container ct) {
                JLabel r = findHeaderTitleLabel(ct);
                if (r != null) return r;
            }
        }
        return null;
    }
}
