package ar.edu.unse.siga.ui.inventario;

import ar.edu.unse.siga.domain.Insumo;
import ar.edu.unse.siga.service.InventarioService;
import ar.edu.unse.siga.ui.base.Ui;

import javax.swing.*;
import java.awt.*;
import java.util.List;

public class ModificarInsumoPanel extends JPanel {

    private final InventarioService service;

    private final JTextField txtCodigo = new JTextField(16);
    private final JTextField txtDescripcion = new JTextField(24);
    private final JTextField txtCategoria = new JTextField(16);
    private final JTextField txtStockMin = new JTextField(8);
    private final JTextField txtUbicacion = new JTextField(16);
    private final JComboBox<String> cbEstado = new JComboBox<>(new String[]{"ACTIVO","INACTIVO"});
    private final JButton btnBuscar = new JButton("Buscar");
    private final JButton btnGuardar = new JButton("Guardar cambios");

    private Insumo seleccionado;

    public ModificarInsumoPanel(InventarioService service) {
        this.service = service;
        setLayout(new BorderLayout(12,12));
        setOpaque(false);

        var form = new JPanel(new GridBagLayout());
        var gbc = new GridBagConstraints();
        gbc.insets = new Insets(6,6,6,6);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        int r = 0;
        gbc.gridx=0; gbc.gridy=r; form.add(new JLabel("Código:"), gbc);
        gbc.gridx=1; form.add(txtCodigo, gbc);
        gbc.gridx=2; form.add(btnBuscar, gbc); r++;

        gbc.gridx=0; gbc.gridy=r; form.add(new JLabel("Descripción:"), gbc);
        gbc.gridx=1; gbc.gridwidth=2; form.add(txtDescripcion, gbc); gbc.gridwidth=1; r++;

        gbc.gridx=0; gbc.gridy=r; form.add(new JLabel("Categoría:"), gbc);
        gbc.gridx=1; gbc.gridwidth=2; form.add(txtCategoria, gbc); gbc.gridwidth=1; r++;

        gbc.gridx=0; gbc.gridy=r; form.add(new JLabel("Stock mínimo:"), gbc);
        gbc.gridx=1; form.add(txtStockMin, gbc); r++;

        gbc.gridx=0; gbc.gridy=r; form.add(new JLabel("Ubicación:"), gbc);
        gbc.gridx=1; gbc.gridwidth=2; form.add(txtUbicacion, gbc); gbc.gridwidth=1; r++;

        gbc.gridx=0; gbc.gridy=r; form.add(new JLabel("Estado:"), gbc);
        gbc.gridx=1; form.add(cbEstado, gbc); r++;

        add(form, BorderLayout.CENTER);

        var south = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        south.add(btnGuardar);
        add(south, BorderLayout.SOUTH);

        // acciones
        btnBuscar.addActionListener(e -> onBuscar());
        btnGuardar.addActionListener(e -> onGuardar());
    }

    private void onBuscar() {
        try {
            String codigo = txtCodigo.getText().trim();
            if (codigo.isEmpty()) throw new IllegalArgumentException("Ingresá un código para buscar");
            List<Insumo> all = service.listarTodos();
            seleccionado = all.stream().filter(i -> codigo.equalsIgnoreCase(i.getCodigo())).findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("No se encontró insumo con código " + codigo));
            // bind
            txtDescripcion.setText(seleccionado.getDescripcion());
            txtCategoria.setText(seleccionado.getCategoria() != null ? seleccionado.getCategoria().getNombre() : "");
            txtStockMin.setText(String.valueOf(seleccionado.getStockMinimo()));
            txtUbicacion.setText(seleccionado.getUbicacion() != null ? seleccionado.getUbicacion() : "");
            cbEstado.setSelectedItem(seleccionado.getEstado() != null ? seleccionado.getEstado() : "ACTIVO");
        } catch (Exception ex) {
            Ui.error(this, ex);
        }
    }

    private void onGuardar() {
        if (seleccionado == null) {
            Ui.warn(this, "Primero buscá un insumo.");
            return;
        }
        try {
            seleccionado.setDescripcion(txtDescripcion.getText().trim());
            seleccionado.setStockMinimo(Integer.parseInt(txtStockMin.getText().trim()));
            seleccionado.setUbicacion(txtUbicacion.getText().trim());
            seleccionado.setEstado((String) cbEstado.getSelectedItem());
            // NOTA: categoría real la resolvemos más adelante; hoy solo dejamos el nombre en el objeto existente
            if (seleccionado.getCategoria() != null) {
                seleccionado.getCategoria().setNombre(txtCategoria.getText().trim());
            }
            service.editarInsumo(seleccionado);
            Ui.info(this, "Cambios guardados.");
        } catch (Exception ex) {
            Ui.error(this, ex);
        }
    }
}
