package com.shoppilist.shared.sync

import kotlinx.cinterop.ExperimentalForeignApi
import platform.BackgroundTasks.BGAppRefreshTaskRequest
import platform.BackgroundTasks.BGTaskScheduler
import platform.Foundation.NSDate
import platform.Foundation.dateByAddingTimeInterval

private const val TASK_IDENTIFIER = "com.shoppilist.proactiveSuggestions"
private const val ONE_DAY_SECONDS = 24.0 * 60.0 * 60.0

/**
 * iOS equivalent of the Android WorkManager-backed scheduler, using BGTaskScheduler's app-refresh
 * task API. Submitting a request here only succeeds once [TASK_IDENTIFIER] is registered in the
 * host app's Info.plist (`BGTaskSchedulerPermittedIdentifiers`) and the task handler is registered
 * via `BGTaskScheduler.sharedScheduler.registerForTaskWithIdentifier` at app launch -- neither of
 * which exists yet since there's no real iOS app target (that's Phase 6). This compiles and is
 * functionally correct, but isn't reachable/testable until then.
 */
@OptIn(ExperimentalForeignApi::class)
class IosProactiveSuggestionScheduler : ProactiveSuggestionScheduler {
    override fun schedule() {
        val request = BGAppRefreshTaskRequest(TASK_IDENTIFIER)
        request.earliestBeginDate = NSDate().dateByAddingTimeInterval(ONE_DAY_SECONDS)
        try {
            BGTaskScheduler.sharedScheduler.submitTaskRequest(request)
        } catch (e: Exception) {
            // No registered handler for TASK_IDENTIFIER yet (see class doc) -- ignore until Phase 6.
        }
    }
}
