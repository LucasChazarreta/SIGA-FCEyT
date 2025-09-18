package ar.edu.unse.siga.ui.pages;

import ar.edu.unse.siga.ui.base.ScalableSvgPanel;

import javax.swing.*;
import java.awt.*;

/**
 * Portada de inicio: sólo muestra el SVG centrado y escalable.
 * Si querés agregar textos/botones overlay, podés envolverlo en otro panel.
 */
public class HomePage extends JPanel {

    public HomePage() {
        setOpaque(false);
        setLayout(new BorderLayout());

        // Cambiá la ruta si usaste otro nombre/carpeta
        ScalableSvgPanel hero = new ScalableSvgPanel("hero/home.svg");
        hero.setMaxPadding(64); // margen alrededor (opcional)

        add(hero, BorderLayout.CENTER);
    }
}
