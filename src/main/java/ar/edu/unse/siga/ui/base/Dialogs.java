package ar.edu.unse.siga.ui.base;

import javax.swing.*;

public final class Dialogs {

    private Dialogs() {
    }

    public static void require(JTextField tf, String fieldName) {
        if (tf.getText() == null || tf.getText().trim().isEmpty()) {
            throw new IllegalArgumentException("El campo \"" + fieldName + "\" es obligatorio");
        }
    }

    public static int intFromSpinner(JSpinner sp) {
        return ((Number) sp.getValue()).intValue();
    }

    // ✅ Nuevo método para mostrar advertencias
    public static void warn(java.awt.Component parent, String message) {
        JOptionPane.showMessageDialog(
                parent,
                message,
                "Atención",
                JOptionPane.WARNING_MESSAGE
        );
    }
}
