package ar.edu.unse.siga.ui.reportes;

import ar.edu.unse.siga.common.CsvExporter;
import ar.edu.unse.siga.persistence.DataSourceFactory;
import ar.edu.unse.siga.ui.base.Ui;

import javax.swing.*;
import java.awt.*;
import java.nio.file.Path;
import java.sql.ResultSet;
import java.text.SimpleDateFormat;
import java.util.ArrayList;

public class ReporteMovimientosDialog extends JDialog {
    private final JSpinner spDesde = new JSpinner(new SpinnerDateModel());
    private final JSpinner spHasta = new JSpinner(new SpinnerDateModel());

    public ReporteMovimientosDialog(Window owner) {
        super(owner, "Reporte de Movimientos (CSV)", ModalityType.APPLICATION_MODAL);
        ((JSpinner.DateEditor)spDesde.getEditor()).getFormat().applyPattern("yyyy-MM-dd");
        ((JSpinner.DateEditor)spHasta.getEditor()).getFormat().applyPattern("yyyy-MM-dd");

        var form = new JPanel(new FlowLayout(FlowLayout.LEFT));
        form.add(new JLabel("Desde:")); form.add(spDesde);
        form.add(new JLabel("Hasta:")); form.add(spHasta);

        var btn = new JButton("Exportar CSV");
        btn.addActionListener(e -> exportCsv());

        setLayout(new BorderLayout(10,10));
        add(form, BorderLayout.CENTER);
        add(Ui.flowRight(btn), BorderLayout.SOUTH);
        pack(); setLocationRelativeTo(owner);
    }

    private void exportCsv() {
        try {
            var sdf = new SimpleDateFormat("yyyy-MM-dd");
            String f1 = sdf.format(((SpinnerDateModel)spDesde.getModel()).getDate()) + " 00:00:00";
            String f2 = sdf.format(((SpinnerDateModel)spHasta.getModel()).getDate()) + " 23:59:59";

            String sql = """
                SELECT m.id, i.codigo, i.descripcion, m.tipo, m.cantidad, m.destino_fuente, m.fecha
                FROM movimiento m JOIN insumo i ON i.id = m.insumo_id
                WHERE m.fecha BETWEEN ? AND ?
                ORDER BY m.fecha DESC
            """;

            var rows = new ArrayList<String[]>();
            rows.add(new String[]{"ID","CODIGO","DESCRIPCION","TIPO","CANTIDAD","DESTINO_FUENTE","FECHA"});

            try (var cn = DataSourceFactory.getConnection();
                 var ps = cn.prepareStatement(sql)) {
                ps.setString(1, f1); ps.setString(2, f2);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        rows.add(new String[]{
                                String.valueOf(rs.getLong("id")),
                                rs.getString("codigo"),
                                rs.getString("descripcion"),
                                rs.getString("tipo"),
                                String.valueOf(rs.getInt("cantidad")),
                                rs.getString("destino_fuente"),
                                String.valueOf(rs.getTimestamp("fecha"))
                        });
                    }
                }
            }

            var chooser = new JFileChooser();
            chooser.setSelectedFile(new java.io.File("reporte_movimientos.csv"));
            if (chooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
                Path path = chooser.getSelectedFile().toPath();
                CsvExporter.write(path, rows);
                Ui.info(this, "CSV exportado en:\n" + path);
            }
        } catch (Exception e){ Ui.error(this,e); }
    }
}
