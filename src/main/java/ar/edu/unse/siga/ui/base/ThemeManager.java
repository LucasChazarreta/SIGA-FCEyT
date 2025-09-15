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
        // Tipografía y tamaños globales
        UIManager.put("defaultFont", new Font("Segoe UI", Font.PLAIN, 13));
        UIManager.put("Component.arc", 10);
        UIManager.put("Button.arc", 10);
        UIManager.put("TextComponent.arc", 8);
        UIManager.put("ScrollBar.thumbArc", 8);

        setLight(); // por defecto claro
    }

    public static void setLight() {
        dark = false;
        FlatLightLaf.setup();
        updateIfPossible();
    }

    public static void setDark() {
        dark = true;
        FlatDarkLaf.setup();
        updateIfPossible();
    }

    public static boolean isDark() { return dark; }

    public static Icon svg(String path, int size) {
        return new FlatSVGIcon(path, size, size);
    }

    private static void updateIfPossible() {
        // Evitar NPE cuando aún no hay ventanas creadas
        SwingUtilities.invokeLater(() -> {
            for (Window w : Window.getWindows()) {
                // Sólo actualizamos si la window ya está "realizada"
                if (w != null && w.isDisplayable()) {
                    SwingUtilities.updateComponentTreeUI(w);
                    w.pack(); // opcional: recalcula tamaños con el nuevo L&F
                }
            }
        });
    }
}
