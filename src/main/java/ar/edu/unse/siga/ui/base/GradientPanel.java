package ar.edu.unse.siga.ui.base;

import javax.swing.*;
import java.awt.*;

public class GradientPanel extends JPanel {
    public GradientPanel() { setOpaque(false); }
    @Override protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        int w = getWidth(), h = getHeight();
        Color c1 = new Color(236, 245, 255);
        Color c2 = new Color(214, 229, 255);
        g2.setPaint(new GradientPaint(0, 0, c1, 0, h, c2));
        g2.fillRect(0, 0, w, h);
        g2.dispose();
    }
}
