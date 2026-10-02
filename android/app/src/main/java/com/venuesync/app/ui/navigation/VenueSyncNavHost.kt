package com.venuesync.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.lifecycle.Lifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.venuesync.app.ui.events.EventDetailScreen
import com.venuesync.app.ui.events.EventDetailViewModel
import com.venuesync.app.ui.events.EventListScreen
import com.venuesync.app.ui.login.LoginScreen
import com.venuesync.app.ui.purchase.PurchaseResultScreen
import com.venuesync.app.ui.tickets.MyTicketsScreen

/*
 * Route names for the 6 frozen screens (architecture.md). Destinations get added
 * milestone by milestone — not before.
 */
object Routes {
    const val LOGIN = "login"
    const val EVENT_LIST = "events"
    const val EVENT_DETAIL = "events/{${EventDetailViewModel.EVENT_ID_ARG}}"
    const val TICKET_ID_ARG = "ticketId"
    const val PURCHASE_RESULT = "purchase-result/{$TICKET_ID_ARG}"
    const val MY_TICKETS = "my-tickets"
    const val TICKET_DETAIL = "tickets/{ticketId}"

    fun eventDetail(eventId: String) = "events/$eventId"
    fun purchaseResult(ticketId: String) = "purchase-result/$ticketId"
}

@Composable
fun VenueSyncNavHost() {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = Routes.EVENT_LIST) {
        composable(Routes.EVENT_LIST) { entry ->
            // Once a navigation starts this entry leaves RESUMED, so a double-tap can't push twice.
            fun navigateOnce(route: String) {
                if (entry.lifecycle.currentState == Lifecycle.State.RESUMED) navController.navigate(route)
            }
            EventListScreen(
                onEventClick = { eventId -> navigateOnce(Routes.eventDetail(eventId)) },
                onSignInClick = { navigateOnce(Routes.LOGIN) },
                onMyTicketsClick = { navigateOnce(Routes.MY_TICKETS) },
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
                onTicketClick = {}, // ticket detail lands with the next commit
                onSignInClick = {
                    if (entry.lifecycle.currentState == Lifecycle.State.RESUMED) navController.navigate(Routes.LOGIN)
                },
            )
        }
        composable(Routes.PURCHASE_RESULT) {
            // Same idempotent pop as login: a double-tapped Done can't pop the detail screen too.
            PurchaseResultScreen(onDone = { navController.popBackStack(Routes.PURCHASE_RESULT, inclusive = true) })
        }
        composable(Routes.LOGIN) {
            // Pops exactly the login entry and is a no-op if it's already gone, so a double
            // trigger (back tap + sign-in finishing) can never pop the screen underneath.
            val leave: () -> Unit = { navController.popBackStack(Routes.LOGIN, inclusive = true) }
            LoginScreen(onBack = leave, onSignedIn = leave)
        }
    }
}
