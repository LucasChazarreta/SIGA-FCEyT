package ar.edu.unse.siga.ui.pages;

import ar.edu.unse.siga.service.InventarioService;
import ar.edu.unse.siga.ui.base.CardPanel;

import javax.swing.*;
import java.awt.*;

import ar.edu.unse.siga.domain.Insumo;
import ar.edu.unse.siga.domain.Categoria;
import ar.edu.unse.siga.persistence.jdbc.JdbcInsumoDao;
import ar.edu.unse.siga.persistence.jdbc.JdbcCategoriaDao;
import ar.edu.unse.siga.ui.base.Ui;
import javax.swing.SpinnerNumberModel;

public class InventoryPage extends JPanel {

    private final InventarioService service;   // <--- agregar
    private final CardLayout cardLayout = new CardLayout();
    private final JPanel cards = new JPanel(cardLayout);

    public InventoryPage(InventarioService service) {
        this.service = service;
        setOpaque(false);
        setLayout(new BorderLayout(16, 16));

        add(buildHeader(), BorderLayout.NORTH);
        add(buildCardHolder(), BorderLayout.CENTER);
    }

    private JComponent buildHeader() {
        JPanel header = new JPanel(new BorderLayout(12, 12));
        header.setOpaque(false);

        JLabel title = new JLabel("Gestión de inventario");
        title.setForeground(new Color(24, 63, 150));
        title.setFont(title.getFont().deriveFont(Font.BOLD, 28f));
        header.add(title, BorderLayout.WEST);

        return header;
    }

    private JComponent buildCardHolder() {
        JPanel wrapper = new JPanel(new BorderLayout(16, 24));
        wrapper.setOpaque(false);

        ButtonGroup group = new ButtonGroup();
        var bar = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        bar.setOpaque(false);

        JToggleButton btnRegistrar = pillButton("Cargar");
        JToggleButton btnModificar = pillButton("Modificar");
        JToggleButton btnEliminar = pillButton("Eliminar");
        JToggleButton btnMovimiento = pillButton("Registrar movimiento");

        group.add(btnRegistrar);
        group.add(btnModificar);
        group.add(btnEliminar);
        group.add(btnMovimiento);
        btnRegistrar.setSelected(true);

        bar.add(btnRegistrar);
        bar.add(btnModificar);
        bar.add(btnEliminar);
        bar.add(btnMovimiento);

        wrapper.add(bar, BorderLayout.NORTH);

        cards.setOpaque(false);
        cards.add(buildRegistroCard(), "reg");
        cards.add(buildModificarCard(), "mod");
        cards.add(buildEliminarCard(), "del");
        cards.add(buildMovimientoCard(), "mov");

        btnRegistrar.addActionListener(e -> cardLayout.show(cards, "reg"));
        btnModificar.addActionListener(e -> cardLayout.show(cards, "mod"));
        btnEliminar.addActionListener(e -> cardLayout.show(cards, "del"));
        btnMovimiento.addActionListener(e -> cardLayout.show(cards, "mov"));

        wrapper.add(cards, BorderLayout.CENTER);
        cardLayout.show(cards, "reg");

        return wrapper;
    }

