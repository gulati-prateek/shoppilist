package com.shoppilist.shared

import kotlinx.datetime.Clock

/** `System.currentTimeMillis()` doesn't exist on Kotlin/Native — this is the portable equivalent. */
fun currentTimeMillis(): Long = Clock.System.now().toEpochMilliseconds()
