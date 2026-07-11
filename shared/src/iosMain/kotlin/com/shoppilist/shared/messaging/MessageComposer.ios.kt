package com.shoppilist.shared.messaging

import androidx.compose.runtime.Composable

/** iOS composer stub (same pattern as the other iOS platform stubs) — the copy-link fallback in
 *  the invite screen covers sharing until the iOS platform integrations land. */
@Composable
actual fun rememberMessageComposer(): MessageComposer = StubMessageComposer

private object StubMessageComposer : MessageComposer {
    override fun composeEmail(to: String, subject: String, body: String): Boolean = false
    override fun composeSms(to: String, body: String): Boolean = false
}
