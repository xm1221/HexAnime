package cn.xm1221.hexanime.casting.item

import kotlin.test.Test
import kotlin.test.assertNotNull

class CastItemProfilesTest {
    @Test
    fun mishapEnvironmentFallbacksResolveOnTheClientToo() {
        assertNotNull(CastItemProfiles.byId(CastItemProfiles.mishapFallbackId(true).toString()))
        assertNotNull(CastItemProfiles.byId(CastItemProfiles.mishapFallbackId(false).toString()))
    }
}
