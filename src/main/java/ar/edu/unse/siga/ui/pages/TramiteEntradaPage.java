package ar.edu.unse.siga.ui.pages;

import ar.edu.unse.siga.domain.Tramite;
import ar.edu.unse.siga.service.TramiteService;
import ar.edu.unse.siga.ui.base.CardPanel;
import ar.edu.unse.siga.ui.base.Ui;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import java.awt.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

public class TramiteEntradaPage extends JPanel {

    private final TramiteService service;

    // Control para evitar bucle de eventos en edición de estado
    private boolean updatingEstado = false;

    // Mapea cada fila visible a su Tramite real (para usar el ID al actualizar)
    private final java.util.List<Tramite> currentRows = new java.util.ArrayList<>();

    // --- Campos de Registro ---
    private final JTextField txtAsunto = new JTextField(25);
    private final JTextField txtRemitente = new JTextField(25);
    private final JTextArea txtDescripcion = new JTextArea(4, 25);
    private final JTextField txtDestino = new JTextField(25);
    private final JTextField txtDestinatario = new JTextField(25);
    private final JLabel lblNumero = new JLabel();

    // --- Filtros de la tabla ---
    private final JTextField filterSearch = new JTextField(18);
    private final JComboBox<String> filterEstado = new JComboBox<>(
            new String[]{"Todos", "Completado", "En proceso", "Pendiente", "Alta"}
    );

    private final CardLayout cardLayout = new CardLayout();
    private final JPanel cards = new JPanel(cardLayout);

