package ar.edu.unse.siga.ui.pages;

import ar.edu.unse.siga.service.TramiteService;
import ar.edu.unse.siga.ui.base.Ui;

import javax.swing.*;
import java.awt.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class TramiteEntradaPage extends JPanel {
    private final TramiteService service;

    private final JTextField txtNro = new JTextField(20);
    private final JSpinner spFecha = new JSpinner(new SpinnerDateModel());
    private final JTextField txtSolicitante = new JTextField(30);
    private final JTextField txtAsunto = new JTextField(30);

    public TramiteEntradaPage(TramiteService service) {
        this.service = service;
        setOpaque(false);
        setLayout(new BorderLayout());
        add(buildForm(), BorderLayout.NORTH);
        // default: nro leído/fecha hoy
        txtNro.setText(generateNumero(LocalDate.now()));
        JSpinner.DateEditor ed = new JSpinner.DateEditor(spFecha, "dd/MM/yyyy");
        spFecha.setEditor(ed);
    }

    private JComponent buildForm() {
        JPanel p = new JPanel(new GridBagLayout());
        p.setOpaque(false);
        GridBagConstraints gc = new GridBagConstraints();
        gc.insets = new Insets(8,8,8,8);
        gc.fill = GridBagConstraints.HORIZONTAL;
        gc.weightx = 1;

        JLabel title = new JLabel("Mesa de Entrada");
        title.setFont(title.getFont().deriveFont(Font.BOLD, 22f));
        gc.gridx=0; gc.gridy=0; gc.gridwidth=4;
        p.add(title, gc);

        gc.gridwidth=1;
        gc.gridy++;

        addRow(p, gc, 0, "N° de Trámite", txtNro);
        addRow(p, gc, 2, "Fecha de Recepción", spFecha);

        gc.gridy++;
        addRow(p, gc, 0, "Solicitante", txtSolicitante);
        addRow(p, gc, 2, "Asunto", txtAsunto);

        gc.gridy++;
        gc.gridx=0; gc.gridwidth=4; gc.anchor=GridBagConstraints.CENTER;
        JButton btn = new JButton("Registrar");
        btn.setPreferredSize(new Dimension(160,36));
        btn.addActionListener(e -> onSave());
        p.add(btn, gc);

        return p;
    }

    private void addRow(JPanel p, GridBagConstraints gc, int col, String label, JComponent input) {
        gc.gridx = col;
        gc.weightx = 0.2;
        p.add(new JLabel(label), gc);
        gc.gridx = col+1;
        gc.weightx = 0.8;
        p.add(input, gc);
    }

    private String generateNumero(LocalDate d) {
        // Ej: 20240915-00123 (simplificado)
        return d.format(DateTimeFormatter.BASIC_ISO_DATE) + "-" + (int)(Math.random()*90000+10000);
    }

    private void onSave() {
        try {
            String nro = txtNro.getText().trim();
            String asunto = txtAsunto.getText().trim();
            String solicitante = txtSolicitante.getText().trim();
            if (nro.isEmpty()) throw new IllegalArgumentException("El número es obligatorio");
            if (asunto.isEmpty()) throw new IllegalArgumentException("El asunto es obligatorio");

            // Nuestro TramiteService usa la fecha actual; si necesitás la del control,
            // podrías extender TramiteService para aceptar fecha explícita.
            service.registrarTramite(nro, asunto, solicitante);

            Ui.info(this, "Trámite registrado.");
            txtNro.setText(generateNumero(LocalDate.now()));
            txtAsunto.setText("");
            txtSolicitante.setText("");
        } catch(Exception e) {
            Ui.error(this, e);
        }
    }
}
