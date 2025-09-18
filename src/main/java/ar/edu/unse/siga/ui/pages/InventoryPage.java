package ar.edu.unse.siga.ui.pages;

import ar.edu.unse.siga.domain.Categoria;
import ar.edu.unse.siga.domain.Insumo;
import ar.edu.unse.siga.persistence.DataSourceFactory;
import ar.edu.unse.siga.service.InventarioService;
import ar.edu.unse.siga.ui.base.Ui;

import javax.swing.*;
import java.awt.*;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

public class InventoryPage extends JPanel {
    private final InventarioService service;

    private final JComboBox<Categoria> cbCategoria = new JComboBox<>();
    private final JTextField txtNombre = new JTextField(30); // mapea a descripcion
    private final JSpinner spCantidad = new JSpinner(new SpinnerNumberModel(0,0,1_000_000,1));
    private final JComboBox<String> cbEstado = new JComboBox<>(new String[]{"ACTIVO","INACTIVO"});
    private final JTextField txtUbicacion = new JTextField(25);

    public InventoryPage(InventarioService service) {
        this.service = service;
        setOpaque(false);
        setLayout(new BorderLayout());
        add(buildForm(), BorderLayout.NORTH);
        loadCategorias();
    }

    private JComponent buildForm() {
        JPanel p = new JPanel();
        p.setOpaque(false);
        p.setLayout(new GridBagLayout());
        GridBagConstraints gc = new GridBagConstraints();
        gc.insets = new Insets(8,8,8,8);
        gc.fill = GridBagConstraints.HORIZONTAL;
        gc.weightx = 1;

        JLabel title = new JLabel("Gestión de Inventario");
        title.setFont(title.getFont().deriveFont(Font.BOLD, 22f));
        gc.gridx=0; gc.gridy=0; gc.gridwidth=4;
        p.add(title, gc);

        gc.gridwidth=1;
        gc.gridy++;

        // Fila 1
        addRow(p, gc, 0, "Categoría", cbCategoria);
        addRow(p, gc, 2, "Nombre", txtNombre);

        // Fila 2
        gc.gridy++;
        addRow(p, gc, 0, "Cantidad", spCantidad);
        addRow(p, gc, 2, "Ubicación", txtUbicacion);

        // Fila 3
        gc.gridy++;
        addRow(p, gc, 0, "Estado", cbEstado);

        // Botón guardar
        gc.gridy++;
        gc.gridx=0; gc.gridwidth=4; gc.anchor=GridBagConstraints.CENTER;
        JButton btn = new JButton("Guardar");
        btn.addActionListener(e -> onSave());
        btn.setPreferredSize(new Dimension(160,36));
        p.add(btn, gc);

        return p;
    }

    private void addRow(JPanel p, GridBagConstraints gc, int col, String label, JComponent input) {
        gc.gridx = col;
        gc.weightx = 0.2;
        p.add(new JLabel(label), gc);
        gc.gridx = col+1;
        gc.weightx = 0.8;
        p.add(input, gc);
    }

    private void loadCategorias() {
        List<Categoria> list = new ArrayList<>();
        try (var cn = DataSourceFactory.getConnection();
             var ps = cn.prepareStatement("SELECT id, nombre FROM categoria ORDER BY nombre");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(new Categoria(rs.getInt("id"), rs.getString("nombre")));
        } catch(Exception e) { e.printStackTrace(); }

        DefaultComboBoxModel<Categoria> model = new DefaultComboBoxModel<>();
        list.forEach(model::addElement);
        cbCategoria.setModel(model);
    }

    private void onSave() {
        try {
            String nombre = txtNombre.getText().trim();
            if (nombre.isEmpty()) throw new IllegalArgumentException("El nombre es obligatorio");

            var cat = (Categoria) cbCategoria.getSelectedItem();
            int cant = ((Number) spCantidad.getValue()).intValue();
            String estado = (String) cbEstado.getSelectedItem();
            String ubic = txtUbicacion.getText().trim();

            // Generamos un código a partir del nombre (si no querés “auto”, agregá un campo Código)
            String codigo = "AUTO-" + nombre.toUpperCase().replaceAll("[^A-Z0-9]+","-")
                    + "-" + System.currentTimeMillis();

            Insumo i = new Insumo();
            i.setCodigo(codigo);
            i.setDescripcion(nombre);
            i.setCategoria(cat);
            i.setStockMinimo(0);
            i.setUbicacion(ubic);
            i.setEstado(estado);

            Long id = service.registrarInsumo(i);

            if (cant > 0) {
                // Registramos una ENTRADA inicial, como en el mockup
                service.registrarMovimiento(id, "ENTRADA", cant, "Alta inicial");
            }
            Ui.info(this, "Insumo guardado correctamente.");
            // Limpiar
            txtNombre.setText("");
            spCantidad.setValue(0);
            txtUbicacion.setText("");
            cbEstado.setSelectedItem("ACTIVO");
        } catch(Exception e) {
            Ui.error(this, e);
        }
    }
}
