package ar.edu.unse.siga.ui.base;

import javax.swing.*;
import java.awt.*;

/**
 * Botón de navegación para el sidebar. Es un JToggleButton para que ButtonGroup
 * maneje la selección exclusiva.
 */
public class NavButton extends JToggleButton {

    private static final Color BG_NORMAL = new Color(255, 255, 255, 30);
    private static final Color BG_HOVER = new Color(255, 255, 255, 50);
    private static final Color BG_SELECTED = new Color(255, 255, 255, 110);
    private static final Color BORDER_COL = new Color(0, 0, 0, 28);
    private static final Color FG_TEXT = new Color(245, 248, 255);

    public NavButton(String text, String iconPath, int level) {
        super(text, ThemeManager.svg(iconPath, 18));
        setModel(new ToggleButtonModel());          // Asegura modelo toggle
        setHorizontalAlignment(LEFT);
        setIconTextGap(8);
        setFocusPainted(false);
        setBorderPainted(false);
        setContentAreaFilled(false);
        setOpaque(false);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        setForeground(Color.WHITE);

        // padding y sangría opcional (para subniveles)
        int left = 10 + Math.max(0, level) * 12;
        setBorder(BorderFactory.createEmptyBorder(8, left, 8, 12));
    }

    public NavButton(String text, String iconPath) {
        this(text, iconPath, 0);
    }

    @Override
    protected void paintComponent(Graphics g) {
        int w = getWidth();
        int h = getHeight();
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Fondo según estado
        Color bg = isSelected() ? BG_SELECTED
                : (getModel().isRollover() ? BG_HOVER : BG_NORMAL);

        g2.setColor(bg);
        int arc = 10;
        g2.fillRoundRect(0, 0, w, h, arc, arc);

        g2.dispose();
        super.paintComponent(g);
    }
}
