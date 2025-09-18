package ar.edu.unse.siga.ui.shell;

import ar.edu.unse.siga.ui.base.ThemeManager;

import javax.swing.*;
import java.awt.*;

public class NavButton extends JToggleButton {
    public NavButton(String text, String iconPath) {
        super("  " + text);
        setIcon((Icon) ThemeManager.svg(iconPath,16));
        setFocusPainted(false);
        setHorizontalAlignment(LEFT);
        setBorder(BorderFactory.createEmptyBorder(10,16,10,12));
        putClientProperty("JButton.buttonType", "toolBarButton"); // FlatLaf
    }
}
