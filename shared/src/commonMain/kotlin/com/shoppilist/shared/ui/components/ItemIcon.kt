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

/**
 * Common item-name keywords -> emoji, checked before falling back to the category icon so the
 * list rows show recognizable thumbnails. Matching is TOKEN-PREFIX based (see [emojiForItem]):
 * a single-word keyword must be the start of a whole word of the item name, so "oat" no longer
 * matches "cOATs", "cola" no longer matches "choCOLAte", "pan" no longer matches "PANts", and
 * "phone" no longer matches "headPHONEs". Keywords containing a space are matched as phrases.
 * The list is evaluated LONGEST KEYWORD FIRST (ties keep this order), so specific entries like
 * "eggplant"/"pancake"/"headphone" naturally beat "egg"/"pan"/"phone".
 */
private val ITEM_KEYWORD_EMOJI: List<Pair<String, String>> = listOf(
    // Vegetables & herbs
    "spinach" to "🥬", "lettuce" to "🥬", "cabbage" to "🥬", "kale" to "🥬", "celery" to "🥬",
    "bok choy" to "🥬", "mint" to "🌿", "basil" to "🌿", "parsley" to "🌿", "coriander" to "🌿",
    "cilantro" to "🌿", "dill" to "🌿", "rosemary" to "🌿", "thyme" to "🌿", "oregano" to "🌿",
    "lemongrass" to "🌿", "curry leav" to "🌿", "peppermint" to "🌿", "sprout" to "🌱",
    "tomato" to "🍅", "potato" to "🥔", "sweet potato" to "🍠", "carrot" to "🥕",
    "cucumber" to "🥒", "zucchini" to "🥒", "pepper" to "🫑", "chilli" to "🌶️", "chili" to "🌶️",
    "onion" to "🧅", "garlic" to "🧄", "mushroom" to "🍄", "broccoli" to "🥦", "eggplant" to "🍆",
    "aubergine" to "🍆", "pumpkin" to "🎃", "corn" to "🌽", "avocado" to "🥑",
    // Fruit
    "apple" to "🍎", "banana" to "🍌", "orange" to "🍊", "clementine" to "🍊", "tangerine" to "🍊",
    "mandarin" to "🍊", "grapefruit" to "🍊", "lemon" to "🍋", "lime" to "🍋", "grape" to "🍇",
    "raisin" to "🍇", "strawberr" to "🍓", "blueberr" to "🫐", "blackberr" to "🫐",
    "raspberr" to "🫐", "cranberr" to "🍒", "berr" to "🫐", "watermelon" to "🍉",
    "muskmelon" to "🍈", "melon" to "🍈", "pineapple" to "🍍", "mango" to "🥭", "papaya" to "🥭",
    "peach" to "🍑", "apricot" to "🍑", "cherr" to "🍒", "pear" to "🍐", "kiwi" to "🥝",
    "coconut" to "🥥",
    // Dairy & eggs
    "buttermilk" to "🥛", "milkshake" to "🥤", "milk" to "🥛", "yogurt" to "🥛", "yoghurt" to "🥛",
    "curd" to "🥛", "lassi" to "🥛", "cheesecake" to "🍰", "cheese" to "🧀", "paneer" to "🧀",
    "butter" to "🧈", "ghee" to "🧈", "egg" to "🥚",
    // Bakery
    "bread" to "🍞", "baguette" to "🥖", "croissant" to "🥐", "bagel" to "🥯", "toaster" to "🍞",
    "pancake" to "🥞", "cupcake" to "🧁", "muffin" to "🧁", "cake" to "🍰", "brownie" to "🍫",
    "donut" to "🍩", "doughnut" to "🍩", "pastry" to "🥐", "pie" to "🥧", "waffle" to "🧇",
    "pizza" to "🍕", "pepperoni" to "🍕", "roti" to "🫓", "naan" to "🫓", "chapati" to "🫓",
    "paratha" to "🫓", "tortilla" to "🫓", "pita" to "🫓", "shortbread" to "🍪",
    // Meat & seafood
    "chicken" to "🍗", "meat" to "🥩", "beef" to "🥩", "steak" to "🥩", "bacon" to "🥓",
    "mutton" to "🍖", "lamb" to "🍖", "pork" to "🍖", "salami" to "🍖", "turkey" to "🦃",
    "duck" to "🦆", "sausage" to "🌭", "hot dog" to "🌭", "hotdog" to "🌭", "kebab" to "🍢",
    "fish" to "🐟", "salmon" to "🐟", "tuna" to "🐟", "sardine" to "🐟", "mackerel" to "🐟",
    "pomfret" to "🐟", "tilapia" to "🐟", "basa" to "🐟", "cod" to "🐟", "prawn" to "🦐",
    "shrimp" to "🦐", "crab" to "🦀", "lobster" to "🦞", "octopus" to "🐙", "squid" to "🦑",
    // Staples & packaged
    "rice" to "🍚", "pasta" to "🍝", "noodle" to "🍜", "flour" to "🌾", "oat" to "🥣",
    "cornflake" to "🥣", "cereal" to "🥣", "granola" to "🥣", "muesli" to "🥣", "bran" to "🥣",
    "baking soda" to "🧂", "baking powder" to "🧂", "black pepper" to "🧂", "salt" to "🧂",
    "sugar" to "🍬", "oil" to "🫒", "ketchup" to "🍅", "saucepan" to "🍳", "sauce" to "🥫",
    "honey" to "🍯", "cough syrup" to "💊", "syrup" to "🍯", "jam" to "🍓", "dumpling" to "🥟",
    "momo" to "🥟", "samosa" to "🥟", "fries" to "🍟", "burrito" to "🌯",
    // Beverages
    "coffee" to "☕", "tea" to "🍵", "kettle" to "🫖", "water" to "💧", "juice" to "🧃",
    "soda" to "🥤", "cola" to "🥤", "soft drink" to "🥤", "smoothie" to "🥤", "beer" to "🍺",
    "wine" to "🍷",
    // Snacks & sweets
    "chocolate" to "🍫", "candy" to "🍬", "gum" to "🍬", "cookie" to "🍪", "wafer" to "🍪",
    "biscuit" to "🍪", "cracker" to "🥨", "pretzel" to "🥨", "chip" to "🍟", "popcorn" to "🍿",
    "ice cream" to "🍦", "icecream" to "🍦", "peanut" to "🥜", "almond" to "🥜",
    "cashew" to "🥜", "pista" to "🥜", "walnut" to "🥜", "nut" to "🥜",
    // Household & cleaning
    "soap" to "🧼", "shampoo" to "🧴", "toothpaste" to "🪥", "toothbrush" to "🪥",
    "tissue" to "🧻", "toilet paper" to "🧻", "napkin" to "🧻", "detergent" to "🧴",
    "laundry" to "🧺", "basket" to "🧺", "mop" to "🧹", "sponge" to "🧽", "dishwash" to "🧽",
    "candle" to "🕯️", "bucket" to "🪣", "trash bag" to "🗑️", "garbage bag" to "🗑️",
    "vacuum" to "🧹",
    // Baby & pets
    "diaper" to "🧷", "wipe" to "🧻", "sock" to "🧦", "shoe" to "👟", "baby" to "🍼",
    "dog" to "🐕", "cat" to "🐈", "bird" to "🐦", "hamster" to "🐹", "rabbit" to "🐰",
    "aquarium" to "🐠",
    // Electronics
    "smartphone" to "📱", "iphone" to "📱", "phone" to "📱", "laptop" to "💻",
    "charger" to "🔌", "adapter" to "🔌", "cable" to "🔌", "plug" to "🔌",
    "battery" to "🔋", "batteries" to "🔋", "power bank" to "🔋", "powerbank" to "🔋",
    "headphone" to "🎧", "earbud" to "🎧", "earphone" to "🎧", "airpod" to "🎧",
    "speaker" to "🔊", "soundbar" to "🔊", "television" to "📺", "tv" to "📺",
    "camera" to "📷", "webcam" to "📷", "mouse" to "🖱️", "keyboard" to "⌨️",
    "monitor" to "🖥️", "printer" to "🖨️", "pendrive" to "💾", "pen drive" to "💾",
    "usb" to "💾", "torch" to "🔦", "flashlight" to "🔦", "bulb" to "💡", "lamp" to "💡",
    "joystick" to "🕹️", "capture" to "🎮",
    // Clothing & fashion
    "sweatshirt" to "🧥", "shirt" to "👕", "blouse" to "👚", "jean" to "👖", "pant" to "👖",
    "trouser" to "👖", "legging" to "👖", "capri" to "👖", "pajama" to "👖", "pyjama" to "👖",
    "short" to "🩳", "dress" to "👗", "frock" to "👗", "skirt" to "👗", "lehenga" to "👗",
    "saree" to "🥻", "sari" to "🥻", "kurta" to "👕", "coat" to "🧥", "jacket" to "🧥",
    "blazer" to "🧥", "hoodie" to "🧥", "sweater" to "🧥", "cardigan" to "🧥",
    "scarf" to "🧣", "shawl" to "🧣", "dupatta" to "🧣", "glove" to "🧤", "tie" to "👔",
    "suitcase" to "🧳", "briefcase" to "🧳", "luggage" to "🧳", "trolley" to "🧳",
    "suit" to "👔", "capsule" to "💊", "cap" to "🧢", "beanie" to "🧢", "hat" to "👒",
    "underwear" to "🩲", "boxer" to "🩲", "brief" to "🩲", "bracelet" to "📿", "bra" to "👙",
    "sandal" to "👡", "slipper" to "🩴", "flip flop" to "🩴", "heel" to "👠", "boot" to "🥾",
    "sneaker" to "👟", "sunglasses" to "🕶️", "goggles" to "🕶️",
    // Watches, jewelry & bags
    "watch" to "⌚", "earring" to "💎", "brooch" to "💎", "ring" to "💍", "necklace" to "📿",
    "pendant" to "📿", "chain" to "📿", "bangle" to "📿", "anklet" to "📿",
    "backpack" to "🎒", "purse" to "👛", "wallet" to "👛", "clutch" to "👛", "bag" to "👜",
    "perfume" to "🌸", "cologne" to "🌸", "sandalwood" to "🌸",
    // Personal care & pharmacy
    "razor" to "🪒", "shav" to "🪒", "lipstick" to "💄", "mascara" to "💄", "kajal" to "💄",
    "foundation" to "💄", "nail polish" to "💅", "mirror" to "🪞", "beard oil" to "🧴",
    "medicine" to "💊", "vitamin" to "💊", "painkiller" to "💊", "cough" to "💊",
    "bandage" to "🩹", "band" to "🩹", "first aid" to "🩹", "thermometer" to "🌡️",
    "syringe" to "💉",
    // Kitchen & home
    "pancetta" to "🥓", "pancake mix" to "🥞", "paneer tikka" to "🧀", "pan" to "🍳",
    "knife" to "🔪", "knives" to "🔪", "cutting board" to "🔪", "chopping board" to "🔪",
    "spoon" to "🥄", "fork" to "🍴", "plate" to "🍽️", "bowl" to "🥣", "colander" to "🥣",
    "cup" to "🥤", "glass" to "🥛", "pressure cooker" to "🍲", "cooker" to "🍲", "wok" to "🥘",
    "kadai" to "🥘", "kadhai" to "🥘", "tawa" to "🍳", "casserole" to "🥘", "lunch box" to "🍱",
    "tiffin" to "🍱", "cupboard" to "🗄️", "towel" to "🧻", "pillow" to "🛏️",
    // Gifts & stationery
    "gift" to "🎁", "pencil" to "✏️", "pen" to "🖊️", "marker" to "🖊️", "crayon" to "🖍️",
    "notebook" to "📓", "diary" to "📓", "envelope" to "✉️", "greeting card" to "💌",
    "balloon" to "🎈", "ribbon" to "🎀", "toy" to "🧸", "teddy" to "🧸", "doll" to "🧸",
    "puzzle" to "🧩", "board game" to "🎲"
)

