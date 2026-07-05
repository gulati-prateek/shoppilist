package com.shoppilist.shared.sync

/**
 * Schedules the nightly proactive-suggestion check (§2.8): "You usually add Eggs on Sundays.
 * Add to Weekly list?" Android backs this with WorkManager; iOS backs it with BGTaskScheduler.
 */
interface ProactiveSuggestionScheduler {
    fun schedule()
}
