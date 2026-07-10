@file:OptIn(kotlin.uuid.ExperimentalUuidApi::class)

package com.shoppilist.shared.data.local

import androidx.room.*
import com.shoppilist.shared.currentTimeMillis
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import kotlin.uuid.Uuid

/**
 * Timestamps are stored as `Long` epoch-millis rather than `java.util.Date` — `Date` doesn't
 * exist on Kotlin/Native, so this keeps entities portable ahead of the Room KMP migration.
 */
class Converters {
    @TypeConverter
    fun fromStringList(value: String?): List<String> {
        if (value.isNullOrEmpty()) return emptyList()
        return Json.decodeFromString(ListSerializer(String.serializer()), value)
    }

    @TypeConverter
    fun toStringList(list: List<String>?): String =
        Json.encodeToString(ListSerializer(String.serializer()), list ?: emptyList())

    @TypeConverter
    fun fromStringMap(value: String?): Map<String, String> {
        if (value.isNullOrEmpty()) return emptyMap()
        return Json.decodeFromString(MapSerializer(String.serializer(), String.serializer()), value)
    }

    @TypeConverter
    fun toStringMap(map: Map<String, String>?): String =
        Json.encodeToString(MapSerializer(String.serializer(), String.serializer()), map ?: emptyMap())
}

enum class MemberRole { OWNER, ADMIN, MEMBER }

/** Per-list collaboration role (distinct from the legacy household-level MemberRole). */
enum class ListRole { OWNER, EDITOR, VIEWER }

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey val userId: String,
    val fullName: String,
    val phone: String?,
    val email: String?,
    val country: String?,
    val state: String?,
    val city: String?,
    val pincode: String?,
    val profileImageUrl: String?,
    // Structured name from the profile-setup form; fullName stays the display fallback for
    // accounts created before the form existed. A null firstName marks the profile incomplete.
    val firstName: String? = null,
    val lastName: String? = null,
    val address: String? = null,
    val countryCode: String? = null,
    val languageCode: String? = null,
    val hideSponsoredLinks: Boolean = false,
    val groceryCardDismissed: Boolean = false,
    val createdAt: Long = currentTimeMillis()
)

@Entity(tableName = "households")
data class HouseholdEntity(
    @PrimaryKey val householdId: String,
    val name: String,
    val description: String?,
    val ownerId: String,
    val createdAt: Long = currentTimeMillis()
)

@Entity(
    tableName = "household_members",
    indices = [Index("householdId"), Index("userId")]
)
data class HouseholdMemberEntity(
    @PrimaryKey val memberId: String,
    val householdId: String,
    val userId: String,
    val role: MemberRole = MemberRole.MEMBER,
    val joinedAt: Long = currentTimeMillis()
)

/** Per-list membership/role — the real collaboration model (§2.5). */
@Entity(
    tableName = "list_members",
    indices = [Index("listId"), Index("userId")]
)
data class ListMemberEntity(
    @PrimaryKey val memberId: String,
    val listId: String,
    val userId: String,
    val role: ListRole = ListRole.EDITOR,
    val joinedAt: Long = currentTimeMillis()
)

@Entity(
    tableName = "shopping_lists",
    indices = [Index("ownerId"), Index("householdId"), Index("parentListId")]
)
data class ShoppingListEntity(
    @PrimaryKey val listId: String,
    val name: String,
    val description: String?,
    val ownerId: String,
    val householdId: String?,
    val parentListId: String? = null,
    val colorHex: String? = null,
    val isFavorite: Boolean = false,
    val pinned: Boolean = false,
    val archived: Boolean = false,
    val createdAt: Long = currentTimeMillis()
)

@Entity(
    tableName = "shopping_items",
    indices = [Index("listId"), Index("category"), Index("categoryId"), Index("assignedTo")]
)
data class ShoppingItemEntity(
    @PrimaryKey val itemId: String,
    val listId: String,
    val name: String,
    val quantity: Double = 1.0,
    val unit: String? = null,
    val category: String? = null,
    val categoryId: String? = null,
    val categoryOverriddenBy: String? = null,
    val thumbnailUrl: String? = null,
    val notes: String? = null,
    val estimatedPrice: Double? = null,
    val checked: Boolean = false,
    val addedBy: String? = null,
    val assignedTo: String? = null,
    val assignedBy: String? = null,
    val assignedAt: Long? = null,
    val sortOrder: Int = 0,
    val createdAt: Long = currentTimeMillis()
)

