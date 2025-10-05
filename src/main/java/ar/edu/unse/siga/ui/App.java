package ar.edu.unse.siga.ui;


import ar.edu.unse.siga.config.AppServices;
import ar.edu.unse.siga.ui.shell.MainShellFrame;

public class App {
    public static void main(String[] args) {
        // com.formdev.flatlaf.FlatLightLaf.setup(); // si usás FlatLaf
        javax.swing.SwingUtilities.invokeLater(() -> {
            // inicializamos una vez los servicios
            AppServices.init();
            new MainShellFrame().setVisible(true);
        });
    }
}
    

