package com.venuesync.app.ui.navigation

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.Lifecycle
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavOptionsBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.venuesync.app.ui.account.AccountAction
import com.venuesync.app.ui.account.isOrganizer
import com.venuesync.app.ui.events.EventDetailScreen
import com.venuesync.app.ui.events.EventDetailViewModel
import com.venuesync.app.ui.events.EventListScreen
import com.venuesync.app.ui.events.EventsViewModel
import com.venuesync.app.ui.feed.FeedScreen
import com.venuesync.app.ui.feed.HypeTab
import com.venuesync.app.ui.feed.HypeTabBar
import com.venuesync.app.ui.login.LoginScreen
import com.venuesync.app.ui.organizer.BecomeOrganizerScreen
import com.venuesync.app.ui.organizer.EventFormScreen
import com.venuesync.app.ui.organizer.OrganizerEventScreen
import com.venuesync.app.ui.organizer.OrganizerEventViewModel
import com.venuesync.app.ui.organizer.OrganizerEventsScreen
import com.venuesync.app.ui.organizer.OrganizerStaffScreen
import com.venuesync.app.ui.purchase.PurchaseResultScreen
import com.venuesync.app.ui.scanner.ScanEventPickerScreen
import com.venuesync.app.ui.scanner.ScannerScreen
import com.venuesync.app.ui.scanner.ScannerViewModel
import com.venuesync.app.ui.theme.Experience
import com.venuesync.app.ui.tickets.MyTicketsScreen
import com.venuesync.app.ui.tickets.TicketDetailScreen
import com.venuesync.app.ui.tickets.TicketDetailViewModel

object Routes {
    const val LOGIN = "login"
    const val EVENT_LIST = "events"
    /** Hype's home: one event per screen. Declares the feed's page size as a route argument. */
    const val FEED = "feed?${EventsViewModel.PAGE_SIZE_ARG}={${EventsViewModel.PAGE_SIZE_ARG}}"
    const val EVENT_DETAIL = "events/{${EventDetailViewModel.EVENT_ID_ARG}}"
    const val PURCHASE_RESULT = "purchase-result/{${TicketDetailViewModel.TICKET_ID_ARG}}"
    const val MY_TICKETS = "my-tickets"
    const val SCAN_EVENTS = "scan"
    const val SCANNER = "scan/{${ScannerViewModel.EVENT_ID_ARG}}"
    const val TICKET_DETAIL = "tickets/{${TicketDetailViewModel.TICKET_ID_ARG}}"
    const val ORGANIZER_EVENTS = "organizer/events"
    const val ORGANIZER_EVENT = "organizer/events/{${OrganizerEventViewModel.EVENT_ID_ARG}}"
    const val NEW_EVENT = "organizer/new-event"
    const val BECOME_ORGANIZER = "organizer/become"
    const val EDIT_EVENT = "organizer/events/{${OrganizerEventViewModel.EVENT_ID_ARG}}/edit"
    const val EVENT_STAFF = "organizer/events/{${OrganizerEventViewModel.EVENT_ID_ARG}}/staff"

    fun eventDetail(eventId: String) = "events/$eventId"
    fun purchaseResult(ticketId: String) = "purchase-result/$ticketId"
    fun ticketDetail(ticketId: String) = "tickets/$ticketId"
    fun scanner(eventId: String) = "scan/$eventId"
    fun organizerEvent(eventId: String) = "organizer/events/$eventId"
    fun editEvent(eventId: String) = "organizer/events/$eventId/edit"
    fun eventStaff(eventId: String) = "organizer/events/$eventId/staff"
}

/** The web's feed asks for 8: one screen per event, so a page is 8 screens. */
private const val FeedPageSize = 8

/**
 * Two experiences, two navigation models over one set of screens. Classic: a top bar and a back stack from the event
 * list. Hype: the feed at the root and a bottom tab bar as the whole nav (the web's HypeTabBar).
 */
