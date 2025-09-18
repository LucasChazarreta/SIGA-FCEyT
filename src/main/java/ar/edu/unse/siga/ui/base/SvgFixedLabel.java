package ar.edu.unse.siga.ui.base;

import com.formdev.flatlaf.extras.FlatSVGIcon;
import javax.swing.*;
import java.awt.*;

/**
 * Renderiza un SVG a un ANCHO FIJO (mantiene proporción del SVG real).
 * No cambia de tamaño si se redimensiona el contenedor.
 */
public class SvgFixedLabel extends JLabel {
    private final String path;
    private final int targetWidth;

    public SvgFixedLabel(String path, int targetWidth) {
        this.path = path;
        this.targetWidth = targetWidth;
        setOpaque(false);
        setHorizontalAlignment(LEFT);
        buildIcon();
    }

    private void buildIcon() {
        // Cargamos sin tamaño para leer la relación de aspecto verdadera del SVG
        FlatSVGIcon base = new FlatSVGIcon(path);
        int iw = Math.max(1, base.getIconWidth());
        int ih = Math.max(1, base.getIconHeight());
        double ratio = ih / (double) iw;

        int w = targetWidth;
        int h = (int) Math.round(w * ratio);

        FlatSVGIcon scaled = new FlatSVGIcon(path, w, h);
        setIcon(scaled);

        Dimension d = new Dimension(w, h);
        setPreferredSize(d);
        setMinimumSize(d);
        setMaximumSize(d);
    }
}
