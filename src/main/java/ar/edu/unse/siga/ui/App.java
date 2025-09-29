package ar.edu.unse.siga.ui;

import ar.edu.unse.siga.config.AppServices;
import ar.edu.unse.siga.ui.shell.MainShellFrame;

import javax.swing.*;

public class App {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            AppServices.init();
            new MainShellFrame().setVisible(true);
        });
    }
}