/** Supermarket-aisle taxonomy (§2.12). `listId = null` rows are global/shared; a non-null
 *  `listId` row is a per-list rename or a custom category the user added for that list. */
@Entity(
    tableName = "item_categories",
    indices = [Index("listId"), Index("countryCode")]
)
data class ItemCategoryEntity(
    @PrimaryKey val categoryId: String,
    val name: String,
    val emoji: String,
    val displayOrder: Int,
    val countryCode: String? = null,
    val listId: String? = null
)

/** User corrections to auto-categorization, checked before the keyword matcher so the
 *  whole family benefits from a single correction (§2.12). */
@Entity(tableName = "category_corrections", indices = [Index("itemName")])
data class CategoryCorrectionEntity(
    @PrimaryKey val id: String,
    val itemName: String,
    val suggestedCategoryId: String?,
    val correctCategoryId: String,
    val correctedBy: String,
    val createdAt: Long = currentTimeMillis()
)

/** Master item catalog. The bundled seed rows carry region "GLOBAL" and are never deleted;
 *  region "IN"/"US"/"EU" rows are a local cache of the Firestore catalog for the user's
 *  region and get replaced wholesale on each sync (§ backend catalog). */
@Entity(tableName = "global_items", indices = [Index("name"), Index("region")])
data class GlobalItemEntity(
    @PrimaryKey val id: String,
    val name: String,
    val categoryId: String,
    val countryCodes: List<String> = emptyList(),
    val languageTranslations: Map<String, String> = emptyMap(),
    val isSeasonal: Boolean = false,
    val seasonMonths: List<String> = emptyList(),
    val region: String = "GLOBAL"
)

/** Per-user purchase history, feeding the suggestion engine (§2.8). */
@Entity(
    tableName = "item_history",
    indices = [Index("userId"), Index(value = ["userId", "itemName", "listId"], unique = true)]
)
data class ItemHistoryEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val itemName: String,
    val listId: String,
    val addedCount: Int = 1,
    val lastAddedAt: Long = currentTimeMillis()
)

/** Tracks dismissed suggestions per user so they rank lower next time (§2.8). */
@Entity(
    tableName = "suggestion_dismissals",
    indices = [Index(value = ["userId", "itemName"], unique = true)]
)
data class SuggestionDismissalEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val itemName: String,
    val dismissCount: Int = 1,
    val lastDismissedAt: Long = currentTimeMillis()
)

/** Local grocery delivery app recommendations per country (§2.10). */
@Entity(tableName = "grocery_apps", indices = [Index("countryCode")])
data class GroceryAppEntity(
    @PrimaryKey val id: String,
    val name: String,
    val countryCode: String,
    val logoEmoji: String,
    val deepLinkAndroid: String,
    val deepLinkIos: String? = null,
    val storeUrl: String,
    val displayOrder: Int = 0
)

/** Sponsored/organic online-ordering retailers per country (§2.13). */
@Entity(tableName = "sponsored_retailers", indices = [Index("countryCode")])
data class SponsoredRetailerEntity(
    @PrimaryKey val id: String,
    val name: String,
    val countryCode: String,
    val logoEmoji: String,
    val isSponsored: Boolean,
    val cpcRate: Double? = null,
    val affiliateProgram: String? = null,
    val deepLinkAndroid: String? = null,
    val searchUrlTemplate: String,
    val basketApiSupported: Boolean = false,
    val basketApiEndpoint: String? = null,
    val displayOrder: Int = 0,
    val isActive: Boolean = true
)

/** Click log for CPC / affiliate revenue tracking (§2.13). */
@Entity(tableName = "sponsored_clicks", indices = [Index("retailerId"), Index("userId")])
data class SponsoredClickEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val retailerId: String,
    val itemId: String? = null,
    val listId: String? = null,
    val clickType: String,
    val countryCode: String,
    val createdAt: Long = currentTimeMillis()
)

/** Lightweight "X is shopping now" presence (§2.5) — local mirror of the sample Firestore broadcast. */
@Entity(tableName = "presence", primaryKeys = ["listId", "userId"])
data class PresenceEntity(
    val listId: String,
    val userId: String,
    val lastActiveAt: Long = currentTimeMillis()
)

@Entity(tableName = "invitations", indices = [Index("listId"), Index("token")])
data class InvitationEntity(
    @PrimaryKey val inviteId: String,
    val householdId: String? = null,
    val listId: String? = null,
    val inviterUserId: String,
    val inviteeContact: String,
    val channel: String?,
    val role: ListRole = ListRole.EDITOR,
    val token: String = Uuid.random().toString(),
    val status: String = "PENDING",
    val expiresAt: Long? = null,
    val createdAt: Long = currentTimeMillis()
)

