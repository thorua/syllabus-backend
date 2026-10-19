# Sistema de Gestión de Syllabus — Backend

API del sistema de syllabus de la Universidad Autónoma de Manizales: Java 21, Spring Boot 3.5 (Security con JWT, Data JPA, Flyway), PostgreSQL 16, PDF en formato Almera y firma digital con PDFBox.

El frontend está en el repositorio `syllabus-frontend`. Este repositorio trae el `docker-compose.yml` que levanta todo el sistema.

## Ponerlo a funcionar

Requisito: Docker Desktop abierto.

1. Clone los dos repositorios en la misma carpeta:

   ```bash
   git clone <url>/syllabus-backend.git
   git clone <url>/syllabus-frontend.git
   ```

2. En `syllabus-backend`, cree la configuración (solo la primera vez): `copy .env.example .env` en Windows o `cp .env.example .env` en macOS/Linux.
3. Levante el sistema: `docker compose up --build`.
4. Abra http://localhost con un usuario demo, por ejemplo `admin@autonoma.edu.co` / `Uam2026!`.

| Dirección | Qué es |
|---|---|
| http://localhost | Aplicación |
| http://localhost/api/docs | Documentación interactiva de la API |
| http://localhost:8025 | Bandeja de correos de prueba (Mailpit) |

Detener: `docker compose down`. Empezar de cero con los datos demo: `docker compose down -v`.

## Usuarios demo

Contraseña de todos: `Uam2026!`. Se cargan cuando `FLYWAY_LOCATIONS` incluye `classpath:db/demo`, como en `.env.example`.

| Correo | Rol |
|---|---|
| `admin@autonoma.edu.co` | Administrador |
| `registro@autonoma.edu.co` | Registro Académico (segunda firma) |
| `coordinador@autonoma.edu.co` | Coord. del Departamento de Ciencias Computacionales |
| `coordinador.matematicas@autonoma.edu.co` | Coord. del Departamento de Física y Matemáticas |
| `coordinador.programa@autonoma.edu.co` | Coord. de Ingeniería Electrónica y de la Maestría en Software |
| `profesor@autonoma.edu.co` | Profesor y Coord. de Ingeniería de Sistemas |
| `estudiante@autonoma.edu.co` | Estudiante de Ingeniería de Sistemas |

## Desarrollo

Requisitos: JDK 21 y Docker. Use `mvnw.cmd` en Windows.

```bash
docker compose up -d db mailpit   # base de datos y correo de prueba
./mvnw spring-boot:run            # API en http://localhost:8080
./mvnw verify                     # pruebas (JUnit + Testcontainers, requiere Docker)
```

Producción: `COMPOSE_PROFILES` vacío, `FLYWAY_LOCATIONS=classpath:db/migration` (sin datos demo), y secretos y correo reales en `.env`.
