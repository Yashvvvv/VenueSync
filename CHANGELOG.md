# Changelog

All notable changes to VenueSync will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [1.0.0] - 2024-12-09

### 🎉 Initial Release

#### Core Features
- **Event Management**
  - Create, update, and delete events with flexible scheduling
  - Multiple ticket types per event with custom pricing
  - Event lifecycle management (Draft → Published → Cancelled → Completed)
  - Sales period configuration with automatic enforcement

- **Ticketing System**
  - Race-condition-proof ticket purchasing using JPA pessimistic locking
  - Atomic seat allocation preventing overselling during concurrent purchases
  - Automatic QR code generation for each ticket (Google ZXing)
  - Digital ticket wallet for attendees

- **QR Validation**
  - Real-time QR code scanning for ticket validation
  - Duplicate usage prevention (ticket marked as USED after first scan)
  - Validation status feedback (VALID, INVALID, EXPIRED, ALREADY_USED)
  - Manual ticket ID entry as fallback

- **Authentication & Authorization**
  - OAuth2/OIDC integration with Keycloak
  - Role-based access control (ORGANIZER, ATTENDEE, STAFF)
  - JWT-based stateless authentication
  - Protected routes on frontend with role verification

#### Technical Implementation

##### Backend
- Spring Boot 3.4.4 REST API
- Spring Security OAuth2 Resource Server
- Spring Data JPA with PostgreSQL
- Pessimistic locking for concurrent purchase handling
- MapStruct for DTO mapping
- Bean Validation for request validation
- Global exception handling with custom error responses

##### Frontend
- React 19 with TypeScript
- Vite build tool for fast development
- Tailwind CSS with custom design system
- Framer Motion animations
- Radix UI accessible components
- react-oidc-context for authentication
- Responsive design (mobile-first approach)

##### Infrastructure
- Docker Compose for local development
- PostgreSQL database
- Keycloak identity management
- CORS configuration for frontend-backend communication

#### Security Features
- JWT token validation on all protected endpoints
- Role-based endpoint protection
- Pessimistic database locking for data integrity
- Input validation and sanitization

---

## [1.1.0] - 2026-03-09

### ✨ Enhancements & Fixes

#### Ticket Lifecycle Management
- **Ticket status tracking**: Full `PURCHASED → USED → EXPIRED / CANCELLED` lifecycle with automatic expiration on event end
- **Scheduled expiration job**: `TicketExpirationService` runs every 5 minutes, marking tickets `EXPIRED` when their event has ended and they were never scanned
- **Automatic event completion**: `EventStatusServiceImpl` scheduled task auto-completes events (`PUBLISHED → COMPLETED`) based on end date
- **Active / Past ticket filtering**: `GET /api/v1/tickets?filter=active|past` endpoint added; frontend shows Upcoming / Past tabs

#### Timezone & Date Handling
- **Wall-clock time approach**: All event times stored as `LocalDateTime` (no timezone offsets). Frontend sends ISO strings without a `Z` suffix; backend stores them exactly
- **JVM timezone config**: `TimezoneConfig.java` reads `app.timezone=Asia/Kolkata` from properties and pins the JVM at startup
- **PostgreSQL timezone**: Keycloak and DB containers configured to match application timezone

#### Sales Period Validation
- Backend enforces `salesStart` / `salesEnd` on purchase attempts with descriptive `SalesPeriodException` responses

#### Organizer Event Filtering
- `GET /api/v1/events?status=DRAFT|PUBLISHED|CANCELLED|COMPLETED` with paginated results
- `GET /api/v1/events/counts` — per-status count map for organizer dashboard badges

#### Role-Based Access Control (RBAC)
- `/api/v1/tickets/**` — restricted to `ROLE_ATTENDEE`
- `/api/v1/events/*/ticket-types/*/tickets` (purchase) — restricted to `ROLE_ATTENDEE`
- `RoleProtectedRoute` component added to the React SPA for frontend route guards
- Navbar updated to show role-specific links (Events / Tickets / Validate QR)

#### QR Code & Validation Error Handling
- Improved error messages for `ALREADY_USED`, `EXPIRED`, `INVALID` scan results
- `dashboard-validate-qr-page.tsx` shows user-friendly status descriptions

#### Backend Architecture — Modular Monolith
- Restructured from layer-based to 4-module layout: `shared`, `events`, `tickets`, `validation`
- 85 Java source files, `mvn clean compile` passes with 0 errors
- Base exception renamed `EventTicketException → VenueSyncException`
- All module packages follow `com.fullstack.venuesync.{module}/domain|dto|repository|mapper|service|controller|exception`

#### Infrastructure
- `docker-compose.yml` updated with `postgres-data` and `keycloak-data` named volumes for data persistence
- PostgreSQL mapped to host port `5433` to avoid conflict with local installs
- Adminer added on port `8888`
- `.gitignore` added for `target/`, `node_modules/`, IDE files

#### Testing
- Unit tests added for all service implementations (`EventServiceImpl`, `EventStatusServiceImpl`, `TicketServiceImpl`, `TicketTypeServiceImpl`, `TicketExpirationServiceImpl`, `QrCodeServiceImpl`, `TicketValidationServiceImpl`)
- Controller tests added with `@WebMvcTest` + `MockMvc` for all five controllers
- H2 in-memory database used in test profile

#### UI Polish
- `switch.tsx` dark unchecked state (`bg-zinc-700`)
- Ticket card status badges with colour-coded icons (`PURCHASED` green, `USED` blue, `EXPIRED` yellow, `CANCELLED` red)
- Auto-refresh on ticket view page
- Expired ticket display correct on `dashboard-list-tickets.tsx`

### 🐛 Bug Fixes
- Fixed `null` availability on ticket types when `availableQuantity` not initialised
- Removed `DevSecurityConfig.java` and `DevDataInitializer.java` (dev-only classes that leaked into other profiles)
- Fixed database constraint names for `tickets_status_check` and `ticket_validations_status_check`

---

## [Unreleased]

### Planned Features
- Email notifications (purchase confirmation, event reminders)
- Event analytics dashboard (sales over time, revenue, check-in rate)
- Stripe/PayPal payment integration and refund workflow
- CFP (Call for Proposals) module — speaker submissions, proposal review, session scheduling
- Drag-and-drop agenda builder
- Multi-language support
- Dark / Light theme toggle
- PWA support for mobile devices
- Production deployment (AWS EC2 + RDS + S3 + CloudFront)
- Externalized configuration (environment variables for DB, Keycloak, CORS)

---

## Version History

| Version | Date | Description |
|---------|------|-------------|
| 1.1.0 | 2026-03-09 | Ticket lifecycle, RBAC, modular monolith, tests, infrastructure |
| 1.0.0 | 2024-12-09 | Initial release with core ticketing features |
