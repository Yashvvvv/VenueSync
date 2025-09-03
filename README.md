# Event Management Platform

Monorepo containing a Spring Boot backend and a Vite + React TypeScript frontend for an event/ticketing platform.

## Repository layout

- `backend/` — Spring Boot (Maven) backend
- `frontend/` — Vite + React + TypeScript frontend

## Quick start

Prerequisites:
- Java 17+ (or compatible JDK used by the project)
- Maven (or use the included Maven wrapper `mvnw` / `mvnw.cmd`)
- Node 18+ and npm
- Docker (optional, used by `docker-compose.yml` under `backend/`)

Run the backend (Docker):

```powershell
cd backend
docker-compose up -d
```

Or run the backend locally with the Maven wrapper:

```powershell
cd backend
.\mvnw.cmd spring-boot:run
```

Run the frontend:

```powershell
cd frontend
npm install
npm run dev
```

Run tests (backend):

```powershell
cd backend
.\mvnw.cmd test
```

Notes:
- The backend contains database and app configuration in `backend/src/main/resources/application.properties` and `backend/docker-compose.yml`.
- The repo already contains LICENSE files in the subprojects; add or update a top-level license if desired.

If you want, I can add a GitHub Actions workflow to run tests/build on push.
