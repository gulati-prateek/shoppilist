package com.shoppilist.shared.location

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.shoppilist.shared.data.session.StoredLocation

/**
 * Placeholder, mirroring StubAuthService/VoiceInputController.ios: reports unavailable so the
 * shared UI hides location affordances. A real CLLocationManager integration is future work
 * that needs a Mac toolchain to verify.
 */
@Composable
actual fun rememberLocationController(
    onLocation: (StoredLocation) -> Unit,
    onError: (String) -> Unit
): LocationController = remember {
    object : LocationController {
        override val isAvailable: Boolean = false

        override fun requestLocation() {
            onError("Location isn't available on iOS yet")
        }
    }
}
