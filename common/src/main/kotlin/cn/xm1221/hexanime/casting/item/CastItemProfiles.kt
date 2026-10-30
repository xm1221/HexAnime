package cn.xm1221.hexanime.casting.item

import at.petrak.hexcasting.api.mod.HexTags
import at.petrak.hexcasting.common.items.ItemStaff
import at.petrak.hexcasting.common.items.magic.ItemPackagedHex
import cn.xm1221.hexanime.HexAnime
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.item.ItemStack

/**
 * Ordered item-to-animation registry. Packaged Hex items all resolve to one family profile; their
 * visual Variant selects the motion. Third-party profiles can still override by priority.
 */
object CastItemProfiles {
    private val profiles = mutableListOf<CastItemProfile>()
    private var bootstrapped = false

    fun bootstrap() {
        if (bootstrapped) return
        bootstrapped = true
        register(
            CastItemProfile(
                id = HexAnime.id("packaged/hex"),
                priority = 70,
                supportsAim = false,
                supportsPulse = true,
                damage = DamagePose(),
            ) { stack -> stack.item is ItemPackagedHex },
        )
        register(
            staffProfile(
                "staff/quenched",
                priority = 60,
                damage = DamagePose(threshold = 0.55f, tremorRoll = 0.07f),
                material = StaffMaterial.QUENCHED,
            ) { stack -> idOf(stack).path == StaffIds.QUENCHED },
        )
        register(
            staffProfile(
                "staff/mindsplice",
                priority = 59,
                damage = DamagePose(frequency = 1.05f, tremorRoll = 0.08f),
                material = StaffMaterial.MINDSPLICE,
            ) { stack -> idOf(stack).path == StaffIds.MINDSPLICE },
        )
        register(
            staffProfile(
                "staff/wood",
                priority = 40,
                damage = DamagePose(),
                material = StaffMaterial.WOOD,
            ) { stack ->
                stack.item is ItemStaff && !StaffIds.isOrnate(idOf(stack).path)
            },
        )
        register(
            staffProfile(
                "staff/tagged",
                priority = 20,
                damage = DamagePose(),
                material = StaffMaterial.TAGGED,
            ) { stack -> stack.`is`(HexTags.Items.STAVES) },
        )
    }

    fun register(profile: CastItemProfile) {
        require(profiles.none { it.id == profile.id }) { "Duplicate cast profile: ${profile.id}" }
        profiles += profile
        profiles.sortByDescending { it.priority }
    }

    fun resolve(stack: ItemStack): CastItemProfile? {
        if (stack.isEmpty) return null
        bootstrap()
        return profiles.firstOrNull { it.matches(stack) }
    }

    fun byId(id: String): CastItemProfile? {
        bootstrap()
        val location = ResourceLocation.tryParse(id) ?: return null
        return profiles.firstOrNull { it.id == location }
    }

    private fun staffProfile(
        path: String,
        priority: Int,
        damage: DamagePose,
        material: StaffMaterial,
        matcher: (ItemStack) -> Boolean,
    ) = CastItemProfile(
        id = HexAnime.id(path),
        priority = priority,
        supportsAim = true,
        supportsPulse = true,
        damage = damage,
        staffMaterial = material,
        matcher = matcher,
    )

    private fun idOf(stack: ItemStack): ResourceLocation = BuiltInRegistries.ITEM.getKey(stack.item)
}
