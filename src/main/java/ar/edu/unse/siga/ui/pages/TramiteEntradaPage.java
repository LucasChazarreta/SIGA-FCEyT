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
import javax.swing.table.TableColumn;
import javax.swing.event.TableModelEvent;

public class TramiteEntradaPage extends JPanel {

    private final TramiteService service;

    private final JTextField txtAsunto = new JTextField(25);
    private final JTextField txtRemitente = new JTextField(25);
    private final JTextArea txtDescripcion = new JTextArea();
    private final JTextField txtDestino = new JTextField(25);
    private final JLabel lblNumero = new JLabel();
    // listas laterales que vamos a rellenar/refrescar
    private JPanel recentList;
    private JPanel oldList;


    private final JTextField filterSearch = new JTextField(18);
    
    private final JComboBox<String> filterEstado = new JComboBox<>(
            new String[]{"Completado", "En proceso", "Pendiente", }
            );
    
    private final CardLayout cardLayout = new CardLayout();
    private final JPanel cards = new JPanel(cardLayout);

    private final JTable table = new JTable(new DefaultTableModel(
        new Object[][]{},
        new String[]{"ID Trámite", "Asunto", "Fecha actualización", "Última actualización", "Descripción", "Estado", "ID_DB"}
    ) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return column == 5; // Solo "Estado"
        }
    }) {
        @Override
        public boolean isCellEditable(int row, int column) {
            // Delegar en el modelo para claridad
            return ((DefaultTableModel) getModel()).isCellEditable(row, column);
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
    }

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

        JPanel form = new JPanel();
        form.setOpaque(false);
        form.setLayout(new BoxLayout(form, BoxLayout.Y_AXIS));
        
        // AQUI ES DONDE ESTAN LOS DETALLES DEL TRAMITE 
        form.add(infoLabel("Número generado", lblNumero));
        form.add(Box.createVerticalStrut(14));
        form.add(field("Asunto", txtAsunto));
        form.add(Box.createVerticalStrut(12));
        form.add(field("Remitente", txtRemitente));
        form.add(Box.createVerticalStrut(12));
        txtDescripcion.setLineWrap(true);
        txtDescripcion.setWrapStyleWord(true);
        form.add(textAreaField("Descripción", txtDescripcion));
        form.add(Box.createVerticalStrut(14));
        form.add(field("Destino", txtDestino));
        form.add(Box.createVerticalStrut(14));
        JButton btn = primaryButton("Aceptar");
        btn.addActionListener(e -> onSave());
        btn.setAlignmentX(Component.LEFT_ALIGNMENT);
        form.add(Box.createVerticalStrut(22));
        form.add(btn);
        
        JPanel column = new JPanel();
        column.setOpaque(false);
        column.setLayout(new BoxLayout(column, BoxLayout.Y_AXIS));
        column.setAlignmentX(Component.LEFT_ALIGNMENT);
        title.setAlignmentX(Component.LEFT_ALIGNMENT);
        form.setAlignmentX(Component.LEFT_ALIGNMENT);
        column.add(title);
        column.add(Box.createVerticalStrut(12));
        column.add(form);
        
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

        recentList = new JPanel();
        recentList.setOpaque(false);
        recentList.setLayout(new BoxLayout(recentList, BoxLayout.Y_AXIS));

        card.add(scrollList(recentList, 260), BorderLayout.CENTER);

        // llenar la lista desde la BD
        refreshRecientes();

        return card;
    }
    
    private void refreshRecientes() {
        if (recentList == null) return;
        recentList.removeAll();
        try {
            var tramites = service.listarTodos();
            tramites.stream()
                    .sorted((a,b) -> {
                        var fa = a.getFecha(); var fb = b.getFecha();
                        if (fa == null && fb == null) return 0;
                        if (fa == null) return 1;
                        if (fb == null) return -1;
                        return fb.compareTo(fa); // más nuevos primero
                    })
                    .limit(20)
                    .forEach(t -> {
                        String titulo = "Asunto: " + (t.getAsunto()==null? "-" : t.getAsunto());
                        String subt   = "Estado: " + (t.getEstado()==null? "-" : t.getEstado());
                        String badge  = estadoFriendly(t.getEstado());
                        java.awt.Color c = new java.awt.Color(200,210,230);
                        String e = badge.toLowerCase(java.util.Locale.ROOT);
                        if (e.contains("complet")) c = new java.awt.Color(73,198,154);
                        else if (e.contains("proceso")) c = new java.awt.Color(86,127,255);
                        else if (e.contains("pend")) c = new java.awt.Color(255,170,70);
                        recentList.add(recentItem(titulo, subt, badge, c));
                        recentList.add(Box.createVerticalStrut(4));
                    });
        } catch (Exception ex) {
            Ui.error(this, ex);
        }
        recentList.revalidate();
        recentList.repaint();
    }

    private void refreshAntiguos() {
        if (oldList == null) return;
        oldList.removeAll();
        try {
            var fmt = java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy");
            var tramites = service.listarTodos();
            tramites.stream()
                    .sorted(java.util.Comparator.comparing(
                            ar.edu.unse.siga.domain.Tramite::getFecha,
                            java.util.Comparator.nullsLast(java.util.Comparator.naturalOrder())
                    ))
                    .limit(50)
                    .forEach(t -> {
                        String asunto = (t.getAsunto()==null || t.getAsunto().isBlank()) ? "-" : t.getAsunto();
                        String fecha  = (t.getFecha()==null) ? "-" : t.getFecha().format(fmt);
                        oldList.add(twoLineItem("Asunto: " + asunto, "Fecha: " + fecha));
                        oldList.add(Box.createVerticalStrut(4));
                    });
        } catch (Exception ex) {
            Ui.error(this, ex);
        }
        oldList.revalidate();
        oldList.repaint();
    }