    // Solo la columna "Estado" (índice 5) será editable
    private final JTable table = new JTable(new DefaultTableModel(
            new Object[][]{},
            new String[]{"ID Trámite", "Asunto", "Fecha actualización", "Última actualización", "Descripcion", "Estado"}
    )) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return column == 5;
        }
    };

    public TramiteEntradaPage(TramiteService service) {
        this.service = service;
        setOpaque(false);
        setLayout(new BorderLayout(0, 20));

        add(buildHeader(), BorderLayout.NORTH);
        add(buildContent(), BorderLayout.CENTER);

        lblNumero.setText(generateNumero(LocalDate.now()));
        cardLayout.show(cards, "registro");

        configureTable();
        installFilters();
        installEstadoEditor();    // combo editable en "Estado"
        installEstadoListener();  // persistencia en BD al cambiar
    }

    // ==== Estado editable (editor + listener) ====

    private void installEstadoEditor() {
        // Asegura que las columnas estén creadas antes de setear el editor
        SwingUtilities.invokeLater(() -> {
            try {
                if (table.getColumnCount() > 5) {
                    String[] estados = {"PENDIENTE", "EN PROCESO", "COMPLETADO"};
                    JComboBox<String> comboEstado = new JComboBox<>(estados);
                    table.getColumnModel().getColumn(5).setCellEditor(new DefaultCellEditor(comboEstado));
                    if (table.getRowHeight() < 28) table.setRowHeight(28);
                }
            } catch (Exception ex) {
                Ui.error(this, ex);
            }
        });
    }

    private void installEstadoListener() {
        table.getModel().addTableModelListener(e -> {
            if (updatingEstado) return;
            int col = e.getColumn();
            int row = e.getFirstRow();
            if (row >= 0 && col == 5) { // columna ESTADO
                try {
                    String elegido = String.valueOf(table.getValueAt(row, col)).trim().toUpperCase(Locale.ROOT);
                    String canon = switch (elegido) {
                        case "EN PROCESO" -> "EN_PROCESO";
                        case "COMPLETADO" -> "COMPLETADO";
                        case "PENDIENTE"  -> "PENDIENTE";
                        default -> elegido;
                    };

                    // Usamos el ID real del trámite de esa fila
                    Tramite t = currentRows.get(row);
                    service.actualizarEstado(t.getId(), canon);

                    // Actualizamos objeto y celda visible
                    t.setEstado(canon);
                    String friendly = estadoFriendly(canon);
                    updatingEstado = true;
                    table.setValueAt(friendly, row, 5);
                } catch (Exception ex) {
                    Ui.error(this, ex);
                } finally {
                    updatingEstado = false;
                }
            }
        });
    }

    // ==== Header / Tabs ====

    private JComponent buildHeader() {
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);

        JLabel title = new JLabel("Trámites");
        title.setFont(new Font("Segoe UI", Font.BOLD, 32));
        title.setForeground(new Color(35, 55, 120));
        header.add(title, BorderLayout.WEST);

        ButtonGroup tabs = new ButtonGroup();
        JToggleButton btnRegistrar = tabButton("Registrar nuevo trámite");
        JToggleButton btnActivos = tabButton("Trámites activos");
        tabs.add(btnRegistrar);
        tabs.add(btnActivos);
        btnRegistrar.setSelected(true);

        JPanel switches = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 0));
        switches.setOpaque(false);
        switches.add(btnRegistrar);
        switches.add(btnActivos);
        header.add(switches, BorderLayout.EAST);

        btnRegistrar.addActionListener(e -> cardLayout.show(cards, "registro"));
        btnActivos.addActionListener(e -> {
            loadTableData();
            cardLayout.show(cards, "activos");
        });

        return header;
    }

    private JComponent buildContent() {
        cards.setOpaque(false);
        cards.add(buildRegistroCard(), "registro");
        cards.add(buildActivosCard(), "activos");
        return cards;
    }

    // ==== Registro ====

    private CardPanel buildRegistroCard() {
        CardPanel card = new CardPanel();
        card.setLayout(new BorderLayout(24, 24));

        JPanel columns = new JPanel(new GridBagLayout());
        columns.setOpaque(false);

        GridBagConstraints gc = new GridBagConstraints();
        gc.gridx = 0;
        gc.gridy = 0;
        gc.weightx = 0.6;
        gc.fill = GridBagConstraints.BOTH;
        gc.insets = new Insets(0, 0, 0, 18);
        columns.add(buildFormPanel(), gc);

        gc.gridx = 1;
        gc.weightx = 0.4;
        gc.insets = new Insets(0, 18, 0, 0);
        columns.add(buildSidebarPanel(), gc);

        card.add(columns, BorderLayout.CENTER);
        return card;
    }

    private CardPanel buildFormPanel() {
        CardPanel card = new CardPanel();
        card.setLayout(new BorderLayout(18, 18));

        JLabel title = new JLabel("Detalle del trámite");
        title.setFont(new Font("Segoe UI", Font.BOLD, 16));
        title.setForeground(new Color(54, 92, 190));
        card.add(title, BorderLayout.NORTH);

        JPanel form = new JPanel();
        form.setOpaque(false);
        form.setLayout(new BoxLayout(form, BoxLayout.Y_AXIS));

        form.add(infoLabel("Número generado", lblNumero));
        form.add(Box.createVerticalStrut(16));
        form.add(field("Asunto", txtAsunto));
        form.add(Box.createVerticalStrut(14));
        form.add(field("Remitente", txtRemitente));
        form.add(Box.createVerticalStrut(14));
        txtDescripcion.setLineWrap(true);
        txtDescripcion.setWrapStyleWord(true);
        form.add(textAreaField("Descripción", txtDescripcion));
        form.add(Box.createVerticalStrut(14));
        form.add(field("Destino", txtDestino));
        form.add(Box.createVerticalStrut(14));
        //form.add(field("Destinatario", txtDestinatario));

        JButton btn = primaryButton("Aceptar");
        btn.addActionListener(e -> onSave());
        btn.setAlignmentX(Component.LEFT_ALIGNMENT);
        form.add(Box.createVerticalStrut(22));
        form.add(btn);

        card.add(form, BorderLayout.CENTER);
        return card;
    }

    private JComponent buildSidebarPanel() {
        JPanel grid = new JPanel(new GridLayout(2, 1, 18, 18));
        grid.setOpaque(false);
        grid.add(recentTramitesCard());
        grid.add(oldTramitesCard());
        return grid;
    }

    private CardPanel recentTramitesCard() {
        CardPanel card = new CardPanel();
        card.setLayout(new BorderLayout(0, 12));
        card.add(sideTitle("Trámites recientes"), BorderLayout.NORTH);

        JPanel list = new JPanel();
        list.setOpaque(false);
        list.setLayout(new BoxLayout(list, BoxLayout.Y_AXIS));
        list.add(recentItem("Asunto: Presupuesto 2026", "Estado: COMPLETADO", "Completo", new Color(73, 198, 154)));
        list.add(recentItem("Asunto: Pedido resma A4", "Estado: EN PROCESO", "En proceso", new Color(86, 127, 255)));
        list.add(recentItem("Asunto: Solicitud equipamiento", "Estado: PENDIENTE", "Pendiente", new Color(255, 170, 70)));

        JButton link = subtleLink("Ver historial completo");
        list.add(Box.createVerticalStrut(12));
        list.add(link);

        card.add(list, BorderLayout.CENTER);
        return card;
    }

    private CardPanel oldTramitesCard() {
        CardPanel card = new CardPanel();
        card.setLayout(new BorderLayout(0, 12));
        card.add(sideTitle("Trámites más antiguos"), BorderLayout.NORTH);

        JPanel list = new JPanel();
        list.setOpaque(false);
        list.setLayout(new BoxLayout(list, BoxLayout.Y_AXIS));
        list.add(twoLineItem("Asunto: Compra equipos Lab.", "Fecha: 23/09/2024"));
        list.add(twoLineItem("Asunto: Presupuesto Decanato", "Fecha: 12/01/2025"));
        list.add(twoLineItem("Asunto: Renovación licencias", "Fecha: 08/02/2025"));

        JButton link = subtleLink("Ver historial completo");
        list.add(Box.createVerticalStrut(12));
        list.add(link);

        card.add(list, BorderLayout.CENTER);
        return card;
    }

    // ==== Activos (tabla) ====

    private CardPanel buildActivosCard() {
        CardPanel card = new CardPanel();
        card.setLayout(new BorderLayout(20, 24));
        card.add(buildFilters(), BorderLayout.NORTH);
        card.add(buildTableScroll(), BorderLayout.CENTER);
        return card;
    }

    // Filtros sin categoría
    private Component buildFilters() {
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

    private JScrollPane buildTableScroll() {
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

        // Descripción sin renderer de color
        // Estado mantiene badge de colores
        table.getColumnModel().getColumn(5).setCellRenderer(new BadgeRenderer(BadgeRenderer.Type.STATUS));

        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.getViewport().setBackground(Color.WHITE);
        return scroll;
    }

    private void configureTable() { loadTableData(); }

    private void installFilters() {
        filterSearch.getDocument().addDocumentListener(new SimpleDocumentListener(this::loadTableData));
        filterEstado.addActionListener(e -> loadTableData());
    }

    private String generateNumero(LocalDate date) {
        return date.format(DateTimeFormatter.BASIC_ISO_DATE) + "-" + (int) (Math.random() * 90000 + 10000);
    }

    private void onSave() {
        try {
            String nro = lblNumero.getText() != null ? lblNumero.getText().trim() : null;
            String asunto = txtAsunto.getText() != null ? txtAsunto.getText().trim() : "";
            String solicitante = txtRemitente.getText() != null ? txtRemitente.getText().trim() : "";
            String descripcion = txtDescripcion.getText() != null ? txtDescripcion.getText().trim() : null;

            if (asunto.isEmpty()) throw new IllegalArgumentException("El asunto es obligatorio");

            service.registrarTramite(nro, asunto, solicitante,
                    (descripcion != null && !descripcion.isBlank()) ? descripcion : null);

            Ui.info(this, "Trámite registrado correctamente.");

            // Reset de campos
            lblNumero.setText(generateNumero(LocalDate.now()));
            txtAsunto.setText("");
            txtRemitente.setText("");
            txtDescripcion.setText("");
            txtDestino.setText("");
            //txtDestinatario.setText("");

            // refrescar
            loadTableData();
        } catch (Exception e) {
            Ui.error(this, e);
        }
    }

    private void loadTableData() {
        DefaultTableModel model = (DefaultTableModel) table.getModel();
        model.setRowCount(0);
        currentRows.clear(); // MUY IMPORTANTE: mantener buffer fila->Tramite

        try {
            String search = filterSearch.getText().trim().toLowerCase(Locale.ROOT);
            String estadoFiltro = (String) filterEstado.getSelectedItem();

            // Mostrar todos; el combo de estado filtra
            List<Tramite> tramites = service.listarTodos();

            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

            for (Tramite t : tramites) {
                // búsqueda por nro + asunto + descripción
                if (!search.isEmpty()) {
                    String texto = (t.getAsunto() + " " + t.getNro() + " " +
                            (t.getDescripcion() != null ? t.getDescripcion() : ""))
                            .toLowerCase(Locale.ROOT);
                    if (!texto.contains(search)) continue;
                }

                // filtro por estado
                if (!"Todos".equalsIgnoreCase(estadoFiltro) && !estadoMatches(t.getEstado(), estadoFiltro)) {
                    continue;
                }

                String fecha = t.getFecha() != null ? t.getFecha().format(fmt) : "-";
                String ultima = t.getFecha() != null ? t.getFecha().plusDays(1).format(fmt) : "-"; // si tenés campo real, usalo acá
                String descripcion = (t.getDescripcion() == null || t.getDescripcion().isBlank()) ? "-" : t.getDescripcion();
                String estado = estadoFriendly(t.getEstado());

                model.addRow(new Object[]{ t.getNro(), t.getAsunto(), fecha, ultima, descripcion, estado });

                // mantener alineado el objeto real con la fila visible
                currentRows.add(t);
            }
        } catch (Exception ex) {
            Ui.error(this, ex);
        }
    }

    private boolean estadoMatches(String estado, String filtro) {
        if (estado == null) return false;
        String normalized = estadoFriendly(estado).toLowerCase(Locale.ROOT);
        return normalized.equals(filtro.toLowerCase(Locale.ROOT));
    }

    private String prioridadDesdeEstado(String estado) {
        if (estado == null) return "Media";
        return switch (estado.toUpperCase(Locale.ROOT)) {
            case "CERRADO", "COMPLETADO" -> "Baja";
            case "EN_PROCESO" -> "Media";
            case "ALTA" -> "Alta";
            default -> "Alta";
        };
    }

    private String estadoFriendly(String estado) {
        if (estado == null) return "Pendiente";
        return switch (estado.toUpperCase(Locale.ROOT)) {
            case "COMPLETADO", "CERRADO" -> "Completado";
            case "EN_PROCESO" -> "En proceso";
            case "ALTA" -> "Alta";
            default -> capitalize(estado);
        };
    }

    private String capitalize(String text) {
        if (text == null || text.isEmpty()) return "";
        return text.substring(0, 1).toUpperCase(Locale.ROOT) + text.substring(1).toLowerCase(Locale.ROOT);
    }

    // ==== UI Helpers ====

    private JPanel field(String label, JComponent component) {
        JPanel p = new JPanel();
        p.setOpaque(false);
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        JLabel lbl = new JLabel(label.toUpperCase(Locale.ROOT));
        lbl.setAlignmentX(Component.LEFT_ALIGNMENT);
        lbl.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lbl.setForeground(new Color(87, 110, 178));
        component.setAlignmentX(Component.LEFT_ALIGNMENT);
        styleInput(component);
        p.add(lbl);
        p.add(Box.createVerticalStrut(6));
        p.add(component);
        return p;
    }

    private JPanel textAreaField(String label, JTextArea area) {
        JPanel p = new JPanel();
        p.setOpaque(false);
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        JLabel lbl = new JLabel(label.toUpperCase(Locale.ROOT));
        lbl.setAlignmentX(Component.LEFT_ALIGNMENT);
        lbl.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lbl.setForeground(new Color(87, 110, 178));
        area.setAlignmentX(Component.LEFT_ALIGNMENT);
        area.setBorder(new EmptyBorder(8, 12, 8, 12));
        area.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        JScrollPane scroll = new JScrollPane(area);
        scroll.setBorder(BorderFactory.createLineBorder(new Color(211, 220, 249)));
        scroll.setAlignmentX(Component.LEFT_ALIGNMENT);
        p.add(lbl);
        p.add(Box.createVerticalStrut(6));
        p.add(scroll);
        return p;
    }

    private JPanel infoLabel(String title, JLabel value) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setOpaque(false);
        JLabel lblTitle = new JLabel(title.toUpperCase(Locale.ROOT));
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lblTitle.setForeground(new Color(87, 110, 178));
        panel.add(lblTitle, BorderLayout.NORTH);
        value.setFont(new Font("Segoe UI", Font.BOLD, 20));
        value.setForeground(new Color(35, 63, 150));
        panel.add(value, BorderLayout.CENTER);
        return panel;
    }

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

    private Component twoLineItem(String title, String subtitle) {
        JPanel row = new JPanel();
        row.setOpaque(false);
        row.setLayout(new BoxLayout(row, BoxLayout.Y_AXIS));
        JLabel lblTitle = new JLabel(title);
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 13));
        lblTitle.setForeground(new Color(43, 57, 120));
        JLabel lblSubtitle = new JLabel(subtitle);
        lblSubtitle.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lblSubtitle.setForeground(new Color(118, 133, 182));
        row.add(lblTitle);
        row.add(Box.createVerticalStrut(4));
        row.add(lblSubtitle);
        row.setBorder(new EmptyBorder(4, 0, 4, 0));
        return row;
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

    private void styleInput(JComponent component) {
        component.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(211, 220, 249)),
                new EmptyBorder(10, 14, 10, 14)));
        component.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        component.setBackground(Color.WHITE);
        component.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
    }

    private void styleFilterField(JComponent component, int width) {
        component.putClientProperty("JComponent.sizeVariant", "regular");
        component.setPreferredSize(new Dimension(width, 40));
        component.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        if (component instanceof JTextField textField) {
            textField.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(new Color(208, 218, 245)),
                    new EmptyBorder(8, 12, 8, 12)));
            textField.setBackground(Color.WHITE);
        } else if (component instanceof JComboBox<?>) {
            component.setBorder(BorderFactory.createLineBorder(new Color(208, 218, 245)));
            component.setBackground(Color.WHITE);
        }
    }

    private JButton primaryButton(String text) {
        JButton b = new JButton(text);
        b.setBackground(new Color(58, 96, 224));
        b.setForeground(Color.WHITE);
        b.setFocusPainted(false);
        b.setBorder(BorderFactory.createEmptyBorder(12, 26, 12, 26));
        b.putClientProperty("JButton.buttonType", "roundRect");
        return b;
    }

    private JButton outlineButton(String text) {
        JButton b = new JButton(text);
        b.setFocusPainted(false);
        b.setFont(new Font("Segoe UI", Font.BOLD, 13));
        b.setForeground(new Color(58, 96, 224));
        b.setBackground(Color.WHITE);
        b.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(180, 200, 255)),
                new EmptyBorder(10, 18, 10, 18)));
        b.putClientProperty("JButton.buttonType", "roundRect");
        return b;
    }

    private JToggleButton tabButton(String text) {
        JToggleButton t = new JToggleButton(text.toUpperCase(Locale.ROOT));
        t.setFocusPainted(false);
        t.setFont(new Font("Segoe UI", Font.BOLD, 12));
        t.setBackground(new Color(235, 240, 255));
        t.setForeground(new Color(66, 98, 190));
        t.setBorder(BorderFactory.createEmptyBorder(12, 24, 12, 24));
        t.putClientProperty("JButton.buttonType", "roundRect");
        t.addChangeListener(e -> {
            if (t.isSelected()) {
                t.setBackground(new Color(58, 96, 224));
                t.setForeground(Color.WHITE);
            } else {
                t.setBackground(new Color(235, 240, 255));
                t.setForeground(new Color(66, 98, 190));
            }
        });
        return t;
    }

    private static class SimpleDocumentListener implements DocumentListener {
        private final Runnable onChange;
        private SimpleDocumentListener(Runnable onChange) { this.onChange = onChange; }
        @Override public void insertUpdate(DocumentEvent e) { onChange.run(); }
        @Override public void removeUpdate(DocumentEvent e) { onChange.run(); }
        @Override public void changedUpdate(DocumentEvent e) { onChange.run(); }
    }

    private static class TableHeaderRenderer extends DefaultTableCellRenderer {
        TableHeaderRenderer() {
            setHorizontalAlignment(LEFT);
            setFont(new Font("Segoe UI", Font.BOLD, 13));
            setForeground(new Color(70, 88, 140));
            setBackground(new Color(238, 242, 255));
            setBorder(new EmptyBorder(12, 14, 12, 14));
        }
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                                                       boolean hasFocus, int row, int column) {
            super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            setText(value == null ? "" : value.toString().toUpperCase(Locale.ROOT));
            return this;
        }
    }

    private static class BadgeRenderer extends DefaultTableCellRenderer {
        enum Type { PRIORITY, STATUS }
        private final Type type;
        BadgeRenderer(Type type) {
            this.type = type;
            setHorizontalAlignment(CENTER);
            setFont(new Font("Segoe UI", Font.BOLD, 12));
        }
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                                                       boolean hasFocus, int row, int column) {
            super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            String text = value == null ? "-" : value.toString();
            setText(text);
            setOpaque(true);
            setBorder(new EmptyBorder(6, 12, 6, 12));

            Color base;
            String normalized = text.toLowerCase(Locale.ROOT);
            if (type == Type.PRIORITY) {
                if (normalized.contains("alta"))       base = new Color(255, 120, 102);
                else if (normalized.contains("media")) base = new Color(255, 188, 75);
                else                                    base = new Color(200, 210, 230);
            } else {
                if (normalized.contains("complet"))     base = new Color(73, 198, 154);
                else if (normalized.contains("proceso"))base = new Color(86, 127, 255);
                else if (normalized.contains("alta"))   base = new Color(255, 120, 102);
                else                                     base = new Color(255, 188, 75);
            }

            if (isSelected) {
                setForeground(Color.WHITE);
                setBackground(base.darker());
            } else {
                setForeground(Color.WHITE);
                setBackground(base);
            }
            return this;
        }
    }
}
