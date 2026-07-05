package com.shoppilist.shared.affiliate

object AffiliateUrlBuilder {
    fun amazonSearchUrl(itemName: String, affiliateTag: String): String {
        val q = encodeUrlComponent(itemName)
        return "https://www.amazon.in/s?k=$q&tag=$affiliateTag"
    }

    fun flipkartSearchUrl(itemName: String, affiliateToken: String): String {
        val q = encodeUrlComponent(itemName)
        // Flipkart affiliate token usage varies; this is illustrative.
        return "https://www.flipkart.com/search?q=$q&affid=$affiliateToken"
    }

    fun bigbasketSearchUrl(itemName: String): String {
        val q = encodeUrlComponent(itemName)
        return "https://www.bigbasket.com/search/?q=$q"
    }

    // Add more builders for Blinkit, Zepto, Instamart, JioMart.
}

