package ar.edu.unse.siga.ui;

import ar.edu.unse.siga.service.TramiteService;

import javax.swing.*;
import java.awt.*;

public class TramiteFrame extends JFrame {
    private final TramiteService service;
    private final TramiteTableModel model = new TramiteTableModel();
    private final JTable table = new JTable(model);

    public TramiteFrame(TramiteService service) {
        super("Gestión de Trámites");
        this.service = service;

        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setLayout(new BorderLayout(10, 10));

        // Tabla.a
        add(new JScrollPane(table), BorderLayout.CENTER);

        // Botonera
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton btnNuevo = new JButton("Nuevo");
        JButton btnEstado = new JButton("Cambiar Estado");
        JButton btnRefresh = new JButton("Refrescar");
        actions.add(btnNuevo);
        actions.add(btnEstado);
        actions.add(btnRefresh);
        add(actions, BorderLayout.NORTH);

        // Acciones
        btnRefresh.addActionListener(e -> loadData());

        btnNuevo.addActionListener(e -> {
            TramiteFormDialog d = new TramiteFormDialog(this);
            d.setVisible(true);
            if (d.isAccepted()) {
                try {
                    service.registrarTramite(d.getNro(), d.getAsunto(), d.getSolicitante());
                    loadData();
                } catch (Exception ex) {
                    showError(ex);
                }
            }
        });

        btnEstado.addActionListener(e -> {
            int row = table.getSelectedRow();
            if (row < 0) {
                JOptionPane.showMessageDialog(this, "Seleccioná un trámite");
                return;
            }
            var t = model.getAt(row);
            String nuevo = JOptionPane.showInputDialog(this, "Nuevo estado:", t.getEstado());
            if (nuevo != null && !nuevo.isBlank()) {
                try {
                    service.cambiarEstado(t.getId(), nuevo);
                    loadData();
                } catch (Exception ex) {
                    showError(ex);
                }
            }
        });

        setSize(700, 400);
        setLocationRelativeTo(null);
        loadData();
    }

    private void loadData() {
        try {
            model.setData(service.listarTodos());
        } catch (Exception e) {
            showError(e);
        }
    }

    private void showError(Exception e) {
        e.printStackTrace();
        JOptionPane.showMessageDialog(this, e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
    }
}