//    private CardPanel recentTramitesCard() {
//        CardPanel card = new CardPanel();
//        card.setLayout(new BorderLayout(0, 12));
//        card.add(sideTitle("Trámites recientes"), BorderLayout.NORTH);
//
//        JPanel list = new JPanel();
//        list.setOpaque(false);
//        list.setLayout(new BoxLayout(list, BoxLayout.Y_AXIS));
//
//        //muestra los ultimos tramites cargado
//        list.removeAll();
//        try {
//            var tramites = service.listarTodos(); // trae todos
//            tramites.stream()
//                    .sorted((a,b) -> {
//                        var fa = a.getFecha(); var fb = b.getFecha();
//                        if (fa == null && fb == null) return 0;
//                        if (fa == null) return 1;
//                        if (fb == null) return -1;
//                        return fb.compareTo(fa); // orden descendente
//                    })
//                    .limit(20) // cuántos mostrar con scroll
//                    .forEach(t -> {
//                        String titulo = "Asunto: " + (t.getAsunto() == null ? "-" : t.getAsunto());
//                        String subt   = "Estado: " + (t.getEstado() == null ? "-" : t.getEstado());
//                        Color c = new Color(200, 210, 230);
//                        String e = (t.getEstado() == null ? "" : t.getEstado().toLowerCase());
//                        if (e.contains("complet")) c = new Color(73, 198, 154);
//                        else if (e.contains("proceso")) c = new Color(86, 127, 255);
//                        else if (e.contains("pend")) c = new Color(255, 170, 70);
//                        list.add(recentItem(titulo, subt, t.getEstado(), c));
//                    });
//            list.revalidate();
//            list.repaint();
//        } catch (Exception ex) {
//            Ui.error(this, ex);
//        }
//
//
//        // >>> Envolver la lista en un scroll y NO agregar más link
//        card.add(scrollList(list, 260), BorderLayout.CENTER);
//        return card;
//    }


    // Reemplazá TODO el método por este:
