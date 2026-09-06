package com.fullstack.venuesync.shared.config;

// ===========================================================================
// LEGACY - built the Keycloak admin-client bean that KeycloakAdminServiceImpl
// injected. Commented out rather than deleted; see keycloak/README.md.
//
// Auth0RoleService needs no equivalent @Configuration class: it builds its own
// RestClient in its constructor from three @Value properties. The whole bean
// definition disappeared because the SDK it existed to configure disappeared.
//
// Worth noticing what this class reveals about the old setup - it authenticated
// against realm "master" with an admin USERNAME AND PASSWORD. That is a human
// superuser credential sitting in application config, able to do anything in
// the Keycloak instance, not just assign one role. The Auth0 replacement uses a
// machine credential scoped to exactly three permissions (read:users,
// read:roles, create:role_members). Strictly less blast radius, and that
// improvement came free with the migration rather than being designed for.
//
// Uncommenting this requires uncommenting keycloak-admin-client in pom.xml and
// the keycloak.admin.* properties in application.properties.
// ===========================================================================
//
// import org.keycloak.admin.client.Keycloak;
// import org.keycloak.admin.client.KeycloakBuilder;
// import org.springframework.beans.factory.annotation.Value;
// import org.springframework.context.annotation.Bean;
// import org.springframework.context.annotation.Configuration;
//
// @Configuration
// public class KeycloakAdminConfig {
//
//     @Value("${keycloak.admin.server-url}")
//     private String serverUrl;
//
//     @Value("${keycloak.admin.client-id}")
//     private String clientId;
//
//     @Value("${keycloak.admin.username}")
//     private String username;
//
//     @Value("${keycloak.admin.password}")
//     private String password;
//
//     @Bean
//     public Keycloak keycloakAdminClient() {
//         return KeycloakBuilder.builder()
//                 .serverUrl(serverUrl)
//                 .realm("master") // Admin users are usually in the master realm
//                 .clientId(clientId)
//                 .username(username)
//                 .password(password)
//                 .build();
//     }
// }
