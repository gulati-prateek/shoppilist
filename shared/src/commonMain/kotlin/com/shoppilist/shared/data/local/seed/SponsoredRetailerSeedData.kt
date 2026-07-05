package com.shoppilist.shared.data.local.seed

import com.shoppilist.shared.data.local.SponsoredRetailerEntity

/** Sponsored/organic online-ordering retailers per country (§2.13). No real basket APIs are wired
 *  up in this project, so [SponsoredRetailerEntity.basketApiSupported] is always false. */
object SponsoredRetailerSeedData {
    val retailers: List<SponsoredRetailerEntity> = listOf(
        // India
        SponsoredRetailerEntity("sr_blinkit_in", "Blinkit", "IN", "🛒", isSponsored = true, cpcRate = 0.12, affiliateProgram = "blinkit_affiliate", searchUrlTemplate = "https://blinkit.com/s/?q={item}", displayOrder = 1),
        SponsoredRetailerEntity("sr_bigbasket_in", "BigBasket", "IN", "🧺", isSponsored = true, cpcRate = 0.10, affiliateProgram = "bigbasket_affiliate", searchUrlTemplate = "https://www.bigbasket.com/search/?q={item}", displayOrder = 2),
        SponsoredRetailerEntity("sr_jiomart_in", "JioMart", "IN", "🛍️", isSponsored = true, cpcRate = 0.08, affiliateProgram = "jiomart_affiliate", searchUrlTemplate = "https://www.jiomart.com/search/{item}", displayOrder = 3),
        SponsoredRetailerEntity("sr_zepto_in", "Zepto", "IN", "⚡", isSponsored = false, searchUrlTemplate = "https://www.zepto.com/search?query={item}", displayOrder = 4),
        SponsoredRetailerEntity("sr_swiggy_instamart_in", "Swiggy Instamart", "IN", "🥬", isSponsored = false, searchUrlTemplate = "https://www.swiggy.com/instamart/search?query={item}", displayOrder = 5),

        // USA
        SponsoredRetailerEntity("sr_instacart_us", "Instacart", "US", "🥕", isSponsored = true, cpcRate = 0.25, affiliateProgram = "instacart_affiliate", searchUrlTemplate = "https://www.instacart.com/store/search_v3/term?term={item}", displayOrder = 1),
        SponsoredRetailerEntity("sr_walmart_plus_us", "Walmart+", "US", "🏬", isSponsored = true, cpcRate = 0.18, affiliateProgram = "walmart_creator", searchUrlTemplate = "https://www.walmart.com/search?q={item}", displayOrder = 2),
        SponsoredRetailerEntity("sr_amazon_fresh_us", "Amazon Fresh", "US", "📦", isSponsored = true, cpcRate = 0.20, affiliateProgram = "amazon_associates", searchUrlTemplate = "https://www.amazon.com/s?k={item}", displayOrder = 3),
        SponsoredRetailerEntity("sr_target_us", "Target", "US", "🎯", isSponsored = false, searchUrlTemplate = "https://www.target.com/s?searchTerm={item}", displayOrder = 4),
        SponsoredRetailerEntity("sr_kroger_us", "Kroger", "US", "🛒", isSponsored = false, searchUrlTemplate = "https://www.kroger.com/search?query={item}", displayOrder = 5),
        SponsoredRetailerEntity("sr_costco_us", "Costco", "US", "📦", isSponsored = false, searchUrlTemplate = "https://www.costco.com/CatalogSearch?keyword={item}", displayOrder = 6),

        // UK
        SponsoredRetailerEntity("sr_tesco_gb", "Tesco", "GB", "🛒", isSponsored = true, cpcRate = 0.15, affiliateProgram = "tesco_affiliate", searchUrlTemplate = "https://www.tesco.com/groceries/en-GB/search?query={item}", displayOrder = 1),
        SponsoredRetailerEntity("sr_ocado_gb", "Ocado", "GB", "🚚", isSponsored = true, cpcRate = 0.14, affiliateProgram = "ocado_affiliate", searchUrlTemplate = "https://www.ocado.com/search?entry={item}", displayOrder = 2),
        SponsoredRetailerEntity("sr_sainsburys_gb", "Sainsbury's", "GB", "🍊", isSponsored = true, cpcRate = 0.13, affiliateProgram = "sainsburys_affiliate", searchUrlTemplate = "https://www.sainsburys.co.uk/gol-ui/SearchResults/{item}", displayOrder = 3),
        SponsoredRetailerEntity("sr_morrisons_gb", "Morrisons", "GB", "🍏", isSponsored = false, searchUrlTemplate = "https://groceries.morrisons.com/search?entry={item}", displayOrder = 4),
        SponsoredRetailerEntity("sr_asda_gb", "Asda", "GB", "🟢", isSponsored = false, searchUrlTemplate = "https://groceries.asda.com/search/{item}", displayOrder = 5),
        SponsoredRetailerEntity("sr_waitrose_gb", "Waitrose", "GB", "🍇", isSponsored = false, searchUrlTemplate = "https://www.waitrose.com/ecom/shop/search?searchTerm={item}", displayOrder = 6),

        // UAE
        SponsoredRetailerEntity("sr_noon_ae", "Noon", "AE", "🌙", isSponsored = true, cpcRate = 0.16, affiliateProgram = "noon_affiliate", searchUrlTemplate = "https://www.noon.com/uae-en/search/?q={item}", displayOrder = 1),
        SponsoredRetailerEntity("sr_carrefour_now_ae", "Carrefour Now", "AE", "🔵", isSponsored = true, cpcRate = 0.14, affiliateProgram = "carrefour_affiliate", searchUrlTemplate = "https://www.carrefouruae.com/mafuae/en/search?text={item}", displayOrder = 2),
        SponsoredRetailerEntity("sr_talabat_ae", "Talabat", "AE", "🍽️", isSponsored = true, cpcRate = 0.12, affiliateProgram = "talabat_affiliate", searchUrlTemplate = "https://www.talabat.com/uae/groceries/search?query={item}", displayOrder = 3),
        SponsoredRetailerEntity("sr_lulu_hypermarket_ae", "Lulu Hypermarket", "AE", "🏪", isSponsored = false, searchUrlTemplate = "https://www.luluhypermarket.com/en-ae/search?q={item}", displayOrder = 4),
        SponsoredRetailerEntity("sr_spinneys_ae", "Spinneys", "AE", "🥖", isSponsored = false, searchUrlTemplate = "https://www.spinneys.com/en-ae/search?q={item}", displayOrder = 5),

        // Germany
        SponsoredRetailerEntity("sr_rewe_de", "REWE", "DE", "🛒", isSponsored = true, cpcRate = 0.11, affiliateProgram = "rewe_affiliate", searchUrlTemplate = "https://shop.rewe.de/productList?search={item}", displayOrder = 1),
        SponsoredRetailerEntity("sr_kaufland_de", "Kaufland", "DE", "🅺", isSponsored = true, cpcRate = 0.09, affiliateProgram = "kaufland_affiliate", searchUrlTemplate = "https://www.kaufland.de/search/?search_value={item}", displayOrder = 2),
        SponsoredRetailerEntity("sr_lidl_de", "Lidl", "DE", "🟨", isSponsored = false, searchUrlTemplate = "https://www.lidl.de/q/query/{item}", displayOrder = 3),
        SponsoredRetailerEntity("sr_aldi_de", "Aldi", "DE", "🟦", isSponsored = false, searchUrlTemplate = "https://www.aldi-sued.de/de/suche.html?query={item}", displayOrder = 4),
        SponsoredRetailerEntity("sr_gorillas_de", "Gorillas", "DE", "🦍", isSponsored = false, searchUrlTemplate = "https://gorillas.io/search?q={item}", displayOrder = 5),

        // Australia
        SponsoredRetailerEntity("sr_woolworths_au", "Woolworths", "AU", "🐏", isSponsored = true, cpcRate = 0.15, affiliateProgram = "woolworths_affiliate", searchUrlTemplate = "https://www.woolworths.com.au/shop/search/products?searchTerm={item}", displayOrder = 1),
        SponsoredRetailerEntity("sr_coles_au", "Coles", "AU", "🟥", isSponsored = true, cpcRate = 0.13, affiliateProgram = "coles_affiliate", searchUrlTemplate = "https://www.coles.com.au/search?q={item}", displayOrder = 2),
        SponsoredRetailerEntity("sr_amazon_au_au", "Amazon AU", "AU", "📦", isSponsored = false, searchUrlTemplate = "https://www.amazon.com.au/s?k={item}", displayOrder = 3),
        SponsoredRetailerEntity("sr_costco_au", "Costco", "AU", "📦", isSponsored = false, searchUrlTemplate = "https://www.costco.com.au/CatalogSearch?keyword={item}", displayOrder = 4),

        // Singapore
        SponsoredRetailerEntity("sr_redmart_sg", "RedMart (Lazada)", "SG", "🛍️", isSponsored = true, cpcRate = 0.14, affiliateProgram = "lazada_affiliate", searchUrlTemplate = "https://www.lazada.sg/catalog/?q={item}", displayOrder = 1),
        SponsoredRetailerEntity("sr_fairprice_on_sg", "FairPrice On", "SG", "🟩", isSponsored = true, cpcRate = 0.12, affiliateProgram = "fairprice_affiliate", searchUrlTemplate = "https://www.fairprice.com.sg/search?query={item}", displayOrder = 2),
        SponsoredRetailerEntity("sr_shopee_mart_sg", "Shopee Mart", "SG", "🛒", isSponsored = false, searchUrlTemplate = "https://shopee.sg/search?keyword={item}", displayOrder = 3),
        SponsoredRetailerEntity("sr_cold_storage_sg", "Cold Storage", "SG", "❄️", isSponsored = false, searchUrlTemplate = "https://coldstorage.com.sg/search?q={item}", displayOrder = 4),

        // Brazil
        SponsoredRetailerEntity("sr_ifood_br", "iFood", "BR", "🍔", isSponsored = true, cpcRate = 0.10, affiliateProgram = "ifood_affiliate", searchUrlTemplate = "https://www.ifood.com.br/busca?q={item}", displayOrder = 1),
        SponsoredRetailerEntity("sr_rappi_br", "Rappi", "BR", "🛵", isSponsored = true, cpcRate = 0.09, affiliateProgram = "rappi_affiliate", searchUrlTemplate = "https://www.rappi.com.br/busca/{item}", displayOrder = 2),
        SponsoredRetailerEntity("sr_carrefour_br_br", "Carrefour BR", "BR", "🔵", isSponsored = false, searchUrlTemplate = "https://www.carrefour.com.br/busca/{item}", displayOrder = 3),
        SponsoredRetailerEntity("sr_pao_de_acucar_br", "Pão de Açúcar", "BR", "🍞", isSponsored = false, searchUrlTemplate = "https://www.paodeacucar.com/busca?terms={item}", displayOrder = 4)
    )
}
