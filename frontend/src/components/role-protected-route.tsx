import { ReactNode } from "react";
import { useAuth } from "react-oidc-context";
import { Navigate, useLocation } from "react-router";
import { useRoles } from "@/hooks/use-roles";
import { PageLoader } from "./common/loading-skeleton";

interface RoleProtectedRouteProperties {
  children: ReactNode;
  allowedRoles: ("ORGANIZER" | "ATTENDEE" | "STAFF")[];
}

const RoleProtectedRoute: React.FC<RoleProtectedRouteProperties> = ({ 
  children, 
  allowedRoles 
}) => {
  const { isLoading: isAuthLoading, isAuthenticated } = useAuth();
  const { isLoading: isRolesLoading, isOrganizer, isAttendee, isStaff } = useRoles();
  const location = useLocation();

  if (isAuthLoading || isRolesLoading) {
    return <PageLoader />;
  }

  if (!isAuthenticated) {
    localStorage.setItem(
      "redirectPath",
      globalThis.location.pathname + globalThis.location.search,
    );
    return <Navigate to="/login" state={{ from: location }} replace />;
  }

  // Check if user has at least one of the allowed roles
  const hasRequiredRole = allowedRoles.some(role => {
    switch (role) {
      case "ORGANIZER":
        return isOrganizer;
      case "ATTENDEE":
        return isAttendee;
      case "STAFF":
        return isStaff;
      default:
        return false;
    }
  });

  if (!hasRequiredRole) {
    // Redirect directly to the user's appropriate dashboard based on their role
    // This avoids double redirects through /dashboard
    if (isOrganizer) {
      return <Navigate to="/dashboard/events" replace />;
    } else if (isStaff) {
      return <Navigate to="/dashboard/validate-qr" replace />;
    } else if (isAttendee) {
      return <Navigate to="/dashboard/tickets" replace />;
    }
    // Fallback to home if no roles
    return <Navigate to="/" replace />;
  }

  return children;
};

export default RoleProtectedRoute;
