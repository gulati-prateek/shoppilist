package com.shoppilist.shared.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
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
 * How the product typically sits on a supermarket shelf — the thumbnail draws this silhouette
 * behind the item emoji so rows read like mini packshots rather than bare food emojis.
 * [NONE] keeps the plain emoji tile (loose fresh produce, clothing on hangers…).
 */
enum class PackagingType { NONE, CARTON, BOTTLE, PUMP, CAN, JAR, BOX, POUCH, TUBE, TRAY }

/** Default packaging per catalog category (keyword overrides below refine within a category). */
private val CATEGORY_PACKAGING: Map<String, PackagingType> = mapOf(
    "fresh_produce" to PackagingType.NONE,
    "meat_seafood" to PackagingType.TRAY,
    "dairy_eggs" to PackagingType.CARTON,
    "bakery_bread" to PackagingType.POUCH,
    "frozen_foods" to PackagingType.BOX,
    "canned_packaged" to PackagingType.CAN,
    "personal_care" to PackagingType.PUMP,
    "household_cleaning" to PackagingType.PUMP,
    "beverages" to PackagingType.BOTTLE,
    "snacks_confectionery" to PackagingType.POUCH,
    "baby_kids" to PackagingType.BOX,
    "pet_care" to PackagingType.POUCH,
    "health_pharmacy" to PackagingType.BOX,
    "spices_condiments" to PackagingType.JAR,
    "mobile_accessories" to PackagingType.BOX,
    "computers_laptops" to PackagingType.BOX,
    "home_entertainment" to PackagingType.BOX,
    "home_appliances" to PackagingType.BOX,
    "gaming" to PackagingType.BOX,
    "mens_wear" to PackagingType.NONE,
    "womens_wear" to PackagingType.NONE,
    "kids_wear" to PackagingType.NONE,
    "footwear" to PackagingType.BOX,          // shoebox
    "ethnic_wear" to PackagingType.NONE,
    "watches_jewelry" to PackagingType.BOX,   // gift box
    "bags_accessories" to PackagingType.NONE,
    "perfumes_fragrances" to PackagingType.BOTTLE,
    "gifts_stationery" to PackagingType.BOX,
    "kitchenware_cookware" to PackagingType.BOX,
    "bedding_linen" to PackagingType.NONE,
    "storage_organization" to PackagingType.BOX
)

/** Item-level packaging overrides (checked with the same token-prefix rules as the emoji map). */
private val PACKAGING_OVERRIDES: List<Pair<String, PackagingType>> = listOf(
    // Dairy & beverages
    "egg" to PackagingType.TRAY, "cheese" to PackagingType.POUCH, "butter" to PackagingType.BOX,
    "yogurt" to PackagingType.JAR, "yoghurt" to PackagingType.JAR, "curd" to PackagingType.JAR,
    "ghee" to PackagingType.JAR, "ice cream" to PackagingType.JAR, "icecream" to PackagingType.JAR,
    "juice" to PackagingType.CARTON, "milk" to PackagingType.CARTON, "lassi" to PackagingType.CARTON,
    "beer" to PackagingType.CAN, "wine" to PackagingType.BOTTLE, "water" to PackagingType.BOTTLE,
    "coffee" to PackagingType.JAR, "tea" to PackagingType.BOX,
    // Staples & condiments
    "flour" to PackagingType.POUCH, "rice" to PackagingType.POUCH, "sugar" to PackagingType.POUCH,
    "salt" to PackagingType.POUCH, "oat" to PackagingType.BOX, "cereal" to PackagingType.BOX,
    "cornflake" to PackagingType.BOX, "granola" to PackagingType.BOX, "muesli" to PackagingType.BOX,
    "pasta" to PackagingType.POUCH, "noodle" to PackagingType.POUCH,
    "oil" to PackagingType.BOTTLE, "ketchup" to PackagingType.BOTTLE, "sauce" to PackagingType.BOTTLE,
    "vinegar" to PackagingType.BOTTLE, "honey" to PackagingType.JAR, "jam" to PackagingType.JAR,
    "pickle" to PackagingType.JAR, "syrup" to PackagingType.BOTTLE, "cough syrup" to PackagingType.BOTTLE,
    // Seafood that ships tinned
    "tuna" to PackagingType.CAN, "sardine" to PackagingType.CAN,
    // Bakery exceptions
    "cake" to PackagingType.BOX, "donut" to PackagingType.BOX, "doughnut" to PackagingType.BOX,
    "pie" to PackagingType.BOX,
    // Personal care & household
    "toothpaste" to PackagingType.TUBE, "paste" to PackagingType.TUBE,
    "face cream" to PackagingType.TUBE, "shaving cream" to PackagingType.TUBE,
    "gel" to PackagingType.TUBE, "soap" to PackagingType.BOX, "tissue" to PackagingType.BOX,
    "toothbrush" to PackagingType.BOX, "razor" to PackagingType.BOX, "lipstick" to PackagingType.TUBE,
    "perfume" to PackagingType.BOTTLE, "cologne" to PackagingType.BOTTLE,
    "toilet paper" to PackagingType.POUCH, "diaper" to PackagingType.POUCH, "wipe" to PackagingType.POUCH,
    "candle" to PackagingType.BOX,
    // Pharmacy liquids
    "vitamin" to PackagingType.JAR
)