    private CardPanel buildRegistroCard() {
        CardPanel card = new CardPanel();
        card.setLayout(new BorderLayout(12, 16));

        JLabel lbl = new JLabel("REGISTRO DE INVENTARIO");
        lbl.setFont(lbl.getFont().deriveFont(Font.BOLD, 16f));
        lbl.setForeground(new Color(70, 96, 180));
        card.add(lbl, BorderLayout.NORTH);

        // --- Campos
        JTextField txtCodigo = new JTextField();
        txtCodigo.putClientProperty("JTextField.placeholderText", "Ej: INS001");
        txtCodigo.putClientProperty("JComponent.roundRect", true);

        JTextField txtDescripcion = new JTextField();
        txtDescripcion.putClientProperty("JTextField.placeholderText", "Descripción...");
        txtDescripcion.putClientProperty("JComponent.roundRect", true);

        // >>> CATEGORÍA COMO COMBO (llenado desde BD)
        var catDao = new JdbcCategoriaDao();
        var categorias = catDao.listAll(); // usa toString() de Categoria -> muestra el nombre
        JComboBox<Categoria> cbCategoria = new JComboBox<>(categorias.toArray(new Categoria[0]));
        cbCategoria.putClientProperty("JComponent.roundRect", true);
        if (cbCategoria.getItemCount() > 0) {
            cbCategoria.setSelectedIndex(0);
        }

        // Ubicación
        JTextField txtUbicacion = new JTextField();
        txtUbicacion.putClientProperty("JTextField.placeholderText", "Depósito 1, Estante A, etc.");
        txtUbicacion.putClientProperty("JComponent.roundRect", true);

        // Stock mínimo
        JSpinner spStockMin = new JSpinner(new SpinnerNumberModel(0, 0, 999999, 1));

        JPanel form = new JPanel(new GridLayout(0, 2, 18, 18));
        form.setOpaque(false);
        form.add(labeled("CÓDIGO", txtCodigo));
        form.add(labeled("DESCRIPCIÓN", txtDescripcion));
        form.add(labeled("CATEGORÍA", cbCategoria));   // ahora es un combo
        form.add(labeled("UBICACIÓN", txtUbicacion));  // nuevo campo
        form.add(labeled("STOCK MÍNIMO", spStockMin));
        card.add(form, BorderLayout.CENTER);

        // --- Botón Aceptar
        JButton btn = new JButton("Aceptar");
        btn.setPreferredSize(new Dimension(160, 40));
        btn.setBackground(new Color(58, 96, 224));
        btn.setForeground(Color.WHITE);
        btn.setFocusPainted(false);

        btn.addActionListener(e -> {
            try {
                String codigo = txtCodigo.getText().trim();
                String desc = txtDescripcion.getText().trim();
                Categoria cat = (Categoria) cbCategoria.getSelectedItem();
                String ubic = txtUbicacion.getText().trim();
                int stockMin = (Integer) spStockMin.getValue();

                if (codigo.isEmpty() || desc.isEmpty()) {
                    throw new IllegalArgumentException("Código y descripción son obligatorios.");
                }

                if (cat == null) {
                    throw new IllegalArgumentException("Seleccioná una categoría.");
                }

                Insumo ins = new Insumo();
                ins.setCodigo(codigo);
                ins.setDescripcion(desc);
                ins.setCategoria(cat);
                ins.setUbicacion(ubic);        // <<<< se guarda la ubicación
                ins.setStockMinimo(stockMin);
                ins.setEstado("ACTIVO");

                Long id = new JdbcInsumoDao().create(ins);
                Ui.info(this, "Insumo guardado. ID = " + id);

                // limpiar
                txtCodigo.setText("");
                txtDescripcion.setText("");
                if (cbCategoria.getItemCount() > 0) {
                    cbCategoria.setSelectedIndex(0);
                }
                txtUbicacion.setText("");
                spStockMin.setValue(0);
            } catch (Exception ex) {
                Ui.error(this, ex);
            }
        });

        JPanel south = new JPanel(new FlowLayout(FlowLayout.CENTER));
        south.setOpaque(false);
        south.add(btn);
        card.add(south, BorderLayout.SOUTH);

        return card;
    }

