package ar.edu.unse.siga.ui.shell;

import ar.edu.unse.siga.ui.base.ThemeManager;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;

public class NavButton extends JToggleButton implements ChangeListener {

    private static final Color LEVEL0_SELECTED_BG = Color.WHITE;
    private static final Color LEVEL0_SELECTED_FG = new Color(20, 67, 140);
    private static final Color LEVEL0_NORMAL_FG = new Color(245, 247, 255);
    private static final Color LEVEL0_HOVER_BG = new Color(255, 255, 255, 60);

    private static final Color LEVEL1_SELECTED_BG = new Color(255, 255, 255, 230);
    private static final Color LEVEL1_SELECTED_FG = new Color(29, 84, 173);
    private static final Color LEVEL1_NORMAL_FG = new Color(224, 235, 255);
    private static final Color LEVEL1_HOVER_BG = new Color(255, 255, 255, 80);

    private final int level;
    private final Color normalBg;
    private final Color normalFg;
    private final Color hoverBg;
    private final Color selectedBg;
    private final Color selectedFg;

    public NavButton(String text, String iconPath) {
        this(text, iconPath, 0);
    }

    public NavButton(String text, String iconPath, int level) {
        super("  " + text);
        this.level = Math.max(level, 0);
        if (iconPath != null && !iconPath.isBlank()) {
            setIcon((Icon) ThemeManager.svg(iconPath, 18));
        }
        setHorizontalAlignment(LEFT);
        setBorder(BorderFactory.createEmptyBorder(this.level == 0 ? 10 : 8,
                this.level == 0 ? 20 : 46,
                this.level == 0 ? 10 : 8,
                16));
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        setFocusPainted(false);
        setOpaque(true);
        setFont(getFont().deriveFont(this.level == 0 ? Font.BOLD : Font.PLAIN,
                this.level == 0 ? 14f : 13f));
        setIconTextGap(12);
        putClientProperty("JButton.buttonType", "toolBarButton");
        putClientProperty("JComponent.minimumWidth", this.level == 0 ? 180 : 168);
        putClientProperty("JComponent.minimumHeight", this.level == 0 ? 46 : 40);

        if (this.level == 0) {
            normalBg = new Color(255, 255, 255, 20);
            normalFg = LEVEL0_NORMAL_FG;
            hoverBg = LEVEL0_HOVER_BG;
            selectedBg = LEVEL0_SELECTED_BG;
            selectedFg = LEVEL0_SELECTED_FG;
        } else {
            normalBg = new Color(255, 255, 255, 18);
            normalFg = LEVEL1_NORMAL_FG;
            hoverBg = LEVEL1_HOVER_BG;
            selectedBg = LEVEL1_SELECTED_BG;
            selectedFg = LEVEL1_SELECTED_FG;
        }

        setBackground(normalBg);
        setForeground(normalFg);
        addChangeListener(this);
    }

    @Override
    public void stateChanged(ChangeEvent e) {
        if (isSelected()) {
            setBackground(selectedBg);
            setForeground(selectedFg);
        } else if (getModel().isRollover()) {
            setBackground(hoverBg);
            setForeground(Color.WHITE);
        } else {
            setBackground(normalBg);
            setForeground(normalFg);
        }
    }
}
