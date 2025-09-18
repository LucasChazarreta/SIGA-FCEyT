package ar.edu.unse.siga.ui.pages;

import ar.edu.unse.siga.domain.Usuario;
import ar.edu.unse.siga.ui.base.CardPanel;
import ar.edu.unse.siga.ui.base.ScalableSvgPanel;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.time.LocalDate;

public class HomePage extends JPanel {

    private final Usuario usuarioActual;

    // Si hoy no pasás el usuario, podés dejar este ctor y delegar.
    public HomePage() {
        this(null);
    }

    public HomePage(Usuario usuarioActual) {
        this.usuarioActual = usuarioActual;

        setOpaque(true);
        setBackground(new Color(245, 249, 255));
        setLayout(new BorderLayout());

        // ========== HERO (igual que tenías) ==========
        var hero = new ScalableSvgPanel("hero/home.svg");
        hero.setMaxPadding(64);
        add(hero, BorderLayout.NORTH);

        // ========== CONTENIDO (dashboard) ==========
        var content = new JPanel(new GridBagLayout());
        content.setOpaque(false);
        add(content, BorderLayout.CENTER);

        GridBagConstraints gc = new GridBagConstraints();
        gc.insets = new Insets(8, 16, 8, 16);
        gc.anchor = GridBagConstraints.FIRST_LINE_START;
        gc.fill = GridBagConstraints.BOTH;

        // HEADER (título + meta, como en el login)
        var header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        header.setBorder(BorderFactory.createEmptyBorder(6, 0, 2, 0));

        var title = new JLabel("Inicio");
        title.setFont(new Font("Segoe UI", Font.BOLD, 22));
        header.add(title, BorderLayout.WEST);

        var meta = new JLabel(
                (usuarioActual != null ? usuarioActual.getUsuario() : "") +
                "   |   " + LocalDate.now()
        );
        meta.setForeground(new Color(100, 100, 100));
        meta.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        header.add(meta, BorderLayout.EAST);

        gc.gridx = 0; gc.gridy = 0; gc.weightx = 1; gc.weighty = 0;
        content.add(header, gc);

        // ====== Fila KPI ======
        var kpis = new JPanel(new GridLayout(1, 3, 16, 0));
        kpis.setOpaque(false);
        kpis.add(kpi("Inventario total", "2,345", "bienes registrados"));
        kpis.add(kpi("Faltantes", "15", "requieren reposición"));
        kpis.add(kpi("Expedientes abiertos", "27", "en trámite"));

        gc.gridy = 1;
        content.add(kpis, gc);

        // ====== Acciones rápidas ======
        var quick = new CardPanel();
        quick.setLayout(new FlowLayout(FlowLayout.LEFT, 12, 2));
        quick.add(primary("Nuevo ítem",        e -> {/* TODO abrir alta inventario */}));
        quick.add(primary("Nuevo expediente",  e -> {/* TODO abrir mesa de entrada */}));
        quick.add(primary("Generar reporte",   e -> {/* TODO abrir reportes */}));

        gc.gridy = 2;
        content.add(quick, gc);

        // ====== Actividad reciente ======
        var recent = new CardPanel();
        recent.setLayout(new BorderLayout(8,8));

        var recentTitle = new JLabel("Actividad reciente");
        recentTitle.setFont(new Font("Segoe UI", Font.BOLD, 16));
        recent.add(recentTitle, BorderLayout.NORTH);

        String[] cols = {"Fecha/Hora","Módulo","Acción","Detalle","Usuario"};
        Object[][] rows = {
                {"18:02","Inventario","Alta","PC HP 400 G9","admin"},
                {"17:51","Mesa Entrada","Derivación","Exp. 123/25 a Compras","admin"},
                {"16:40","Inventario","Movimiento","Salida de 10 tóner","mlopez"}
        };
        var model = new DefaultTableModel(rows, cols) { @Override public boolean isCellEditable(int r, int c) { return false; } };
        var table = new JTable(model);
        table.setRowHeight(24);
        table.setShowGrid(false);
        table.setFillsViewportHeight(true);
        recent.add(new JScrollPane(table), BorderLayout.CENTER);

        gc.gridy = 3; gc.weighty = 1; // esta crece
        content.add(recent, gc);
    }

    // --------- helpers de UI (reusan tu CardPanel) ---------
    private CardPanel kpi(String title, String value, String hint) {
        var card = new CardPanel();
        card.setLayout(new BorderLayout(8, 8));

        var center = new JPanel();
        center.setOpaque(false);
        center.setLayout(new BoxLayout(center, BoxLayout.Y_AXIS));

        var lblTitle = new JLabel(title);
        lblTitle.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        lblTitle.setForeground(new Color(110,110,110));

        var lblValue = new JLabel(value);
        lblValue.setFont(new Font("Segoe UI", Font.BOLD, 24));
        lblValue.setForeground(new Color(30,30,30));

        var lblHint = new JLabel(hint);
        lblHint.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lblHint.setForeground(new Color(130,130,130));

        center.add(lblTitle);
        center.add(Box.createVerticalStrut(2));
        center.add(lblValue);
        center.add(Box.createVerticalStrut(2));
        center.add(lblHint);

        card.add(center, BorderLayout.CENTER);
        return card;
    }

    private JButton primary(String text, java.awt.event.ActionListener onClick) {
        JButton b = new JButton(text);
        b.setFocusPainted(false);
        b.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        b.putClientProperty("JButton.buttonType", "roundRect"); // FlatLaf
        b.setPreferredSize(new Dimension(170, 36));
        b.addActionListener(onClick);
        return b;
    }
}
