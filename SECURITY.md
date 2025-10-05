# Seguridad – SIGA

- **No subir credenciales**: `src/main/resources/application.properties` y `src/test/resources/application.properties` están en `.gitignore`.
- Usar `application.example.properties` (main y test) como plantilla.
- Passwords de usuarios de aplicación: **BCrypt** (jBCrypt). No almacenar en claro.
- Usuarios de MySQL:
  - Crear uno por dev (ej: `tito`, `enzo`…).
  - Permisos limitados a la BD `siga`.
- Commits/PR:
  - Revisar que no haya `password`, `db.user`, `db.password` en cambios de texto.
