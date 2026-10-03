package com.venuesync.app.core.repository

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job

/** In memory, always "signed in". */
class FakeTicketCache(var saved: List<CachedTicket> = emptyList()) : TicketCache {
    override suspend fun read() = saved
    override suspend fun write(tickets: List<CachedTicket>) {
        saved = tickets
    }
    override suspend fun clear() {
        saved = emptyList()
    }
}

/** For tests that aren't about syncing: work launched here never runs, so it adds no requests. */
val NoBackgroundWork = CoroutineScope(Job().apply { cancel() })
