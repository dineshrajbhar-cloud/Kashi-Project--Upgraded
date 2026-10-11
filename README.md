# Kashi Tourism Portal 2.0 - Backend

REST API for the Kashi (Varanasi) Tourism Portal. It keeps the original idea of the old portal (sign up, log in, explore ghats and temples, send feedback) as a clean Spring Boot backend for a separate React frontend.

## 1. What it does

- Register and log in with email and password (JWT, stateless)
- `USER` and `ADMIN` roles, enforced on the server
- Places (ghats and temples): list, search by name, filter by type, pagination and sorting; admins can create, update and delete
- Gallery: list images; admins manage image URLs
- Feedback: guests and logged-in users can send it; admins can read it
- Swagger UI for trying every endpoint

## 2. Technology stack

Java 17, Spring Boot 4.1.1, Spring Web MVC, Spring Security, Spring Data JPA (Hibernate), MySQL, Flyway, JJWT 0.13.0 (with `jjwt-gson`), Springdoc OpenAPI, Lombok, Bean Validation, JUnit 5 and Mockito, Maven.

Why `jjwt-gson`: Spring Boot 4 uses Jackson 3, while `jjwt-jackson` needs Jackson 2. The Gson module avoids the clash.

## 3. Prerequisites

- JDK 17 or newer (check with `java -version`)
- MySQL 8.x running locally
- Internet access on the first build (Maven and the dependencies are downloaded)
- Git and IntelliJ IDEA or STS (optional)

You do not need to install Maven. The project includes the Maven wrapper (`mvnw`, `mvnw.cmd`).

## 4. Create the MySQL database

Only the empty database is needed. Flyway creates the tables on first start.

```sql
CREATE DATABASE kashi_tourism CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

Use a MySQL user that can create tables in this database. Do not use the root account for the application in a real deployment.

## 5. Environment variables

| Variable | Required | Meaning |
|---|---|---|
| `DB_HOST`, `DB_PORT`, `DB_NAME` | no (defaults: localhost, 3306, kashi_tourism) | Database location |
| `DB_USERNAME`, `DB_PASSWORD` | **yes** | Database login |
| `JWT_SECRET` | **yes** | Signing secret, at least 32 bytes. The app refuses to start without it. |
| `JWT_EXPIRATION_MS` | no (default 3600000 = 60 minutes) | Token lifetime |
| `ADMIN_EMAIL`, `ADMIN_PASSWORD` | for the first admin | See section 14 |
| `FRONTEND_URL` | no (default http://localhost:5173) | Origin allowed by CORS. Comma-separated for several. |
| `SERVER_PORT` | no (default 8080) | HTTP port |

## 6. Create your local `.env`

PowerShell:

```powershell
Copy-Item .env.example .env
notepad .env
```

macOS / Linux:

```bash
cp .env.example .env
```

Replace every placeholder. To generate a JWT secret:

```powershell
[Convert]::ToBase64String((1..48 | ForEach-Object { Get-Random -Maximum 256 }) -as [byte[]])
```

```bash
openssl rand -base64 48
```

`.env` is listed in `.gitignore`. Never commit it. The application reads it automatically from the folder you start it in, and normal environment variables work too.

The placeholder values from `.env.example` are rejected on purpose (the app will not start with the placeholder `JWT_SECRET`, and no admin is created with the placeholder password).

## 7. Run in IntelliJ IDEA or STS

1. Open the `kashi-tourism-backend` folder as a Maven project.
2. Make sure the project SDK is JDK 17 or newer.
3. Make sure `.env` is in the project root and the run configuration's working directory is the project root (the default).
4. Run `KashiPortalApplication`.

## 8. Run with Maven

PowerShell:

```powershell
.\mvnw.cmd spring-boot:run
```

macOS / Linux:

```bash
./mvnw spring-boot:run
```

The API starts on http://localhost:8080. To build a runnable jar:

```powershell
.\mvnw.cmd clean package
java -jar target\kashi-tourism-backend-1.0.0.jar
```

## 9. Run the tests

```powershell
.\mvnw.cmd clean test
```

The tests do not need MySQL or a `.env` file. Service tests use Mockito, and the web tests (`@WebMvcTest`) run the real security rules, JWT filter, validation and error handling with mocked services.

There are no automated tests against a real MySQL database. The Flyway migrations and the entities are checked when the application starts (see Troubleshooting).

## 10. Swagger UI

- Swagger UI: http://localhost:8080/swagger-ui/index.html (also reachable at `/swagger-ui.html`)
- OpenAPI JSON: http://localhost:8080/v3/api-docs

## 11. API summary

All paths start with `/api`. Errors always use the same JSON shape.

| Method | Path | Access |
|---|---|---|
| POST | `/auth/register` | Public |
| POST | `/auth/login` | Public |
| GET | `/users/me` | Logged in |
| GET | `/places` (`type`, `name`, `page`, `size`, `sort`) | Public |
| GET | `/places/{id}` | Public |
| POST / PUT / DELETE | `/places`, `/places/{id}` | ADMIN |
| GET | `/gallery` (`page`, `size`, `sort`) | Public |
| POST / PUT / DELETE | `/gallery`, `/gallery/{id}` | ADMIN |
| POST | `/feedback` | Public (linked to the account if a valid token is sent) |
| GET | `/feedback` | ADMIN |
| GET | `/admin/users` | ADMIN |

Paging: `page` starts at 0. `size` defaults to 10 (gallery 12) and is capped at 50. `sort` looks like `name,asc`. Places can be sorted by `name`, `type`, `createdAt`; gallery by `title`, `createdAt`. `type` must be `GHAT` or `TEMPLE` (uppercase).

Error response:

```json
{
  "timestamp": "2026-10-10T10:00:00",
  "status": 400,
  "error": "Bad Request",
  "message": "Validation failed",
  "path": "/api/auth/register",
  "fieldErrors": { "email": "Enter a valid email address" }
}
```

## 12. Register and log in

PowerShell:

```powershell
$body = @{ name = "Asha Verma"; email = "asha@example.com"; password = "StrongPass123"; city = "Varanasi" } | ConvertTo-Json
Invoke-RestMethod -Method Post -Uri http://localhost:8080/api/auth/register -ContentType "application/json" -Body $body

