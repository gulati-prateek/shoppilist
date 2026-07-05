package com.shoppilist.shared.data.local

import androidx.room.RoomDatabaseConstructor

/**
 * Room's KMP codegen hook: on each platform, the Room compiler generates the real `actual`
 * implementation of this object to back [initialize]. The `actual` declarations below are
 * placeholders that are never actually called at runtime — Room's KSP output replaces them.
 */
@Suppress("NO_ACTUAL_FOR_EXPECT_CLASS")
expect object AppDatabaseConstructor : RoomDatabaseConstructor<AppDatabase> {
    override fun initialize(): AppDatabase
}
