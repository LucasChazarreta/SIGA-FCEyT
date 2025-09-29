package ar.edu.unse.siga.ui.finanzas;

import ar.edu.unse.siga.domain.FinanzaMovimiento;
import ar.edu.unse.siga.domain.FinanzaTipo;
import ar.edu.unse.siga.service.FinanzasService;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public class FinanzasTransaccionesPanel extends JPanel {

    private final FinanzasService service;

    // Reemplazamos JFormattedTextField por JTextField
    private final JTextField txtDesde = new JTextField(10);
    private final JTextField txtHasta = new JTextField(10);
    private final JComboBox<String> cmbTipo = new JComboBox<>(new String[]{"TODOS","INGRESO","EGRESO"});
    private final JTextField txtCategoria = new JTextField(12);

    private final DefaultTableModel model = new DefaultTableModel(
            new Object[]{"ID","Fecha","Tipo","Categoría","Monto","Referencia"}, 0) {
        @Override public boolean isCellEditable(int r, int c) { return false; }
    };
    private final JTable table = new JTable(model);

    public FinanzasTransaccionesPanel(FinanzasService service) {
        this.service = service;
        setOpaque(false);
        setLayout(new BorderLayout(8,8));

        JPanel filtros = new JPanel(new FlowLayout(FlowLayout.LEFT));
        filtros.add(new JLabel("Desde:")); filtros.add(txtDesde);
        filtros.add(new JLabel("Hasta:")); filtros.add(txtHasta);
        filtros.add(new JLabel("Tipo:")); filtros.add(cmbTipo);
        filtros.add(new JLabel("Categoría:")); filtros.add(txtCategoria);

        JButton btnBuscar = new JButton("Buscar");
        btnBuscar.addActionListener(e -> buscar());
        JButton btnNuevoIng = new JButton("+ Ingreso");
        btnNuevoIng.addActionListener(e -> nuevo(FinanzaTipo.INGRESO));
        JButton btnNuevoEgr = new JButton("+ Egreso");
        btnNuevoEgr.addActionListener(e -> nuevo(FinanzaTipo.EGRESO));
        JButton btnEditar = new JButton("Editar");
        btnEditar.addActionListener(e -> editar());
        JButton btnEliminar = new JButton("Eliminar");
        btnEliminar.addActionListener(e -> eliminar());

        JPanel acciones = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        acciones.add(btnBuscar);
        acciones.add(btnNuevoIng);
        acciones.add(btnNuevoEgr);
        acciones.add(btnEditar);
        acciones.add(btnEliminar);

        add(filtros, BorderLayout.NORTH);
        add(new JScrollPane(table), BorderLayout.CENTER);
        add(acciones, BorderLayout.SOUTH);

        LocalDate hoy = LocalDate.now();
        txtDesde.setText(hoy.withDayOfMonth(1).toString());
        txtHasta.setText(hoy.toString());
        buscar();
    }

    private void buscar() {
        model.setRowCount(0);
        LocalDate d = parse(txtDesde.getText());
        LocalDate h = parse(txtHasta.getText());
        FinanzaTipo t = switch (String.valueOf(cmbTipo.getSelectedItem())) {
            case "INGRESO" -> FinanzaTipo.INGRESO;
            case "EGRESO" -> FinanzaTipo.EGRESO;
            default -> null;
        };
        String cat = txtCategoria.getText();
        List<FinanzaMovimiento> lista = service.buscar(d, h, t, cat);
        for (FinanzaMovimiento m : lista) {
            model.addRow(new Object[]{
                m.getId(), m.getFecha(), m.getTipo(), m.getCategoria(),
                m.getMonto(), m.getReferencia()
            });
        }
    }

    private void nuevo(FinanzaTipo tipo) {
        FinanzaMovimiento m = editarDialog(new FinanzaMovimiento(null, tipo, "", new BigDecimal("0.00"), LocalDate.now(), ""));
        if (m != null) {
            service.registrar(m);
            buscar();
        }
    }

    private void editar() {
        int row = table.getSelectedRow();
        if (row < 0) { JOptionPane.showMessageDialog(this,"Seleccioná una fila"); return; }
        FinanzaMovimiento m = new FinanzaMovimiento(
                ((Number)model.getValueAt(row,0)).longValue(),
                FinanzaTipo.valueOf(model.getValueAt(row,2).toString()),
                model.getValueAt(row,3).toString(),
                new BigDecimal(model.getValueAt(row,4).toString()),
                LocalDate.parse(model.getValueAt(row,1).toString()),
                (String)model.getValueAt(row,5)
        );
        FinanzaMovimiento edit = editarDialog(m);
        if (edit != null) {
            service.actualizar(edit);
            buscar();
        }
    }

    private void eliminar() {
        int row = table.getSelectedRow();
        if (row < 0) { JOptionPane.showMessageDialog(this,"Seleccioná una fila"); return; }
        Long id = ((Number)model.getValueAt(row,0)).longValue();
        int opt = JOptionPane.showConfirmDialog(this, "¿Eliminar transacción " + id + "?", "Confirmar", JOptionPane.YES_NO_OPTION);
        if (opt == JOptionPane.YES_OPTION) {
            service.eliminar(id);
            buscar();
        }
    }

    private FinanzaMovimiento editarDialog(FinanzaMovimiento base) {
        JTextField txtFecha = new JTextField(base.getFecha() != null ? base.getFecha().toString() : LocalDate.now().toString(), 10);
        JComboBox<String> cmbTipoLocal = new JComboBox<>(new String[]{"INGRESO","EGRESO"});
        cmbTipoLocal.setSelectedItem(base.getTipo() != null ? base.getTipo().name() : "INGRESO");
        JTextField txtCategoriaLocal = new JTextField(base.getCategoria() != null ? base.getCategoria() : "", 15);
        JTextField txtMontoLocal = new JTextField(base.getMonto() != null ? base.getMonto().toPlainString() : "0.00", 10);
        JTextField txtRefLocal = new JTextField(base.getReferencia() != null ? base.getReferencia() : "", 20);

        JPanel form = new JPanel(new GridLayout(0,2,6,6));
        form.add(new JLabel("Fecha (YYYY-MM-DD):")); form.add(txtFecha);
        form.add(new JLabel("Tipo:")); form.add(cmbTipoLocal);
        form.add(new JLabel("Categoría:")); form.add(txtCategoriaLocal);
        form.add(new JLabel("Monto:")); form.add(txtMontoLocal);
        form.add(new JLabel("Referencia:")); form.add(txtRefLocal);

        int opt = JOptionPane.showConfirmDialog(this, form,
                (base.getId()==null? "Nueva" : "Editar") + " transacción", JOptionPane.OK_CANCEL_OPTION);
        if (opt == JOptionPane.OK_OPTION) {
            try {
                LocalDate f = LocalDate.parse(txtFecha.getText().trim());
                BigDecimal monto = new BigDecimal(txtMontoLocal.getText().trim());
                FinanzaTipo tipo = FinanzaTipo.valueOf(String.valueOf(cmbTipoLocal.getSelectedItem()));
                String cat = txtCategoriaLocal.getText().trim();
                String ref = txtRefLocal.getText().trim();
                if (base.getId() == null) {
                    return new FinanzaMovimiento(null, tipo, cat, monto, f, ref);
                } else {
                    base.setTipo(tipo); base.setCategoria(cat); base.setMonto(monto); base.setFecha(f); base.setReferencia(ref);
                    return base;
                }
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Datos inválidos: " + ex.getMessage());
            }
        }
        return null;
    }

    private LocalDate parse(String s) { return (s==null || s.isBlank())? null : LocalDate.parse(s.trim()); }
}
