package com.shoppilist.shared

/**
 * The device's current region as an ISO-3166 alpha-2 country code (e.g. "IN", "US"), or null if it
 * can't be determined. Used to pick a sensible default phone-number country code on first use,
 * before the user has selected one explicitly.
 */
expect fun deviceRegionCode(): String?
