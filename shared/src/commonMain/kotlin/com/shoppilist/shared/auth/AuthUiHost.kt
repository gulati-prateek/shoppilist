package com.shoppilist.shared.auth

import androidx.compose.runtime.Composable

/** Platform UI handle for [AuthService.startPhoneVerification] — the enclosing Activity on
 *  Android (Firebase phone auth requires one), null on iOS. */
@Composable
expect fun rememberAuthUiHost(): Any?
