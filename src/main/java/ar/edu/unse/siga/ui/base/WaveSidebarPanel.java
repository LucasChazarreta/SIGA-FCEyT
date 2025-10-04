package ar.edu.unse.siga.ui.base;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Path2D;

/**
 * Sidebar panel with a soft blue gradient background and abstract wave
 * decorations inspired by the new SIGA mockups.  It paints the gradient
 * once and reuses it for the navigation column so that any content placed
 * inside the panel keeps the translucent waves behind it.
 */
public class WaveSidebarPanel extends JPanel {

    private final Color top = new Color(26, 86, 198);
    private final Color bottom = new Color(24, 61, 169);

    private final Color waveLight = new Color(255, 255, 255, 60);
    private final Color waveDark = new Color(15, 77, 170, 140);

    public WaveSidebarPanel() {
        setOpaque(false);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        int w = getWidth();
        int h = getHeight();

        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Base gradient.
        var gradient = new GradientPaint(0, 0, top, 0, h, bottom);
        g2.setPaint(gradient);
        g2.fillRect(0, 0, w, h);

        // First light wave.
        Path2D wave1 = new Path2D.Double();
        wave1.moveTo(0, h * 0.15);
        wave1.curveTo(w * 0.35, h * 0.05, w * 0.65, h * 0.32, w, h * 0.18);
        wave1.lineTo(w, 0);
        wave1.lineTo(0, 0);
        wave1.closePath();
        g2.setColor(waveLight);
        g2.fill(wave1);

        // Second darker wave (bottom).
        Path2D wave2 = new Path2D.Double();
        wave2.moveTo(0, h);
        wave2.curveTo(w * 0.25, h * 0.82, w * 0.55, h * 0.92, w, h * 0.7);
        wave2.lineTo(w, h);
        wave2.closePath();
        g2.setColor(waveDark);
        g2.fill(wave2);

        g2.dispose();
    }
}