private fun keywordHit(keyword: String, lower: String, tokens: List<String>): Boolean =
    if (' ' in keyword) lower.contains(keyword) else tokens.any { it.startsWith(keyword) }

/** Sorted longest-first like the emoji map, so "cough syrup" beats "syrup" etc. */
private val SORTED_PACKAGING_OVERRIDES: List<Pair<String, PackagingType>> =
    PACKAGING_OVERRIDES.sortedByDescending { it.first.length }

fun packagingForItem(name: String, categoryId: String?): PackagingType {
    val lower = name.lowercase()
    val tokens = lower.split(TOKEN_SPLIT).filter { it.isNotEmpty() }
    SORTED_PACKAGING_OVERRIDES.firstOrNull { keywordHit(it.first, lower, tokens) }?.let { return it.second }
    return categoryId?.let { CATEGORY_PACKAGING[it] } ?: PackagingType.NONE
}

// Soft shelf-packaging tints; picked per category (stable) so a section looks cohesive.
private val PACKAGING_TINTS = listOf(
    Color(0xFFDCE8DD), // sage
    Color(0xFFF3E5C3), // cream gold
    Color(0xFFDBE7F0), // powder blue
    Color(0xFFF3DDDD), // rose
    Color(0xFFEDE3F1), // lilac
    Color(0xFFE7EBD9)  // olive
)

private fun tintFor(seed: String): Color {
    var h = 0
    for (c in seed) h = 31 * h + c.code
    val idx = ((h % PACKAGING_TINTS.size) + PACKAGING_TINTS.size) % PACKAGING_TINTS.size
    return PACKAGING_TINTS[idx]
}

