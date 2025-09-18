package ar.edu.unse.siga.ui.base;

import com.formdev.flatlaf.extras.FlatSVGIcon;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;

/**
 * Panel que muestra un SVG centrado y lo escala manteniendo la relación de aspecto.
 * Usa FlatSVGIcon con una instancia que se re-crea al redimensionar.
 */
public class ScalableSvgPanel extends JPanel {
    private final String resourcePath; // ej: "hero/home.svg"
    private JLabel imageLabel;
    private int maxPadding = 48; // margen respirable alrededor

    public ScalableSvgPanel(String resourcePath) {
        this.resourcePath = resourcePath;
        setOpaque(false);
        setLayout(new GridBagLayout());
        imageLabel = new JLabel();
        imageLabel.setHorizontalAlignment(SwingConstants.CENTER);
        imageLabel.setVerticalAlignment(SwingConstants.CENTER);
        add(imageLabel, new GridBagConstraints());

        // primera carga perezosa (cuando tenga tamaño válido)
        addComponentListener(new ComponentAdapter() {
            @Override public void componentShown(ComponentEvent e) { refreshIcon(); }
            @Override public void componentResized(ComponentEvent e) { refreshIcon(); }
        });
    }

    public void setMaxPadding(int px) {
        this.maxPadding = Math.max(0, px);
        refreshIcon();
    }

    private void refreshIcon() {
        int w = getWidth();
        int h = getHeight();
        if (w <= 0 || h <= 0) return;

        // Dejo padding para que no “toque” bordes
        int usableW = Math.max(50, w - maxPadding * 2);
        int usableH = Math.max(50, h - maxPadding * 2);

        // Proporción objetivo: ancho dominante (pero no exceder alto)
        // Elegimos un tamaño “cuadrado” razonable basados en el espacio
        int target = Math.min(usableW, usableH);

        // Si querés limitar un máximo absoluto (por ejemplo 1000 px):
        target = Math.min(target, 1000);

        // Recreo el icono con ancho/alto iguales (mantiene aspecto interno del SVG)
        FlatSVGIcon icon = new FlatSVGIcon(resourcePath, target, target);
        imageLabel.setIcon(icon);
        revalidate();
        repaint();
    }
}
