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
}

/** The 14 store-aisle-aligned categories from the product spec (§2.12), in default display order. */
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
        ItemCategoryEntity(CategoryIds.SPICES_CONDIMENTS, "Spices & Condiments", "🌶️", 14)
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