/**
 * A rounded thumbnail showing the item's emoji on its typical supermarket packaging silhouette
 * (carton/bottle/can/box/pouch… — user feedback: icons should look like real shelf products).
 * Never blank: name-keyword match first, then the category icon ([categoryEmoji] — e.g. the
 * category row's own emoji from the DB — beats the static map so custom categories work too),
 * then a generic shopping-bag fallback.
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
    val packaging = packagingForItem(name, categoryId)
    val body = tintFor(categoryId ?: name)
    val outline = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.18f)
    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center
    ) {
        if (packaging != PackagingType.NONE) {
            Canvas(modifier = Modifier.fillMaxSize().padding(2.dp)) {
                drawPackaging(packaging, body, outline)
            }
        }
        // The emoji becomes the product "label", sitting on the silhouette's label area.
        val (labelCenterY, emojiScale) = when (packaging) {
            PackagingType.NONE -> 0.50f to 0.50f
            PackagingType.CARTON -> 0.63f to 0.34f
            PackagingType.BOTTLE -> 0.66f to 0.32f
            PackagingType.PUMP -> 0.66f to 0.32f
            PackagingType.CAN -> 0.55f to 0.34f
            PackagingType.JAR -> 0.60f to 0.34f
            PackagingType.BOX -> 0.53f to 0.36f
            PackagingType.POUCH -> 0.53f to 0.36f
            PackagingType.TUBE -> 0.55f to 0.30f
            PackagingType.TRAY -> 0.40f to 0.42f // product sits ON the tray
        }
        Text(
            emoji,
            fontSize = (size.value * emojiScale).sp,
            modifier = Modifier.offset(y = ((labelCenterY - 0.5f) * size.value).dp)
        )
    }
}

/** Draws one packaging archetype in normalized 0..1 space scaled to the canvas. */
private fun DrawScope.drawPackaging(type: PackagingType, body: Color, outline: Color) {
    val w = size.width
    val h = size.height
    fun x(f: Float) = f * w
    fun y(f: Float) = f * h
    val accent = lerp(body, Color.Black, 0.22f)
    val label = Color.White.copy(alpha = 0.85f)
    val stroke = Stroke(width = w * 0.03f)

    fun labelRect(l: Float, t: Float, r: Float, b: Float) {
        drawRoundRect(
            color = label,
            topLeft = Offset(x(l), y(t)),
            size = Size(x(r - l), y(b - t)),
            cornerRadius = CornerRadius(w * 0.08f)
        )
    }

    when (type) {
        PackagingType.CARTON -> {
            // Gable-top milk/juice carton.
            val path = Path().apply {
                moveTo(x(0.22f), y(0.95f)); lineTo(x(0.22f), y(0.32f)); lineTo(x(0.38f), y(0.10f))
                lineTo(x(0.62f), y(0.10f)); lineTo(x(0.78f), y(0.32f)); lineTo(x(0.78f), y(0.95f)); close()
            }
            drawPath(path, body); drawPath(path, outline, style = stroke)
            drawLine(accent, Offset(x(0.22f), y(0.32f)), Offset(x(0.78f), y(0.32f)), stroke.width)
            labelRect(0.29f, 0.44f, 0.71f, 0.84f)
        }
        PackagingType.BOTTLE -> {
            drawRoundRect(accent, Offset(x(0.42f), y(0.04f)), Size(x(0.16f), y(0.10f)), CornerRadius(w * 0.04f))
            val path = Path().apply {
                moveTo(x(0.44f), y(0.14f)); lineTo(x(0.56f), y(0.14f)); lineTo(x(0.74f), y(0.38f))
                lineTo(x(0.74f), y(0.90f)); lineTo(x(0.68f), y(0.95f)); lineTo(x(0.32f), y(0.95f))
                lineTo(x(0.26f), y(0.90f)); lineTo(x(0.26f), y(0.38f)); close()
            }
            drawPath(path, body); drawPath(path, outline, style = stroke)
            labelRect(0.32f, 0.48f, 0.68f, 0.86f)
        }
        PackagingType.PUMP -> {
            // Pump/spray bottle (shampoo, cleaner).
            drawRoundRect(accent, Offset(x(0.40f), y(0.06f)), Size(x(0.20f), y(0.10f)), CornerRadius(w * 0.03f))
            drawRect(accent, Offset(x(0.60f), y(0.06f)), Size(x(0.14f), y(0.05f)))
            drawRect(accent, Offset(x(0.46f), y(0.16f)), Size(x(0.08f), y(0.10f)))
            drawRoundRect(body, Offset(x(0.28f), y(0.26f)), Size(x(0.44f), y(0.70f)), CornerRadius(w * 0.10f))
            drawRoundRect(outline, Offset(x(0.28f), y(0.26f)), Size(x(0.44f), y(0.70f)), CornerRadius(w * 0.10f), style = stroke)
            labelRect(0.34f, 0.46f, 0.66f, 0.86f)
        }
        PackagingType.CAN -> {
            drawRoundRect(body, Offset(x(0.22f), y(0.14f)), Size(x(0.56f), y(0.78f)), CornerRadius(w * 0.07f))
            drawRoundRect(outline, Offset(x(0.22f), y(0.14f)), Size(x(0.56f), y(0.78f)), CornerRadius(w * 0.07f), style = stroke)
            drawLine(accent, Offset(x(0.22f), y(0.22f)), Offset(x(0.78f), y(0.22f)), stroke.width)
            drawLine(accent, Offset(x(0.22f), y(0.84f)), Offset(x(0.78f), y(0.84f)), stroke.width)
            labelRect(0.28f, 0.32f, 0.72f, 0.78f)
        }
        PackagingType.JAR -> {
            drawRoundRect(accent, Offset(x(0.28f), y(0.08f)), Size(x(0.44f), y(0.14f)), CornerRadius(w * 0.05f))
            drawRoundRect(body, Offset(x(0.20f), y(0.22f)), Size(x(0.60f), y(0.72f)), CornerRadius(w * 0.16f))
            drawRoundRect(outline, Offset(x(0.20f), y(0.22f)), Size(x(0.60f), y(0.72f)), CornerRadius(w * 0.16f), style = stroke)
            labelRect(0.28f, 0.40f, 0.72f, 0.82f)
        }
        PackagingType.BOX -> {
            drawRoundRect(body, Offset(x(0.20f), y(0.10f)), Size(x(0.60f), y(0.84f)), CornerRadius(w * 0.05f))
            drawRoundRect(outline, Offset(x(0.20f), y(0.10f)), Size(x(0.60f), y(0.84f)), CornerRadius(w * 0.05f), style = stroke)
            drawLine(accent, Offset(x(0.20f), y(0.20f)), Offset(x(0.80f), y(0.20f)), stroke.width)
            labelRect(0.27f, 0.28f, 0.73f, 0.80f)
        }
        PackagingType.POUCH -> {
            // Crimped snack pouch.
            drawRect(accent, Offset(x(0.26f), y(0.08f)), Size(x(0.48f), y(0.07f)))
            drawRoundRect(body, Offset(x(0.20f), y(0.15f)), Size(x(0.60f), y(0.72f)), CornerRadius(w * 0.12f))
            drawRoundRect(outline, Offset(x(0.20f), y(0.15f)), Size(x(0.60f), y(0.72f)), CornerRadius(w * 0.12f), style = stroke)
            drawRect(accent, Offset(x(0.26f), y(0.87f)), Size(x(0.48f), y(0.07f)))
            labelRect(0.28f, 0.30f, 0.72f, 0.78f)
        }
        PackagingType.TUBE -> {
            drawRoundRect(accent, Offset(x(0.40f), y(0.04f)), Size(x(0.20f), y(0.12f)), CornerRadius(w * 0.04f))
            val path = Path().apply {
                moveTo(x(0.38f), y(0.16f)); lineTo(x(0.62f), y(0.16f)); lineTo(x(0.74f), y(0.90f))
                lineTo(x(0.26f), y(0.90f)); close()
            }
            drawPath(path, body); drawPath(path, outline, style = stroke)
            drawLine(accent, Offset(x(0.28f), y(0.84f)), Offset(x(0.72f), y(0.84f)), stroke.width)
            labelRect(0.38f, 0.34f, 0.62f, 0.74f)
        }
        PackagingType.TRAY -> {
            // Shallow supermarket tray (meat/eggs); the product emoji sits above it.
            val path = Path().apply {
                moveTo(x(0.12f), y(0.60f)); lineTo(x(0.88f), y(0.60f)); lineTo(x(0.80f), y(0.92f))
                lineTo(x(0.20f), y(0.92f)); close()
            }
            drawPath(path, body); drawPath(path, outline, style = stroke)
            drawLine(accent, Offset(x(0.14f), y(0.67f)), Offset(x(0.86f), y(0.67f)), stroke.width)
        }
        PackagingType.NONE -> Unit
    }
}
