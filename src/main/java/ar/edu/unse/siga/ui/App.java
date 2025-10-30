package ar.edu.unse.siga.ui;
import ar.edu.unse.siga.ui.AppLauncher;

public class App {
    public static void main(String[] args) {
        // com.formdev.flatlaf.FlatLightLaf.setup(); // si usás FlatLaf
        javax.swing.SwingUtilities.invokeLater(AppLauncher::launch);
    }
}
    

