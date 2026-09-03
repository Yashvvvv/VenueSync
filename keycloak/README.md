# Self-hosted Keycloak setup (reference / demo — not the live auth path)

This folder is a **self-contained record of a self-hosted Keycloak deployment** that was
built and run in production for VenueSync before the project moved to a hosted identity
provider. It's kept — not deleted — because it demonstrates real OIDC/IAM and container
tuning work worth walking someone through. It is not wired into the current deployment;
nothing outside this folder depends on these files at runtime except the local Docker
Compose stack in `backend/docker-compose.yml`, which still runs a *stock* Keycloak image
for local development.

## Why this existed

VenueSync uses OAuth2/OIDC for authentication with three realm roles
(`ORGANIZER`, `ATTENDEE`, `STAFF`). Keycloak was the identity provider: it issues the JWTs
that `backend/src/main/resources/application*.properties`
(`spring.security.oauth2.resourceserver.jwt.issuer-uri`) validates on every API request.

## Why it moved off Render

Render's free tier grants **750 instance-hours/month, shared across the whole account** —
not per service. Running the backend *and* Keycloak as two always-on free services burns
2 × 24 = 48 hours/day, which exhausts the monthly pool in ~15.6 days
(750 ÷ 48). That's the exact math behind the free hours disappearing — it wasn't
misconfiguration, it was two always-on services on one shared free-hour budget.
The project moved authentication to a managed IDP to remove the second always-on service
entirely, rather than working around Render's limits indefinitely.

## What's in this folder

| File | Purpose |
|---|---|
| `Dockerfile` | Custom Keycloak image built for Render's free tier: Postgres storage (survives restarts — Render's free web services have no persistent disk), `--cache=local` (drops clustering overhead), and a capped JVM heap so the whole process stays under ~400MB. |
| `RENDER_DEPLOY.md` | Step-by-step guide to deploying this image on Render, including which env vars each service needs and how they cross-reference each other. |
| `sync-to-render.ps1` | Pushes a local realm export (roles, the `event-ticket-platform-app` client, redirect URIs, test users) to a live Keycloak instance via its Admin REST API — reproduces the whole realm without manually clicking through the admin console. |
| `event-ticket-platform-realm.json` | Exported realm: client config, realm roles, redirect/logout URIs. |
| `event-ticket-platform-users-0.json` | Exported test users with hashed credentials (`organizer`, `attendee`, `staff`). |

The last three files are gitignored (they're local-only, not pushed to the repo) — see the
root `.gitignore` entries under "Keycloak — only Dockerfile is tracked."

## Reviving it for a demo or interview walkthrough

You don't need Render for this — everything can run locally:

```powershell
cd backend
docker-compose up -d      # brings up Postgres, Adminer, and Keycloak together
```

Keycloak comes up at `http://localhost:9090` (admin console at `/admin`,
credentials from `backend/.env` / `.env.example`). The realm import used locally is the
stock Keycloak dev-file store defined in `backend/docker-compose.yml` — to walk through
the *Render-specific* image (Postgres-backed, memory-capped) instead, build `keycloak/Dockerfile`
directly:

```powershell
docker build -t venuesync-keycloak-render ./keycloak
```

To talk through the redeploy-automation story, open `sync-to-render.ps1` — it authenticates
as the Keycloak admin, imports the realm/users if they don't already exist, and rewrites the
client's redirect/logout URIs for whatever frontend URL you pass in. That script is the piece
that turns "reconfigure Keycloak by hand after every redeploy" into "run one command."

## Current status

As of now, this self-hosted setup is still what the live deployment authenticates through —
the migration to a hosted identity provider (replacing this folder as the *live* auth path)
is planned but not yet done. Once that migration lands, update this section to point at the
new setup and this folder becomes purely a reference/demo artifact.