@Composable
fun VenueSyncNavHost(experience: Experience) {
    val navController = rememberNavController()
    when (experience) {
        Experience.Classic -> NavHost(navController, startDestination = Routes.EVENT_LIST) {
            screens(navController, Experience.Classic)
        }
        Experience.Hype -> HypeShell(navController)
    }
}

@Composable
private fun HypeShell(navController: NavHostController) {
    val route = navController.currentBackStackEntryAsState().value?.destination?.route
    val organizer = isOrganizer()
    val active = when {
        route == Routes.FEED -> HypeTab.Feed
        route == Routes.EVENT_LIST -> HypeTab.Explore
        route == Routes.MY_TICKETS -> HypeTab.Tickets
        // The whole organizer area sits under its tab, so the bar says where you are inside it too.
        route == Routes.NEW_EVENT || route?.startsWith(Routes.ORGANIZER_EVENTS) == true -> HypeTab.Events
        else -> null
    }
    Scaffold(
        contentWindowInsets = WindowInsets(0), // screens draw their own top insets (the event photo runs under the clock)
        bottomBar = {
            // The door scanner keeps its full-screen colours; sign-in stays a focused, single task.
            if (route != Routes.SCANNER && route != Routes.LOGIN) {
                HypeTabBar(active, showEvents = organizer, onSelect = { tab ->
                    navController.toTab(
                        when (tab) {
                            HypeTab.Feed -> Routes.FEED
                            HypeTab.Explore -> Routes.EVENT_LIST
                            HypeTab.Tickets -> Routes.MY_TICKETS
                            HypeTab.Events -> Routes.ORGANIZER_EVENTS
                        },
                    )
                })
            }
        },
    ) { padding ->
        // consumeWindowInsets: the tab bar already covers the navigation bar, so screens below don't pad for it twice.
        NavHost(
            navController,
            startDestination = Routes.FEED,
            modifier = Modifier.padding(padding).consumeWindowInsets(padding),
        ) {
            screens(navController, Experience.Hype)
        }
    }
}

/** A tab switch: back to the feed's entry (kept alive), one copy of each tab, each tab's state restored. */
private fun NavController.toTab(route: String) = navigate(if (route == Routes.FEED) "feed" else route) {
    popUpTo(Routes.FEED) { saveState = true }
    launchSingleTop = true
    restoreState = true
}

