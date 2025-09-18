package ar.edu.unse.siga.ui.base;

import com.formdev.flatlaf.extras.FlatSVGIcon;

import javax.swing.*;
import java.awt.*;

public class SvgLabel extends JLabel {
    private final String path;
    private int preferredWidth = 420;   // ancho objetivo por defecto
    private int maxWidth = 520;         // tope para pantallas grandes

    public SvgLabel(String path) {
        this.path = path;
        setOpaque(false);
        setHorizontalAlignment(LEFT);
    }

    public SvgLabel setPreferredWidth(int w) { this.preferredWidth = w; return this; }
    public SvgLabel setMaxWidth(int w)       { this.maxWidth = w;       return this; }

    @Override public Dimension getPreferredSize() {
        FlatSVGIcon tmp = new FlatSVGIcon(path, 1024, 1024);
        double ratio = tmp.getIconHeight() / (double) tmp.getIconWidth();
        int w = preferredWidth;
        int h = (int) Math.round(w * ratio);
        return new Dimension(w, h);
    }

    @Override public void addNotify() { super.addNotify(); refresh(); }
    @Override public void invalidate() { super.invalidate(); refresh(); }

    private void refresh() {
        // ancho disponible: priorizamos el ancho del padre, con topes
        int avail = preferredWidth;
        Container p = getParent();
        if (p != null) {
            int pad = 80; // margen aproximado
            avail = Math.max(200, Math.min(p.getWidth() - pad, maxWidth));
        }
        FlatSVGIcon tmp = new FlatSVGIcon(path, 1024, 1024);
        double ratio = tmp.getIconHeight() / (double) tmp.getIconWidth();
        int w = Math.max(200, avail);
        int h = (int) Math.round(w * ratio);
        setIcon(new FlatSVGIcon(path, w, h));
        revalidate();
        repaint();
    }
}
