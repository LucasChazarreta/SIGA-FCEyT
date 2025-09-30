package ar.edu.unse.siga.ui.pages;

import ar.edu.unse.siga.service.InventarioService;
import ar.edu.unse.siga.ui.base.CardPanel;

import javax.swing.*;
import java.awt.*;

public class InventoryPage extends JPanel {
    private final CardLayout cardLayout = new CardLayout();
    private final JPanel cards = new JPanel(cardLayout);

    public InventoryPage(InventarioService service) {
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

        group.add(btnRegistrar); group.add(btnModificar); group.add(btnEliminar); group.add(btnMovimiento);
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
        return buildForm("Registro de inventario", new String[][]{
                {"Código", "Ej: INS001"},
                {"Descripción", "Descripción..."},
                {"Categoría", "Seleccioná una categoría"},
                {"Stock mínimo", "0"}
        });
    }

    private CardPanel buildModificarCard() {
        return buildForm("Modificación de inventario", new String[][]{
                {"Estado", "Activo/Inactivo"},
                {"Descripción", "Descripción..."},
                {"Categoría", "Categoría..."},
                {"Stock mínimo", "0"}
        });
    }

    private CardPanel buildEliminarCard() {
        return buildForm("Eliminación de inventario", new String[][]{
                {"Código", "Código..."},
                {"Descripción", "Descripción..."},
                {"Categoría", "Categoría..."},
                {"Stock mínimo", "0"}
        });
    }

    private CardPanel buildMovimientoCard() {
        return buildForm("Registro de un movimiento", new String[][]{
                {"Código", "Código..."},
                {"Descripción", "Descripción..."},
                {"Categoría", "Categoría..."},
                {"Stock mínimo", "0"}
        });
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
