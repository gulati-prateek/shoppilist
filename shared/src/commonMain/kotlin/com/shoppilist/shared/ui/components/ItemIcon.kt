package com.shoppilist.shared.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Category id -> representative emoji, matching the seeded category taxonomy. */
private val CATEGORY_EMOJI: Map<String, String> = mapOf(
    "fresh_produce" to "🥦", "meat_seafood" to "🥩", "dairy_eggs" to "🥛", "bakery_bread" to "🍞",
    "frozen_foods" to "🧊", "canned_packaged" to "🥫", "personal_care" to "🧴",
    "household_cleaning" to "🧹", "beverages" to "🥤", "snacks_confectionery" to "🍫",
    "baby_kids" to "🍼", "pet_care" to "🐾", "health_pharmacy" to "💊", "spices_condiments" to "🌶️",
    "mobile_accessories" to "📱", "computers_laptops" to "💻", "home_entertainment" to "📺",
    "home_appliances" to "🔌", "gaming" to "🎮", "mens_wear" to "👔", "womens_wear" to "👗",
    "kids_wear" to "🧒", "footwear" to "👟", "ethnic_wear" to "🥻", "watches_jewelry" to "⌚",
    "bags_accessories" to "👜", "perfumes_fragrances" to "🌸", "gifts_stationery" to "🎁",
    "kitchenware_cookware" to "🍳", "bedding_linen" to "🛏️", "storage_organization" to "🗄️"
)

/** Common item-name keywords -> emoji, checked before falling back to the category icon so the
 *  list rows show recognizable thumbnails (mockup: spinach, milk, bread, eggs, apple…). */
private val ITEM_KEYWORD_EMOJI: List<Pair<String, String>> = listOf(
    "spinach" to "🥬", "lettuce" to "🥬", "cabbage" to "🥬", "kale" to "🥬",
    "milk" to "🥛", "yogurt" to "🥛", "curd" to "🥛", "cheese" to "🧀", "butter" to "🧈",
    "bread" to "🍞", "baguette" to "🥖", "croissant" to "🥐", "bagel" to "🥯",
    "egg" to "🥚", "apple" to "🍎", "banana" to "🍌", "orange" to "🍊", "lemon" to "🍋",
    "grape" to "🍇", "strawberr" to "🍓", "watermelon" to "🍉", "melon" to "🍈", "pineapple" to "🍍",
    "mango" to "🥭", "peach" to "🍑", "cherr" to "🍒", "pear" to "🍐", "kiwi" to "🥝",
    "coconut" to "🥥", "avocado" to "🥑", "tomato" to "🍅", "potato" to "🥔", "carrot" to "🥕",
    "corn" to "🌽", "cucumber" to "🥒", "pepper" to "🫑", "chilli" to "🌶️", "chili" to "🌶️",
    "onion" to "🧅", "garlic" to "🧄", "mushroom" to "🍄", "broccoli" to "🥦", "eggplant" to "🍆",
    "chicken" to "🍗", "meat" to "🥩", "beef" to "🥩", "steak" to "🥩", "bacon" to "🥓",
    "fish" to "🐟", "salmon" to "🐟", "prawn" to "🦐", "shrimp" to "🦐", "crab" to "🦀",
    "rice" to "🍚", "pasta" to "🍝", "noodle" to "🍜", "flour" to "🌾", "oat" to "🥣",
    "coffee" to "☕", "tea" to "🍵", "water" to "💧", "juice" to "🧃", "soda" to "🥤",
    "cola" to "🥤", "beer" to "🍺", "wine" to "🍷", "chocolate" to "🍫", "candy" to "🍬",
    "cookie" to "🍪", "chip" to "🍟", "popcorn" to "🍿", "honey" to "🍯", "jam" to "🍓",
    "salt" to "🧂", "sugar" to "🍬", "oil" to "🫒", "ketchup" to "🍅", "ice cream" to "🍦",
    "soap" to "🧼", "shampoo" to "🧴", "toothpaste" to "🪥", "toothbrush" to "🪥", "tissue" to "🧻",
    "toilet paper" to "🧻", "detergent" to "🧴", "diaper" to "🧷", "baby" to "🍼",
    "phone" to "📱", "laptop" to "💻", "charger" to "🔌", "headphone" to "🎧", "camera" to "📷",
    "shirt" to "👕", "jean" to "👖", "dress" to "👗", "shoe" to "👟", "sock" to "🧦",
    "watch" to "⌚", "ring" to "💍", "bag" to "👜", "perfume" to "🌸", "gift" to "🎁",
    "pan" to "🍳", "plate" to "🍽️", "cup" to "🥤", "towel" to "🧻", "pillow" to "🛏️",
    "medicine" to "💊", "vitamin" to "💊", "bandage" to "🩹"
)

/**
 * Best-effort emoji for an item — a specific name-keyword match first, then the item's category
 * icon. Returns null when neither matches (L11: rather show a blank thumbnail than a misleading one).
 */
fun emojiForItem(name: String, categoryId: String?): String? {
    val lower = name.lowercase()
    ITEM_KEYWORD_EMOJI.firstOrNull { lower.contains(it.first) }?.let { return it.second }
    return categoryId?.let { CATEGORY_EMOJI[it] }
}

/** A rounded thumbnail showing the item's emoji on a soft surface; blank when no good match (L11). */
@Composable
fun ItemIcon(name: String, categoryId: String?, modifier: Modifier = Modifier, size: Dp = 40.dp) {
    val emoji = emojiForItem(name, categoryId)
    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center
    ) {
        if (emoji != null) Text(emoji, fontSize = (size.value * 0.5f).sp)
    }
}
