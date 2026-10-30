package cn.xm1221.hexanime.client

import cn.xm1221.hexanime.casting.item.CastPulseKind
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CastItemActivationPolicyTest {
    @Test
    fun depletionAlwaysGetsTheItemActivation() {
        assertTrue(CastItemActivationPolicy.shouldShow(CastPulseKind.BREAK, 0f, 0))
    }

    @Test
    fun ordinaryCastsDoNotTriggerTheScreenActivation() {
        assertFalse(CastItemActivationPolicy.shouldShow(CastPulseKind.CAST, 0.4f, 1_000))
        assertFalse(CastItemActivationPolicy.shouldShow(CastPulseKind.CAST, 0.9f, 199))
    }

    @Test
    fun highStrainCastRespectsTheCooldown() {
        assertTrue(
            CastItemActivationPolicy.shouldShow(
                CastPulseKind.CAST,
                CastItemActivationPolicy.MIN_CAST_STRAIN,
                CastItemActivationPolicy.COOLDOWN_TICKS,
            ),
        )
    }
}
