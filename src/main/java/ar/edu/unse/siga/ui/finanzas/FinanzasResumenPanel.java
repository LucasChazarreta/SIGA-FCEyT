package ar.edu.unse.siga.ui.finanzas;

import ar.edu.unse.siga.domain.FinanzaTipo;
import ar.edu.unse.siga.service.FinanzasService;

import javax.swing.*;
import java.awt.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

public class FinanzasResumenPanel extends JPanel {

    private final FinanzasService service;

    // Reemplazamos JFormattedTextField por JTextField
    private final JTextField txtDesde = new JTextField(10);
    private final JTextField txtHasta = new JTextField(10);

    private final JLabel lblIngresos = new JLabel("0.00");
    private final JLabel lblEgresos = new JLabel("0.00");
    private final JLabel lblBalance = new JLabel("0.00");
    private final JTextArea areaCategorias = new JTextArea(10, 40);

    public FinanzasResumenPanel(FinanzasService service) {
        this.service = service;
        setOpaque(false);
        setLayout(new BorderLayout(12,12));

        JPanel filtros = new JPanel(new FlowLayout(FlowLayout.LEFT));
        filtros.add(new JLabel("Desde (YYYY-MM-DD):"));
        filtros.add(txtDesde);
        filtros.add(new JLabel("Hasta:"));
        filtros.add(txtHasta);

        JButton btnActualizar = new JButton("Actualizar");
        btnActualizar.addActionListener(e -> refrescar());
        filtros.add(btnActualizar);

        JPanel kpis = new JPanel(new GridLayout(1,3,12,12));
        kpis.add(kpi("Ingresos", lblIngresos));
        kpis.add(kpi("Egresos", lblEgresos));
        kpis.add(kpi("Balance", lblBalance));

        JPanel centro = new JPanel(new BorderLayout());
        areaCategorias.setEditable(false);
        centro.add(new JLabel("Totales por categoría"), BorderLayout.NORTH);
        centro.add(new JScrollPane(areaCategorias), BorderLayout.CENTER);

        add(filtros, BorderLayout.NORTH);
        add(kpis, BorderLayout.CENTER);
        add(centro, BorderLayout.SOUTH);

        // Inicial: mes actual
        LocalDate hoy = LocalDate.now();
        txtDesde.setText(hoy.withDayOfMonth(1).toString());
        txtHasta.setText(hoy.toString());
        refrescar();
    }

    private JPanel kpi(String titulo, JLabel valor) {
        JPanel p = new JPanel(new BorderLayout());
        p.setBorder(BorderFactory.createTitledBorder(titulo));
        valor.setFont(valor.getFont().deriveFont(Font.BOLD, 18f));
        valor.setHorizontalAlignment(SwingConstants.CENTER);
        p.add(valor, BorderLayout.CENTER);
        return p;
    }

    private void refrescar() {
        try {
            LocalDate desde = parse(txtDesde.getText());
            LocalDate hasta = parse(txtHasta.getText());
            BigDecimal ing = service.totalIngresos(desde, hasta);
            BigDecimal egr = service.totalEgresos(desde, hasta);
            BigDecimal bal = ing.subtract(egr);

            lblIngresos.setText(ing.toPlainString());
            lblEgresos.setText(egr.toPlainString());
            lblBalance.setText(bal.toPlainString());

            StringBuilder sb = new StringBuilder();
            sb.append("[INGRESOS]\n");
            for (Map.Entry<String, BigDecimal> e : service.totalPorCategoria(desde, hasta, FinanzaTipo.INGRESO).entrySet()) {
                sb.append(" - ").append(e.getKey()).append(": ").append(e.getValue().toPlainString()).append("\n");
            }
            sb.append("\n[EGRESOS]\n");
            for (Map.Entry<String, BigDecimal> e : service.totalPorCategoria(desde, hasta, FinanzaTipo.EGRESO).entrySet()) {
                sb.append(" - ").append(e.getKey()).append(": ").append(e.getValue().toPlainString()).append("\n");
            }
            areaCategorias.setText(sb.toString());
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Rango de fechas inválido o error al consultar.\n" + ex.getMessage(),
                    "Finanzas", JOptionPane.ERROR_MESSAGE);
        }
    }

    private LocalDate parse(String s) {
        return (s == null || s.isBlank()) ? null : LocalDate.parse(s.trim());
    }
}

