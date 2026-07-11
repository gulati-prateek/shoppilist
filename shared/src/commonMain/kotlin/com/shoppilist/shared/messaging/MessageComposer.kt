package com.shoppilist.shared.messaging

import androidx.compose.runtime.Composable

/**
 * Opens the platform's SMS/email composer pre-filled with the invite message, so "Send Invite"
 * actually hands the message to the user's messaging app instead of only generating a link.
 * (There is no server-side mailer/SMS gateway; the user hits Send in their own app.)
 */
interface MessageComposer {
    /** Email composer with recipient/subject/body pre-filled. False when unavailable. */
    fun composeEmail(to: String, subject: String, body: String): Boolean

    /** SMS composer with recipient + body pre-filled. False when unavailable. */
    fun composeSms(to: String, body: String): Boolean
}

@Composable
expect fun rememberMessageComposer(): MessageComposer
