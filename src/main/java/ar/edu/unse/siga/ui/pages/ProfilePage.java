package ar.edu.unse.siga.ui.pages;

import ar.edu.unse.siga.domain.Usuario;
import ar.edu.unse.siga.ui.base.CardPanel;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

public class ProfilePage extends JPanel {

    private String obtenerCampo(Usuario u, String campo, String defecto) {
        if (u == null) {
            return defecto;
        }
        try {
            var m = u.getClass().getMethod("get" + Character.toUpperCase(campo.charAt(0)) + campo.substring(1));
            Object val = m.invoke(u);
            return val != null ? val.toString() : defecto;
        } catch (Exception e) {
            return defecto;
        }
    }

    public ProfilePage(Usuario usuario) {
        setOpaque(false);
        setLayout(new BorderLayout());

        CardPanel card = new CardPanel();
        card.setLayout(new BorderLayout(16, 16));
        card.setBorder(new EmptyBorder(8, 8, 8, 8));

        // Header
        JLabel title = new JLabel("Perfil de usuario");
        title.setFont(title.getFont().deriveFont(Font.BOLD, 18f));
        title.setForeground(new Color(35, 55, 110));
        card.add(title, BorderLayout.NORTH);

        // Datos
        JPanel form = new JPanel(new GridBagLayout());
        form.setOpaque(false);
        GridBagConstraints gc = new GridBagConstraints();
        gc.insets = new Insets(8, 8, 8, 8);
        gc.anchor = GridBagConstraints.LINE_START;

        int r = 0;
        addRow(form, gc, r++, "Usuario:",
                value(obtenerCampo(usuario, "usuario", "admin")));
        addRow(form, gc, r++, "Nombre completo:",
                value(obtenerCampo(usuario, "nombre", "Administrador")));
        addRow(form, gc, r++, "Rol:",
                value(obtenerCampo(usuario, "rol", "ADMIN")));
        addRow(form, gc, r++, "Email:",
                value(obtenerCampo(usuario, "email", "admin@fceyt.unse.edu.ar")));

        // Botones
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        actions.setOpaque(false);
        JButton btnCambiarPass = new JButton("Cambiar contraseña");
        JButton btnEditarPerfil = new JButton("Editar perfil");
        actions.add(btnCambiarPass);
        actions.add(btnEditarPerfil);

        card.add(form, BorderLayout.CENTER);
        card.add(actions, BorderLayout.SOUTH);

        add(card, BorderLayout.CENTER);
    }

    private void addRow(JPanel p, GridBagConstraints gc, int row, String label, JComponent field) {
        gc.gridx = 0;
        gc.gridy = row;
        p.add(new JLabel(label), gc);
        gc.gridx = 1;
        p.add(field, gc);
    }

    private JTextField value(String text) {
        JTextField tf = new JTextField(text);
        tf.setEditable(false);
        tf.setColumns(24);
        return tf;
    }
}
