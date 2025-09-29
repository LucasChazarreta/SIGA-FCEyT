package ar.edu.unse.siga.ui;

import ar.edu.unse.siga.config.AppServices;
import ar.edu.unse.siga.service.AuthService;
import ar.edu.unse.siga.ui.shell.ShellFrame;
import ar.edu.unse.siga.ui.auth.ThemedLoginDialog;

import javax.swing.*;
import java.awt.*;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

public class App {

    // Agregá aquí el FQCN de tu login si no está en la lista
    private static final String[] CANDIDATES = new String[] {
            "ar.edu.unse.siga.ui.auth.ModernLoginDialog",
            "ar.edu.unse.siga.ui.auth.LoginDialog",
            "ar.edu.unse.siga.ui.LoginDialog",
            "ar.edu.unse.siga.ui.login.LoginDialog",
            "ar.edu.unse.siga.ui.LoginUI",
            "ar.edu.unse.siga.ui.auth.UILogin"
    };

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                AppServices.init();
                AuthService auth = AppServices.get().getAuthService();

                // 1) Intentar usar TU login “moderno” si existe
                boolean ok = tryLaunchCustomLogin(auth);

                // 2) Si no se pudo, usar el login temático de fallback (no se pierde más tiempo acá)
                if (!ok) {
                    ThemedLoginDialog login = new ThemedLoginDialog(null, auth);
                    login.setVisible(true);
                    ok = login.isLoggedIn();
                }

                if (ok) {
                    // 3) Abrir tu Shell estilado (usa tus ThemeManager/GradientPanel/svg, etc.)
                    //    Nota: asegurate de tener el constructor sin args que llama a AppServices.get()
                    new ShellFrame().setVisible(true);
                } else {
                    System.exit(0);
                }

            } catch (Throwable e) {
                e.printStackTrace();
                JOptionPane.showMessageDialog(null, "Error al iniciar: " + e.getMessage());
            }
        });
    }

    /**
     * Intenta cargar e invocar TU login “moderno” por reflexión.
     * Requisitos que intento cubrir:
     *  - Constructor (Frame, AuthService)  O  constructor () y luego setAuthService(AuthService)
     *  - Que sea un JDialog modal: setVisible(true) bloquea hasta cerrar
     *  - Para saber si logueó:
     *      * método boolean isLoggedIn()
     *      * o método getUsuarioAutenticado() != null
     */
    private static boolean tryLaunchCustomLogin(AuthService auth) {
        for (String fqcn : CANDIDATES) {
            try {
                Class<?> clazz = Class.forName(fqcn);
                Object dialog;

                // Intentar constructor (Frame, AuthService)
                Constructor<?> ctor = null;
                try {
                    ctor = clazz.getConstructor(Frame.class, AuthService.class);
                } catch (NoSuchMethodException ignored) { }

                if (ctor != null) {
                    dialog = ctor.newInstance((Frame) null, auth);
                } else {
                    // Probar constructor vacío
                    Constructor<?> empty = clazz.getConstructor();
                    dialog = empty.newInstance();

                    // Si tiene setAuthService(AuthService), inyectar
                    try {
                        Method setAuth = clazz.getMethod("setAuthService", AuthService.class);
                        setAuth.invoke(dialog, auth);
                    } catch (NoSuchMethodException ignored) { /* ok si no existe */ }
                }

                if (!(dialog instanceof Window)) {
                    continue; // no es una ventana utilizable
                }

                // Mostrar
                ((Window) dialog).setVisible(true);

                // ¿logueó? 1) isLoggedIn()
                try {
                    Method isLoggedIn = clazz.getMethod("isLoggedIn");
                    Object res = isLoggedIn.invoke(dialog);
                    if (res instanceof Boolean && (Boolean) res) return true;
                } catch (NoSuchMethodException ignored) { }

                // ¿logueó? 2) getUsuarioAutenticado() != null
                try {
                    Method getUser = clazz.getMethod("getUsuarioAutenticado");
                    Object user = getUser.invoke(dialog);
                    if (user != null) return true;
                } catch (NoSuchMethodException ignored) { }

                // Si llegó acá, ese login no nos dice si autenticó; probar siguiente candidato
            } catch (ClassNotFoundException e) {
                // no existe este candidato; seguimos
            } catch (Throwable t) {
                // si un candidato rompe, seguimos con el siguiente
                t.printStackTrace();
            }
        }
        return false;
    }
}
