package cn.xm1221.hexanime.client.anim

import cn.xm1221.hexanime.casting.item.ItemCastMotion
import kotlin.math.PI
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ItemFloatMotionsTest {
    @Test
    fun armFlightAndChestHoverHaveDistinctTargets() {
        val hand = ItemFloatMotions.pose(ItemCastMotion.ARM_FLIGHT, 0.5f, -1f)!!
        val body = ItemFloatMotions.pose(ItemCastMotion.CHEST_HOVER, 0.5f, -1f)!!

        assertTrue(hand.itemZ < body.itemZ)
        assertTrue(abs(body.itemX) > abs(hand.itemX))
        assertTrue(abs(hand.itemRoll) < 0.2f)
    }

    @Test
    fun floatingItemReturnsBeforeAndAfterItsWindow() {
        assertNull(ItemFloatMotions.pose(ItemCastMotion.ARM_FLIGHT, 0.1f, 1f))
        assertNull(ItemFloatMotions.pose(ItemCastMotion.CHEST_HOVER, 0.9f, 1f))
    }

    @Test
    fun bodyHoverMovesInwardFromTheCastingArm() {
        val right = ItemFloatMotions.pose(ItemCastMotion.CHEST_HOVER, 0.5f, -1f)!!
        val left = ItemFloatMotions.pose(ItemCastMotion.CHEST_HOVER, 0.5f, 1f)!!

        assertTrue(right.itemX < 0f)
        assertTrue(left.itemX > 0f)
    }

    @Test
    fun ringSpinUsesTheVerticalAxis() {
        val ring = ItemFloatMotions.pose(ItemCastMotion.CHEST_SPIN, 0.5f, -1f)!!

        assertTrue(abs(ring.itemYaw) > PI.toFloat() / 2f)
        assertTrue(abs(ring.itemRoll) < 0.2f)
        assertTrue(abs(ring.itemPitch) < 0.2f)
    }

    @Test
    fun cadSilhouettesSelectHoverAndSpinFamilies() {
        assertTrue(ItemCastMotion.forVariant(0) == ItemCastMotion.ARM_FLIGHT)
        assertTrue(ItemCastMotion.forVariant(2) == ItemCastMotion.ARM_FLIGHT)
        assertTrue(ItemCastMotion.forVariant(5) == ItemCastMotion.HAND_THRUST)
        assertTrue(ItemCastMotion.forVariant(1) == ItemCastMotion.CHEST_SPIN)
        assertTrue(ItemCastMotion.forVariant(4) == ItemCastMotion.CHEST_SPIN)
        assertTrue(ItemCastMotion.forVariant(6) == ItemCastMotion.CHEST_SPIN)
        assertTrue(ItemCastMotion.forVariant(3) == ItemCastMotion.CHEST_HOVER)
        assertTrue(ItemCastMotion.forVariant(7) == ItemCastMotion.CHEST_HOVER)
    }

    @Test
    fun wandVariantAlignsItsAxisThenThrustsForward() {
        val rolling = ItemFloatMotions.pose(ItemCastMotion.HAND_THRUST, 0.3f, 1f)!!
        val aligned = ItemFloatMotions.pose(ItemCastMotion.HAND_THRUST, 0.48f, 1f)!!
        val thrusting = ItemFloatMotions.pose(ItemCastMotion.HAND_THRUST, 0.62f, 1f)!!

        assertTrue(rolling.itemRoll < -0.5f)
        assertTrue(aligned.itemPitch < -1.4f)
        assertTrue(thrusting.itemZ < -0.3f)
        assertTrue(thrusting.armPitch < -1f)
        assertTrue(abs(thrusting.itemYaw) < 0.1f)
    }

    @Test
    fun authoredChestFloatUsesCameraLocalPixelsAndUpwardSign() {
        val chest = ItemFloatMotions.pose(ItemCastMotion.CHEST_HOVER, 0.5f, 1f)!!
        val target = chest.firstPersonTarget(1f)

        assertTrue(abs(target.x) < 0.05f)
        assertTrue(target.y in -0.3f..-0.1f)
        assertTrue(target.z in -0.55f..-0.35f)
    }
}
