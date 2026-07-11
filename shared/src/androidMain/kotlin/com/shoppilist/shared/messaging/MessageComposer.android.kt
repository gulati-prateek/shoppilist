package com.shoppilist.shared.messaging

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

@Composable
actual fun rememberMessageComposer(): MessageComposer {
    val context = LocalContext.current
    return remember(context) { AndroidMessageComposer(context) }
}

private class AndroidMessageComposer(private val context: Context) : MessageComposer {

    // Fully-encoded mailto: URI (rather than EXTRA_EMAIL and friends) — the most portable way to
    // pre-fill recipient/subject/body across mail clients.
    override fun composeEmail(to: String, subject: String, body: String): Boolean = launch(
        Intent(
            Intent.ACTION_SENDTO,
            Uri.parse("mailto:${Uri.encode(to)}?subject=${Uri.encode(subject)}&body=${Uri.encode(body)}")
        )
    )

    override fun composeSms(to: String, body: String): Boolean = launch(
        Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:${Uri.encode(to)}")).apply {
            putExtra("sms_body", body)
        }
    )

    private fun launch(intent: Intent): Boolean = try {
        context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        true
    } catch (e: ActivityNotFoundException) {
        false
    }
}
