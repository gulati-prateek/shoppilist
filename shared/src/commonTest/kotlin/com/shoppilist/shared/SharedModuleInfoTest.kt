package com.shoppilist.shared

import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals

class SharedModuleInfoTest {

    @Test
    fun `SharedModule info round-trips through kotlinx-serialization`() {
        val encoded = Json.encodeToString(SharedModuleInfo.serializer(), SharedModule.info)
        val decoded = Json.decodeFromString(SharedModuleInfo.serializer(), encoded)

        assertEquals(SharedModule.info, decoded)
    }
}
