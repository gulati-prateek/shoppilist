package com.shoppilist.shared.location

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import com.shoppilist.shared.data.session.StoredLocation
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Locale

@Composable
actual fun rememberLocationController(
    onLocation: (StoredLocation) -> Unit,
    onError: (String) -> Unit
): LocationController {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Fine is requested but coarse alone is enough — Android lets users downgrade the grant,
    // and city-level accuracy is all the dashboard/catalog need.
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { grants ->
        if (grants.values.any { it }) {
            fetchLocation(context, scope, onLocation, onError)
        } else {
            onError("Location permission is needed to detect where you are")
        }
    }

    return remember {
        object : LocationController {
            override val isAvailable: Boolean = true

            override fun requestLocation() {
                val granted = LOCATION_PERMISSIONS.any {
                    ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
                }
                if (granted) {
                    fetchLocation(context, scope, onLocation, onError)
                } else {
                    permissionLauncher.launch(LOCATION_PERMISSIONS)
                }
            }
        }
    }
}

private val LOCATION_PERMISSIONS = arrayOf(
    Manifest.permission.ACCESS_FINE_LOCATION,
    Manifest.permission.ACCESS_COARSE_LOCATION
)

@SuppressLint("MissingPermission") // callers reach here only after a successful permission check
private fun fetchLocation(
    context: Context,
    scope: CoroutineScope,
    onLocation: (StoredLocation) -> Unit,
    onError: (String) -> Unit
) {
    val client = LocationServices.getFusedLocationProviderClient(context)
    client.getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, CancellationTokenSource().token)
        .addOnSuccessListener { fix: Location? ->
            if (fix != null) {
                reverseGeocode(context, scope, fix, onLocation)
            } else {
                // getCurrentLocation can return null (e.g. location just toggled on) — the
                // cached last fix is better than failing outright.
                client.lastLocation
                    .addOnSuccessListener { last: Location? ->
                        if (last != null) reverseGeocode(context, scope, last, onLocation)
                        else onError("Couldn't determine your location — is device location turned on?")
                    }
                    .addOnFailureListener { onError("Couldn't determine your location — is device location turned on?") }
            }
        }
        .addOnFailureListener { e -> onError(e.message ?: "Couldn't determine your location") }
}

private fun reverseGeocode(
    context: Context,
    scope: CoroutineScope,
    fix: Location,
    onLocation: (StoredLocation) -> Unit
) {
    // Geocoder's synchronous API blocks on a network call; keep it off the main thread. A
    // geocoding failure still yields a usable result — coordinates alone are worth saving.
    scope.launch(Dispatchers.IO) {
        val address = try {
            @Suppress("DEPRECATION") // listener variant is API 33+; minSdk is 24
            Geocoder(context, Locale.getDefault())
                .getFromLocation(fix.latitude, fix.longitude, 1)
                ?.firstOrNull()
        } catch (_: Exception) {
            null
        }
        val result = StoredLocation(
            latitude = fix.latitude,
            longitude = fix.longitude,
            city = address?.locality ?: address?.subAdminArea,
            state = address?.adminArea,
            countryCode = address?.countryCode,
            addressLine = address?.getAddressLine(0)
        )
        launch(Dispatchers.Main) { onLocation(result) }
    }
}
