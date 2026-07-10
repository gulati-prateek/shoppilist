package com.shoppilist.shared.location

import androidx.compose.runtime.Composable
import com.shoppilist.shared.data.session.StoredLocation

/**
 * Abstracts platform geolocation (FusedLocationProvider/Geocoder have no Kotlin/Native
 * equivalent). The Android `actual` handles the runtime permission flow and reverse-geocodes
 * the fix into a [StoredLocation]; the iOS `actual` reports unavailable until a real
 * CLLocationManager integration is written on a Mac toolchain.
 */
interface LocationController {
    val isAvailable: Boolean

    /** Requests the location permission if needed, then fetches and reverse-geocodes the
     *  device's current location. Exactly one of the remember-callbacks fires. */
    fun requestLocation()
}

@Composable
expect fun rememberLocationController(
    onLocation: (StoredLocation) -> Unit,
    onError: (String) -> Unit
): LocationController
