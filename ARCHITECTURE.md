# VenueSync — Architecture & Restructuring Guide

## Table of Contents

1. [Project Vision](#project-vision)
2. [Architecture Decision: Modular Monolith](#architecture-decision-modular-monolith)
3. [Backend Architecture](#backend-architecture)
4. [Frontend Architecture](#frontend-architecture)
5. [Cloud Deployment Strategy](#cloud-deployment-strategy)
6. [Feature Roadmap](#feature-roadmap)
7. [Monetization Models](#monetization-models)
8. [Resume Bullet Points](#resume-bullet-points)

---

## Project Vision

VenueSync is evolving from a **ticketing platform** into a combined **Event + CFP (Call for Proposals) Management Platform** — merging the functionality of Eventbrite (event management & ticketing) with Sessionize (speaker submissions & session management).

### Tech Stack

| Layer      | Technology                                                        |
| ---------- | ----------------------------------------------------------------- |
| Backend    | Spring Boot 3.4.4, Java 21, Maven                                |
| Frontend   | React 19, TypeScript 5.7, Vite, shadcn/ui                        |
| Database   | PostgreSQL with JPA/Hibernate                                     |
| Auth       | Keycloak OAuth2/OIDC with multi-role RBAC (Organizer/Attendee/Staff) |
| Mapping    | MapStruct 1.6.3                                                   |
| QR Codes   | ZXing                                                             |
| Deployment | Monorepo (root `package.json` + `vercel.json` for frontend)      |

---

## Architecture Decision: Modular Monolith

### Why Not Microservices?

| Factor             | Microservices                          | Modular Monolith (Chosen)             |
| ------------------ | -------------------------------------- | ------------------------------------- |
| **Complexity**     | High — service discovery, API gateways, distributed tracing | Low — single deployable JAR |
| **Cost**           | $50-150+/month (multiple containers)   | $10-25/month (single instance)        |
| **DevOps**         | Kubernetes, Docker Compose, CI/CD per service | Single CI/CD pipeline          |
| **Team Size**      | Best for 5+ developers                 | Perfect for 1-3 developers            |
| **Data**           | Distributed transactions are painful   | Single database, simple transactions  |
| **Debugging**      | Log aggregation across services        | Single log stream                     |
| **Migration Path** | Hard to go back                        | Easy to extract into microservices later |

### Key Insight

> **Start monolithic, split when you need to.** A modular monolith gives you clean module boundaries (like microservices) without the operational overhead. When a module needs independent scaling, you extract it — the boundaries are already there.

### When to Consider Microservices

- 10,000+ concurrent users needing independent scaling
- Multiple teams (5+) working on different modules
- Need to deploy modules on completely different schedules
- A specific module needs a different tech stack (e.g., Python ML for recommendations)

---

## Backend Architecture

### Before: Layer-Based Structure (Old)

```
com.fullstack.venuesync/
├── config/           ← All configs mixed together
├── controllers/      ← All controllers in one package
├── domain/
│   ├── entities/     ← All JPA entities together
│   └── dtos/         ← All DTOs together
├── exceptions/       ← All exceptions together
├── filters/
├── mappers/          ← All MapStruct mappers together
├── repositories/     ← All repositories together
├── services/
│   └── impl/         ← All service implementations together
└── util/
```

**Problems with this approach:**

- As the app grows, finding related code requires jumping across 8+ packages
- No clear boundaries — any class can import any other class
- Hard to extract a feature into its own service later
- A new developer can't quickly understand what the "tickets" feature involves

### After: Modular Monolith Structure (New)

```
com.fullstack.venuesync/
├── VenueSyncApplication.java          ← Entry point (scans all sub-packages)
│
├── shared/                            ← Cross-cutting concerns
│   ├── config/
│   │   ├── SecurityConfig.java        ← Security filter chain, CORS, OAuth2
│   │   ├── JpaConfiguration.java      ← JPA auditing
│   │   ├── JwtAuthenticationConverter.java ← Keycloak role extraction
│   │   ├── TimezoneConfig.java        ← JVM timezone setup
│   │   ├── ScheduledTasksConfig.java  ← Cron jobs (ticket expiry, event completion)
│   │   └── DataLoader.java           ← Sample data seeding
│   ├── security/
│   │   └── JwtUtil.java              ← JWT userId parsing utility
│   ├── filters/
│   │   └── UserProvisioningFilter.java ← Auto-provisions users from Keycloak JWT
│   ├── domain/
│   │   ├── User.java                 ← User JPA entity (shared across modules)
│   │   ├── UserRepository.java       ← User data access
│   │   └── ErrorDto.java             ← Standard error response
│   └── exceptions/
│       ├── VenueSyncException.java    ← Base exception for all modules
│       ├── UserNotFoundException.java
│       └── GlobalExceptionHandler.java ← @RestControllerAdvice (handles all module exceptions)
│
├── events/                            ← Event Management module
│   ├── domain/
│   │   ├── Event.java                ← Event JPA entity
│   │   ├── EventStatusEnum.java      ← DRAFT, PUBLISHED, CANCELLED, COMPLETED
│   │   ├── CreateEventRequest.java
│   │   └── UpdateEventRequest.java
│   ├── dto/
│   │   ├── CreateEventRequestDto.java
│   │   ├── CreateEventResponseDto.java
│   │   ├── UpdateEventRequestDto.java
│   │   ├── UpdateEventResponseDto.java
│   │   ├── GetEventDetailsResponseDto.java
│   │   ├── GetEventDetailsTicketTypesResponseDto.java
│   │   ├── GetPublishedEventDetailsResponseDto.java
│   │   ├── GetPublishedEventDetailsTicketTypesResponseDto.java
│   │   ├── ListEventResponseDto.java
│   │   └── ListPublishedEventResponseDto.java
│   ├── repository/
│   │   └── EventRepository.java      ← Search, pagination, bulk update queries
│   ├── mapper/
│   │   └── EventMapper.java          ← MapStruct mapper
│   ├── service/
│   │   ├── EventService.java         ← Interface
│   │   ├── EventServiceImpl.java     ← CRUD, search, filtering
│   │   ├── EventStatusService.java   ← Interface
│   │   └── EventStatusServiceImpl.java ← Auto-completes ended events
│   ├── controller/
│   │   ├── EventController.java      ← /api/v1/events (organizer CRUD)
│   │   └── PublishedEventController.java ← /api/v1/published-events (public)
│   └── exception/
│       ├── EventNotFoundException.java
│       ├── EventUpdateException.java
│       └── SalesPeriodException.java
│
├── tickets/                           ← Ticket Management module
│   ├── domain/
│   │   ├── Ticket.java               ← Ticket JPA entity
│   │   ├── TicketStatusEnum.java     ← PURCHASED, USED, EXPIRED, CANCELLED
│   │   ├── TicketType.java           ← Ticket type entity (VIP, General, etc.)
│   │   ├── CreateTicketTypeRequest.java
│   │   └── UpdateTicketTypeRequest.java
│   ├── dto/
│   │   ├── CreateTicketTypeRequestDto.java
│   │   ├── CreateTicketTypeResponseDto.java
│   │   ├── UpdateTicketTypeRequestDto.java
│   │   ├── UpdateTicketTypeResponseDto.java
│   │   ├── GetTicketResponseDto.java
│   │   ├── ListTicketResponseDto.java
│   │   ├── ListTicketTicketTypeResponseDto.java
│   │   └── ListEventTicketTypeResponseDto.java
│   ├── repository/
│   │   ├── TicketRepository.java     ← Active/past tickets, expiration queries
│   │   └── TicketTypeRepository.java ← Pessimistic locking for purchases
│   ├── mapper/
│   │   └── TicketMapper.java         ← MapStruct mapper with nested event fields
│   ├── service/
│   │   ├── TicketService.java        ← Interface
│   │   ├── TicketServiceImpl.java    ← List active/past tickets
│   │   ├── TicketTypeService.java    ← Interface
│   │   ├── TicketTypeServiceImpl.java ← Purchase with pessimistic locking
│   │   ├── TicketExpirationService.java ← Interface
│   │   └── TicketExpirationServiceImpl.java ← Scheduled ticket expiry
│   ├── controller/
│   │   ├── TicketController.java     ← /api/v1/tickets (attendee views)
│   │   └── TicketTypeController.java ← /api/v1/events/{id}/ticket-types (purchase)
│   └── exception/
│       ├── TicketNotFoundException.java
│       ├── TicketsSoldOutException.java
│       └── TicketTypeNotFoundException.java
│
└── validation/                        ← Ticket Validation & QR Code module
    ├── domain/
    │   ├── QrCode.java               ← QR code JPA entity
    │   ├── QrCodeStatusEnum.java     ← ACTIVE, EXPIRED
    │   ├── TicketValidation.java     ← Validation attempt entity
    │   ├── TicketValidationMethod.java ← QR_SCAN, MANUAL
    │   └── TicketValidationStatusEnum.java ← VALID, INVALID, EXPIRED, ALREADY_USED
    ├── dto/
    │   ├── TicketValidationRequestDto.java
    │   └── TicketValidationResponseDto.java
    ├── config/
    │   └── QrCodeConfig.java         ← QRCodeWriter bean
    ├── repository/
    │   ├── QrCodeRepository.java
    │   └── TicketValidationRepository.java
    ├── mapper/
    │   └── TicketValidationMapper.java
    ├── service/
    │   ├── QrCodeService.java        ← Interface
    │   ├── QrCodeServiceImpl.java    ← Generate & decode QR codes (ZXing)
    │   ├── TicketValidationService.java ← Interface
    │   └── TicketValidationServiceImpl.java ← QR scan & manual validation logic
    ├── controller/
    │   └── TicketValidationController.java ← /api/v1/ticket-validations
    └── exception/
        ├── QrCodeNotFoundException.java
        └── QrCodeGenerationException.java
```

### Module Dependency Rules

```
┌─────────────┐     ┌─────────────┐     ┌──────────────┐
│   events    │────→│   tickets   │────→│  validation  │
└─────────────┘     └─────────────┘     └──────────────┘
       │                   │                    │
       └───────────────────┴────────────────────┘
                           │
                    ┌──────┴──────┐
                    │   shared    │
                    └─────────────┘
```

- **All modules** depend on `shared` (User entity, exceptions, security)
- **events** → references `tickets.domain.TicketType` (events have ticket types)
- **tickets** → references `events.domain.Event` (tickets belong to events), `validation.service.QrCodeService` (generates QR on purchase)
- **validation** → references `tickets.domain.Ticket` and `tickets.repository.TicketRepository` (validates tickets)
- **Cross-module communication** happens through service interfaces, not direct repository access where possible

### Key Naming Changes

| Old                    | New                             |
| ---------------------- | ------------------------------- |
| `EventTicketException` | `VenueSyncException`            |
| `domain.entities.*`    | `{module}.domain.*`             |
| `domain.dtos.*`        | `{module}.dto.*`                |
| `services.impl.*`      | `{module}.service.*Impl`        |
| `controllers.*`        | `{module}.controller.*`         |
| `repositories.*`       | `{module}.repository.*`         |
| `mappers.*`            | `{module}.mapper.*`             |

---

## Frontend Architecture

### Current Structure (Keep As-Is)

```
frontend/src/
├── assets/              ← Static assets
├── components/
│   ├── common/          ← Reusable components (empty-state, pagination, etc.)
│   ├── events/          ← Event-specific components (event-card, event-grid, etc.)
│   ├── forms/           ← Form components (search-bar)
│   ├── layout/          ← Layout components (navbar, footer, page-container)
│   ├── tickets/         ← Ticket-specific components (ticket-card)
│   └── ui/              ← shadcn/ui primitives (button, card, dialog, etc.)
├── domain/
│   └── domain.ts        ← TypeScript type definitions
├── hooks/
│   ├── use-mobile.ts    ← Responsive breakpoint hook
│   └── use-roles.tsx    ← Role-based access hook
├── lib/
│   ├── api.ts           ← API client (Axios/fetch wrapper)
│   ├── date-utils.ts    ← Date formatting utilities
│   └── utils.ts         ← General utilities (cn, etc.)
├── pages/               ← Page-level components (18 pages)
├── index.css
└── main.tsx             ← App entry point
```

### Why NOT Restructure the Frontend Right Now

1. **"Modular monolith" is a backend pattern** — it addresses data ownership, domain logic, and service boundaries. Frontends are stateless UI — they don't own data or databases.

2. **The frontend is small (~25 component files, 18 pages)** — restructuring adds complexity without real benefit at this scale.

3. **It's already feature-grouped** — `components/events/`, `components/tickets/` already provide clear groupings.

4. **Convention over configuration** — the current `pages/` + `components/` + `lib/` structure is the most common React pattern and is immediately recognizable to any React developer.

### When to Restructure the Frontend

Restructure when the frontend roughly **doubles in size** — specifically when the CFP module is added. The trigger points:

- `pages/` grows to **40+ files**
- `domain.ts` grows to **500+ lines**
- You have **3+ API client files** (`events-api.ts`, `tickets-api.ts`, `cfp-api.ts`)
- New developers take more than 5 minutes to find where a feature's code lives

### Future Frontend Structure (Feature-Based)

When the time comes, restructure to this **feature-based** pattern:

```
frontend/src/
├── features/
│   ├── events/
│   │   ├── components/       (event-card, event-grid, event-hero)
│   │   ├── pages/            (all-events-page, dashboard-manage-event-page)
│   │   ├── hooks/            (useEvents, useEventForm)
│   │   ├── api.ts            (event API calls)
│   │   └── types.ts          (event-related TypeScript types)
│   ├── tickets/
│   │   ├── components/       (ticket-card, ticket-selector)
│   │   ├── pages/            (purchase-ticket-page, dashboard-list-tickets)
│   │   ├── hooks/            (useTickets)
│   │   ├── api.ts            (ticket API calls)
│   │   └── types.ts
│   ├── validation/
│   │   ├── pages/            (dashboard-validate-qr-page)
│   │   ├── api.ts
│   │   └── types.ts
│   └── cfp/                  (future)
│       ├── components/       (proposal-form, reviewer-dashboard)
│       ├── pages/            (submit-proposal, review-proposals)
│       ├── hooks/
│       ├── api.ts
│       └── types.ts
├── shared/
│   ├── components/           (ui/, common/, layout/)
│   ├── hooks/                (use-mobile, use-roles)
│   ├── lib/                  (utils, date-utils)
│   └── types/                (shared TypeScript types)
├── pages/                    (static pages: about, contact, terms, privacy)
├── index.css
└── main.tsx
```

---

## Cloud Deployment Strategy

### Recommended: AWS (with $100 Student Credits)

#### Estimated Monthly Cost: **$10–25/month**

| Service                     | Purpose                        | Cost          |
| --------------------------- | ------------------------------ | ------------- |
| EC2 t3.micro (free tier)    | Spring Boot backend            | $0–8/month    |
| RDS PostgreSQL (free tier)  | Database                       | $0–15/month   |
| S3                          | Static assets, QR code storage | $0.50/month   |
| CloudFront                  | CDN for frontend               | $1/month      |
| Keycloak on EC2 or ECS      | Auth server                    | Shared with backend EC2 |
| **Total**                   |                                | **$10–25/month** |

#### Deployment Architecture

```
                    ┌──────────────┐
    Users ─────────→│  CloudFront  │──→ S3 (React SPA)
                    └──────┬───────┘
                           │ /api/*
                    ┌──────▼───────┐
                    │   ALB / EC2  │──→ Spring Boot JAR
                    └──────┬───────┘
                           │
                    ┌──────▼───────┐
                    │ RDS Postgres │
                    └──────────────┘
                           │
                    ┌──────▼───────┐
                    │   Keycloak   │ (same EC2 or separate)
                    └──────────────┘
```

#### Alternative Cloud Providers Comparison

| Provider          | Free Tier               | Best For           | Monthly Cost |
| ----------------- | ----------------------- | ------------------ | ------------ |
| **AWS**           | 12 months + $100 credit | Full control       | $10–25       |
| **Railway**       | $5/month credit         | Simplest deploy    | $5–20        |
| **Render**        | Free tier available     | Heroku replacement | $7–25        |
| **Azure**         | $200 credit + free tier | Enterprise/resume  | $15–30       |
| **GCP**           | $300 credit             | Scaling later      | $10–25       |

### Deployment Roadmap

| Phase | When          | Action                                          |
| ----- | ------------- | ------------------------------------------------ |
| 1     | Now           | Local development with Docker Compose (Postgres + Keycloak) |
| 2     | MVP ready     | Deploy to Railway or Render (easiest)            |
| 3     | With credits  | Migrate to AWS (EC2 + RDS + S3 + CloudFront)    |
| 4     | If scaling    | Add auto-scaling group, ElastiCache (Redis)      |

---

## Feature Roadmap

### Phase 1: Current (Completed ✅)

- Event CRUD (create, update, delete, list)
- Event publishing workflow (DRAFT → PUBLISHED → COMPLETED)
- Ticket types with flexible pricing
- Ticket purchasing with pessimistic locking (no overselling)
- QR code generation on purchase
- QR scan + manual ticket validation
- Multi-role RBAC (Organizer, Attendee, Staff)
- User auto-provisioning from Keycloak
- Scheduled tasks (ticket expiry, event auto-completion)
- Search and pagination for published events

### Phase 2: Platform Polish (Next)

- Email notifications (purchase confirmation, event reminders)
- Event images and rich descriptions
- Attendee profile pages
- Ticket transfer between users
- Event analytics dashboard (sales, check-ins, revenue)
- Stripe/PayPal payment integration

### Phase 3: CFP Module (Event + Sessionize Fusion)

- Call for Proposals creation per event
- Speaker profile and submission portal
- Proposal review and scoring system
- Session scheduling and track management
- Speaker notifications (accepted/rejected/waitlisted)
- Public event agenda page

### Phase 4: Platform Scale

- Multi-tenant support (white-label for organizations)
- Webhook integrations (Slack, Discord, Zapier)
- API keys for third-party integrations
- Mobile app (React Native)
- Advanced analytics and reporting
- AI-powered session recommendations

---

## Monetization Models

| Model             | Description                                    | Implementation Complexity |
| ------------------ | ---------------------------------------------- | ------------------------- |
| **Freemium**       | Free for small events (< 50 attendees), paid for larger | Low               |
| **Per-ticket fee** | $0.50–2.00 per ticket sold + payment processing | Medium                   |
| **SaaS tiers**     | Free / Pro ($29/mo) / Enterprise ($99/mo)       | Medium                   |
| **White-label**    | Custom branding for organizations ($199+/mo)    | High                     |

**Recommended starting model:** Freemium + per-ticket fee (most common in the events industry — same as Eventbrite and Luma).

---

## Resume Bullet Points

Use these for your resume/portfolio when describing VenueSync:

- **Architected a modular monolith** backend using Spring Boot 3.4 and Java 21 with 4 feature modules (events, tickets, validation, shared), enabling clean domain boundaries and future microservice extraction
- **Implemented role-based access control** using Keycloak OAuth2/OIDC with automatic user provisioning, supporting Organizer, Attendee, and Staff roles
- **Built a ticket purchasing system** with pessimistic database locking to prevent overselling under concurrent load
- **Designed QR code-based ticket validation** using ZXing, supporting both QR scan and manual validation with real-time status tracking (VALID, EXPIRED, ALREADY_USED)
- **Developed a React 19 + TypeScript SPA** with Vite, shadcn/ui, and feature-grouped component architecture
- **Implemented automated event lifecycle management** with scheduled tasks for ticket expiration and event completion based on date-driven state transitions
- **Built RESTful APIs** with Spring Data JPA, MapStruct DTO mapping, pagination, full-text search, and comprehensive exception handling

---

## File Counts Summary

| Module       | Files Created | Key Concerns                          |
| ------------ | ------------- | ------------------------------------- |
| `shared`     | 14            | Security, auth, config, User entity, base exceptions |
| `events`     | 24            | Event CRUD, publishing, search        |
| `tickets`    | 22            | Purchasing, locking, expiration       |
| `validation` | 15            | QR generation, scan, manual validation |
| **Total**    | **75+ new files** | (85 total including Application class and test) |

---

*Last updated: February 13, 2026*
*Architecture restructuring completed and verified — `mvn clean compile` passes with 0 errors.*
