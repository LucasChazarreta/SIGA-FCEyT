package ar.edu.unse.siga.domain;

public class Usuario {
    private Long id;
    private String username;
    private String passwordHash;
    private boolean activo;

    public Usuario() {}

    public Usuario(Long id, String username, String passwordHash, boolean activo) {
        this.id = id;
        this.username = username;
        this.passwordHash = passwordHash;
        this.activo = activo;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
    public boolean isActivo() { return activo; }
    public void setActivo(boolean activo) { this.activo = activo; }
}
