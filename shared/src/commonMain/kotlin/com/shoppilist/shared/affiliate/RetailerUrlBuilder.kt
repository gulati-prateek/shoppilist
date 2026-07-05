package com.shoppilist.shared.affiliate

import com.shoppilist.shared.data.local.SponsoredRetailerEntity

/** Builds a retailer search URL from its `searchUrlTemplate` (contains a literal "{item}" placeholder). */
object RetailerUrlBuilder {
    fun searchUrl(retailer: SponsoredRetailerEntity, itemName: String): String {
        val encoded = encodeUrlComponent(itemName)
        return retailer.searchUrlTemplate.replace("{item}", encoded)
    }

    /**
     * "Order Whole List" (§2.13): none of the seeded retailers support a real basket API, so the
     * documented fallback is used — open the first item's search and let the rest be added manually.
     */
    fun wholeListPlan(retailer: SponsoredRetailerEntity, itemNames: List<String>): WholeListPlan {
        if (itemNames.isEmpty()) return WholeListPlan(null, emptyList())
        if (retailer.basketApiSupported) {
            // Not exercised by any seeded retailer, but kept for when a real basket API is added.
            return WholeListPlan(searchUrl(retailer, itemNames.joinToString(",")), emptyList())
        }
        return WholeListPlan(searchUrl(retailer, itemNames.first()), itemNames.drop(1))
    }

    data class WholeListPlan(val urlToOpen: String?, val remainingItemsToAddManually: List<String>)
}
