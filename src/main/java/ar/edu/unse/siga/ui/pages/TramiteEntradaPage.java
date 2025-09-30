package ar.edu.unse.siga.ui.pages;

import ar.edu.unse.siga.service.TramiteService;
import ar.edu.unse.siga.domain.Tramite;
import ar.edu.unse.siga.ui.base.CardPanel;
import ar.edu.unse.siga.ui.base.Ui;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class TramiteEntradaPage extends JPanel {
    private final TramiteService service;

    private final JTextField txtAsunto = new JTextField(25);
    private final JTextField txtRemitente = new JTextField(25);
    private final JTextArea txtDescripcion = new JTextArea(4, 25);
    private final JTextField txtDestino = new JTextField(25);
    private final JTextField txtDestinatario = new JTextField(25);
    private final JLabel lblNumero = new JLabel();

    private final CardLayout cardLayout = new CardLayout();
    private final JPanel cards = new JPanel(cardLayout);

    public TramiteEntradaPage(TramiteService service) {
        this.service = service;
        setOpaque(false);
        setLayout(new BorderLayout(20, 20));

        add(buildHeader(), BorderLayout.NORTH);
        add(buildContent(), BorderLayout.CENTER);

        lblNumero.setText(generateNumero(LocalDate.now()));
        cardLayout.show(cards, "registro");
    }

    private JComponent buildHeader() {
        JPanel header = new JPanel(new BorderLayout(12, 12));
        header.setOpaque(false);

        JLabel title = new JLabel("Trámites");
        title.setFont(title.getFont().deriveFont(Font.BOLD, 30f));
        title.setForeground(new Color(24, 63, 150));
        header.add(title, BorderLayout.WEST);

        return header;
    }

    private JComponent buildContent() {
        JPanel wrapper = new JPanel(new BorderLayout(12, 18));
        wrapper.setOpaque(false);

        ButtonGroup tabs = new ButtonGroup();
        JToggleButton btnRegistrar = pill("Registrar nuevo trámite");
        JToggleButton btnActivos = pill("Trámites activos");
        btnRegistrar.setSelected(true);
        tabs.add(btnRegistrar);
        tabs.add(btnActivos);

        JPanel tabRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        tabRow.setOpaque(false);
        tabRow.add(btnRegistrar);
        tabRow.add(btnActivos);
        wrapper.add(tabRow, BorderLayout.NORTH);

        cards.setOpaque(false);
        cards.add(buildRegistroCard(), "registro");
        cards.add(buildActivosCard(), "activos");

        btnRegistrar.addActionListener(e -> cardLayout.show(cards, "registro"));
        btnActivos.addActionListener(e -> {
            loadTableData();
            cardLayout.show(cards, "activos");
        });

        wrapper.add(cards, BorderLayout.CENTER);
        return wrapper;
    }

    private CardPanel buildRegistroCard() {
        CardPanel card = new CardPanel();
        card.setLayout(new BorderLayout(18, 18));

        JPanel columns = new JPanel(new GridLayout(1, 2, 18, 0));
        columns.setOpaque(false);

        columns.add(buildFormPanel());
        columns.add(buildSidebarPanel());
        card.add(columns, BorderLayout.CENTER);
        return card;
    }

    private CardPanel buildFormPanel() {
        CardPanel card = new CardPanel();
        card.setLayout(new BorderLayout(12, 12));

        JLabel title = new JLabel("Detalle del trámite".toUpperCase());
        title.setForeground(new Color(73, 103, 204));
        title.setFont(title.getFont().deriveFont(Font.BOLD, 14f));
        card.add(title, BorderLayout.NORTH);

        JPanel form = new JPanel();
        form.setOpaque(false);
        form.setLayout(new BoxLayout(form, BoxLayout.Y_AXIS));

        form.add(infoLabel("Número generado", lblNumero));
        form.add(Box.createVerticalStrut(12));
        form.add(field("Asunto", txtAsunto));
        form.add(Box.createVerticalStrut(12));
        form.add(field("Remitente", txtRemitente));
        form.add(Box.createVerticalStrut(12));
        txtDescripcion.setLineWrap(true);
        txtDescripcion.setWrapStyleWord(true);
        form.add(textAreaField("Descripción", txtDescripcion));
        form.add(Box.createVerticalStrut(12));
        form.add(field("Destino", txtDestino));
        form.add(Box.createVerticalStrut(12));
        form.add(field("Destinatario", txtDestinatario));

        JButton btn = primaryButton("Aceptar");
        btn.addActionListener(e -> onSave());
        btn.setAlignmentX(Component.CENTER_ALIGNMENT);
        form.add(Box.createVerticalStrut(18));
        form.add(btn);

        card.add(form, BorderLayout.CENTER);
        return card;
    }

    private CardPanel buildSidebarPanel() {
        CardPanel container = new CardPanel();
        container.setLayout(new GridLayout(2, 1, 12, 12));
        container.add(listCard("Trámites recientes", new String[][]{
                {"Asunto: Presupuesto 2026", "Estado: EN PROCESO"},
                {"Asunto: Pedido resma A4", "Estado: COMPLETADO"},
                {"Asunto: Solicitud equipamiento", "Estado: PENDIENTE"}
        }));
        container.add(listCard("Trámites más antiguos", new String[][]{
                {"Asunto: Compra equipos Lab.", "Fecha: 23/09/2024"},
                {"Asunto: Presupuesto Decanato", "Fecha: 12/01/2025"}
        }));
        return container;
    }

    private CardPanel buildActivosCard() {
        CardPanel card = new CardPanel();
        card.setLayout(new BorderLayout(12, 12));

        JLabel title = new JLabel("Trámites activos".toUpperCase());
        title.setForeground(new Color(73, 103, 204));
        title.setFont(title.getFont().deriveFont(Font.BOLD, 14f));
        card.add(title, BorderLayout.NORTH);

        card.add(new JScrollPane(table), BorderLayout.CENTER);
        return card;
    }

    private String generateNumero(LocalDate d) {
        // Ej: 20240915-00123 (simplificado)
        return d.format(DateTimeFormatter.BASIC_ISO_DATE) + "-" + (int)(Math.random()*90000+10000);
    }

    private void onSave() {
        try {
            String nro = lblNumero.getText();
            String asunto = txtAsunto.getText().trim();
            String solicitante = txtRemitente.getText().trim();
            if (asunto.isEmpty()) throw new IllegalArgumentException("El asunto es obligatorio");

            service.registrarTramite(nro, asunto, solicitante);

            Ui.info(this, "Trámite registrado.");
            lblNumero.setText(generateNumero(LocalDate.now()));
            txtAsunto.setText("");
            txtRemitente.setText("");
            txtDescripcion.setText("");
            txtDestino.setText("");
            txtDestinatario.setText("");
        } catch(Exception e) {
            Ui.error(this, e);
        }
    }

    private final JTable table = new JTable(new DefaultTableModel(
            new Object[][]{},
            new Object[]{"Número", "Asunto", "Estado", "Fecha"}
    )) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };

    private void loadTableData() {
        DefaultTableModel model = (DefaultTableModel) table.getModel();
        model.setRowCount(0);
        try {
            List<Tramite> tramites = service.listarTodos();
            for (Tramite t : tramites) {
                LocalDateTime fecha = t.getFecha();
                String f = fecha != null ? fecha.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) : "-";
                model.addRow(new Object[]{t.getNro(), t.getAsunto(), t.getEstado(), f});
            }
        } catch (Exception ex) {
            Ui.error(this, ex);
        }
    }

    private JPanel field(String label, JComponent component) {
        JPanel p = new JPanel();
        p.setOpaque(false);
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        JLabel lbl = new JLabel(label.toUpperCase());
        lbl.setAlignmentX(Component.LEFT_ALIGNMENT);
        lbl.setFont(lbl.getFont().deriveFont(Font.PLAIN, 12f));
        component.setAlignmentX(Component.LEFT_ALIGNMENT);
        component.putClientProperty("JComponent.roundRect", true);
        p.add(lbl);
        p.add(Box.createVerticalStrut(4));
        p.add(component);
        return p;
    }

    private JPanel textAreaField(String label, JTextArea area) {
        JPanel p = new JPanel();
        p.setOpaque(false);
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        JLabel lbl = new JLabel(label.toUpperCase());
        lbl.setAlignmentX(Component.LEFT_ALIGNMENT);
        lbl.setFont(lbl.getFont().deriveFont(Font.PLAIN, 12f));
        area.setAlignmentX(Component.LEFT_ALIGNMENT);
        area.setBorder(BorderFactory.createLineBorder(new Color(205, 218, 255)));
        p.add(lbl);
        p.add(Box.createVerticalStrut(4));
        p.add(new JScrollPane(area));
        return p;
    }

    private JPanel infoLabel(String title, JLabel value) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setOpaque(false);
        JLabel lblTitle = new JLabel(title.toUpperCase());
        lblTitle.setFont(lblTitle.getFont().deriveFont(Font.PLAIN, 12f));
        panel.add(lblTitle, BorderLayout.NORTH);
        value.setFont(value.getFont().deriveFont(Font.BOLD, 16f));
        panel.add(value, BorderLayout.CENTER);
        return panel;
    }

    private CardPanel listCard(String title, String[][] rows) {
        CardPanel card = new CardPanel();
        card.setLayout(new BorderLayout(6, 6));
        JLabel lblTitle = new JLabel(title.toUpperCase());
        lblTitle.setForeground(new Color(73, 103, 204));
        lblTitle.setFont(lblTitle.getFont().deriveFont(Font.BOLD, 13f));
        card.add(lblTitle, BorderLayout.NORTH);

        JPanel content = new JPanel();
        content.setOpaque(false);
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        for (String[] row : rows) {
            JLabel line = new JLabel("<html>" + row[0] + "<br>" + row[1] + "</html>");
            line.setBorder(BorderFactory.createEmptyBorder(4, 0, 4, 0));
            content.add(line);
        }
        JButton link = new JButton("Ver historial completo");
        link.setContentAreaFilled(false);
        link.setBorderPainted(false);
        link.setForeground(new Color(58, 96, 224));
        link.setAlignmentX(Component.LEFT_ALIGNMENT);
        content.add(Box.createVerticalStrut(8));
        content.add(link);

        card.add(content, BorderLayout.CENTER);
        return card;
    }

    private JButton primaryButton(String text) {
        JButton b = new JButton(text);
        b.setBackground(new Color(58, 96, 224));
        b.setForeground(Color.WHITE);
        b.setFocusPainted(false);
        b.putClientProperty("JButton.buttonType", "roundRect");
        return b;
    }

    private JToggleButton pill(String text) {
        JToggleButton t = new JToggleButton(text.toUpperCase());
        t.setFocusPainted(false);
        t.setBackground(Color.WHITE);
        t.setForeground(new Color(66, 100, 189));
        t.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(205, 218, 255)),
                BorderFactory.createEmptyBorder(8, 18, 8, 18)
        ));
        t.addChangeListener(e -> {
            if (t.isSelected()) {
                t.setBackground(new Color(58, 96, 224));
                t.setForeground(Color.WHITE);
            } else {
                t.setBackground(Color.WHITE);
                t.setForeground(new Color(66, 100, 189));
            }
        });
        return t;
    }
}
