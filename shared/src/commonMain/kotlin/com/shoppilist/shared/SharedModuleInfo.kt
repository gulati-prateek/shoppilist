package com.shoppilist.shared

import kotlinx.serialization.Serializable

@Serializable
data class SharedModuleInfo(val moduleName: String, val phase: Int)

object SharedModule {
    val info = SharedModuleInfo(moduleName = "shared", phase = 2)
}
