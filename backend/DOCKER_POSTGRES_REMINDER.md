# Docker Postgres — quick reminder

Date: 2025-11-28

This note documents the local Postgres vs Docker Postgres situation for this project and the minimal steps/commands to verify and change behavior in the future.

Summary
- Your machine had PostgreSQL installed and running on the host, binding port 5432.
- The project runs a Postgres container via `docker-compose`.
- To avoid a port conflict we mapped the container to host port `5433` and updated the Spring Boot config to connect to `localhost:5433`.

Current important values (project)
- Docker Compose `db` service: `POSTGRES_PASSWORD=changemeinprod!`
- Docker Compose host mapping: `5433:5432` (container 5432 → host 5433)
- Spring Boot `application.properties` currently points to:

\`\`\`ini
spring.datasource.url=jdbc:postgresql://localhost:5433/postgres
spring.datasource.username=postgres
spring.datasource.password=changemeinprod!
\`\`\`

Why the conflict happened
- When an app connects to `jdbc:postgresql://localhost:5432`, Windows chooses whichever Postgres server is listening on host port 5432 (your local install). Because the host Postgres had different credentials, the connection attempt failed with `FATAL: password authentication failed for user "postgres"`.

Quick verification commands (PowerShell)
- Show which process is listening on port 5432/5433:

\`\`\`powershell
# check host Postgres (port 5432) and our docker-mapped port (5433)
netstat -ano | Select-String ':5432'
netstat -ano | Select-String ':5433'

# find process info for a PID (replace <PID> with the number shown)
Get-Process -Id <PID> -ErrorAction SilentlyContinue | Format-List Id,ProcessName,Path
\`\`\`

- Docker container status and ports

\`\`\`powershell
# show running compose containers and port mappings
cd backend
docker ps --format "table {{.Names}}\t{{.Image}}\t{{.Status}}\t{{.Ports}}"

# inspect db container env + ports
docker inspect backend-db-1 --format "Container: {{.Name}}\nEnv:\n{{range .Config.Env}}{{println .}}{{end}}\nPorts:\n{{json .NetworkSettings.Ports}}"
\`\`\`

- Tail postgres logs

\`\`\`powershell
docker logs backend-db-1 --tail 200
\`\`\`

- Check active DB connections (inside container)

\`\`\`powershell
docker exec backend-db-1 psql -U postgres -c "SELECT pid, usename, application_name, client_addr, state, query_start, query FROM pg_stat_activity ORDER BY pid DESC;"
\`\`\`

Connect from host tools
- pgAdmin / psql client: host = `localhost`, port = `5433`, user = `postgres`, password = `changemeinprod!`, database = `postgres`.

Options (pick one)
1) Keep Docker Postgres (current safe option)
   - Keep `docker-compose.yml` mapping `5433:5432` and keep `application.properties` pointing to `localhost:5433`.
   - Pros: non-destructive, both local and container Postgres can coexist.
   - How to reinitialize container DB (destroys container volumes):

\`\`\`powershell
cd backend
# WARNING: removes volumes (data)
docker-compose down -v
docker-compose up -d --build
\`\`\`

2) Stop or uninstall local Postgres so container can bind to 5432
   - Stop the Windows service for local Postgres, revert `docker-compose.yml` to `5432:5432` and change `application.properties` back to 5432.
   - Pros: easier addressing (default port), no per-project port fiddling.
   - Cons: affects other projects relying on the host Postgres.

3) Use the host Postgres (do not run a Postgres container)
   - Update `application.properties` to point to `jdbc:postgresql://localhost:5432/...` and set the correct host credentials.
   - Pros: access existing local databases and data.
   - Cons: loses isolation; you must manage schema/migrations on your own host instance.

4) (Recommended for reproducible dev) Run the Spring backend as a Compose service
   - Add the backend as a service in `docker-compose.yml` and let it connect to the `db` service using `jdbc:postgresql://db:5432/postgres`.
   - Pros: environment parity, no host-port issues.
   - Cons: small additional Compose config work (I can do this for you if you want).

Notes about security and actuator
- The backend is protected by Spring Security / OAuth2. Requests to `/` and `/actuator/health` responded with `401 Unauthorized` when unauthenticated; that is expected if security is enabled.
- For quick local testing you can run with the `dev` profile to switch to H2 (see `application-dev.properties`) but note that this uses an in-memory DB and will not use the Docker Postgres.

How to revert my changes (if desired)
- Revert the Docker port mapping to 5432:5432 in `backend/docker-compose.yml` and update `backend/src/main/resources/application.properties` to jdbc url using port 5432.
- If a volume was already created with a different init password, you may need to remove the volume (`docker-compose down -v`) to reinitialize the container with the new env.

Useful git commands I used / recommend
\`\`\`bash
# show file changes
git status
git diff backend/docker-compose.yml backend/src/main/resources/application.properties
# if you want to revert my edits
git checkout -- backend/docker-compose.yml backend/src/main/resources/application.properties
\`\`\`

Contact me if you want
- I can add the backend as a Compose service so it uses `db` by service name (recommended).
- I can add a short `backend/README.md` with the above commands integrated into quick one-liners for your day-to-day.

--
Small note: keep passwords out of source control for production; for local development this is fine but consider using `.env` or Docker secrets for anything sensitive.
