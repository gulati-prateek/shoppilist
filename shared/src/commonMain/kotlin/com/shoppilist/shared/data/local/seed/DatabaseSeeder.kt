package com.shoppilist.shared.data.local.seed

import com.shoppilist.shared.data.local.AppDatabase

/** Seeds category taxonomy, grocery apps, sponsored retailers, and the global item catalog on first DB creation. */
object DatabaseSeeder {
    suspend fun seed(db: AppDatabase) {
        val categoryDao = db.itemCategoryDao()
        CategorySeedData.categories.forEach { categoryDao.upsert(it) }

        db.groceryAppDao().insertAll(GroceryAppSeedData.apps)
        db.sponsoredRetailerDao().insertAll(SponsoredRetailerSeedData.retailers)
        db.globalItemDao().insertAll(GlobalItemSeedData.items)
        db.globalItemDao().insertAll(CsvCatalogSeedData.items)
    }
}
