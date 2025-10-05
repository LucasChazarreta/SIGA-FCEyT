package ar.edu.unse.siga.ui.base;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;

public class ImageCoverPanel extends JPanel {
    private final String resourcePath;
    private BufferedImage img;

    public ImageCoverPanel(String resourcePath) {
        this.resourcePath = resourcePath;
        setOpaque(true);
        setBackground(new Color(230, 241, 252));
        try {
            img = ImageIO.read(getClass().getClassLoader().getResource(resourcePath));
        } catch (IOException | IllegalArgumentException ex) {
            ex.printStackTrace();
        }
    }

    @Override protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (img == null) return;

        int pw = getWidth(), ph = getHeight();
        int iw = img.getWidth(), ih = img.getHeight();
        double scale = Math.max(pw / (double) iw, ph / (double) ih);
        int w = (int) Math.ceil(iw * scale);
        int h = (int) Math.ceil(ih * scale);
        int x = (pw - w) / 2, y = (ph - h) / 2;

        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2.drawImage(img, x, y, w, h, null);
        g2.dispose();
    }
}
