package ar.edu.unse.siga.ui.reportes;

import ar.edu.unse.siga.domain.Insumo;
import ar.edu.unse.siga.service.InventarioService;
import ar.edu.unse.siga.service.TramiteService;
import ar.edu.unse.siga.ui.base.Ui;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

public class InformesPanel extends JPanel {

    private final InventarioService invService;
    private final TramiteService traService;

    private final JLabel lblTotalInsumos = new JLabel("-");
    private final JLabel lblTotalTramites = new JLabel("-");
    private final JLabel lblPendientes = new JLabel("-");
    private final JLabel lblGastos = new JLabel("$-");

    private final JComboBox<String> cbCategoria = new JComboBox<>(new String[]{
            "Todas", "Oficina", "Bienes", "Insumo"
    });
    private final JTextField tfDesde = new JTextField(10);
    private final JTextField tfHasta = new JTextField(10);

    private final DefaultTableModel model = new DefaultTableModel(
            new Object[]{"Código","Descripción","Estado","Fecha"}, 0
    );

    public InformesPanel(InventarioService invService, TramiteService traService) {
        this.invService = invService;
        this.traService = traService;

        setLayout(new BorderLayout(12,12));
        setOpaque(false);

        // header
        var title = new JLabel("INFORMES");
        title.setFont(title.getFont().deriveFont(Font.BOLD, 28f));
        title.setBorder(BorderFactory.createEmptyBorder(12,12,6,12));
        add(title, BorderLayout.NORTH);

        // métricas
        var metrics = new JPanel(new GridLayout(1,4,10,10));
        metrics.add(metricBox("TOTAL INSUMOS", lblTotalInsumos));
        metrics.add(metricBox("TOTAL TRÁMITES", lblTotalTramites));
        metrics.add(metricBox("PENDIENTES", lblPendientes));
        metrics.add(metricBox("GASTOS MENSUALES", lblGastos));
        add(metrics, BorderLayout.BEFORE_FIRST_LINE);

        // filtros + tabla
        var left = new JPanel();
        left.setLayout(new BoxLayout(left, BoxLayout.Y_AXIS));
        left.setBorder(BorderFactory.createEmptyBorder(8,12,8,12));
        left.add(new JLabel("Categoría"));
        left.add(cbCategoria);
        left.add(Box.createVerticalStrut(8));
        left.add(new JLabel("Desde (dd/mm/aaaa)"));
        left.add(tfDesde);
        left.add(Box.createVerticalStrut(8));
        left.add(new JLabel("Hasta (dd/mm/aaaa)"));
        left.add(tfHasta);
        left.add(Box.createVerticalStrut(10));
        var btnFiltrar = new JButton("APLICAR FILTROS");
        left.add(btnFiltrar);

        var table = new JTable(model);
        var center = new JPanel(new BorderLayout());
        center.setBorder(BorderFactory.createEmptyBorder(8,8,8,12));
        center.add(new JScrollPane(table), BorderLayout.CENTER);

        var mid = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, left, center);
        mid.setResizeWeight(0.25);
        add(mid, BorderLayout.CENTER);

        // acciones
        btnFiltrar.addActionListener(e -> runQuery());

        // carga inicial
        reloadMetrics();
        runQuery();
    }

    private JPanel metricBox(String title, JLabel value) {
        var p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        var t = new JLabel(title);
        t.setFont(t.getFont().deriveFont(Font.PLAIN, 12f));
        value.setFont(value.getFont().deriveFont(Font.BOLD, 22f));
        p.add(t); p.add(value);
        p.setBorder(BorderFactory.createEmptyBorder(8,12,8,12));
        return p;
    }

    // ==== reemplazos sin depender de nuevos métodos del service ====

    private void reloadMetrics() {
        try {
            // total insumos por listarTodos()
            lblTotalInsumos.setText(String.valueOf(invService.listarTodos().size()));
        } catch (Exception e) { lblTotalInsumos.setText("-"); }

        try {
            var tramites = traService.listarTodos();
            lblTotalTramites.setText(String.valueOf(tramites.size()));
            long pend = tramites.stream()
                    .filter(t -> "PENDIENTE".equalsIgnoreCase(String.valueOf(t.getEstado())))
                    .count();
            lblPendientes.setText(String.valueOf(pend));
        } catch (Exception e) { lblTotalTramites.setText("-"); lblPendientes.setText("-"); }

        try {
            // si no tenés movimientos aún, mostramos 0
            lblGastos.setText("+$0");
        } catch (Exception e) { lblGastos.setText("$-"); }
    }

    private void runQuery() {
        model.setRowCount(0);
        try {
            String cat = cbCategoria.getSelectedIndex() == 0 ? null : cbCategoria.getSelectedItem().toString();
            LocalDate d1 = parse(tfDesde.getText());
            LocalDate d2 = parse(tfHasta.getText());

            List<Insumo> data = filtrarLocal(invService.listarTodos(), cat, d1, d2);
            for (var i : data) {
                // muchos dominios no tienen fechaAlta; mostramos "-"
                model.addRow(new Object[]{ i.getCodigo(), i.getDescripcion(), i.getEstado(), "-" });
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private static List<Insumo> filtrarLocal(List<Insumo> base, String cat, LocalDate d1, LocalDate d2) {
        return base.stream().filter(i -> {
            boolean ok = true;
            if (cat != null && i.getCategoria() != null) {
                ok &= cat.equalsIgnoreCase(i.getCategoria().getNombre());
            }
            // si luego agregamos fechaAlta en Insumo, filtramos por d1/d2 aquí
            return ok;
        }).collect(Collectors.toList());
    }

    private static LocalDate parse(String ddmmyyyy) {
        if (ddmmyyyy == null) return null;
        ddmmyyyy = ddmmyyyy.trim();
        if (ddmmyyyy.isEmpty()) return null;
        try {
            String[] p = ddmmyyyy.split("[/\\-]");
            int d = Integer.parseInt(p[0]);
            int m = Integer.parseInt(p[1]);
            int y = Integer.parseInt(p[2]);
            return LocalDate.of(y, m, d);
        } catch (Exception e) {
            throw new IllegalArgumentException("Fecha inválida: use dd/mm/aaaa");
        }
    }
}
