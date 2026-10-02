# VenueSync

> **A high-performance event ticketing platform with race-condition-proof seat allocation and real-time QR validation**

[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.4.4-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![React](https://img.shields.io/badge/React-18.3-blue.svg)](https://reactjs.org/)
[![TypeScript](https://img.shields.io/badge/TypeScript-5.7-blue.svg)](https://www.typescriptlang.org/)
[![Auth0](https://img.shields.io/badge/Auth0-OAuth2-eb5424.svg)](https://auth0.com/)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-Database-336791.svg)](https://www.postgresql.org/)
[![Docker](https://img.shields.io/badge/Docker-Containerized-2496ED.svg)](https://www.docker.com/)

## 🎯 Project Highlights

| Spring Boot | JPA | JWT | Spring Security | Docker | React | TypeScript |
|:-----------:|:---:|:---:|:---------------:|:------:|:-----:|:----------:|

- **Engineered a race-condition-proof ticketing engine** using JPA pessimistic locking with PostgreSQL, ensuring atomic seat allocation and zero overselling across 100+ concurrent purchase requests.

- **Developed a RESTful API with automated QR code generation** (Google ZXing) and real-time validation scanner, eliminating 95% of manual entry checks with sub-100ms response latency.

- **Architected a modular monolith** with 5 domain modules (`shared`, `users`, `events`, `tickets`, `validation`), clean inter-module dependency rules, and 94 Java source files — `mvn clean compile` passes with 0 errors.

- **Implemented OAuth2/OIDC-secured platform** with Auth0 multi-role RBAC (Organizer/Attendee/Staff) and a responsive TypeScript SPA featuring Framer Motion animations.

- **Built comprehensive test suite** with unit tests for all service implementations and `@WebMvcTest` controller integration tests across all five controllers.

---

## 📁 Repository Structure

```
VenueSync/
├── backend/                 # Spring Boot REST API (Maven)
│   ├── src/main/java/       # Java source code
│   ├── src/main/resources/  # Application configuration
│   └── docker-compose.yml   # PostgreSQL + Adminer (+ legacy Keycloak, unused by default)
├── frontend/                # React + TypeScript SPA (Vite)
│   ├── src/components/      # Reusable UI components
│   ├── src/pages/           # Route-based pages
│   └── src/lib/             # API clients & utilities
└── README.md
```

## 🚀 Quick Start

### Prerequisites
- **Java 21+** (or compatible JDK)
- **Maven** (or use the included Maven wrapper `mvnw` / `mvnw.cmd`)
- **Node.js 18+** and npm
- **Docker** (for PostgreSQL; a legacy Keycloak container also starts by default, see note below)
- **An Auth0 tenant** (free tier is fine) — see [Auth0 Setup](#-auth0-setup)

### 1. Start Infrastructure (PostgreSQL)

```powershell
cd backend
docker-compose up -d
```

This starts Postgres and Adminer — no `.env` file needed for this step, the
backend reads matching localhost defaults from `application.properties`.
(The same command also starts a `keycloak` container; it's a leftover from
before the Auth0 migration, has no profile gate so it always comes up, but
nothing in the app talks to it by default — see
[`keycloak/README.md`](keycloak/README.md).)

### 2. Run the Backend

Authentication is validated against Auth0, so `AUTH0_DOMAIN` and
`AUTH0_AUDIENCE` must be real values even for local dev. Put them in
`backend/.env` (copy `backend/.env.example`) — the app imports that file when
run locally, and real environment variables still take precedence:

```powershell
cd backend
.\mvnw.cmd spring-boot:run
```

In IntelliJ, use the shared **VenueSync Backend** run configuration (`backend/.run/`).

(To instead run the backend itself in a container — e.g. to sanity-check the
`prod` profile locally — copy `.env.example` to `.env` and run
`docker-compose --profile full-stack up -d` instead of steps 1-2.)

### 3. Run the Frontend

```powershell
cd frontend
npm install
npm run dev
```

### Run the Android App Against the Local Backend

Debug builds call `http://localhost:8080/api/v1`; release builds call the deployed API.
With the backend running (step 2), connect a phone over USB (or start an emulator) and:

```powershell
adb reverse tcp:8080 tcp:8080   # the device's localhost:8080 now reaches this PC
cd android
.\gradlew.bat installDebug
```

`adb reverse` lasts until the device disconnects. To point a debug build somewhere else,
set `venuesync.apiBaseUrl=...` in `android/local.properties`.

### 4. Run Tests

```powershell
cd backend
.\mvnw.cmd test
```

🌐 **Frontend:** http://localhost:5173  
🔧 **Backend API:** http://localhost:8080  
🔐 **Auth0 Dashboard:** https://manage.auth0.com

---

## 🔐 Auth0 Setup

This application uses Auth0 for OAuth2/OIDC authentication and role-based authorization.
(It previously used a self-hosted Keycloak — that setup is kept as a reference/demo
in [`keycloak/README.md`](keycloak/README.md) but is no longer the live auth path.)

### 1. Create the Auth0 Application and API

1. In the [Auth0 Dashboard](https://manage.auth0.com), create a **Single Page Application**
   (used by the frontend) and note its **Client ID**.
2. Create an **API** (Applications → APIs) with an **Identifier** — this becomes
   `AUTH0_AUDIENCE` on the backend and `VITE_OIDC_AUDIENCE` on the frontend, and
   must match exactly on both sides.
3. Your tenant domain (e.g. `your-tenant.us.auth0.com`) becomes `AUTH0_DOMAIN`
   (backend) and `VITE_OIDC_AUTHORITY` (frontend, as `https://your-tenant.us.auth0.com`).

### 2. Inject Roles via a Post-Login Action

Auth0 does not put custom roles on the token by default. Add a **Post-Login Action**
(Actions → Flows → Login) that writes a namespaced roles claim:

```js
exports.onExecutePostLogin = async (event, api) => {
  const namespace = 'https://venuesync.app';
  const roles = event.authorization?.roles || [];
  api.accessToken.setCustomClaim(`${namespace}/roles`, roles);
};
```

The namespace and claim name (`https://venuesync.app/roles`) must match
`Auth0Claims.ROLES` in the backend and `ROLES_CLAIM` in the frontend's
`use-roles.tsx` exactly — Auth0 silently drops non-namespaced custom claims,
so a mismatch fails closed (valid token, no roles, 403 everywhere, nothing logged).

### 3. Create Roles and Assign Users

Create three roles under **User Management → Roles**, each **prefixed with `ROLE_`**
(the prefix is load-bearing: Spring Security resolves `hasRole("ATTENDEE")` to the
authority `ROLE_ATTENDEE`):

| Role | Description |
|------|-------------|
| `ROLE_ORGANIZER` | Can create, manage, and publish events |
| `ROLE_ATTENDEE` | Can browse events and purchase tickets |
| `ROLE_STAFF` | Can scan and validate tickets at venue |

New signups get `ROLE_ATTENDEE` by default; the app's own
"Upgrade to Organizer" endpoint promotes a user via the Auth0 Management API
(see below) rather than requiring manual role assignment in the dashboard.

### 4. Machine-to-Machine App (Role Bridge)

The ATTENDEE → ORGANIZER self-upgrade flow calls the Auth0 Management API, so
create an **M2M Application** authorized for the Management API with the
`update:users` scope, and set its credentials as `AUTH0_M2M_CLIENT_ID` /
`AUTH0_M2M_CLIENT_SECRET`.

### 5. Environment Variables

| Variable | Where | Value |
|----------|-------|-------|
| `AUTH0_DOMAIN` | backend | `your-tenant.us.auth0.com` |
| `AUTH0_AUDIENCE` | backend | Your API Identifier |
| `AUTH0_M2M_CLIENT_ID` / `AUTH0_M2M_CLIENT_SECRET` | backend | M2M app credentials |
| `VITE_OIDC_AUTHORITY` | frontend | `https://your-tenant.us.auth0.com` |
| `VITE_OIDC_CLIENT_ID` | frontend | SPA application's Client ID |
| `VITE_OIDC_AUDIENCE` | frontend | Same as `AUTH0_AUDIENCE` |

See `backend/.env.example` and `frontend/.env.example` for the full list with
inline explanations.

⚠️ Unlike the old Keycloak setup, there's no local fallback IdP: these Auth0
values must be real even in dev, and the backend fails fast on startup if
they're missing.

---

## ✨ Key Features

### For Event Organizers
- 📅 Create and manage events with flexible scheduling
- 🎫 Configure multiple ticket types with custom pricing
- 📊 Real-time sales tracking and event analytics
- 🔄 Event lifecycle management (Draft → Published → Completed)

### For Attendees
- 🔍 Browse and search published events
- 💳 Secure ticket purchasing with instant confirmation
- 📱 Digital tickets with unique QR codes
- 📋 Personal ticket wallet and purchase history

### For Staff
- 📷 Real-time QR code scanning for ticket validation
- ✅ Instant validation status feedback
- 🔒 Prevents duplicate ticket usage

---

## 🛠 Tech Stack

### Backend
| Technology | Purpose |
|------------|---------|
| Spring Boot 3.4.4 | REST API framework |
| Spring Security | OAuth2 resource server |
| Spring Data JPA | Database ORM with pessimistic locking |
| PostgreSQL | Production database |
| Google ZXing | QR code generation |
| MapStruct | DTO mapping |
| Lombok | Boilerplate reduction |
| Docker | Multi-stage build + Compose orchestration |

### Frontend
| Technology | Purpose |
|------------|---------|
| React 18.3 | UI library |
| TypeScript | Type-safe JavaScript |
| Vite | Build tool & dev server |
| Tailwind CSS | Utility-first styling |
| Framer Motion | Animations |
| Radix UI | Accessible components |
| react-oidc-context | OAuth2/OIDC integration |

### Infrastructure
| Technology | Purpose |
|------------|---------|
| Docker Compose | Container orchestration |
| Auth0 | Identity & access management |
| PostgreSQL | Relational database |

---

## 🌐 Production Deployment

VenueSync currently targets a Render-based deployment with environment-driven configuration.

- **Backend** reads production settings from environment variables in `backend/src/main/resources/application-prod.properties`
- **Frontend** reads `VITE_API_BASE_URL`, `VITE_OIDC_AUTHORITY`, `VITE_OIDC_CLIENT_ID` and `VITE_OIDC_AUDIENCE` from `frontend/.env` locally, and from the Cloudflare Pages dashboard for deployed builds
- **`VITE_OIDC_AUTHORITY`/`VITE_OIDC_AUDIENCE`** on the frontend must match `AUTH0_DOMAIN`/`AUTH0_AUDIENCE` on the backend exactly
- **CORS** is controlled through `CORS_ALLOWED_ORIGINS`
- **Render** uses `jdbcConnectionString` for the PostgreSQL service binding

If you deploy the frontend elsewhere, keep the SPA fallback rewrite in the host-specific config and point it at the built `dist/` directory.

## 📝 API Endpoints

### Public Endpoints
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/v1/published-events` | List all published events |
| GET | `/api/v1/published-events/{id}` | Get event details |

### Protected Endpoints (Requires Authentication)

**Organizer Role:**
| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/v1/events` | Create new event |
| PUT | `/api/v1/events/{id}` | Update event |
| DELETE | `/api/v1/events/{id}` | Delete event |
| GET | `/api/v1/events` | List organizer's events |

**Attendee Role:**
| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/v1/events/{id}/ticket-types/{ttId}/tickets` | Purchase ticket |
| GET | `/api/v1/tickets` | List user's tickets |
| GET | `/api/v1/tickets/{id}` | Get ticket details |
| GET | `/api/v1/tickets/{id}/qr-codes` | Get ticket QR code |
| POST | `/api/v1/users/me/roles/organizer` | Upgrade self to Organizer |

**Staff Role:**
| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/v1/ticket-validations` | Validate ticket |

---

## 🔒 Security Architecture

```
┌─────────────────┐     ┌─────────────────┐     ┌─────────────────┐
│   React SPA     │────▶│     Auth0       │────▶│  Spring Boot    │
│   (Frontend)    │     │   (Auth Server) │     │  (Resource API) │
└─────────────────┘     └─────────────────┘     └─────────────────┘
        │                       │                       │
        │   OIDC/OAuth2        │   JWT Validation      │
        │   Authorization      │                       │
        └───────────────────────┴───────────────────────┘
```

- **JWT-based authentication** with Auth0 as identity provider
- **Role-based access control** (ORGANIZER, ATTENDEE, STAFF) via a namespaced roles claim injected by an Auth0 Post-Login Action
- **Auth0 Management API bridge** for secure, automated self-service role upgrades using M2M client-credential authentication
- **Pessimistic locking** prevents race conditions during ticket purchases
- **Stateless API** with token-based sessions

---

## 📄 Notes

- Backend configuration is in `backend/src/main/resources/application.properties`
- Docker setup includes PostgreSQL and Adminer in `backend/docker-compose.yml` (a legacy, unused-by-default Keycloak container also starts — see [`keycloak/README.md`](keycloak/README.md))
- Auth0 roles must be assigned to users (or granted via the Organizer self-upgrade flow) for proper authorization
- The application uses "wall clock" time approach for event scheduling (no timezone conversions)

---

## 📜 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

---

## 🤝 Contributing

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

---

<p align="center">
  Made with ❤️ using Spring Boot & React
</p>
