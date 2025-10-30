package ar.edu.unse.siga.ui;

import ar.edu.unse.siga.config.AppServices;

public class App {
    public static void main(String[] args) {
        // com.formdev.flatlaf.FlatLightLaf.setup(); // si usás FlatLaf
        javax.swing.SwingUtilities.invokeLater(() -> {
            // inicializamos una vez los servicios
            AppServices.init();
            AppLauncher.launch();
        });
    }
}
    