@Entity(tableName = "notifications")
data class NotificationEntity(
    @PrimaryKey val notificationId: String,
    val userId: String,
    val title: String,
    val body: String,
    val dataPayload: String?,
    val seen: Boolean = false,
    val createdAt: Long = currentTimeMillis()
)

@Entity(tableName = "voice_command_history")
data class VoiceCommandHistoryEntity(
    @PrimaryKey val commandId: String,
    val userId: String,
    val rawText: String,
    val intentType: String?,
    val processedAt: Long = currentTimeMillis(),
    val success: Boolean = false,
    val errorMessage: String? = null
)

@Entity(tableName = "affiliate_clicks")
data class AffiliateClickEntity(
    @PrimaryKey val clickId: String,
    val itemId: String?,
    val platform: String,
    val affiliateUrl: String,
    val attributed: Boolean = false,
    val createdAt: Long = currentTimeMillis()
)

@Entity(tableName = "pending_ops")
data class PendingOpEntity(
    @PrimaryKey val opId: String,
    val opType: String, // CREATE_LIST, ADD_ITEM, UPDATE_ITEM, DELETE_ITEM, etc.
    val targetId: String, // listId or itemId
    val payload: String, // JSON serialized
    val status: String = "PENDING", // PENDING, SYNCED, FAILED
    val createdAt: Long = currentTimeMillis(),
    val updatedAt: Long = currentTimeMillis()
)

@Dao
interface UserDao {
    @Query("SELECT * FROM users WHERE userId = :id LIMIT 1")
    fun getUserFlow(id: String): Flow<UserEntity?>

    @Query("SELECT * FROM users WHERE userId = :id LIMIT 1")
    suspend fun getUserOnce(id: String): UserEntity?

    @Query("SELECT * FROM users WHERE email = :email OR phone = :phone LIMIT 1")
    suspend fun findByContact(email: String?, phone: String?): UserEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(user: UserEntity)

    @Query("DELETE FROM users WHERE userId = :id")
    suspend fun delete(id: String)
}

@Dao
interface HouseholdDao {
    @Query("SELECT * FROM households WHERE householdId = :id LIMIT 1")
    fun getHouseholdFlow(id: String): Flow<HouseholdEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(household: HouseholdEntity)

    @Query("DELETE FROM households WHERE householdId = :id")
    suspend fun delete(id: String)
}

@Dao
interface HouseholdMemberDao {
    @Query("SELECT * FROM household_members WHERE householdId = :householdId")
    fun getMembersFlow(householdId: String): Flow<List<HouseholdMemberEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(member: HouseholdMemberEntity)
}

@Dao
interface ListMemberDao {
    @Query("SELECT * FROM list_members WHERE listId = :listId")
    fun getMembersForList(listId: String): Flow<List<ListMemberEntity>>

    @Query("SELECT * FROM list_members WHERE listId = :listId AND userId = :userId LIMIT 1")
    suspend fun getMember(listId: String, userId: String): ListMemberEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(member: ListMemberEntity)

    @Query("DELETE FROM list_members WHERE listId = :listId AND userId = :userId")
    suspend fun remove(listId: String, userId: String)
}

@Dao
interface ShoppingListDao {
    @Query("SELECT * FROM shopping_lists WHERE parentListId IS NULL ORDER BY pinned DESC, createdAt DESC")
    fun getAllLists(): Flow<List<ShoppingListEntity>>

    @Query("SELECT * FROM shopping_lists WHERE parentListId IS NULL AND archived = 0")
    suspend fun getAllListsOnce(): List<ShoppingListEntity>

    @Query("SELECT * FROM shopping_lists WHERE listId = :id LIMIT 1")
    fun getListFlow(id: String): Flow<ShoppingListEntity?>

    @Query("SELECT * FROM shopping_lists WHERE listId = :id LIMIT 1")
    suspend fun getListOnce(id: String): ShoppingListEntity?

    @Query("SELECT * FROM shopping_lists WHERE parentListId = :parentListId ORDER BY createdAt ASC")
    fun getSubLists(parentListId: String): Flow<List<ShoppingListEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(list: ShoppingListEntity)

    @Query("UPDATE shopping_lists SET archived = 1 WHERE listId = :id")
    suspend fun archive(id: String)

