# HOWTO DB – SIGA

## 1) Crear base y tablas
- Abrir `db/migrations/001_init.sql` en MySQL Workbench.
- Ejecutar (bloque por bloque si hace falta).
- Verificar con `db/scripts/check.sql`.

## 2) Crear usuario local (opción recomendada)
- Abrir `db/scripts/create_user.sql`.
- Reemplazar `:USER` y `:PASS` por los deseados (ej: `enzo`/`enzo`).
- Ejecutar con un usuario con permisos (root).
- Alternativa: cambiar a `mysql_native_password` (ver comentarios del script).

## 3) Configurar la app
- Copiar `src/main/resources/application.example.properties` → `application.properties`
- Editar:
db.url=jdbc:mysql://localhost:3306/siga?allowPublicKeyRetrieval=true&useSSL=false&serverTimezone=UTC
db.user=<TU_USER>
db.password=<TU_PASS>

- Para tests: copiar `src/test/resources/application.example.properties` → `src/test/resources/application.properties` y completar user/pass.

## 4) Usuarios de demo
- Ejecutar `ar.edu.unse.siga.tools.CreateAdminUser` una vez (crea admin/admin123).
- Luego correr la app con `ar.edu.unse.siga.ui.AppLauncher`.

## 5) Problemas comunes
- **Public Key Retrieval is not allowed**  
Agregar `allowPublicKeyRetrieval=true` en la URL o usar `mysql_native_password`.
- **No database selected**  
Ejecutar `USE siga;` o elegir `siga` como esquema activo en Workbench.
- **Duplicate entry**  
Ya existe el registro (reseteá la BD o cambiá valores).