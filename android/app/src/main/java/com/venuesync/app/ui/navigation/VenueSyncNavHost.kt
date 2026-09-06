package com.venuesync.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.venuesync.app.ui.events.EventListScreen

/*
 * Route names for the 6 frozen screens (architecture.md). Only EventList exists in M0;
 * destinations get added milestone by milestone — not before.
 */
object Routes {
    const val LOGIN = "login"
    const val EVENT_LIST = "events"
    const val EVENT_DETAIL = "events/{eventId}"
    const val PURCHASE_RESULT = "purchase-result"
    const val MY_TICKETS = "my-tickets"
    const val TICKET_DETAIL = "tickets/{ticketId}"
}

@Composable
fun VenueSyncNavHost() {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = Routes.EVENT_LIST) {
        composable(Routes.EVENT_LIST) { EventListScreen() }
    }
}