    private CardPanel buildEliminarCard() {
        CardPanel card = new CardPanel();
        card.setLayout(new BorderLayout(12, 16));

        JLabel lbl = new JLabel("ELIMINACIÓN DE INVENTARIO");
        lbl.setFont(lbl.getFont().deriveFont(Font.BOLD, 16f));
        lbl.setForeground(new Color(180, 70, 70));
        card.add(lbl, BorderLayout.NORTH);

        // ---- Búsqueda por código
        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        top.setOpaque(false);
        JTextField txtBuscarCodigo = new JTextField(18);
        txtBuscarCodigo.putClientProperty("JTextField.placeholderText", "Código a buscar (ej: INS001)");
        JButton btnBuscar = new JButton("Buscar");
        top.add(new JLabel("Código:"));
        top.add(txtBuscarCodigo);
        top.add(btnBuscar);
        card.add(top, BorderLayout.PAGE_START);

        // ---- Campos solo lectura para mostrar el insumo encontrado
        JTextField txtCodigo = new JTextField();
        txtCodigo.setEditable(false);
        JTextField txtDescripcion = new JTextField();
        txtDescripcion.setEditable(false);
        JComboBox<Categoria> cbCategoria = new JComboBox<>();
        cbCategoria.setEnabled(false);
        JTextField txtUbicacion = new JTextField();
        txtUbicacion.setEditable(false);
        JSpinner spStockMin = new JSpinner(new SpinnerNumberModel(0, 0, 999999, 1));
        spStockMin.setEnabled(false);
        JComboBox<String> cbEstado = new JComboBox<>(new String[]{"ACTIVO", "INACTIVO"});
        cbEstado.setEnabled(false);

        // cargar categorías para visualizar nombre
        for (Categoria c : new JdbcCategoriaDao().listAll()) {
            cbCategoria.addItem(c);
        }
        if (cbCategoria.getItemCount() > 0) {
            cbCategoria.setSelectedIndex(0);
        }

        JPanel form = new JPanel(new GridLayout(0, 2, 18, 18));
        form.setOpaque(false);
        form.add(labeled("CÓDIGO", txtCodigo));
        form.add(labeled("DESCRIPCIÓN", txtDescripcion));
        form.add(labeled("CATEGORÍA", cbCategoria));
        form.add(labeled("UBICACIÓN", txtUbicacion));
        form.add(labeled("STOCK MÍNIMO", spStockMin));
        form.add(labeled("ESTADO", cbEstado));
        card.add(form, BorderLayout.CENTER);

        // Mantener el insumo hallado
        final Insumo[] current = new Insumo[1];

        // Buscar
        btnBuscar.addActionListener(e -> {
            try {
                String codigo = txtBuscarCodigo.getText().trim();
                if (codigo.isEmpty()) {
                    Ui.warn(InventoryPage.this, "Ingresá un código para buscar.");
                    return;
                }
                var dao = new JdbcInsumoDao();
                var opt = dao.findByCodigo(codigo);
                if (opt.isEmpty()) {
                    Ui.warn(InventoryPage.this, "No se encontró un insumo con código: " + codigo);
                    current[0] = null;
                    txtCodigo.setText("");
                    txtDescripcion.setText("");
                    txtUbicacion.setText("");
                    spStockMin.setValue(0);
                    if (cbCategoria.getItemCount() > 0) {
                        cbCategoria.setSelectedIndex(0);
                    }
                    cbEstado.setSelectedIndex(0);
                    return;
                }
                // cargar datos en pantalla
                current[0] = opt.get();
                txtCodigo.setText(current[0].getCodigo());
                txtDescripcion.setText(current[0].getDescripcion() == null ? "" : current[0].getDescripcion());
                txtUbicacion.setText(current[0].getUbicacion() == null ? "" : current[0].getUbicacion());
                Integer sm = current[0].getStockMinimo();
                spStockMin.setValue(sm == null ? 0 : sm);

                var cat = current[0].getCategoria();
                if (cat != null) {
                    for (int i = 0; i < cbCategoria.getItemCount(); i++) {
                        if (cbCategoria.getItemAt(i).getId() == cat.getId()) {
                            cbCategoria.setSelectedIndex(i);
                            break;
                        }
                    }
                }
                cbEstado.setSelectedItem(current[0].getEstado() == null ? "ACTIVO" : current[0].getEstado());
            } catch (Exception ex) {
                Ui.error(InventoryPage.this, ex);
            }
        });

        // ---- Botón Eliminar (baja lógica)
        JButton btnEliminar = new JButton("Eliminar");
        btnEliminar.setPreferredSize(new Dimension(140, 40));
        btnEliminar.setBackground(new Color(200, 60, 60));
        btnEliminar.setForeground(Color.WHITE);
        btnEliminar.setFocusPainted(false);

        btnEliminar.addActionListener(e -> {
            try {
                if (current[0] == null) {
                    Ui.warn(InventoryPage.this, "Primero buscá un insumo por código.");
                    return;
                }
                String cod = current[0].getCodigo();
                if (!Ui.confirm(InventoryPage.this, "¿Confirmás la eliminación (baja) de " + cod + "?")) {
                    return;
                }
                new JdbcInsumoDao().softDelete(current[0].getId());
                Ui.info(InventoryPage.this, "El insumo " + cod + " fue dado de baja (INACTIVO).");

                // limpiar UI
                current[0] = null;
                txtCodigo.setText("");
                txtDescripcion.setText("");
                txtUbicacion.setText("");
                spStockMin.setValue(0);
                if (cbCategoria.getItemCount() > 0) {
                    cbCategoria.setSelectedIndex(0);
                }
                cbEstado.setSelectedIndex(0);
                txtBuscarCodigo.setText("");
            } catch (Exception ex) {
                Ui.error(InventoryPage.this, ex);
            }
        });

        JPanel south = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        south.setOpaque(false);
        south.add(btnEliminar);
        card.add(south, BorderLayout.SOUTH);

        return card;
    }

