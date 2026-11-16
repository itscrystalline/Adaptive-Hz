package com.mahmutalperenunal.adaptivehz.core.apps

import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.os.SystemClock

/**
 * Resolves the package of the foreground activity without polling every event.
 *
 * Accessibility events can arrive from background apps, notification publishers and
 * overlay windows; the accessibility window hierarchy reports the overlay/IME as focused,
 * while UsageStats keeps reporting the underlying activity. A short cache keeps the query
 * cost off the event path.
 */
class ForegroundAppResolver(context: Context) {

    private val appContext = context.applicationContext
    private val usageStatsManager =
        appContext.getSystemService(UsageStatsManager::class.java)

    private var cachedPackage: String? = null
    private var cachedAtElapsedMs = Long.MIN_VALUE
    private var cachePopulated = false

    fun resolve(): String? {
        val now = SystemClock.elapsedRealtime()
        if (cachePopulated && now - cachedAtElapsedMs < CACHE_WINDOW_MS) {
            return cachedPackage
        }

        val resolved = queryForegroundActivity()
        cachedPackage = resolved
        cachedAtElapsedMs = now
        cachePopulated = true
        return resolved
    }

    fun invalidate() {
        cachePopulated = false
    }

    private fun queryForegroundActivity(): String? {
        val manager = usageStatsManager ?: return null
        val nowWallMs = System.currentTimeMillis()

        return runCatching {
            var lastResumed: String? = null
            val events = manager.queryEvents(nowWallMs - LOOKBACK_MS, nowWallMs)
            val event = UsageEvents.Event()

            while (events.hasNextEvent()) {
                events.getNextEvent(event)
                if (event.eventType == UsageEvents.Event.ACTIVITY_RESUMED) {
                    lastResumed = event.packageName
                }
            }

            lastResumed?.takeIf { it.isNotBlank() }
        }.getOrNull()
    }

    private companion object {
        private const val CACHE_WINDOW_MS = 500L
        private const val LOOKBACK_MS = 30_000L
    }
}
