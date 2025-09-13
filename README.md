# SIGA — Sistema Integrado de Gestión Administrativa

## Requisitos
- JDK 17+
- Maven 3.9+
- NetBeans 17+ (o cualquier IDE compatible con Maven)
- MySQL 8.x
- Git

## Instalación rápida (dev)
1) Clonar y cambiar a rama `dev`:
   ```bash
   git clone https://github.com/<org>/<repo>.git
   cd <repo>
   git checkout dev

Config de DB (no se versiona):

Copiar src/main/resources/application.example.properties a src/main/resources/application.properties

Editar application.properties con tus credenciales locales:

db.url=jdbc:mysql://localhost:3306/siga?allowPublicKeyRetrieval=true&useSSL=false&serverTimezone=UTC
db.user=TU_USUARIO
db.password=TU_PASSWORD
db.pool.size=10


Base de datos:

Abrir db/migrations/001_init.sql en MySQL Workbench y ejecutar (bloque por bloque si hace falta).

Opcional: verificar con db/check.sql.

Build:

NetBeans → Clean and Build (o mvn -DskipTests=true clean package)

Crear admin (una vez):

Run ar.edu.unse.siga.tools.CreateAdminUser (crea admin/admin123)

Ejecutar app:

Main Class: ar.edu.unse.siga.ui.AppLauncher

Login: admin / admin123

Arquitectura (capas)

domain/ → DTOs (datos)

persistence/ → DAO + JDBC (MySQL)

service/ → reglas de negocio (validaciones/orquestación)

ui/ → Swing (ventanas, diálogos, tablas)



Patrones

DAO / DTO

Service Layer

Properties por entorno



Scripts

db/migrations/001_init.sql → crea esquema y datos semilla

db/check.sql → verificación rápida

db/scripts/create_user.sql → plantilla para crear usuarios locales



Flujo de ramas

main (estable), dev (integración), feature/<tarea> (una por issue)

PRs hacia dev con 1 review mínimo


---

# C) CONTRIBUTING.md (cómo trabajamos)

## dónde crearlo
- **ruta:** `CONTRIBUTING.md` (en la **raíz**)

## qué pegar
```markdown
# Contribuir

## Flujo de ramas
- `main`: estable (releases/demo)
- `dev`: integración del sprint
- `feature/<tarea>`: una por issue (ej.: `feature/ui-tramites`)

## Commits (convención)
- `feat:`, `fix:`, `docs:`, `chore:`, `refactor:`, `test:`
- Mensajes en presente y descriptivos (ej: `feat(ui): alta de insumo`)

## Pull Requests
- Base: `dev`
- Checklist:
  - [ ] Compila sin errores
  - [ ] Sin secretos en el repo (no subir `application.properties`)
  - [ ] Screenshots si toca UI
  - [ ] Tests si toca Service/DAO

## Estándares
- UI **no** accede a DB directo → UI → Service → DAO
- DTOs sin lógica
- Validar en Service
- SQL siempre con PreparedStatement
- Config por properties (no hardcodear credenciales)

## Config local
- Copiar `src/main/resources/application.example.properties` a `application.properties` (no versionar)
- Crear DB con `db/migrations/001_init.sql`