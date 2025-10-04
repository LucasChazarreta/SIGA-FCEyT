package ar.edu.unse.siga.ui.base;

import ar.edu.unse.siga.ui.inventario.ModificarInsumoPanel;
import javax.swing.*;
import java.awt.*;

public final class Ui {

    public static void warn(ModificarInsumoPanel aThis, String primero_buscá_un_insumo) {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }
    private Ui(){}
  
    // ✅ Arreglo: firma genérica y sin excepción
    public static void warn(Component parent, String msg) {
        JOptionPane.showMessageDialog(parent, msg, "Atención", JOptionPane.WARNING_MESSAGE);
    }
    public static void centerAndShow(Window w, Window owner) {
        w.setLocationRelativeTo(owner);
        w.setVisible(true);
    }

    public static JPanel flowLeft(Component... comps) {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT));
        for (var c : comps) p.add(c);
        return p;
    }

    public static JPanel flowRight(Component... comps) {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        for (var c : comps) p.add(c);
        return p;
    }

    public static JPanel grid2Cols(String[] labels, JComponent[] inputs) {
        JPanel p = new JPanel(new GridLayout(labels.length, 2, 8, 8));
        for (int i=0;i<labels.length;i++) {
            p.add(new JLabel(labels[i]));
            p.add(inputs[i]);
        }
        return p;
    }

    public static void error(Component parent, Exception e) {
        e.printStackTrace();
        JOptionPane.showMessageDialog(parent, e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
    }

    public static void info(Component parent, String msg) {
        JOptionPane.showMessageDialog(parent, msg, "Info", JOptionPane.INFORMATION_MESSAGE);
    }

    public static boolean confirm(Component parent, String msg) {
        return JOptionPane.showConfirmDialog(parent, msg, "Confirmar",
                JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION;
    }
}
