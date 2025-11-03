/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package ar.edu.unse.siga.common;

public final class RoleName {

    public static final String ADMIN = "ADMIN";
    public static final String ADMINISTRATIVO = "ADMINISTRATIVO";

    private RoleName() {
    }

    public static boolean isAdmin(ar.edu.unse.siga.domain.Usuario usuario) {
        return usuario != null
                && usuario.getRol() != null
                && usuario.getRol().getNombre() != null
                && ADMIN.equalsIgnoreCase(usuario.getRol().getNombre());
    }

    public static boolean hasRole(ar.edu.unse.siga.domain.Usuario usuario, String roleName) {
        return usuario != null
                && usuario.getRol() != null
                && usuario.getRol().getNombre() != null
                && usuario.getRol().getNombre().equalsIgnoreCase(roleName);
    }
}