$login = @{ email = "asha@example.com"; password = "StrongPass123" } | ConvertTo-Json
$response = Invoke-RestMethod -Method Post -Uri http://localhost:8080/api/auth/login -ContentType "application/json" -Body $login
$response.token
```

curl:

```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"asha@example.com","password":"StrongPass123"}'
```

Registration always creates a `USER`. A `role` field in the request is ignored.

## 13. Use the JWT

Swagger UI: log in through `POST /api/auth/login`, copy the `token` value, press **Authorize** at the top right, paste the token (without the word `Bearer`) and confirm.

Postman or PowerShell: send the header `Authorization: Bearer <token>`.

```powershell
Invoke-RestMethod -Uri http://localhost:8080/api/users/me -Headers @{ Authorization = "Bearer $($response.token)" }
```

How tokens work: the token is signed with HS256 using `JWT_SECRET` and carries the email and an expiry time (60 minutes by default). On every request the server checks the signature and expiry, loads the user from the database and takes the role from the database. A token for a deleted user stops working immediately, and a role change takes effect immediately.

Logout: the client deletes its token. There is no server-side logout endpoint, so a token that was already issued stays valid until it expires. Keep the lifetime short. A deny-list can be added later together with Redis.

## 14. The first ADMIN account

Public registration cannot create admins. At startup the application creates one `ADMIN` from `ADMIN_EMAIL` and `ADMIN_PASSWORD` if no admin exists yet:

- the password is hashed with BCrypt and never logged
- nothing happens if an admin already exists, if the variables are empty, if the password is the placeholder or shorter than 8 characters, or if that email is already registered as a normal user
- watch the startup log for `Initial ADMIN account created` or a warning explaining why none was created

Then log in with those credentials and use the admin endpoints.

## 15. Seed data

`V5__seed_places.sql` loads 8 ghats and 8 temples with short descriptions. Image and source URLs and coordinates are intentionally empty. Add them with `PUT /api/places/{id}`. The text is a starting point: replace it with your own wording as needed. Flyway migrations must not be edited after they have run, so change data through the API or a new migration (`V6__...`).

## 16. Troubleshooting

| Problem | What to check |
|---|---|
| `Could not resolve placeholder 'DB_PASSWORD'` | `DB_USERNAME` and `DB_PASSWORD` are not set. Create `.env` in the folder you start the app from, or set the environment variables. |
| `JWT_SECRET must be set and at least 32 bytes long` | Set a real secret in `.env`. |
| `Access denied for user ...` or `Communications link failure` | MySQL is not running, or the user, password, host or port is wrong. |
| `Unknown database 'kashi_tourism'` | Create the database (section 4). |
| `Schema-validation: wrong column type ... role/gender/type` | The entities map these enums to `VARCHAR`. Report it with the full message. Do not switch to `ddl-auto=update`. |
| Flyway checksum or failed-migration error | A migration file was edited after it ran, or a run failed half way. In a throwaway development database, drop and recreate it. |
| `Port 8080 was already in use` | Set `SERVER_PORT` to another port. |
| Browser shows a CORS error | `FRONTEND_URL` must exactly match the frontend origin, including the port. Restart after changing it. |
| 401 on a protected endpoint | The token is missing, expired or malformed. Log in again. |
| 403 on an admin endpoint | The logged-in account is not an `ADMIN`. |
| Swagger UI does not load, or a startup error mentions springdoc | See the verification notes below. |
| `mvnw` is not executable (Linux or macOS) | `chmod +x mvnw` |
| Lombok errors in the IDE | Install the Lombok plugin and enable annotation processing. |

## 17. Verification notes

This project was assembled in an environment that could not download from Maven Central. Because of that:

- the project has **not** been compiled with Maven and the unit tests have **not** been run
- the Spring Boot 4.1.1 / springdoc 3.0.3 / JJWT 0.13.0 combination has **not** been resolved or started

What was checked: the Java files parse and package declarations match their folders, and the five Flyway migrations were executed in order against a MySQL-compatible server (MariaDB 10.11), including the unique email, role/type checks and `ON DELETE SET NULL` behaviour.

Run these on your machine first. Any failure is a real finding to report:

```powershell
.\mvnw.cmd clean test
.\mvnw.cmd spring-boot:run
```

Then open http://localhost:8080/swagger-ui/index.html. If springdoc 3.0.3 does not work with Spring Boot 4.1.1 on your JDK, check for a newer springdoc 3.x release and update `springdoc.version` in `pom.xml`.