    @Query("UPDATE shopping_lists SET pinned = :pinned WHERE listId = :id")
    suspend fun setPinned(id: String, pinned: Boolean)

    @Query("UPDATE shopping_lists SET parentListId = :parentListId WHERE listId = :id")
    suspend fun setParent(id: String, parentListId: String?)

    @Query("DELETE FROM shopping_lists WHERE listId = :id")
    suspend fun delete(id: String)

    @Query("SELECT listId FROM shopping_lists WHERE parentListId = :parentListId")
    suspend fun getSubListIdsOnce(parentListId: String): List<String>
}

@Dao
interface ShoppingItemDao {
    @Query("SELECT * FROM shopping_items WHERE listId = :listId ORDER BY sortOrder ASC, createdAt ASC")
    fun getItemsForList(listId: String): Flow<List<ShoppingItemEntity>>

    @Query("SELECT * FROM shopping_items WHERE listId = :listId AND assignedTo = :userId ORDER BY sortOrder ASC, createdAt ASC")
    fun getItemsAssignedTo(listId: String, userId: String): Flow<List<ShoppingItemEntity>>

    @Query("SELECT * FROM shopping_items WHERE itemId = :id LIMIT 1")
    fun getItemFlow(id: String): Flow<ShoppingItemEntity?>

    @Query("SELECT * FROM shopping_items WHERE itemId = :id LIMIT 1")
    suspend fun getItemOnce(id: String): ShoppingItemEntity?

    @Query("SELECT * FROM shopping_items WHERE listId = :listId AND checked = 0")
    suspend fun getUncheckedItemsOnce(listId: String): List<ShoppingItemEntity>

    @Query("SELECT * FROM shopping_items WHERE listId = :listId")
    suspend fun getItemsForListOnce(listId: String): List<ShoppingItemEntity>

    @Query("SELECT * FROM shopping_items WHERE itemId IN (:itemIds)")
    suspend fun getItemsByIds(itemIds: List<String>): List<ShoppingItemEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(item: ShoppingItemEntity)

    @Query("UPDATE shopping_items SET checked = :checked WHERE itemId = :id")
    suspend fun setChecked(id: String, checked: Boolean)

    @Query("UPDATE shopping_items SET assignedTo = :userId, assignedBy = :assignedBy, assignedAt = :assignedAt WHERE itemId = :itemId")
    suspend fun assignItem(itemId: String, userId: String?, assignedBy: String?, assignedAt: Long?)

    @Query("UPDATE shopping_items SET assignedTo = NULL, assignedBy = NULL, assignedAt = NULL WHERE itemId = :itemId")
    suspend fun unassignItem(itemId: String)

    @Query("UPDATE shopping_items SET assignedTo = NULL, assignedBy = NULL, assignedAt = NULL WHERE listId = :listId AND assignedTo = :userId")
    suspend fun unassignAllForUserInList(listId: String, userId: String)

    @Query("UPDATE shopping_items SET categoryId = :categoryId, categoryOverriddenBy = :overriddenBy WHERE itemId = :itemId")
    suspend fun updateCategory(itemId: String, categoryId: String?, overriddenBy: String?)

    @Query("UPDATE shopping_items SET sortOrder = :sortOrder WHERE itemId = :itemId")
    suspend fun updateSortOrder(itemId: String, sortOrder: Int)

    @Query("UPDATE shopping_items SET listId = :newListId WHERE itemId IN (:itemIds)")
    suspend fun moveItemsToList(itemIds: List<String>, newListId: String)

    @Query("DELETE FROM shopping_items WHERE itemId = :id")
    suspend fun deleteItem(id: String)

    @Query("DELETE FROM shopping_items WHERE listId = :listId")
    suspend fun deleteAllForList(listId: String)

    @Query("DELETE FROM shopping_items WHERE listId = :listId AND checked = 1")
    suspend fun clearPurchased(listId: String)
}

@Dao
interface ItemCategoryDao {
    @Query("SELECT * FROM item_categories WHERE listId IS NULL ORDER BY displayOrder ASC")
    fun getGlobalCategories(): Flow<List<ItemCategoryEntity>>

    @Query("SELECT * FROM item_categories WHERE listId = :listId OR listId IS NULL ORDER BY displayOrder ASC")
    fun getCategoriesForList(listId: String): Flow<List<ItemCategoryEntity>>

    @Query("SELECT * FROM item_categories WHERE categoryId = :id LIMIT 1")
    suspend fun getById(id: String): ItemCategoryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(category: ItemCategoryEntity)

