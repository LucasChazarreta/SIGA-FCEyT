package ar.edu.unse.siga.ui.base;

import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatLightLaf;
import com.formdev.flatlaf.extras.FlatSVGIcon;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.plaf.BorderUIResource;
import java.awt.*;

public final class ThemeManager {

    private static boolean dark = false;

    private ThemeManager() {
    }

    public static void installDefaults() {
        // Fondo general (no blanco puro)
        UIManager.put("Panel.background", new Color(245, 248, 255));

        // Campos de texto visibles
        Color fieldBg = Color.WHITE;
        Color fieldBor = new Color(200, 210, 235); // gris azulado
        Color fieldFg = new Color(30, 45, 92);    // texto
        Color focusBor = new Color(58, 96, 224);   // foco azul

        EmptyBorder pad = new EmptyBorder(10, 14, 10, 14);
        LineBorder line = new LineBorder(fieldBor, 1, true);

        // TextField / FormattedTextField con padding + borde redondeado
        UIManager.put("TextField.background", fieldBg);
        UIManager.put("TextField.foreground", fieldFg);
        UIManager.put("TextField.border",
                new BorderUIResource.CompoundBorderUIResource(line, pad));

        UIManager.put("FormattedTextField.background", fieldBg);
        UIManager.put("FormattedTextField.foreground", fieldFg);
        UIManager.put("FormattedTextField.border",
                new BorderUIResource.CompoundBorderUIResource(line, pad));

        // TextArea: borde redondeado simple (no hace falta UIResource)
        UIManager.put("TextArea.background", fieldBg);
        UIManager.put("TextArea.foreground", fieldFg);
        UIManager.put("TextArea.border", new LineBorder(fieldBor, 1, true));

        UIManager.put("ComboBox.background", fieldBg);
        UIManager.put("ComboBox.foreground", fieldFg);
        UIManager.put("Spinner.background", fieldBg);
        UIManager.put("Spinner.foreground", fieldFg);

        // Colores de foco globales
        UIManager.put("Component.focusColor", focusBor);
        UIManager.put("TextField.caretForeground", fieldFg);

        // Placeholder más oscuro
        UIManager.put("TextField.inactiveForeground", new Color(130, 140, 160));
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

    public static boolean isDark() {
        return dark;
    }

    public static Icon svg(String path, int size) {
        return new FlatSVGIcon(path, size, size);
    }

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