    private CardPanel buildMovimientoCard() {
        CardPanel card = new CardPanel();
        card.setLayout(new BorderLayout(12, 16));

        JLabel lbl = new JLabel("REGISTRO DE MOVIMIENTO");
        lbl.setFont(lbl.getFont().deriveFont(Font.BOLD, 16f));
        lbl.setForeground(new Color(70, 96, 180));
        card.add(lbl, BorderLayout.NORTH);

        // --- Campos
        JComboBox<Insumo> cbInsumo = new JComboBox<>();
        // cargar insumos desde el servicio
        for (Insumo i : service.listarTodos()) {
            cbInsumo.addItem(i);
        }
        if (cbInsumo.getItemCount() > 0) {
            cbInsumo.setSelectedIndex(0);
        }

        JComboBox<String> cbTipo = new JComboBox<>(new String[]{"ENTRADA", "SALIDA"});
        JSpinner spCantidad = new JSpinner(new SpinnerNumberModel(1, 1, 999999, 1));
        JTextField txtDestinoFuente = new JTextField(); // “destino” si SALIDA / “fuente” si ENTRADA

        JPanel form = new JPanel(new GridLayout(0, 2, 18, 18));
        form.setOpaque(false);
        form.add(labeled("INSUMO", cbInsumo));
        form.add(labeled("TIPO", cbTipo));
        form.add(labeled("CANTIDAD", spCantidad));
        form.add(labeled("DESTINO / FUENTE", txtDestinoFuente));
        card.add(form, BorderLayout.CENTER);

        // --- Botón Registrar
        JButton btnRegistrar = new JButton("Registrar");
        btnRegistrar.setPreferredSize(new Dimension(140, 40));
        btnRegistrar.setBackground(new Color(58, 96, 224));
        btnRegistrar.setForeground(Color.WHITE);
        btnRegistrar.setFocusPainted(false);

        btnRegistrar.addActionListener(e -> {
            try {
                if (cbInsumo.getSelectedItem() == null) {
                    Ui.warn(InventoryPage.this, "No hay insumos para registrar.");
                    return;
                }
                Insumo insumo = (Insumo) cbInsumo.getSelectedItem();
                String tipo = (String) cbTipo.getSelectedItem();
                int cantidad = (Integer) spCantidad.getValue();
                String destinoFuente = txtDestinoFuente.getText().trim();

                if (destinoFuente.isEmpty()) {
                    Ui.warn(InventoryPage.this, "Indicá el destino (salida) o la fuente (entrada).");
                    return;
                }

                // Lógica de negocio: valida stock si es SALIDA e inserta movimiento
                Long id = service.registrarMovimiento(insumo.getId(), tipo, cantidad, destinoFuente);
                Ui.info(InventoryPage.this, "Movimiento registrado. ID = " + id);

                // Limpiar
                cbTipo.setSelectedIndex(0);
                spCantidad.setValue(1);
                txtDestinoFuente.setText("");
            } catch (Exception ex) {
                Ui.error(InventoryPage.this, ex);
            }
        });

        JPanel south = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        south.setOpaque(false);
        south.add(btnRegistrar);
        card.add(south, BorderLayout.SOUTH);

        return card;
    }

