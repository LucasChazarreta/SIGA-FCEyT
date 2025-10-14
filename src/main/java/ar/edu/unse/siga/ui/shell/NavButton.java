package ar.edu.unse.siga.ui.shell;

import ar.edu.unse.siga.ui.base.ThemeManager;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * Botón de navegación con estilo "pill".
 * - Pastilla blanca, sombra y color de texto/ícono azul cuando está seleccionado.
 * - Hover tenue cuando no está seleccionado.
 * - Soporta indentación por nivel (sub-items).
 */
public class NavButton extends JToggleButton {

    // Paleta (ajustá si querés)
    private static final Color COL_SELECTED_BG = Color.WHITE;
    private static final Color COL_SELECTED_FG = new Color(0x1E, 0x40, 0x8A); // azul título
    private static final Color COL_NORMAL_FG   = new Color(0xEAF2FF);         // texto blanco frío
    private static final Color COL_HOVER_VEIL  = new Color(255, 255, 255, 28); // velo sutil
    private static final Color COL_SHADOW      = new Color(0, 0, 0, 28);       // sombra sutil

    private static final int   RADIUS   = 18;   // radio de la pastilla
    private static final int   ICON_SZ  = 18;   // tamaño ícono
    private static final int   PAD_V    = 10;   // padding vertical
    private static final int   PAD_H    = 16;   // padding horizontal base
    private static final int   INDENT_W = 12;   // indent por nivel

    private boolean hovered = false;

    /** Usado desde ShellFrame.nav(..., level) */
    public NavButton(String text, String iconResource, int level) {
        super(text.toUpperCase());
        commonInit(iconResource, level);
    }

    /** Overload sin level por compatibilidad. */
    public NavButton(String text, String iconResource) {
        this(text, iconResource, 0);
    }

    private void commonInit(String iconResource, int level) {
        setModel(new JToggleButton.ToggleButtonModel()); // asegura toggle consistente
        setFocusPainted(false);
        setBorderPainted(false);
        setContentAreaFilled(false); // pintamos nosotros
        setOpaque(false);

        // Tipografía y espaciado
        setFont(getFont().deriveFont(Font.BOLD, 13f));
        setHorizontalAlignment(LEFT);
        setHorizontalTextPosition(RIGHT);
        setIconTextGap(12);

        // Padding con indent para sub-items
        int leftPad = PAD_H + Math.max(0, level) * INDENT_W;
        setBorder(BorderFactory.createEmptyBorder(PAD_V, leftPad, PAD_V, PAD_H));

        // Ícono SVG (si tu ThemeManager soporta tintado por color, podés pasar COL_NORMAL_FG/COL_SELECTED_FG)
        if (iconResource != null) {
            Icon ic = (Icon) ThemeManager.svg(iconResource, ICON_SZ);
            if (ic != null) setIcon(ic);
        }

        // Colores iniciales
        setForeground(COL_NORMAL_FG);

        // Cambios de estilo por selección/hover
        addChangeListener(e -> repaint());
        addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) { hovered = true; repaint(); }
            @Override public void mouseExited(MouseEvent e)  { hovered = false; repaint(); }
        });

        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    }

    @Override
    protected void paintComponent(Graphics g) {
        // Antialiasing
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int w = getWidth();
        int h = getHeight();
        int arc = RADIUS * 2;

        if (isSelected()) {
            // Sombra (ligera, desfasada)
            g2.setColor(COL_SHADOW);
            g2.fillRoundRect(2, 2, w - 4, h - 4, arc, arc);

            // Pastilla blanca
            g2.setColor(COL_SELECTED_BG);
            g2.fillRoundRect(0, 0, w, h, arc, arc);

            setForeground(COL_SELECTED_FG);
        } else {
            // Hover velo
            if (hovered) {
                g2.setColor(COL_HOVER_VEIL);
                g2.fillRoundRect(0, 0, w, h, arc, arc);
            }
            setForeground(COL_NORMAL_FG);
        }

        g2.dispose();
        super.paintComponent(g); // texto + icono por encima
    }
}
