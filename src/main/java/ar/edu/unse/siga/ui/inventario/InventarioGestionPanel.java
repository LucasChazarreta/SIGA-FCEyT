package ar.edu.unse.siga.ui.inventario;

import ar.edu.unse.siga.service.InventarioService;

import javax.swing.*;
import java.awt.*;

public class InventarioGestionPanel extends JPanel {

    private static final String CARD_REG = "reg";
    private static final String CARD_MOD = "mod";
    private static final String CARD_DEL = "del";

    public InventarioGestionPanel(InventarioService service) {
        setLayout(new BorderLayout());
        setOpaque(false);

        var title = new JLabel("GESTIÓN DE INVENTARIO");
        title.setFont(title.getFont().deriveFont(Font.BOLD, 28f));
        title.setBorder(BorderFactory.createEmptyBorder(16,16,8,16));
        add(title, BorderLayout.NORTH);

        // botones
        var actions = new JPanel(new FlowLayout(FlowLayout.CENTER, 18, 12));
        var bReg = new JButton("CARGAR");
        var bMod = new JButton("MODIFICAR");
        var bDel = new JButton("ELIMINAR");
        actions.add(bReg); actions.add(bMod); actions.add(bDel);
        add(actions, BorderLayout.BEFORE_FIRST_LINE);

        // cards
        var cards = new JPanel(new CardLayout());
        cards.add(new RegistroInsumoPanel(service), CARD_REG);
        cards.add(new ModificarInsumoPanel(service), CARD_MOD);
        cards.add(new EliminarInsumoPanel(service), CARD_DEL);
        add(cards, BorderLayout.CENTER);

        // wiring
        var cl = (CardLayout) cards.getLayout();
        bReg.addActionListener(e -> cl.show(cards, CARD_REG));
        bMod.addActionListener(e -> cl.show(cards, CARD_MOD));
        bDel.addActionListener(e -> cl.show(cards, CARD_DEL));

        // default
        cl.show(cards, CARD_REG);
    }
}