    @Query("SELECT COUNT(*) FROM item_categories")
    suspend fun count(): Int
}

@Dao
interface CategoryCorrectionDao {
    @Query("SELECT * FROM category_corrections WHERE itemName = :itemName ORDER BY createdAt DESC LIMIT 1")
    suspend fun getLatestCorrection(itemName: String): CategoryCorrectionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(correction: CategoryCorrectionEntity)
}

@Dao
interface GlobalItemDao {
    @Query("SELECT * FROM global_items WHERE name = :name LIMIT 1")
    suspend fun findByName(name: String): GlobalItemEntity?

    @Query("SELECT * FROM global_items WHERE name LIKE :prefix || '%' LIMIT :limit")
    suspend fun searchByPrefix(prefix: String, limit: Int = 20): List<GlobalItemEntity>

    @Query("SELECT * FROM global_items WHERE isSeasonal = 1")
    suspend fun getSeasonalItems(): List<GlobalItemEntity>

    /** Catalog browse for the create-list picker: the user's region plus the GLOBAL base set. */
    @Query("SELECT * FROM global_items WHERE region IN (:regions) ORDER BY name")
    suspend fun getByRegions(regions: List<String>): List<GlobalItemEntity>

    @Query("DELETE FROM global_items WHERE region = :region")
    suspend fun deleteByRegion(region: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<GlobalItemEntity>)

    @Query("SELECT COUNT(*) FROM global_items")
    suspend fun count(): Int
}

data class ItemNameCount(val itemName: String, val total: Int)

@Dao
interface ItemHistoryDao {
    @Query("SELECT * FROM item_history WHERE userId = :userId AND itemName = :itemName AND listId = :listId LIMIT 1")
    suspend fun find(userId: String, itemName: String, listId: String): ItemHistoryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(history: ItemHistoryEntity)

    @Query("SELECT itemName, SUM(addedCount) as total FROM item_history WHERE userId = :userId GROUP BY itemName ORDER BY total DESC LIMIT :limit")
    suspend fun getTopItemNames(userId: String, limit: Int = 20): List<ItemNameCount>

    @Query("SELECT itemName, SUM(addedCount) as total FROM item_history WHERE userId = :userId AND itemName LIKE :prefix || '%' GROUP BY itemName ORDER BY total DESC LIMIT :limit")
    suspend fun searchByPrefix(userId: String, prefix: String, limit: Int = 5): List<ItemNameCount>

    @Query("SELECT itemName FROM item_history WHERE userId = :userId GROUP BY itemName HAVING SUM(addedCount) >= :minCount")
    suspend fun getFrequentItemNames(userId: String, minCount: Int = 3): List<String>
}

@Dao
interface SuggestionDismissalDao {
    @Query("SELECT * FROM suggestion_dismissals WHERE userId = :userId")
    suspend fun getForUser(userId: String): List<SuggestionDismissalEntity>

    @Query("SELECT * FROM suggestion_dismissals WHERE userId = :userId AND itemName = :itemName LIMIT 1")
    suspend fun find(userId: String, itemName: String): SuggestionDismissalEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(dismissal: SuggestionDismissalEntity)
}

@Dao
interface GroceryAppDao {
    @Query("SELECT * FROM grocery_apps WHERE countryCode = :countryCode ORDER BY displayOrder ASC")
    fun getForCountry(countryCode: String): Flow<List<GroceryAppEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(apps: List<GroceryAppEntity>)

    @Query("SELECT COUNT(*) FROM grocery_apps")
    suspend fun count(): Int
}

@Dao
interface SponsoredRetailerDao {
    @Query("SELECT * FROM sponsored_retailers WHERE countryCode = :countryCode AND isActive = 1 ORDER BY isSponsored DESC, displayOrder ASC")
    fun getForCountry(countryCode: String): Flow<List<SponsoredRetailerEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(retailers: List<SponsoredRetailerEntity>)

    @Query("SELECT COUNT(*) FROM sponsored_retailers")
    suspend fun count(): Int
}

@Dao
interface SponsoredClickDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(click: SponsoredClickEntity)

    @Query("SELECT * FROM sponsored_clicks WHERE retailerId = :retailerId")
    fun getClicksForRetailer(retailerId: String): Flow<List<SponsoredClickEntity>>
}

@Dao
interface PresenceDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(presence: PresenceEntity)

    @Query("SELECT * FROM presence WHERE listId = :listId")
    fun getForList(listId: String): Flow<List<PresenceEntity>>
}

