package ar.edu.unse.siga.ui.base;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Path2D;

/**
 * Sidebar con gradiente azul y ondas suaves. Contraste mejorado para que los
 * controles blancos se lean bien.
 */
public class WaveSidebarPanel extends JPanel {

    // Azul más contrastado
    private final Color top = new Color(17, 57, 150);
    private final Color bottom = new Color(14, 48, 140);

    private final Color waveLight = new Color(255, 255, 255, 40);
    private final Color waveDark = new Color(255, 255, 255, 25);

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

        // Onda superior clara
        Path2D wave1 = new Path2D.Double();
        wave1.moveTo(0, h * 0.17);
        wave1.curveTo(w * 0.35, h * 0.07, w * 0.65, h * 0.30, w, h * 0.16);
        wave1.lineTo(w, 0);
        wave1.lineTo(0, 0);
        wave1.closePath();
        g2.setColor(waveLight);
        g2.fill(wave1);

        // Onda inferior muy suave
        Path2D wave2 = new Path2D.Double();
        wave2.moveTo(0, h);
        wave2.curveTo(w * 0.25, h * 0.82, w * 0.55, h * 0.92, w, h * 0.70);
        wave2.lineTo(w, h);
        wave2.closePath();
        g2.setColor(waveDark);
        g2.fill(wave2);

        g2.dispose();
    }
}
