package cn.xm1221.hexanime.casting.item

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class StaffIdsTest {
    @Test
    fun woodListCoversTheReviewedInventory() {
        assertEquals(
            listOf(
                "staff/oak",
                "staff/spruce",
                "staff/birch",
                "staff/jungle",
                "staff/acacia",
                "staff/dark_oak",
                "staff/crimson",
                "staff/warped",
                "staff/mangrove",
                "staff/cherry",
                "staff/bamboo",
                "staff/edified",
            ),
            StaffIds.WOOD,
        )
    }

    @Test
    fun ornateStavesAreOnlyQuenchedAndMindsplice() {
        assertTrue(StaffIds.isOrnate(StaffIds.QUENCHED))
        assertTrue(StaffIds.isOrnate(StaffIds.MINDSPLICE))
        StaffIds.WOOD.forEach { path -> assertFalse(StaffIds.isOrnate(path)) }
    }
}
