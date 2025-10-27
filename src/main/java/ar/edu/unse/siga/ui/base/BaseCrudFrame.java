package ar.edu.unse.siga.ui.base;

import javax.swing.*;
import java.awt.*;

public abstract class BaseCrudFrame<T> extends JInternalFrame {
    protected final JTable table = new JTable();
    protected final JButton btnNuevo = new JButton("Nuevo");
    protected final JButton btnEditar = new JButton("Editar");
    protected final JButton btnBaja = new JButton("Baja / Eliminar");
    protected final JButton btnRefrescar = new JButton("Refrescar");
    protected final JPanel actionsPanel;

    protected BaseCrudFrame(String title) {
        super(title, true, true, true, true);
        setLayout(new BorderLayout(10,10));
        add(new JScrollPane(table), BorderLayout.CENTER);

        var top = new JPanel(new BorderLayout());
        actionsPanel = Ui.flowLeft(btnNuevo, btnEditar, btnBaja, btnRefrescar);
        top.add(actionsPanel, BorderLayout.WEST);
        add(top, BorderLayout.NORTH);

        btnRefrescar.addActionListener(e -> loadData());
        btnNuevo.addActionListener(e -> onNuevo());
        btnEditar.addActionListener(e -> onEditar());
        btnBaja.addActionListener(e -> onBaja());
        setSize(900, 500);
    }

    protected int selectedRowOrWarn() {
        int r = table.getSelectedRow();
        if (r < 0) {
            Ui.info(this, "Seleccioná un registro");
            return -1;
        }
        return r;
    }

    protected abstract void loadData();
    protected abstract void onNuevo();
    protected abstract void onEditar();
    protected abstract void onBaja();
}
