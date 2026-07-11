package com.shoppilist.shared.ui.components

import kotlin.test.Test
import kotlin.test.assertEquals

/** Locks in the token-prefix matching fixes for the user-reported wrong icons (Icons-screenshot). */
class ItemIconTest {

    @Test
    fun substring_false_positives_are_fixed() {
        assertEquals("🍫", emojiForItem("Chocolate", null))            // was 🥤 via "cola" in choCOLAte
        assertEquals("🍫", emojiForItem("Chocolate bars", null))
        assertEquals("👖", emojiForItem("Cargo pants", null))          // was 🍳 via "pan" in PANts
        assertEquals("🧥", emojiForItem("Coats", null))                // was 🥣 via "oat" in cOATs
        assertEquals("👟", emojiForItem("Boat shoes", null))           // was 🥣 via "oat" in bOAT
        assertEquals("🎧", emojiForItem("Bluetooth headphones", null)) // was 📱 via "phone"
        assertEquals("🧂", emojiForItem("Baking soda", null))          // was 🥤 via "soda"
        assertEquals("🧂", emojiForItem("Black pepper", null))         // was 🫑 via "pepper"
        assertEquals("👟", emojiForItem("Baby shoes", null))           // was 🍼 via "baby"
        assertEquals("🧦", emojiForItem("Baby socks", null))
        assertEquals("🧴", emojiForItem("Beard oil", null))            // was 🫒 via "oil"
        // "foil" no longer matches "oil" — falls back to the category icon.
        assertEquals("🍳", emojiForItem("Aluminum foil", "kitchenware_cookware"))
        assertEquals("🍆", emojiForItem("Eggplant", null))             // not 🥚
        assertEquals("🥞", emojiForItem("Pancakes", null))             // not 🍳
        assertEquals("🧀", emojiForItem("Paneer", null))               // not 🍳
    }

    @Test
    fun new_item_coverage() {
        assertEquals("🌿", emojiForItem("Mint", null))
        assertEquals("🌿", emojiForItem("Coriander/cilantro", null))
        assertEquals("🫐", emojiForItem("Blueberries", null))
        assertEquals("🥭", emojiForItem("Papaya", null))
        assertEquals("🐟", emojiForItem("Cod", null))
        assertEquals("🦆", emojiForItem("Duck", null))
        assertEquals("🥜", emojiForItem("Almonds", null))
        assertEquals("🍰", emojiForItem("Cake", null))
        assertEquals("🍪", emojiForItem("Biscuits", null))
        assertEquals("🧢", emojiForItem("Caps", null))
        assertEquals("🥾", emojiForItem("Ankle boots", null))
        assertEquals("📿", emojiForItem("Bracelet", null))
        assertEquals("🎮", emojiForItem("Capture card", null))         // not 🧢 via "cap"
        assertEquals("💊", emojiForItem("Capsules", null))
    }

    @Test
    fun category_fallback_still_applies_when_no_keyword_matches() {
        assertEquals("🥦", emojiForItem("Okra", "fresh_produce"))
        assertEquals("🎮", emojiForItem("Console skin", "gaming"))
        assertEquals(null, emojiForItem("Mystery thing", null))
    }

    @Test
    fun packaging_silhouettes_match_real_shelf_products() {
        assertEquals(PackagingType.CARTON, packagingForItem("Milk", "dairy_eggs"))
        assertEquals(PackagingType.TRAY, packagingForItem("Eggs", "dairy_eggs"))
        assertEquals(PackagingType.TRAY, packagingForItem("Chicken breast", "meat_seafood"))
        assertEquals(PackagingType.CAN, packagingForItem("Tuna", "meat_seafood"))
        assertEquals(PackagingType.POUCH, packagingForItem("Potato chips", "snacks_confectionery"))
        assertEquals(PackagingType.BOX, packagingForItem("Cornflakes", "canned_packaged"))
        assertEquals(PackagingType.BOTTLE, packagingForItem("Olive oil", "canned_packaged"))
        assertEquals(PackagingType.JAR, packagingForItem("Honey", "canned_packaged"))
        assertEquals(PackagingType.CAN, packagingForItem("Baked beans", "canned_packaged"))
        assertEquals(PackagingType.TUBE, packagingForItem("Toothpaste", "personal_care"))
        assertEquals(PackagingType.PUMP, packagingForItem("Shampoo", "personal_care"))
        assertEquals(PackagingType.BOX, packagingForItem("Sneakers", "footwear"))
        // Loose fresh produce and clothing stay as plain emoji tiles.
        assertEquals(PackagingType.NONE, packagingForItem("Spinach", "fresh_produce"))
        assertEquals(PackagingType.NONE, packagingForItem("Casual shirts", "mens_wear"))
    }
}
