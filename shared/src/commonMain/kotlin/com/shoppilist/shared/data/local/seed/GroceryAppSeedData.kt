package com.shoppilist.shared.data.local.seed

import com.shoppilist.shared.data.local.GroceryAppEntity

/** Local grocery delivery app recommendations per country (§2.10). Deep links are illustrative
 *  custom-scheme placeholders, same spirit as [com.shoppilist.affiliate.AffiliateUrlBuilder]. */
object GroceryAppSeedData {
    val apps: List<GroceryAppEntity> = listOf(
        // India
        GroceryAppEntity("ga_blinkit", "Blinkit", "IN", "🛒", "blinkit://open", storeUrl = "https://www.blinkit.com", displayOrder = 1),
        GroceryAppEntity("ga_zepto", "Zepto", "IN", "⚡", "zepto://open", storeUrl = "https://www.zepto.com", displayOrder = 2),
        GroceryAppEntity("ga_bigbasket", "BigBasket", "IN", "🧺", "bigbasket://open", storeUrl = "https://www.bigbasket.com", displayOrder = 3),
        GroceryAppEntity("ga_jiomart", "JioMart", "IN", "🛍️", "jiomart://open", storeUrl = "https://www.jiomart.com", displayOrder = 4),

        // USA
        GroceryAppEntity("ga_instacart", "Instacart", "US", "🥕", "instacart://open", storeUrl = "https://www.instacart.com", displayOrder = 1),
        GroceryAppEntity("ga_walmart_grocery", "Walmart Grocery", "US", "🏬", "walmart://open", storeUrl = "https://www.walmart.com/grocery", displayOrder = 2),
        GroceryAppEntity("ga_amazon_fresh", "Amazon Fresh", "US", "📦", "amazonfresh://open", storeUrl = "https://www.amazon.com/fresh", displayOrder = 3),

        // UK
        GroceryAppEntity("ga_tesco", "Tesco", "GB", "🛒", "tesco://open", storeUrl = "https://www.tesco.com", displayOrder = 1),
        GroceryAppEntity("ga_ocado", "Ocado", "GB", "🚚", "ocado://open", storeUrl = "https://www.ocado.com", displayOrder = 2),
        GroceryAppEntity("ga_sainsburys", "Sainsbury's", "GB", "🍊", "sainsburys://open", storeUrl = "https://www.sainsburys.co.uk", displayOrder = 3),

        // UAE
        GroceryAppEntity("ga_noon", "Noon", "AE", "🌙", "noon://open", storeUrl = "https://www.noon.com", displayOrder = 1),
        GroceryAppEntity("ga_carrefour", "Carrefour", "AE", "🔵", "carrefour://open", storeUrl = "https://www.carrefouruae.com", displayOrder = 2),
        GroceryAppEntity("ga_talabat_groceries", "Talabat Groceries", "AE", "🍽️", "talabat://open", storeUrl = "https://www.talabat.com", displayOrder = 3),

        // Germany
        GroceryAppEntity("ga_rewe", "Rewe", "DE", "🛒", "rewe://open", storeUrl = "https://www.rewe.de", displayOrder = 1),
        GroceryAppEntity("ga_kaufland", "Kaufland", "DE", "🅺", "kaufland://open", storeUrl = "https://www.kaufland.de", displayOrder = 2),
        GroceryAppEntity("ga_gorillas", "Gorillas", "DE", "🦍", "gorillas://open", storeUrl = "https://gorillas.io", displayOrder = 3)
    )
}
