package cn.xm1221.hexanime.client.anim

/**
 * TODO: staff-follows-cursor animation driven by playerAnimator.
 *
 * Planned (not implemented):
 * - Extend playerAnimator `PlayerAnimationFrame` (or equivalent keyframe driver).
 * - Each frame set rightArm rotation from sampled cursor / aim vector.
 * - Attach via `ModifierLayer` with priority >= 100.
 * - Active only when Hex Casting GuiSpellcasting is open AND the player is holding a staff;
 *   if Hex Casting is absent, fall back to following the crosshair while holding a staff-like item.
 * - Research refs on box: /workspace/research/HexMod, /workspace/research/playerAnimator-1.20, /workspace/research/PAL-1.21.1
 *
 * Keep Hex Casting references behind an optional compat check — do not hard-link Hex classes here.
 */
object StaffFollowAnimation {
    // stub
}