    private CardPanel buildModificarCard() {
        CardPanel card = new CardPanel();
        card.setLayout(new BorderLayout(12, 16));

        JLabel lbl = new JLabel("MODIFICACIÓN DE INVENTARIO");
        lbl.setFont(lbl.getFont().deriveFont(Font.BOLD, 16f));
        lbl.setForeground(new Color(70, 96, 180));
        card.add(lbl, BorderLayout.NORTH);

        // ---- Búsqueda por código
        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        top.setOpaque(false);
        JTextField txtBuscarCodigo = new JTextField(18);
        txtBuscarCodigo.putClientProperty("JTextField.placeholderText", "Código a buscar (ej: INS001)");
        JButton btnBuscar = new JButton("Buscar");
        top.add(new JLabel("Código:"));
        top.add(txtBuscarCodigo);
        top.add(btnBuscar);
        card.add(top, BorderLayout.PAGE_START);

        // ---- Campos editables
        JTextField txtCodigo = new JTextField(); // solo lectura
        txtCodigo.setEditable(false);
        JTextField txtDescripcion = new JTextField();

        JComboBox<Categoria> cbCategoria = new JComboBox<>();
        for (Categoria c : new JdbcCategoriaDao().listAll()) {
            cbCategoria.addItem(c);
        }
        if (cbCategoria.getItemCount() > 0) {
            cbCategoria.setSelectedIndex(0);
        }

        JTextField txtUbicacion = new JTextField();
        JSpinner spStockMin = new JSpinner(new SpinnerNumberModel(0, 0, 999999, 1));
        JComboBox<String> cbEstado = new JComboBox<>(new String[]{"ACTIVO", "INACTIVO"});

        JPanel form = new JPanel(new GridLayout(0, 2, 18, 18));
        form.setOpaque(false);
        form.add(labeled("CÓDIGO", txtCodigo));
        form.add(labeled("DESCRIPCIÓN", txtDescripcion));
        form.add(labeled("CATEGORÍA", cbCategoria));
        form.add(labeled("UBICACIÓN", txtUbicacion));
        form.add(labeled("STOCK MÍNIMO", spStockMin));
        form.add(labeled("ESTADO", cbEstado));
        card.add(form, BorderLayout.CENTER);

        // Mantener el insumo hallado
        final Insumo[] current = new Insumo[1];

        btnBuscar.addActionListener(e -> {
            try {
                String codigo = txtBuscarCodigo.getText().trim();
                if (codigo.isEmpty()) {
                    Ui.warn(InventoryPage.this, "Ingresá un código para buscar.");
                    return;
                }
                var dao = new JdbcInsumoDao();
                var opt = dao.findByCodigo(codigo);
                if (opt.isEmpty()) {
                    Ui.warn(InventoryPage.this, "No se encontró un insumo con código: " + codigo);
                    // limpiar
                    current[0] = null;
                    txtCodigo.setText("");
                    txtDescripcion.setText("");
                    txtUbicacion.setText("");
                    spStockMin.setValue(0);
                    if (cbCategoria.getItemCount() > 0) {
                        cbCategoria.setSelectedIndex(0);
                    }
                    cbEstado.setSelectedIndex(0);
                    return;
                }
                // cargar datos
                current[0] = opt.get();
                txtCodigo.setText(current[0].getCodigo());
                txtDescripcion.setText(current[0].getDescripcion() == null ? "" : current[0].getDescripcion());
                txtUbicacion.setText(current[0].getUbicacion() == null ? "" : current[0].getUbicacion());

                Integer sm = current[0].getStockMinimo();
                spStockMin.setValue(sm == null ? 0 : sm);

                var cat = current[0].getCategoria();
                if (cat != null) {
                    for (int i = 0; i < cbCategoria.getItemCount(); i++) {
                        if (cbCategoria.getItemAt(i).getId() == cat.getId()) {
                            cbCategoria.setSelectedIndex(i);
                            break;
                        }
                    }
                }
                cbEstado.setSelectedItem(current[0].getEstado() == null ? "ACTIVO" : current[0].getEstado());
            } catch (Exception ex) {
                Ui.error(InventoryPage.this, ex);
            }
        });

        // ---- Botón Guardar
        JButton btnGuardar = new JButton("Guardar cambios");
        btnGuardar.setPreferredSize(new Dimension(180, 40));
        btnGuardar.setBackground(new Color(58, 96, 224));
        btnGuardar.setForeground(Color.WHITE);
        btnGuardar.setFocusPainted(false);

        btnGuardar.addActionListener(e -> {
            try {
                if (current[0] == null) {
                    Ui.warn(InventoryPage.this, "Primero buscá un insumo por código.");
                    return;
                }
                if (!Ui.confirm(InventoryPage.this, "¿Confirmás la modificación de " + current[0].getCodigo() + "?")) {
                    return;
                }

                current[0].setDescripcion(txtDescripcion.getText().trim());
                current[0].setCategoria((Categoria) cbCategoria.getSelectedItem());
                current[0].setUbicacion(txtUbicacion.getText().trim());
                current[0].setStockMinimo((Integer) spStockMin.getValue());
                current[0].setEstado((String) cbEstado.getSelectedItem());

                new JdbcInsumoDao().update(current[0]);
                Ui.info(InventoryPage.this, "Cambios guardados.");
            } catch (Exception ex) {
                Ui.error(InventoryPage.this, ex);
            }
        });

        JPanel south = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        south.setOpaque(false);
        south.add(btnGuardar);
        card.add(south, BorderLayout.SOUTH);

        return card;
    }

