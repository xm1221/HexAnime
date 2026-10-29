package cn.xm1221.hexanime.client

/**
 * TODO: client-side wire-up for staff-follows-cursor.
 *
 * Planned (not implemented):
 * - Called from the client entrypoint ([cn.xm1221.hexanime.HexAnimeClient.init]).
 * - Register playerAnimator layer, start/stop [CursorSampler], send [cn.xm1221.hexanime.net.StaffAimC2S],
 *   apply remote aims from [cn.xm1221.hexanime.net.StaffAimS2C].
 * - Hex Casting GuiSpellcasting detection must be optional/compat (crosshair fallback without Hex).
 */
object StaffFollowClient {
    /** Wire-up entry; currently a no-op stub. */
    fun init() {
        // TODO: register animation layer + tick/network hooks
    }
}
