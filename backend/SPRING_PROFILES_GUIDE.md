# Spring Boot Profiles Configuration Guide

## The Issue (Now Fixed ✅)

The application was failing to start with this error:
```
Property 'spring.profiles.active' imported from location 'class path resource [application-dev.properties]' 
is invalid in a profile specific resource
```

## Root Cause

**You cannot set `spring.profiles.active` inside a profile-specific property file** (like `application-dev.properties` or `application-prod.properties`). This creates a circular reference and is explicitly forbidden by Spring Boot.

## Correct Configuration

### ✅ Main Configuration File (`application.properties`)

```properties
# spring.profiles.active is NOT set here, and must never be added.
# Local defaults live in the `#---` document at the bottom of the file,
# activated on the `default` profile:
spring.config.activate.on-profile=default
```

**Do not put `spring.profiles.active` in this file.** Spring Boot rejects it in
any file that also contains profile-activated (`#---`) documents, and fails at
startup with `InactiveConfigDataAccessException` before a single bean is
created. This bit us for real on 2026-09-06: the merge of
`application-dev.properties` into `application.properties` left
`spring.profiles.active=${SPRING_PROFILES_ACTIVE:dev}` in place, and every
Render deploy crashed on boot. It went unnoticed because `mvnw test` uses
`src/test/resources/application.properties` — a different file with no profile
documents — and `docker compose config` never starts Spring at all.

`default` is the profile Spring uses when nothing is set, so the local block
applies to `mvnw spring-boot:run` automatically and switches itself off as soon
as `SPRING_PROFILES_ACTIVE=prod` is present.

> **Render must set `SPRING_PROFILES_ACTIVE=prod` explicitly.** There is no
> in-file fallback any more. Without it the service runs the local block:
> DEBUG logging, SQL echoing, and `application-prod.properties` never loads —
> so `CORS_ALLOWED_ORIGINS` silently falls back to `localhost:5173` and the
> deployed frontend gets blocked.

### ✅ Profile-Specific Config

Only two files now (updated 2026-09-04 — dev overrides used to live in a
separate `application-dev.properties`; that file was pure near-duplication
of the base defaults plus a handful of genuinely dev-only lines, so it was
merged into `application.properties` itself):

- `application.properties` - base defaults (used by every profile) **plus** a
  local-development section at the bottom, separated by a `#---` document marker
  with `spring.config.activate.on-profile=default`. `default` is the profile
  Spring uses when `SPRING_PROFILES_ACTIVE` is unset, so this applies to local
  runs automatically. DO NOT put `spring.profiles.active` anywhere in this file —
  not in that section and not in the base document either.
- `application-prod.properties` - prod-specific overrides, kept as its own file
  since it's substantively different (SSL, connection pool limits, `ddl-auto=validate`,
  restricted actuator exposure) and is the one file you'd want to open standalone
  to see "what does Render need." DO NOT include `spring.profiles.active` here either.

## How to Switch Profiles

### Method 1: Environment Variable (Recommended)
```powershell
# Windows PowerShell
$env:SPRING_PROFILES_ACTIVE="prod"
java -jar venuesync-1.0.0.jar

# Or inline
$env:SPRING_PROFILES_ACTIVE="prod"; java -jar venuesync-1.0.0.jar
```

### Method 2: Command Line Argument
```powershell
java -jar venuesync-1.0.0.jar --spring.profiles.active=prod
```

### Method 3: IntelliJ Run Configuration
1. Run → Edit Configurations
2. Select your Spring Boot run configuration
3. Add to "Environment variables": `SPRING_PROFILES_ACTIVE=prod`
4. Or add to "Program arguments": `--spring.profiles.active=prod`

### Method 4: Maven
```powershell
mvn spring-boot:run -Dspring-boot.run.profiles=prod
```

## Profile Loading Order

When `SPRING_PROFILES_ACTIVE` is unset (local development — Spring falls back to
the `default` profile):

1. Loads `application.properties`' base document (top section, above the `#---` marker)
2. Loads `application.properties`' `default`-profile document (below the `#---` marker) — overrides the base values with the local-only ones (verbose SQL, DEBUG logging)
3. Environment variables override both
4. Command-line arguments override everything

With `SPRING_PROFILES_ACTIVE=prod`, step 2 instead loads `application-prod.properties`
over the same base, and the `default` document is skipped entirely.

## Current Configuration Summary

| Property | application.properties (base) | application.properties (`default` section) | application-prod.properties |
|----------|-------------------------------|------------------------------------------|------------------------------|
| Profile selection | ❌ Not allowed here | ❌ Not allowed here | ❌ Not allowed here |
| Database URL | `localhost:5433` (env-overridable) | — (inherits base) | From `${DATABASE_URL}` or `${SPRING_DATASOURCE_URL}` |
| Show SQL | `false` | `true` | `false` |
| DDL Auto | `update` | — (inherits base) | `update` (from `${JPA_DDL_AUTO}`) |
| Logging | `INFO` | `DEBUG` | `WARN` |
| IDP issuer | `https://${AUTH0_DOMAIN}/` (no fallback) | — (inherits base) | same, no fallback |
| CORS origins | `http://localhost:5173` (env-overridable) | — (inherits base) | From `${CORS_ALLOWED_ORIGINS}` |

> `DDL Auto` in prod is `update`, **not** `validate`. There is no migration tool
> (Flyway/Liquibase) in this project yet, so `update` is a deliberate, documented
> choice rather than an oversight — see the comment in `application-prod.properties`.
> Best Practice 3 below is the goal, not the current state.

## Best Practices

1. **Never** put `spring.profiles.active` in profile-specific files
2. **Always** set sensitive values (passwords, API keys) via environment variables in production
3. **Use** `validate` for `spring.jpa.hibernate.ddl-auto` in production (never `update` or `create-drop`)
4. **Keep** profile-specific files minimal - only override what's different from the base config

## Running the Application Now

With no `SPRING_PROFILES_ACTIVE` set, Spring uses the `default` profile and picks
up the local overrides automatically, so you can just run:

```powershell
# From IntelliJ (just click Run) - will use dev profile by default

# Or from command line
cd E:\IJ_Projects\FullStackProjects\Tickets\Files\VenueSync\backend
mvn spring-boot:run

# Or with the built JAR
java -jar target/venuesync-1.0.0.jar
```

The application will start with the `dev` profile automatically, connecting to your local Docker PostgreSQL on port 5433 and Keycloak on port 9090.

## Production Deployment Note

When deploying to Render, set the following environment variables so the production profile can resolve correctly:

- `DATABASE_URL` or `SPRING_DATASOURCE_URL`
- `DATABASE_USERNAME` or `SPRING_DATASOURCE_USERNAME`
- `DATABASE_PASSWORD` or `SPRING_DATASOURCE_PASSWORD`
- `KEYCLOAK_ISSUER_URI`
- `CORS_ALLOWED_ORIGINS`

The frontend should also receive `VITE_API_BASE_URL`, `VITE_OIDC_AUTHORITY`, and `VITE_OIDC_CLIENT_ID` from the production environment.

---

**Date Fixed:** March 9, 2026  
**Fixed By:** Removing `spring.profiles.active=dev` from `application-dev.properties` and `spring.profiles.active=prod` from `application-prod.properties`

