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

## Keycloak Setup

This application uses Keycloak for authentication and authorization. Follow these steps to set up Keycloak:

### 1. Start Keycloak

Keycloak should be running at `http://localhost:9090`. If using Docker, it should be included in the `docker-compose.yml`.

### 2. Configure Realm and Roles

1. **Access Keycloak Admin Console:** http://localhost:9090/admin
2. **Select Realm:** Choose "event-ticket-platform" from the dropdown
3. **Create Realm Roles:**
   - Navigate to **Realm roles** in the left sidebar
   - Click **"Create role"**
   - Create the following roles:
     - **ORGANIZER** - For users who can create and manage events
     - **ATTENDEE** - For regular users who can purchase tickets

### 3. Create and Configure Users

1. **Navigate to Users** in the left sidebar
2. **Click "Add user"**
3. **Fill in user details:**
   - Username: (e.g., `organiser`)
   - Email: (optional)
   - First name / Last name: (optional)
4. **Click "Create"**
5. **Set Password:**
   - Go to **Credentials** tab
   - Click **"Set password"**
   - Enter a password and set "Temporary" to OFF
   - Click **"Save"**
6. **Assign Roles:**
   - Go to **Role mapping** tab
   - Click **"Assign role"**
   - Select **ORGANIZER** (or ATTENDEE)
   - Click **"Assign"**

### 4. Client Configuration

The application is configured to use the following OIDC settings:
- **Authority:** `http://localhost:9090/realms/event-ticket-platform`
- **Client ID:** `event-ticket-platform-app`
- **Redirect URI:** `http://localhost:5173/callback`

Make sure a client with these settings exists in Keycloak.

### 5. Dev Mode (Optional)

To bypass authentication during development, start the backend with the `dev` profile:

```powershell
cd backend
.\mvnw.cmd spring-boot:run -Dspring-boot.run.profiles=dev
```

This will disable all security checks. **Do not use in production!**

## Notes

- The backend contains database and app configuration in `backend/src/main/resources/application.properties` and `backend/docker-compose.yml`.
- The repo already contains LICENSE files in the subprojects; add or update a top-level license if desired.
- Keycloak roles must be assigned to users for proper authorization.

If you want, I can add a GitHub Actions workflow to run tests/build on push.
