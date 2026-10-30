package cn.xm1221.hexanime.client.anim

import cn.xm1221.hexanime.casting.item.StaffMaterial

/**
 * Display stiffness for each staff body.
 *
 * The reviewed plan names quenched as k = 320, ζ = 0.35 and requires a timestep cap
 * because that pair is underdamped. The other rows are chosen in the same units:
 * wood overshoots visibly, mindsplice is soft, tagged stays close to critical.
 */
object StaffSprings {
    data class Params(val stiffness: Float, val dampingRatio: Float)

    fun forMaterial(material: StaffMaterial): Params = when (material) {
        StaffMaterial.QUENCHED -> Params(stiffness = 320f, dampingRatio = 0.35f)
        StaffMaterial.MINDSPLICE -> Params(stiffness = 96f, dampingRatio = 0.88f)
        StaffMaterial.TAGGED -> Params(stiffness = 140f, dampingRatio = 0.95f)
        StaffMaterial.WOOD, StaffMaterial.NONE -> Params(stiffness = 180f, dampingRatio = 0.45f)
    }
}