//    private CardPanel oldTramitesCard() {
//        CardPanel card = new CardPanel();
//        card.setLayout(new BorderLayout(0, 12));
//        card.add(sideTitle("Trámites más antiguos"), BorderLayout.NORTH);
//
//        JPanel list = new JPanel();
//        list.setOpaque(false);
//        list.setLayout(new BoxLayout(list, BoxLayout.Y_AXIS));
//
//        // --- Carga dinámica desde BD: más antiguos primero
//        list.removeAll();
//        try {
//            var fmt = java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy");
//            var tramites = service.listarTodos(); // Ideal: que venga ya ordenado desde el DAO
//
//            tramites.stream()
//                    .sorted(java.util.Comparator.comparing(
//                            ar.edu.unse.siga.domain.Tramite::getFecha,
//                            java.util.Comparator.nullsLast(java.util.Comparator.naturalOrder())
//                    )) // ascendente (más viejo → más nuevo)
//                    .limit(50) // mostrás hasta 50; el scroll permite verlos
//                    .forEach(t -> {
//                        String asunto = (t.getAsunto() == null || t.getAsunto().isBlank())
//                                ? "-" : t.getAsunto();
//                        String fechaTxt = (t.getFecha() == null)
//                                ? "-" : t.getFecha().format(fmt);
//                        list.add(twoLineItem("Asunto: " + asunto, "Fecha: " + fechaTxt));
//                    });
//
//            list.revalidate();
//            list.repaint();
//        } catch (Exception ex) {
//            Ui.error(this, ex);
//        }
//
//        // Scroll: si hay más ítems que el alto, aparece la barra
//        card.add(scrollList(list, 220), BorderLayout.CENTER);
//        return card;
//    }
    
    private CardPanel oldTramitesCard() {
        CardPanel card = new CardPanel();
        card.setLayout(new BorderLayout(0, 12));
        card.add(sideTitle("Trámites más antiguos"), BorderLayout.NORTH);

        oldList = new JPanel();
        oldList.setOpaque(false);
        oldList.setLayout(new BoxLayout(oldList, BoxLayout.Y_AXIS));

        card.add(scrollList(oldList, 220), BorderLayout.CENTER);

        // llenar la lista desde la BD
        refreshAntiguos();

        return card;
    }



    private CardPanel buildActivosCard() {
        CardPanel card = new CardPanel();
        card.setLayout(new BorderLayout(20, 24));

        card.add(buildFilters(), BorderLayout.NORTH);
        card.add(buildTableScroll(), BorderLayout.CENTER);

        return card;
    }

    private Component buildFilters() {
        JPanel panel = new JPanel(new BorderLayout(18, 0));
        panel.setOpaque(false);

        JLabel lbl = new JLabel("Filtro");
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

        // Editar la celda de Estados con opciones 
        JComboBox<String> estadoEditor = new JComboBox<>(new String[]{
                "Completado", "En proceso", "Pendiente"
        });
        table.getColumnModel().getColumn(5).setCellEditor(new DefaultCellEditor(estadoEditor));

        
        JTableHeader header = table.getTableHeader();
        header.setPreferredSize(new Dimension(header.getPreferredSize().width, 46));
        header.setDefaultRenderer(new TableHeaderRenderer());

        //  ----------- AQUI ESTAN LOS COLORES DE LA TABLA DE TRAMITES ACTIVOS----------
        table.getColumnModel().getColumn(5).setCellRenderer(new BadgeRenderer(BadgeRenderer.Type.STATUS));

        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.getViewport().setBackground(Color.WHITE);
        return scroll;
    }

    private void configureTable() {
        loadTableData();
        // === Editor para columna Estado ===
        JComboBox<String> estadoEditor = new JComboBox<>(new String[]{
                "Completado", "En proceso", "Pendiente", "Nuevo"
        });
        table.getColumnModel().getColumn(5).setCellEditor(new DefaultCellEditor(estadoEditor));
        // === Ocultar la columna ID_DB (si la agregaste) ===
        TableColumn hiddenIdCol = table.getColumnModel().getColumn(6);
        hiddenIdCol.setMinWidth(0);
        hiddenIdCol.setMaxWidth(0);
        hiddenIdCol.setPreferredWidth(0);
        // === Listener para guardar cambios de estado ===
        DefaultTableModel m = (DefaultTableModel) table.getModel();
        m.addTableModelListener(e -> {
            if (e.getType() != TableModelEvent.UPDATE) return;
            int row = e.getFirstRow();
            int col = e.getColumn();
            if (row < 0 || col != 5) return; // solo si cambió "Estado"
            Object idObj = m.getValueAt(row, 6);       // ID_DB oculto
            Object estadoObj = m.getValueAt(row, 5);   // Estado visible
            if (idObj == null || estadoObj == null) return;
            try {
                Long id = (idObj instanceof Long) ? (Long) idObj : Long.valueOf(idObj.toString());
                String nuevoEstado = estadoObj.toString();
                service.cambiarEstado(id, nuevoEstado);   // usa tu método existente
                table.repaint();
            } catch (Exception ex) {
                Ui.error(this, ex);
            }
        });

    }

    private void installFilters() {
        filterSearch.getDocument().addDocumentListener(new SimpleDocumentListener(this::loadTableData));
        filterEstado.addActionListener(e -> loadTableData());
    }

    private String generateNumero(LocalDate date) {
        return date.format(DateTimeFormatter.BASIC_ISO_DATE) + "-" + (int) (Math.random() * 90000 + 10000);
    }

    private void onSave() {
        try {
            String nro = lblNumero.getText();
            String asunto = txtAsunto.getText().trim();
            String solicitante = txtRemitente.getText().trim();
            String descripcion = txtDescripcion.getText().trim();
            String destino = txtDestino.getText().trim();
            
            if (asunto.isEmpty()) {
                throw new IllegalArgumentException("El asunto es obligatorio");
            }

            service.registrarTramite(nro, asunto, solicitante, descripcion, destino);

            Ui.info(this, "Trámite registrado correctamente.");
            lblNumero.setText(generateNumero(LocalDate.now()));
            txtAsunto.setText("");
            txtRemitente.setText("");
            txtDescripcion.setText("");
            txtDestino.setText("");
            loadTableData();
            refreshRecientes();
            refreshAntiguos();
        } catch (Exception e) {
            Ui.error(this, e);
        }
    }

    private void loadTableData() {
        DefaultTableModel model = (DefaultTableModel) table.getModel();
        model.setRowCount(0);
        try {
            String search = filterSearch.getText().trim().toLowerCase(Locale.ROOT);
            String estadoFiltro = (String) filterEstado.getSelectedItem();

            List<Tramite> tramites = service.listarTodos();
            for (Tramite t : tramites) {
                if (!search.isEmpty()) {
                    String texto = (t.getAsunto() + " " + t.getNro()).toLowerCase(Locale.ROOT);
                    if (!texto.contains(search)) {
                        continue;
                    }
                    if (!"Estado".equals(estadoFiltro)&& !estadoFriendly(t.getEstado()).equalsIgnoreCase(estadoFiltro)) {
                        continue;
}

                }
                
                LocalDateTime fecha = t.getFecha();
                String actualizacion = fecha != null
                        ? fecha.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
                        : "-";
                String ultima = fecha != null
                        ? fecha.minusDays(1).format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
                        : "-";

                String descripcion = (t.getDescripcion() == null || t.getDescripcion().isBlank()) ? "-" : t.getDescripcion();
                String estado = estadoFriendly(t.getEstado());

                model.addRow(new Object[]{
                t.getNro(),
                t.getAsunto(),
                actualizacion,
                ultima,
                descripcion,
                estado,
                t.getId() // esta es la nueva columna oculta
                });

            }
        } catch (Exception ex) {
            Ui.error(this, ex);
        }
    }

    private boolean matchesCategoria(Tramite t, String categoria) {
        if (t.getAsunto() == null) {
            return false;
        }
        String asunto = t.getAsunto().toLowerCase(Locale.ROOT);
        return switch (categoria) {
            case "Matrículas" -> asunto.contains("matric");
            case "Ingresos" -> asunto.contains("ingres");
            case "Suministros" -> asunto.contains("suminis") || asunto.contains("pedido");
            case "Pagos" -> asunto.contains("pago") || asunto.contains("orden");
            default -> true;
        };
    }

    private boolean estadoMatches(String estado, String filtro) {
        if (estado == null) {
            return false;
        }
        String normalized = estadoFriendly(estado).toLowerCase(Locale.ROOT);
        return normalized.equals(filtro.toLowerCase(Locale.ROOT));
    }

    private String prioridadDesdeEstado(String estado) {
        if (estado == null) {
            return "Media";
        }
        return switch (estado.toUpperCase(Locale.ROOT)) {
            case "CERRADO", "COMPLETADO" -> "Baja";
            case "EN_PROCESO" -> "Media";
            case "ALTA" -> "Alta";
            default -> "Alta";
        };
    }

    private String estadoFriendly(String estado) {
        if (estado == null) {
            return "Pendiente";
        }
        return switch (estado.toUpperCase(Locale.ROOT)) {
            case "COMPLETADO", "CERRADO" -> "Completado";
            case "EN_PROCESO" -> "En proceso";
            
            default -> capitalize(estado);
        };
    }

    private String capitalize(String text) {
        if (text == null || text.isEmpty()) {
            return "";
        }
        return text.substring(0, 1).toUpperCase(Locale.ROOT) + text.substring(1).toLowerCase(Locale.ROOT);
    }

    private JPanel field(String label, JComponent component) {
        JPanel p = new JPanel();
        p.setOpaque(false);
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setAlignmentX(Component.LEFT_ALIGNMENT);
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
     p.setAlignmentX(Component.LEFT_ALIGNMENT);

     JLabel lbl = new JLabel(label.toUpperCase(Locale.ROOT));
     lbl.setAlignmentX(Component.LEFT_ALIGNMENT);
     lbl.setFont(new Font("Segoe UI", Font.BOLD, 12));
     lbl.setForeground(new Color(87, 110, 178));

     // Config del área
     area.setLineWrap(true);
     area.setWrapStyleWord(true);
     area.setBorder(new EmptyBorder(8, 12, 8, 12));
     area.setFont(new Font("Segoe UI", Font.PLAIN, 13));
     area.setRows(2);   // <- alto base (6 líneas visibles)

     // IMPORTANTÍSIMO: el tamaño se lo damos al SCROLL, no al área
     JScrollPane scroll = new JScrollPane(area);
     scroll.setBorder(BorderFactory.createLineBorder(new Color(211, 220, 249)));
     scroll.setAlignmentX(Component.LEFT_ALIGNMENT);
     scroll.setOpaque(false);
     scroll.getViewport().setOpaque(false);

     // Forzamos altura para BoxLayout (min, pref y max):
     int alto = 100; // subilo si querés: 160/180
     scroll.setMinimumSize(new Dimension(0, alto));
     scroll.setPreferredSize(new Dimension(0, alto));
     scroll.setMaximumSize(new Dimension(Integer.MAX_VALUE, alto));

     p.add(lbl);
     p.add(Box.createVerticalStrut(6));
     p.add(scroll);
     return p;
 }



    private JPanel infoLabel(String title, JLabel value) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setOpaque(false);
        panel.setAlignmentX(Component.LEFT_ALIGNMENT);
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
    private JScrollPane scrollList(JComponent content, int preferredHeight) {
        JScrollPane scroll = new JScrollPane(content);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        // altura “tope” deseada para que, si hay más items, aparezca el scroll
        scroll.setPreferredSize(new Dimension(0, preferredHeight));
        scroll.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.getVerticalScrollBar().setUnitIncrement(16); // scroll suave
        return scroll;
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

        private SimpleDocumentListener(Runnable onChange) {
            this.onChange = onChange;
        }

        @Override
        public void insertUpdate(DocumentEvent e) {
            onChange.run();
        }

        @Override
        public void removeUpdate(DocumentEvent e) {
            onChange.run();
        }

        @Override
        public void changedUpdate(DocumentEvent e) {
            onChange.run();
        }
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
                if (normalized.contains("alta")) {
                    base = new Color(255, 120, 102);
                } else if (normalized.contains("media")) {
                    base = new Color(255, 188, 75);
                } else {
                    base = new Color(200, 210, 230);
                }
            } else {
                if (normalized.contains("complet")) {
                    base = new Color(73, 198, 154);
                } else if (normalized.contains("proceso")) {
                    base = new Color(86, 127, 255);
                } else if (normalized.contains("alta")) {
                    base = new Color(255, 120, 102);
                } else {
                    base = new Color(255, 188, 75);
                }
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
