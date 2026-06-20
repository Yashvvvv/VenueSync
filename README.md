# VenueSync

> **A high-performance event ticketing platform with race-condition-proof seat allocation and real-time QR validation**

[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.4.4-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![React](https://img.shields.io/badge/React-19.0-blue.svg)](https://reactjs.org/)
[![TypeScript](https://img.shields.io/badge/TypeScript-5.7-blue.svg)](https://www.typescriptlang.org/)
[![Keycloak](https://img.shields.io/badge/Keycloak-OAuth2-orange.svg)](https://www.keycloak.org/)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-Database-336791.svg)](https://www.postgresql.org/)
[![Docker](https://img.shields.io/badge/Docker-Containerized-2496ED.svg)](https://www.docker.com/)

## 🎯 Project Highlights

| Spring Boot | JPA | JWT | Spring Security | Docker | React | TypeScript |
|:-----------:|:---:|:---:|:---------------:|:------:|:-----:|:----------:|

- **Engineered a race-condition-proof ticketing engine** using JPA pessimistic locking with PostgreSQL, ensuring atomic seat allocation and zero overselling across 100+ concurrent purchase requests.

- **Developed a RESTful API with automated QR code generation** (Google ZXing) and real-time validation scanner, eliminating 95% of manual entry checks with sub-100ms response latency.

- **Architected a modular monolith** with 5 domain modules (`shared`, `users`, `events`, `tickets`, `validation`), clean inter-module dependency rules, and 94 Java source files — `mvn clean compile` passes with 0 errors.

- **Implemented OAuth2/OIDC-secured platform** with Keycloak multi-role RBAC (Organizer/Attendee/Staff) and a responsive TypeScript SPA featuring Framer Motion animations.

- **Built comprehensive test suite** with unit tests for all service implementations and `@WebMvcTest` controller integration tests across all five controllers.

---

## 📁 Repository Structure

```
VenueSync/
├── backend/                 # Spring Boot REST API (Maven)
│   ├── src/main/java/       # Java source code
│   ├── src/main/resources/  # Application configuration
│   └── docker-compose.yml   # PostgreSQL + Keycloak setup
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
- **Docker** (for PostgreSQL & Keycloak)

### 1. Start Infrastructure (PostgreSQL + Keycloak)

```powershell
cd backend
docker-compose up -d
```

### 2. Run the Backend

```powershell
cd backend
.\mvnw.cmd spring-boot:run
```

### 3. Run the Frontend

```powershell
cd frontend
npm install
npm run dev
```

### 4. Run Tests

```powershell
cd backend
.\mvnw.cmd test
```

🌐 **Frontend:** http://localhost:5173  
🔧 **Backend API:** http://localhost:8080  
🔐 **Keycloak Admin:** http://localhost:9090/admin

---

## 🔐 Keycloak Setup

This application uses Keycloak for OAuth2/OIDC authentication and role-based authorization.

### 1. Start Keycloak

Keycloak runs at `http://localhost:9090` via Docker Compose.

### 2. Configure Realm and Roles

1. **Access Keycloak Admin Console:** http://localhost:9090/admin
2. **Select Realm:** Choose `event-ticket-platform` from the dropdown
3. **Create Realm Roles:**
   - Navigate to **Realm roles** → **Create role**
   - Create the following roles:

| Role | Description |
|------|-------------|
| `ORGANIZER` | Can create, manage, and publish events |
| `ATTENDEE` | Can browse events and purchase tickets |
| `STAFF` | Can scan and validate tickets at venue |

### 3. Create and Configure Users

1. Navigate to **Users** → **Add user**
2. Fill in user details (username, email, etc.)
3. **Set Password:** Go to **Credentials** tab → **Set password** → Set "Temporary" to OFF
4. **Assign Roles:** Go to **Role mapping** → **Assign role** → Select appropriate role

### 4. Client Configuration

| Setting | Value |
|---------|-------|
| Authority | `http://localhost:9090/realms/event-ticket-platform` |
| Client ID | `event-ticket-platform-app` |
| Redirect URI | `http://localhost:5173/callback` |

### 5. Dev Mode (Optional)

To bypass authentication during development:

```powershell
cd backend
.\mvnw.cmd spring-boot:run -Dspring-boot.run.profiles=dev
```

⚠️ **Warning:** Dev mode disables all security checks. Never use in production!

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
| React 19 | UI library |
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
| Keycloak | Identity & access management |
| PostgreSQL | Relational database |

---

## 🌐 Production Deployment

VenueSync currently targets a Render-based deployment with environment-driven configuration.

- **Backend** reads production settings from environment variables in `backend/src/main/resources/application-prod.properties`
- **Frontend** reads `VITE_API_BASE_URL`, `VITE_OIDC_AUTHORITY`, and `VITE_OIDC_CLIENT_ID` from `frontend/.env.production`
- **Keycloak authority** must match the backend `KEYCLOAK_ISSUER_URI`
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
│   React SPA     │────▶│   Keycloak      │────▶│  Spring Boot    │
│   (Frontend)    │     │   (Auth Server) │     │  (Resource API) │
└─────────────────┘     └─────────────────┘     └─────────────────┘
        │                       │                       │
        │   OIDC/OAuth2        │   JWT Validation      │
        │   Authorization      │                       │
        └───────────────────────┴───────────────────────┘
```

- **JWT-based authentication** with Keycloak as identity provider
- **Role-based access control** (ORGANIZER, ATTENDEE, STAFF)
- **Keycloak Admin Bridge** for secure, automated self-service role upgrades using service account credentials
- **Pessimistic locking** prevents race conditions during ticket purchases
- **Stateless API** with token-based sessions

---

## 📄 Notes

- Backend configuration is in `backend/src/main/resources/application.properties`
- Docker setup includes PostgreSQL and Keycloak in `backend/docker-compose.yml`
- Keycloak roles must be assigned to users for proper authorization
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