private fun NavGraphBuilder.screens(
    navController: NavHostController,
    experience: Experience,
) {
    val hype = experience == Experience.Hype
    composable(
        Routes.FEED,
        arguments = listOf(navArgument(EventsViewModel.PAGE_SIZE_ARG) { type = NavType.IntType; defaultValue = FeedPageSize }),
    ) { entry ->
        FeedScreen(
            onEventClick = { navController.navigateOnce(entry, Routes.eventDetail(it)) },
            onBrowseAll = { navController.toTab(Routes.EVENT_LIST) },
        )
    }
    composable(Routes.EVENT_LIST) { entry ->
        EventListScreen(
            onEventClick = { eventId -> navController.navigateOnce(entry, Routes.eventDetail(eventId)) },
            onSignInClick = { navController.navigateOnce(entry, Routes.LOGIN) },
            // In Hype this is the Explore tab: the other tabs are switched to, not pushed.
            onMyTicketsClick = { if (hype) navController.toTab(Routes.MY_TICKETS) else navController.navigateOnce(entry, Routes.MY_TICKETS) },
            onScanClick = { navController.navigateOnce(entry, Routes.SCAN_EVENTS) },
            // Classic's top-bar button; Hype has the Events tab instead.
            onManageEventsClick = if (hype) null else ({ navController.navigateOnce(entry, Routes.ORGANIZER_EVENTS) }),
            onBecomeOrganizerClick = { navController.navigateOnce(entry, Routes.BECOME_ORGANIZER) },
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
            // In Hype this is a tab, a root: no back arrow, and the account menu lives here (Hype has no top nav).
            onBack = if (hype) null else ({ navController.navigateUp() }),
            onTicketClick = { navController.navigateOnce(entry, Routes.ticketDetail(it)) },
            onSignInClick = { navController.navigateOnce(entry, Routes.LOGIN) },
            onBrowse = { if (hype) navController.toTab(Routes.FEED) else navController.navigateUp() },
            actions = {
                if (hype) {
                    AccountAction(
                        onSignInClick = { navController.navigateOnce(entry, Routes.LOGIN) },
                        onMyTicketsClick = {},
                        onScanClick = { navController.navigateOnce(entry, Routes.SCAN_EVENTS) },
                        onBecomeOrganizerClick = { navController.navigateOnce(entry, Routes.BECOME_ORGANIZER) },
                    )
                }
            },
        )
    }
    composable(Routes.SCAN_EVENTS) { entry ->
        ScanEventPickerScreen(
            onBack = { navController.navigateUp() },
            onEventClick = { navController.navigateOnce(entry, Routes.scanner(it)) },
            onSignInClick = { navController.navigateOnce(entry, Routes.LOGIN) },
        )
    }
    composable(Routes.SCANNER) {
        ScannerScreen(onBack = { navController.navigateUp() })
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
    composable(Routes.ORGANIZER_EVENTS) { entry ->
        OrganizerEventsScreen(
            // In Hype this is the Events tab, a root: no back arrow.
            onBack = if (hype) null else ({ navController.navigateUp() }),
            onEventClick = { navController.navigateOnce(entry, Routes.organizerEvent(it)) },
            onNewEvent = { navController.navigateOnce(entry, Routes.NEW_EVENT) },
        )
    }
    composable(Routes.ORGANIZER_EVENT) { entry ->
        OrganizerEventScreen(
            onBack = { navController.navigateUp() },
            onEdit = { navController.navigateOnce(entry, Routes.editEvent(it)) },
            onStaff = { navController.navigateOnce(entry, Routes.eventStaff(it)) },
        )
    }
    composable(Routes.EVENT_STAFF) {
        OrganizerStaffScreen(onBack = { navController.navigateUp() })
    }
    composable(Routes.BECOME_ORGANIZER) {
        BecomeOrganizerScreen(
            onBack = { navController.navigateUp() },
            // The organizer tools replace this screen: Back from them returns to where the menu was opened.
            onDone = {
                if (hype) {
                    navController.toTab(Routes.ORGANIZER_EVENTS) // the new Events tab
                } else {
                    navController.navigate(Routes.ORGANIZER_EVENTS) { popUpTo(Routes.BECOME_ORGANIZER) { inclusive = true } }
                }
            },
        )
    }
    composable(Routes.NEW_EVENT) {
        EventFormScreen(
            onClose = { navController.popBackStack(Routes.NEW_EVENT, inclusive = true) },
            // The new event's overview replaces the form, so back from it returns to My events.
            onSaved = { id ->
                navController.navigate(Routes.organizerEvent(id)) { popUpTo(Routes.NEW_EVENT) { inclusive = true } }
            },
        )
    }
    composable(Routes.EDIT_EVENT) {
        // Idempotent pops, like login: a double trigger can't pop the overview too.
        val leave: () -> Unit = { navController.popBackStack(Routes.EDIT_EVENT, inclusive = true) }
        EventFormScreen(onClose = leave, onSaved = { leave() })
    }
    composable(Routes.LOGIN) {
        // Pops exactly the login entry and is a no-op if it's already gone, so a double
        // trigger (back tap + sign-in finishing) can never pop the screen underneath.
        val leave: () -> Unit = { navController.popBackStack(Routes.LOGIN, inclusive = true) }
        LoginScreen(onBack = leave, onSignedIn = leave)
    }
}

/** For taps: once a navigation starts, [from] leaves RESUMED, so a double tap can't push the screen twice. */
private fun NavController.navigateOnce(from: NavBackStackEntry, route: String, options: NavOptionsBuilder.() -> Unit = {}) {
    if (from.lifecycle.currentState == Lifecycle.State.RESUMED) navigate(route, options)
}
