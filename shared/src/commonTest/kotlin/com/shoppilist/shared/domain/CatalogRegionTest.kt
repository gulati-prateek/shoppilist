package com.shoppilist.shared.domain

import kotlin.test.Test
import kotlin.test.assertEquals

class CatalogRegionTest {

    @Test
    fun india_and_usa_map_to_their_own_regions() {
        assertEquals(CatalogRegion.INDIA, CatalogRegion.forCountry("IN"))
        assertEquals(CatalogRegion.USA, CatalogRegion.forCountry("US"))
    }

    @Test
    fun european_countries_share_the_eu_catalog() {
        listOf("GB", "DE", "FR", "ES", "IT", "NL").forEach { code ->
            assertEquals(CatalogRegion.EUROPE, CatalogRegion.forCountry(code))
        }
    }

    @Test
    fun unknown_null_and_blank_fall_back_to_global() {
        assertEquals(CatalogRegion.GLOBAL, CatalogRegion.forCountry("BR"))
        assertEquals(CatalogRegion.GLOBAL, CatalogRegion.forCountry(null))
        assertEquals(CatalogRegion.GLOBAL, CatalogRegion.forCountry(""))
        assertEquals(CatalogRegion.GLOBAL, CatalogRegion.forCountry("AE"))
    }

    @Test
    fun matching_is_case_and_whitespace_insensitive() {
        assertEquals(CatalogRegion.INDIA, CatalogRegion.forCountry(" in "))
        assertEquals(CatalogRegion.EUROPE, CatalogRegion.forCountry("gb"))
    }

    @Test
    fun global_is_never_a_remote_region() {
        assertEquals(listOf("IN", "US", "EU"), CatalogRegion.REMOTE_REGIONS)
    }
}
