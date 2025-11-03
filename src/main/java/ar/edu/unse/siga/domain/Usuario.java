/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package ar.edu.unse.siga.domain;
import java.util.Objects;

public class Usuario {
    private Long id;
    private String username;
    private String password;
    private String passwordHash;
    private String email;
    private String estado; // ACTIVO / INACTIVO
    private Rol rol;

    public Usuario() {}
  
    public String getUsuario() {
        return this.username;
    }
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }
    public Rol getRol() { return rol; }
    public void setRol(Rol rol) { this.rol = rol; }

    @Override public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Usuario)) return false;
        Usuario that = (Usuario) o;
        return Objects.equals(id, that.id) && Objects.equals(username, that.username);
    }
    @Override public int hashCode() { return Objects.hash(id, username); }
    @Override public String toString() { return "Usuario{id=" + id + ", username='" + username + "'}"; }

  
}