    private CardPanel buildForm(String title, String[][] fields) {
        CardPanel card = new CardPanel();
        card.setLayout(new BorderLayout(12, 16));

        JLabel lbl = new JLabel(title.toUpperCase());
        lbl.setFont(lbl.getFont().deriveFont(Font.BOLD, 16f));
        lbl.setForeground(new Color(70, 96, 180));
        card.add(lbl, BorderLayout.NORTH);

        JPanel form = new JPanel(new GridLayout(0, 2, 18, 18));
        form.setOpaque(false);

        for (String[] field : fields) {
            form.add(fieldPanel(field[0], field[1]));
        }

        card.add(form, BorderLayout.CENTER);

        JButton btn = new JButton("Aceptar");
        btn.setPreferredSize(new Dimension(160, 40));
        btn.setBackground(new Color(58, 96, 224));
        btn.setForeground(Color.WHITE);
        btn.setFocusPainted(false);

        JPanel south = new JPanel(new FlowLayout(FlowLayout.CENTER));
        south.setOpaque(false);
        south.add(btn);
        card.add(south, BorderLayout.SOUTH);

        return card;
    }

    private JPanel fieldPanel(String label, String placeholder) {
        JPanel p = new JPanel(new BorderLayout(6, 6));
        p.setOpaque(false);
        JLabel lbl = new JLabel(label.toUpperCase());
        lbl.setFont(lbl.getFont().deriveFont(Font.PLAIN, 12f));
        p.add(lbl, BorderLayout.NORTH);
        JTextField txt = new JTextField();
        txt.putClientProperty("JTextField.placeholderText", placeholder);
        txt.putClientProperty("JComponent.roundRect", true);
        txt.setPreferredSize(new Dimension(200, 40));
        p.add(txt, BorderLayout.CENTER);
        return p;
    }

    private JPanel labeled(String titulo, JComponent field) {
        JPanel p = new JPanel(new BorderLayout(6, 6));
        p.setOpaque(false);
        JLabel lbl = new JLabel(titulo);
        lbl.setFont(lbl.getFont().deriveFont(Font.PLAIN, 12f));
        p.add(lbl, BorderLayout.NORTH);
        field.setPreferredSize(new Dimension(200, 40));
        p.add(field, BorderLayout.CENTER);
        return p;
    }

    private JToggleButton pillButton(String text) {
        JToggleButton b = new JToggleButton(text.toUpperCase());
        b.setFocusPainted(false);
        b.setFont(b.getFont().deriveFont(Font.BOLD, 12f));
        b.setOpaque(true);
        b.setBackground(Color.WHITE);
        b.setForeground(new Color(66, 100, 189));
        b.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(211, 222, 255)),
                BorderFactory.createEmptyBorder(10, 22, 10, 22)
        ));
        b.putClientProperty("JComponent.minimumWidth", 140);
        b.putClientProperty("JComponent.minimumHeight", 38);
        b.addChangeListener(e -> {
            if (b.isSelected()) {
                b.setBackground(new Color(58, 96, 224));
                b.setForeground(Color.WHITE);
            } else {
                b.setBackground(Color.WHITE);
                b.setForeground(new Color(66, 100, 189));
            }
        });
        return b;
    }
}
