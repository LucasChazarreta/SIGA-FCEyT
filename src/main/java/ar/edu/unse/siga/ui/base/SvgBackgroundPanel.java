package ar.edu.unse.siga.ui.base;

import com.formdev.flatlaf.extras.FlatSVGIcon;

import javax.swing.*;
import java.awt.*;

/**
 * Pinta un SVG como fondo "cover" (llena todo el panel manteniendo proporción).
 */
public class SvgBackgroundPanel extends JPanel {
    private final String resourcePath; // ej: "hero/login.svg"
    private FlatSVGIcon baseIcon;

    public SvgBackgroundPanel(String resourcePath) {
        this.resourcePath = resourcePath;
        setOpaque(true);
        setBackground(new Color(210, 227, 244)); // color de respaldo si el SVG tarda
        // Cargamos un icono base (tamaño arbitrario); lo escalamos manualmente al pintar
        baseIcon = new FlatSVGIcon(resourcePath, 1024, 1024);
    }

    @Override protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (baseIcon == null) return;

        int pw = getWidth();
        int ph = getHeight();
        if (pw <= 0 || ph <= 0) return;

        // Tamaño intrínseco del SVG “base”
        int iw = baseIcon.getIconWidth();
        int ih = baseIcon.getIconHeight();

        // Escala para cubrir todo (cover)
        double scale = Math.max(pw / (double) iw, ph / (double) ih);
        int drawW = (int) Math.ceil(iw * scale);
        int drawH = (int) Math.ceil(ih * scale);

        // Centrado
        int x = (pw - drawW) / 2;
        int y = (ph - drawH) / 2;

        // Derivar ícono a ese tamaño y pintarlo
        FlatSVGIcon scaled = baseIcon.derive(drawW, drawH);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        scaled.paintIcon(this, g2, x, y);
        g2.dispose();
    }
}
