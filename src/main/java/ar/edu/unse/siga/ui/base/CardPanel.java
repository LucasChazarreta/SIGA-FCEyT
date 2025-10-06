package ar.edu.unse.siga.ui.base;

import javax.swing.*;
import java.awt.*;

/**
 * Contenedor con estilo “card”: fondo blanco, borde redondeado y sombra suave.
 */
public class CardPanel extends JPanel {

    public CardPanel() {
        setOpaque(false);
        setBorder(BorderFactory.createEmptyBorder(24, 24, 24, 24));
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int arc = 24;
        int x = 10, y = 10, w = getWidth() - 20, h = getHeight() - 20;

        // Sombra (un poco más visible)
        g2.setColor(new Color(10, 40, 90, 40));
        g2.fillRoundRect(x + 4, y + 6, w, h, arc, arc);

        // Tarjeta
        g2.setColor(new Color(250, 253, 255));
        g2.fillRoundRect(x, y, w, h, arc, arc);

        // Línea de borde muy suave
        g2.setColor(new Color(255, 255, 255, 170));
        g2.drawRoundRect(x, y, w, h, arc, arc);

        g2.dispose();
        super.paintComponent(g);
    }

    @Override
    public boolean isOptimizedDrawingEnabled() {
        return false;
    }
}
