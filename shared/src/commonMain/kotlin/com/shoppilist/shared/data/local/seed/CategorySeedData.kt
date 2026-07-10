package com.shoppilist.shared.data.local.seed

import com.shoppilist.shared.data.local.ItemCategoryEntity

/** Stable category ids referenced by [GlobalItemSeedData] and the category matcher. */
object CategoryIds {
    const val FRESH_PRODUCE = "fresh_produce"
    const val MEAT_SEAFOOD = "meat_seafood"
    const val DAIRY_EGGS = "dairy_eggs"
    const val BAKERY_BREAD = "bakery_bread"
    const val FROZEN_FOODS = "frozen_foods"
    const val CANNED_PACKAGED = "canned_packaged"
    const val PERSONAL_CARE = "personal_care"
    const val HOUSEHOLD_CLEANING = "household_cleaning"
    const val BEVERAGES = "beverages"
    const val SNACKS_CONFECTIONERY = "snacks_confectionery"
    const val BABY_KIDS = "baby_kids"
    const val PET_CARE = "pet_care"
    const val HEALTH_PHARMACY = "health_pharmacy"
    const val SPICES_CONDIMENTS = "spices_condiments"

    // Non-grocery categories added for the CSV-driven catalog expansion (Electronics/Clothing/
    // Mall/Home-Household), see CsvCatalogSeedData.
    const val MOBILE_ACCESSORIES = "mobile_accessories"
    const val COMPUTERS_LAPTOPS = "computers_laptops"
    const val HOME_ENTERTAINMENT = "home_entertainment"
    const val HOME_APPLIANCES = "home_appliances"
    const val GAMING = "gaming"
    const val MENS_WEAR = "mens_wear"
    const val WOMENS_WEAR = "womens_wear"
    const val KIDS_WEAR = "kids_wear"
    const val FOOTWEAR = "footwear"
    const val ETHNIC_WEAR = "ethnic_wear"
    const val WATCHES_JEWELRY = "watches_jewelry"
    const val BAGS_ACCESSORIES = "bags_accessories"
    const val PERFUMES_FRAGRANCES = "perfumes_fragrances"
    const val GIFTS_STATIONERY = "gifts_stationery"
    const val KITCHENWARE_COOKWARE = "kitchenware_cookware"
    const val BEDDING_LINEN = "bedding_linen"
    const val STORAGE_ORGANIZATION = "storage_organization"
}

/** The 14 store-aisle-aligned categories from the product spec (§2.12), plus 17 non-grocery
 *  categories added when the catalog expanded beyond groceries (Electronics/Clothing/Mall/
 *  Home-Household), in default display order. */
object CategorySeedData {
    val categories: List<ItemCategoryEntity> = listOf(
        ItemCategoryEntity(CategoryIds.FRESH_PRODUCE, "Fresh Produce", "🥦", 1),
        ItemCategoryEntity(CategoryIds.MEAT_SEAFOOD, "Meat & Seafood", "🥩", 2),
        ItemCategoryEntity(CategoryIds.DAIRY_EGGS, "Dairy & Eggs", "🥛", 3),
        ItemCategoryEntity(CategoryIds.BAKERY_BREAD, "Bakery & Bread", "🍞", 4),
        ItemCategoryEntity(CategoryIds.FROZEN_FOODS, "Frozen Foods", "🧊", 5),
        ItemCategoryEntity(CategoryIds.CANNED_PACKAGED, "Canned & Packaged", "🥫", 6),
        ItemCategoryEntity(CategoryIds.PERSONAL_CARE, "Personal Care", "🧴", 7),
        ItemCategoryEntity(CategoryIds.HOUSEHOLD_CLEANING, "Household & Cleaning", "🧹", 8),
        ItemCategoryEntity(CategoryIds.BEVERAGES, "Beverages", "🥤", 9),
        ItemCategoryEntity(CategoryIds.SNACKS_CONFECTIONERY, "Snacks & Confectionery", "🍫", 10),
        ItemCategoryEntity(CategoryIds.BABY_KIDS, "Baby & Kids", "🍼", 11),
        ItemCategoryEntity(CategoryIds.PET_CARE, "Pet Care", "🐾", 12),
        ItemCategoryEntity(CategoryIds.HEALTH_PHARMACY, "Health & Pharmacy", "💊", 13),
        ItemCategoryEntity(CategoryIds.SPICES_CONDIMENTS, "Spices & Condiments", "🌶️", 14),
        ItemCategoryEntity(CategoryIds.MOBILE_ACCESSORIES, "Mobile & Accessories", "📱", 15),
        ItemCategoryEntity(CategoryIds.COMPUTERS_LAPTOPS, "Computers & Laptops", "💻", 16),
        ItemCategoryEntity(CategoryIds.HOME_ENTERTAINMENT, "Home Entertainment", "📺", 17),
        ItemCategoryEntity(CategoryIds.HOME_APPLIANCES, "Home Appliances", "🔌", 18),
        ItemCategoryEntity(CategoryIds.GAMING, "Gaming", "🎮", 19),
        ItemCategoryEntity(CategoryIds.MENS_WEAR, "Men's Wear", "👔", 20),
        ItemCategoryEntity(CategoryIds.WOMENS_WEAR, "Women's Wear", "👗", 21),
        ItemCategoryEntity(CategoryIds.KIDS_WEAR, "Kids Wear", "🧒", 22),
        ItemCategoryEntity(CategoryIds.FOOTWEAR, "Footwear", "👟", 23),
        ItemCategoryEntity(CategoryIds.ETHNIC_WEAR, "Traditional & Ethnic Wear", "🥻", 24),
        ItemCategoryEntity(CategoryIds.WATCHES_JEWELRY, "Watches & Jewelry", "⌚", 25),
        ItemCategoryEntity(CategoryIds.BAGS_ACCESSORIES, "Bags & Accessories", "👜", 26),
        ItemCategoryEntity(CategoryIds.PERFUMES_FRAGRANCES, "Perfumes & Fragrances", "🌸", 27),
        ItemCategoryEntity(CategoryIds.GIFTS_STATIONERY, "Gifts & Stationery", "🎁", 28),
        ItemCategoryEntity(CategoryIds.KITCHENWARE_COOKWARE, "Kitchenware & Cookware", "🍳", 29),
        ItemCategoryEntity(CategoryIds.BEDDING_LINEN, "Bedding & Linen", "🛏️", 30),
        ItemCategoryEntity(CategoryIds.STORAGE_ORGANIZATION, "Storage & Organization", "🗄️", 31)
    )

    /**
     * Per-country display-order overrides (§2.12 "Country-aware aisle ordering"), keyed by
     * ISO-2 country code then categoryId. Categories not listed keep their default order.
     * Kept as an in-memory map rather than a DB table: it's a pure sort hint, not user data.
     */
    val countryDisplayOrderOverrides: Map<String, Map<String, Int>> = mapOf(
        "GB" to mapOf(CategoryIds.BAKERY_BREAD to 1, CategoryIds.FRESH_PRODUCE to 2),
        "IN" to mapOf(CategoryIds.FRESH_PRODUCE to 1, CategoryIds.SPICES_CONDIMENTS to 2)
    )
}