@Dao
interface InvitationDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(invite: InvitationEntity)

    @Query("SELECT * FROM invitations WHERE householdId = :householdId")
    fun getInvitesForHousehold(householdId: String): Flow<List<InvitationEntity>>

    @Query("SELECT * FROM invitations WHERE listId = :listId ORDER BY createdAt DESC")
    fun getInvitesForList(listId: String): Flow<List<InvitationEntity>>

    @Query("SELECT * FROM invitations WHERE token = :token LIMIT 1")
    suspend fun findByToken(token: String): InvitationEntity?

    @Query("UPDATE invitations SET status = :status WHERE inviteId = :inviteId")
    suspend fun updateStatus(inviteId: String, status: String)
}

@Dao
interface NotificationDao {
    @Query("SELECT * FROM notifications WHERE userId = :userId ORDER BY createdAt DESC")
    fun getNotifications(userId: String): Flow<List<NotificationEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(notification: NotificationEntity)
}

@Dao
interface VoiceCommandHistoryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(history: VoiceCommandHistoryEntity)

    @Query("SELECT * FROM voice_command_history WHERE userId = :userId ORDER BY processedAt DESC")
    fun getHistory(userId: String): Flow<List<VoiceCommandHistoryEntity>>
}

@Dao
interface AffiliateClickDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(click: AffiliateClickEntity)

    @Query("SELECT * FROM affiliate_clicks WHERE itemId = :itemId")
    fun getClicksForItem(itemId: String): Flow<List<AffiliateClickEntity>>
}

@Dao
interface PendingOpDao {
    @Query("SELECT * FROM pending_ops WHERE status = 'PENDING' ORDER BY createdAt ASC")
    fun getPendingOps(): Flow<List<PendingOpEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(op: PendingOpEntity)

    @Query("UPDATE pending_ops SET status = :status, updatedAt = :now WHERE opId = :opId")
    suspend fun updateStatus(opId: String, status: String, now: Long = currentTimeMillis())

    @Query("DELETE FROM pending_ops WHERE opId = :opId")
    suspend fun delete(opId: String)
}

@Database(
    entities = [
        UserEntity::class,
        HouseholdEntity::class,
        HouseholdMemberEntity::class,
        ListMemberEntity::class,
        ShoppingListEntity::class,
        ShoppingItemEntity::class,
        ItemCategoryEntity::class,
        CategoryCorrectionEntity::class,
        GlobalItemEntity::class,
        ItemHistoryEntity::class,
        SuggestionDismissalEntity::class,
        GroceryAppEntity::class,
        SponsoredRetailerEntity::class,
        SponsoredClickEntity::class,
        PresenceEntity::class,
        InvitationEntity::class,
        NotificationEntity::class,
        VoiceCommandHistoryEntity::class,
        AffiliateClickEntity::class,
        PendingOpEntity::class
    ],
    version = 4,
    // Schema export across multiple KMP compile targets needs extra Gradle-side wiring this
    // project doesn't have yet; the project already relies on fallbackToDestructiveMigration()
    // instead of real migrations, so schema history files aren't load-bearing here.
    exportSchema = false
)
@TypeConverters(Converters::class)
@ConstructedBy(AppDatabaseConstructor::class)
abstract class AppDatabase : androidx.room.RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun householdDao(): HouseholdDao
    abstract fun householdMemberDao(): HouseholdMemberDao
    abstract fun listMemberDao(): ListMemberDao
    abstract fun shoppingListDao(): ShoppingListDao
    abstract fun shoppingItemDao(): ShoppingItemDao
    abstract fun itemCategoryDao(): ItemCategoryDao
    abstract fun categoryCorrectionDao(): CategoryCorrectionDao
    abstract fun globalItemDao(): GlobalItemDao
    abstract fun itemHistoryDao(): ItemHistoryDao
    abstract fun suggestionDismissalDao(): SuggestionDismissalDao
    abstract fun groceryAppDao(): GroceryAppDao
    abstract fun sponsoredRetailerDao(): SponsoredRetailerDao
    abstract fun sponsoredClickDao(): SponsoredClickDao
    abstract fun presenceDao(): PresenceDao
    abstract fun invitationDao(): InvitationDao
    abstract fun notificationDao(): NotificationDao
    abstract fun voiceDao(): VoiceCommandHistoryDao
    abstract fun affiliateDao(): AffiliateClickDao
    abstract fun pendingOpDao(): PendingOpDao
}
