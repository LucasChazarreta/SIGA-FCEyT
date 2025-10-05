package ar.edu.unse.siga.ui.base;

import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatLightLaf;
import com.formdev.flatlaf.extras.FlatSVGIcon;

import javax.swing.*;
import java.awt.*;

public final class ThemeManager {
    private static boolean dark = false;
    private ThemeManager(){}

    public static void installDefaults() {
        UIManager.put("defaultFont", new Font("Segoe UI", Font.PLAIN, 13));
        UIManager.put("Component.arc", 12);
        UIManager.put("Button.arc", 12);
        UIManager.put("TextComponent.arc", 10);
        UIManager.put("ScrollBar.thumbArc", 8);
        setLight();
    }

    public static void setLight() { dark = false; FlatLightLaf.setup(); updateIfPossible(); }
    public static void setDark()  { dark = true;  FlatDarkLaf.setup();  updateIfPossible(); }
    public static boolean isDark(){ return dark; }

    public static Icon svg(String path, int size) { return new FlatSVGIcon(path, size, size); }

    private static void updateIfPossible() {
        SwingUtilities.invokeLater(() -> {
            for (Window w : Window.getWindows()) {
                if (w != null && w.isDisplayable()) {
                    SwingUtilities.updateComponentTreeUI(w);
                    w.pack();
                }
            }
        });
    }
}
