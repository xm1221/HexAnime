package cn.xm1221.hexanime.casting.item

/**
 * Hex Casting 0.11.4 staff item paths. Ornate staves are only quenched and mindsplice;
 * every other [at.petrak.hexcasting.common.items.ItemStaff] uses the wood flourish.
 */
object StaffIds {
    const val QUENCHED = "staff/quenched"
    const val MINDSPLICE = "staff/mindsplice"

    val WOOD = listOf(
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
    )

    fun isOrnate(path: String): Boolean = path == QUENCHED || path == MINDSPLICE
}
