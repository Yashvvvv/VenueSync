package com.venuesync.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.lifecycle.Lifecycle
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavController
import androidx.navigation.NavOptionsBuilder
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.venuesync.app.ui.events.EventDetailScreen
import com.venuesync.app.ui.events.EventDetailViewModel
import com.venuesync.app.ui.events.EventListScreen
import com.venuesync.app.ui.login.LoginScreen
import com.venuesync.app.ui.purchase.PurchaseResultScreen
import com.venuesync.app.ui.scanner.ScanEventPickerScreen
import com.venuesync.app.ui.tickets.MyTicketsScreen
import com.venuesync.app.ui.tickets.TicketDetailScreen
import com.venuesync.app.ui.tickets.TicketDetailViewModel

/*
 * Route names for the 6 frozen screens (architecture.md). Destinations get added
 * milestone by milestone — not before.
 */
object Routes {
    const val LOGIN = "login"
    const val EVENT_LIST = "events"
    const val EVENT_DETAIL = "events/{${EventDetailViewModel.EVENT_ID_ARG}}"
    const val PURCHASE_RESULT = "purchase-result/{${TicketDetailViewModel.TICKET_ID_ARG}}"
    const val MY_TICKETS = "my-tickets"
    const val SCAN_EVENTS = "scan"
    const val TICKET_DETAIL = "tickets/{${TicketDetailViewModel.TICKET_ID_ARG}}"

    fun eventDetail(eventId: String) = "events/$eventId"
    fun purchaseResult(ticketId: String) = "purchase-result/$ticketId"
    fun ticketDetail(ticketId: String) = "tickets/$ticketId"
}

@Composable
fun VenueSyncNavHost() {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = Routes.EVENT_LIST) {
        composable(Routes.EVENT_LIST) { entry ->
            EventListScreen(
                onEventClick = { eventId -> navController.navigateOnce(entry, Routes.eventDetail(eventId)) },
                onSignInClick = { navController.navigateOnce(entry, Routes.LOGIN) },
                onMyTicketsClick = { navController.navigateOnce(entry, Routes.MY_TICKETS) },
                onScanClick = { navController.navigateOnce(entry, Routes.SCAN_EVENTS) },
            )
        }
        composable(Routes.EVENT_DETAIL) {
            EventDetailScreen(
                // navigateUp, not popBackStack: a double-tapped back can't pop the start screen and leave a blank host.
                onBack = { navController.navigateUp() },
                // Plain navigate, no RESUMED guard: these come from VM one-shot states, single by construction,
                // and a guard that drops one would mean the result screen never shows.
                onSignInRequired = { navController.navigate(Routes.LOGIN) },
                onPurchased = { ticketId -> navController.navigate(Routes.purchaseResult(ticketId)) },
            )
        }
        composable(Routes.MY_TICKETS) { entry ->
            MyTicketsScreen(
                onBack = { navController.navigateUp() },
                onTicketClick = { navController.navigateOnce(entry, Routes.ticketDetail(it)) },
                onSignInClick = { navController.navigateOnce(entry, Routes.LOGIN) },
            )
        }
        composable(Routes.SCAN_EVENTS) {
            ScanEventPickerScreen(
                onBack = { navController.navigateUp() },
                onEventClick = {}, // the scanner lands with its own commit
            )
        }
        composable(Routes.TICKET_DETAIL) {
            TicketDetailScreen(onBack = { navController.navigateUp() })
        }
        composable(Routes.PURCHASE_RESULT) { entry ->
            val ticketId = entry.arguments?.getString(TicketDetailViewModel.TICKET_ID_ARG)
            PurchaseResultScreen(
                // Same idempotent pop as login: a double-tapped Done can't pop the detail screen too.
                onDone = { navController.popBackStack(Routes.PURCHASE_RESULT, inclusive = true) },
                // Replaces the result screen, so back from the ticket returns to the event.
                onViewTicket = ticketId?.let { id ->
                    {
                        navController.navigateOnce(entry, Routes.ticketDetail(id)) {
                            popUpTo(Routes.PURCHASE_RESULT) { inclusive = true }
                        }
                    }
                },
            )
        }
        composable(Routes.LOGIN) {
            // Pops exactly the login entry and is a no-op if it's already gone, so a double
            // trigger (back tap + sign-in finishing) can never pop the screen underneath.
            val leave: () -> Unit = { navController.popBackStack(Routes.LOGIN, inclusive = true) }
            LoginScreen(onBack = leave, onSignedIn = leave)
        }
    }
}

/** For taps: once a navigation starts, [from] leaves RESUMED, so a double tap can't push the screen twice. */
private fun NavController.navigateOnce(from: NavBackStackEntry, route: String, options: NavOptionsBuilder.() -> Unit = {}) {
    if (from.lifecycle.currentState == Lifecycle.State.RESUMED) navigate(route, options)
}
