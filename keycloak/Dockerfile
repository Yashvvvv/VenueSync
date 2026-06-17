# Custom Keycloak image for VenueSync.
# Bakes the realm export into the image so Keycloak auto-imports on first boot.
# On Render (or any Docker host), set KEYCLOAK_ADMIN / KEYCLOAK_ADMIN_PASSWORD env vars.
# The realm is only imported when it does not already exist (Keycloak skips duplicate imports).

FROM quay.io/keycloak/keycloak:latest

# Copy realm + users export into the import directory
COPY event-ticket-platform-realm.json /opt/keycloak/data/import/event-ticket-platform-realm.json
COPY event-ticket-platform-users-0.json /opt/keycloak/data/import/event-ticket-platform-users-0.json

# Expose the Keycloak HTTP port
EXPOSE 8080

# Start in production mode with HTTP enabled (Render/proxy handles TLS).
# --import-realm makes Keycloak auto-import /opt/keycloak/data/import/*.json on first start.
ENTRYPOINT ["/opt/keycloak/bin/kc.sh"]
CMD ["start", \
     "--import-realm", \
     "--db=dev-file", \
     "--http-enabled=true", \
     "--hostname-strict=false", \
     "--proxy-headers=xforwarded"]
