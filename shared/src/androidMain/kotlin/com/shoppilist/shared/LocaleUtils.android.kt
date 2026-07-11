package com.shoppilist.shared

import java.util.Locale

actual fun deviceRegionCode(): String? =
    Locale.getDefault().country.takeIf { it.isNotBlank() }?.uppercase()
