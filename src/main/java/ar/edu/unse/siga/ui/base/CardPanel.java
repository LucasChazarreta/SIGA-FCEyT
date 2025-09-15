package ar.edu.unse.siga.ui.base;

import javax.swing.*;
import java.awt.*;

public class CardPanel extends JPanel {
    public CardPanel() {
        setOpaque(false);
        setBorder(BorderFactory.createEmptyBorder(24,24,24,24));
    }

    @Override protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int arc = 24;
        int x = 10, y = 10, w = getWidth()-20, h = getHeight()-20;

        // Sombra simple
        g2.setColor(new Color(0,0,0,35));
        g2.fillRoundRect(x+3, y+4, w, h, arc, arc);

        // Tarjeta
        g2.setColor(new Color(240, 248, 255));
        g2.fillRoundRect(x, y, w, h, arc, arc);

        g2.dispose();
        super.paintComponent(g);
    }

    @Override public boolean isOptimizedDrawingEnabled() { return false; }
}