/** Evaluated longest-first so specific keywords ("eggplant", "headphone") beat short ones. */
private val SORTED_KEYWORDS: List<Pair<String, String>> =
    ITEM_KEYWORD_EMOJI.sortedByDescending { it.first.length }

private val TOKEN_SPLIT = Regex("[^a-z0-9]+")

/**
 * Best-effort emoji for an item — a specific name-keyword match first, then the item's category
 * icon. Returns null when neither matches so callers can supply their own fallback.
 *
 * Single-word keywords match as a PREFIX OF A WHOLE WORD (so "oat" hits "oats" but not "coats");
 * keywords with a space match as a contained phrase.
 */
fun emojiForItem(name: String, categoryId: String?): String? {
    val lower = name.lowercase()
    val tokens = lower.split(TOKEN_SPLIT).filter { it.isNotEmpty() }
    for ((keyword, emoji) in SORTED_KEYWORDS) {
        val hit = if (' ' in keyword) lower.contains(keyword) else tokens.any { it.startsWith(keyword) }
        if (hit) return emoji
    }
    return categoryId?.let { CATEGORY_EMOJI[it] }
}

/**
 * A rounded thumbnail showing the item's emoji on a soft surface. Never blank: name-keyword match
 * first, then the category icon ([categoryEmoji] — e.g. the category row's own emoji from the DB —
 * beats the static map so custom categories work too), then a generic shopping-bag fallback.
 */
@Composable
fun ItemIcon(
    name: String,
    categoryId: String?,
    modifier: Modifier = Modifier,
    size: Dp = 40.dp,
    categoryEmoji: String? = null
) {
    val emoji = emojiForItem(name, categoryId) ?: categoryEmoji ?: "🛍️"
    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center
    ) {
        Text(emoji, fontSize = (size.value * 0.5f).sp)
    }
}
