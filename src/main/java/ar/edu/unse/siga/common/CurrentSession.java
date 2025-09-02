package ar.edu.unse.siga.common;

import ar.edu.unse.siga.domain.Usuario;

public final class CurrentSession {
    private static volatile Usuario current;

    private CurrentSession() {}

    public static void setUser(Usuario u) { current = u; }
    public static Usuario getUser() { return current; }
    public static boolean isLogged() { return current != null; }
    public static void clear() { current = null; }
}

