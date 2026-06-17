# Custom Keycloak image for VenueSync — optimised for Render free tier (512 MB).
#
# Changes vs. original:
#  - Uses PostgreSQL (Supabase) instead of H2 dev-file → no disk needed, survives restarts
#  - --cache=local  → disables Infinispan/JGroups clustering noise (saves ~150 MB RAM)
#  - JAVA_OPTS_APPEND caps the JVM heap so Keycloak stays under 400 MB total
#  - Build stage runs `kc.sh build` to pre-compile Quarkus → faster cold start, less RAM at boot
#
# Required Render env vars (set in the Keycloak service dashboard):
#   KEYCLOAK_ADMIN             = admin
#   KEYCLOAK_ADMIN_PASSWORD    = <secure password>
#   KC_HOSTNAME                = https://keycloak-server-3dsx.onrender.com
#   KC_DB_URL                  = jdbc:postgresql://<host>:5432/postgres?sslmode=require
#   KC_DB_USERNAME             = <supabase user>
#   KC_DB_PASSWORD             = <supabase password>

FROM quay.io/keycloak/keycloak:latest AS builder

# Pre-build Keycloak with PostgreSQL driver (avoids runtime config overhead)
ENV KC_DB=postgres
ENV KC_CACHE=local
ENV KC_HTTP_ENABLED=true
ENV KC_HOSTNAME_STRICT=false
ENV KC_PROXY_HEADERS=xforwarded
ENV KC_DB_SCHEMA=keycloak
ENV KC_HTTP_HOST=0.0.0.0

RUN /opt/keycloak/bin/kc.sh build \
    --db=postgres \
    --cache=local \
    --http-enabled=true \
    --hostname-strict=false \
    --proxy-headers=xforwarded \
    --db-schema=keycloak

# ── Runtime image ──────────────────────────────────────────────────────────────
FROM quay.io/keycloak/keycloak:latest

# Copy the pre-built Keycloak from the builder stage
COPY --from=builder /opt/keycloak/ /opt/keycloak/

# Bake in realm + users — imported automatically on first boot via --import-realm
COPY event-ticket-platform-realm.json  /opt/keycloak/data/import/event-ticket-platform-realm.json
COPY event-ticket-platform-users-0.json /opt/keycloak/data/import/event-ticket-platform-users-0.json

# Cap JVM heap so Keycloak stays well under 512 MB
ENV JAVA_OPTS_APPEND="-Xms64m -Xmx192m -Xss256k -XX:MaxMetaspaceSize=128m -Djava.security.egd=file:/dev/./urandom"

EXPOSE 8080

ENTRYPOINT ["/opt/keycloak/bin/kc.sh"]
CMD ["start", \
     "--optimized", \
     "--import-realm", \
     "--db=postgres", \
     "--cache=local", \
     "--http-enabled=true", \
     "--http-host=0.0.0.0", \
     "--http-port=8080", \
     "--hostname-strict=false", \
     "--proxy-headers=xforwarded"]
