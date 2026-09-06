import { useEffect, useState } from "react";
import { useAuth } from "react-oidc-context";
import { jwtDecode } from "jwt-decode";

interface UseRolesReturn {
  isLoading: boolean;
  roles: string[];
  isOrganizer: boolean;
  isAttendee: boolean;
  isStaff: boolean;
}

// Must match ROLES_CLAIM in the backend's JwtAuthenticationConverter and the
// namespace in the Auth0 Post-Login Action. Auth0 drops custom claims that are
// not URI-namespaced, so a plain "roles" claim would arrive missing, not empty.
const ROLES_CLAIM = "https://venuesync.app/roles";

interface JwtPayload {
  [ROLES_CLAIM]?: string[];

  // LEGACY: Keycloak nested its roles one level deeper, as
  //   "realm_access": { "roles": ["ROLE_ATTENDEE"] }
  // Auth0 emits a flat array instead. That claim shape is the only thing this
  // hook had to change - the ROLE_ filter and everything below it are identical
  // for both providers. See the commented block in JwtAuthenticationConverter.
  //
  // realm_access?: {
  //   roles?: string[];
  // };
}

export const useRoles = (): UseRolesReturn => {
  const { isLoading: isAuthLoading, user } = useAuth();
  const [isLoading, setIsLoading] = useState(true);
  const [roles, setRoles] = useState<string[]>([]);
  const [isOrganizer, setIsOrganizer] = useState(false);
  const [isAttendee, setIsAttendee] = useState(false);
  const [isStaff, setIsStaff] = useState(false);

  useEffect(() => {
    setIsLoading(true);

    if (isAuthLoading || !user?.access_token) {
      setRoles([]);
      setIsOrganizer(false);
      setIsAttendee(false);
      setIsStaff(false);
      setIsLoading(isAuthLoading);
      return;
    }

    try {
      const payload = jwtDecode<JwtPayload>(user?.access_token);
      const allRoles = payload[ROLES_CLAIM] || [];
      const filteredRoles = allRoles.filter((role) => role.startsWith("ROLE_"));
      setRoles(filteredRoles);
      setIsOrganizer(filteredRoles.includes("ROLE_ORGANIZER"));
      setIsAttendee(filteredRoles.includes("ROLE_ATTENDEE"));
      setIsStaff(filteredRoles.includes("ROLE_STAFF"));
    } catch (error) {
      console.error("Error parsing JWT: " + error);
      setRoles([]);
      setIsOrganizer(false);
      setIsAttendee(false);
      setIsStaff(false);
    } finally {
      setIsLoading(false);
    }
  }, [isAuthLoading, user?.access_token]);

  return {
    isLoading,
    roles,
    isOrganizer,
    isAttendee,
    isStaff,
  };
};
