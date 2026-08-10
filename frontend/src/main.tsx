import { StrictMode } from "react"
import { createRoot } from "react-dom/client"
import "@fontsource-variable/archivo"
import "@fontsource-variable/jetbrains-mono"
// Display face for the acid vibe only. Single weight, latin subset.
import "@fontsource/anton/latin-400.css"
import "./index.css"
import { AuthProvider } from "react-oidc-context"
import { createBrowserRouter, RouterProvider } from "react-router"
import OrganizersLandingPage from "./pages/organizers-landing-page.tsx"
import DashboardManageEventPage from "./pages/dashboard-manage-event-page.tsx"
import LoginPage from "./pages/login-page.tsx"
import ProtectedRoute from "./components/protected-route.tsx"
import RoleProtectedRoute from "./components/role-protected-route.tsx"
import CallbackPage from "./pages/callback-page.tsx"
import DashboardListEventsPage from "./pages/dashboard-list-events-page.tsx"
import PublishedEventsPage from "./pages/published-events-page.tsx"
import PurchaseTicketPage from "./pages/purchase-ticket-page.tsx"
import DashboardListTickets from "./pages/dashboard-list-tickets.tsx"
import DashboardPage from "./pages/dashboard-page.tsx"
import DashboardViewTicketPage from "./pages/dashboard-view-ticket-page.tsx"
import DashboardValidateQrPage from "./pages/dashboard-validate-qr-page.tsx"
import { ToastProvider } from "./components/common/toast-provider.tsx"
import { ErrorBoundary } from "./components/common/error-boundary.tsx"
import AboutPage from "./pages/about-page.tsx"
import HelpCenterPage from "./pages/help-center-page.tsx"
import ContactPage from "./pages/contact-page.tsx"
import TermsPage from "./pages/terms-page.tsx"
import PrivacyPage from "./pages/privacy-page.tsx"
import AllEventsPage from "./pages/all-events-page.tsx"
import AppDownloadPage from "./pages/app-download-page.tsx"
import RootLayout from "./components/layout/root-layout.tsx"
import RootLanding from "./pages/root-landing.tsx"
import { AudienceProvider } from "./hooks/use-audience.tsx"

const router = createBrowserRouter([
  {
    element: <RootLayout />,
    children: [
      {
        /* Only this route can show the audience chooser. Everything else
           renders its content immediately on the stored or default choice. */
        path: "/",
        Component: RootLanding,
      },
      {
        path: "/callback",
        Component: CallbackPage,
      },
      {
        path: "/login",
        Component: LoginPage,
      },
      {
        path: "/about",
        Component: AboutPage,
      },
      {
        path: "/help",
        Component: HelpCenterPage,
      },
      {
        path: "/contact",
        Component: ContactPage,
      },
      {
        path: "/terms",
        Component: TermsPage,
      },
      {
        path: "/privacy",
        Component: PrivacyPage,
      },
      {
        path: "/events",
        Component: AllEventsPage,
      },
      {
        path: "/app",
        Component: AppDownloadPage,
      },
      {
        path: "/events/:id",
        Component: PublishedEventsPage,
      },
      {
        path: "/events/:eventId/purchase/:ticketTypeId",
        element: (
          <RoleProtectedRoute allowedRoles={["ATTENDEE"]}>
            <PurchaseTicketPage />
          </RoleProtectedRoute>
        ),
      },
      {
        path: "/organizers",
        Component: OrganizersLandingPage,
      },
      {
        path: "/dashboard",
        element: (
          <ProtectedRoute>
            <DashboardPage />
          </ProtectedRoute>
        ),
      },
      {
        path: "/dashboard/events",
        element: (
          <RoleProtectedRoute allowedRoles={["ORGANIZER"]}>
            <DashboardListEventsPage />
          </RoleProtectedRoute>
        ),
      },
      {
        path: "/dashboard/tickets",
        element: (
          <RoleProtectedRoute allowedRoles={["ATTENDEE"]}>
            <DashboardListTickets />
          </RoleProtectedRoute>
        ),
      },
      {
        path: "/dashboard/tickets/:id",
        element: (
          <RoleProtectedRoute allowedRoles={["ATTENDEE"]}>
            <DashboardViewTicketPage />
          </RoleProtectedRoute>
        ),
      },
      {
        path: "/dashboard/validate-qr",
        element: (
          <RoleProtectedRoute allowedRoles={["STAFF"]}>
            <DashboardValidateQrPage />
          </RoleProtectedRoute>
        ),
      },
      {
        path: "/dashboard/events/create",
        element: (
          <RoleProtectedRoute allowedRoles={["ORGANIZER"]}>
            <DashboardManageEventPage />
          </RoleProtectedRoute>
        ),
      },
      {
        path: "/dashboard/events/update/:id",
        element: (
          <RoleProtectedRoute allowedRoles={["ORGANIZER"]}>
            <DashboardManageEventPage />
          </RoleProtectedRoute>
        ),
      },
    ],
  },
])

const appOrigin = window.location.origin
const oidcConfig = {
  authority:
    import.meta.env.VITE_OIDC_AUTHORITY ??
    "http://localhost:9090/realms/event-ticket-platform",
  client_id: import.meta.env.VITE_OIDC_CLIENT_ID ?? "event-ticket-platform-app",
  redirect_uri: `${appOrigin}/callback`,
  post_logout_redirect_uri: appOrigin,
}

createRoot(document.getElementById("root")!).render(
  <StrictMode>
    <ErrorBoundary>
      <AudienceProvider>
        <AuthProvider {...oidcConfig}>
          <ToastProvider />
          <RouterProvider router={router} />
        </AuthProvider>
      </AudienceProvider>
    </ErrorBoundary>
  </StrictMode>,
)
