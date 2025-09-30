package ar.edu.unse.siga.ui.shell;

import ar.edu.unse.siga.ui.base.ThemeManager;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;

public class NavButton extends JToggleButton implements ChangeListener {

    private static final Color SELECTED_BG = Color.WHITE;
    private static final Color SELECTED_FG = new Color(20, 67, 140);
    private static final Color NORMAL_FG = new Color(245, 247, 255);
    private static final Color HOVER_BG = new Color(255, 255, 255, 60);

    public NavButton(String text, String iconPath) {
        super("  " + text);
        if (iconPath != null && !iconPath.isBlank()) {
            setIcon((Icon) ThemeManager.svg(iconPath, 18));
        }
        setHorizontalAlignment(LEFT);
        setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 16));
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        setFocusPainted(false);
        setOpaque(true);
        setBackground(new Color(255, 255, 255, 20));
        setForeground(NORMAL_FG);
        setFont(getFont().deriveFont(Font.BOLD, 14f));
        setIconTextGap(12);
        putClientProperty("JButton.buttonType", "toolBarButton");
        putClientProperty("JComponent.minimumWidth", 180);
        putClientProperty("JComponent.minimumHeight", 46);
        addChangeListener(this);
    }

    @Override
    public void stateChanged(ChangeEvent e) {
        if (isSelected()) {
            setBackground(SELECTED_BG);
            setForeground(SELECTED_FG);
        } else if (getModel().isRollover()) {
            setBackground(HOVER_BG);
            setForeground(Color.WHITE);
        } else {
            setBackground(new Color(255, 255, 255, 20));
            setForeground(NORMAL_FG);
        }
    }
}
