package cn.xm1221.hexanime.command

import cn.xm1221.hexanime.casting.CastPulseRelay
import cn.xm1221.hexanime.casting.MishapFlavor
import cn.xm1221.hexanime.casting.item.CastItems
import cn.xm1221.hexanime.casting.item.CastPulseKind
import com.mojang.brigadier.arguments.StringArgumentType
import dev.architectury.event.events.common.CommandRegistrationEvent
import net.minecraft.commands.Commands
import net.minecraft.network.chat.Component
import net.minecraft.world.InteractionHand

/** Administrator-only animation probes for deterministic visual testing. */
object HexAnimeCommands {
    private enum class CastProbe(val commandName: String, val variant: Int) {
        HAND("hand", 0),
        CHEST("chest", 3),
        SPIN("spin", 1),
        THRUST("thrust", 5),
    }

    fun init() {
        CommandRegistrationEvent.EVENT.register { dispatcher, _, _ ->
            dispatcher.register(
                Commands.literal("hexanime")
                    .requires { source -> source.hasPermission(2) }
                    .then(
                        Commands.literal("test")
                            .then(
                                Commands.literal("mishap")
                                    .then(
                                        Commands.argument("flavor", StringArgumentType.word())
                                            .suggests { _, builder ->
                                                val remaining = builder.remainingLowerCase
                                                MishapFlavor.entries
                                                    .map(MishapFlavor::commandName)
                                                    .filter { it.startsWith(remaining) }
                                                    .forEach(builder::suggest)
                                                builder.buildFuture()
                                            }
                                            .executes { context ->
                                                val player = context.source.playerOrException
                                                val flavor = MishapFlavor.fromCommandName(
                                                    StringArgumentType.getString(context, "flavor"),
                                                ) ?: return@executes 0
                                                val hand = InteractionHand.entries.firstOrNull { held ->
                                                    CastItems.snapshot(player.getItemInHand(held)) != null
                                                } ?: return@executes 0
                                                CastPulseRelay.broadcast(
                                                    player,
                                                    hand,
                                                    CastPulseKind.MISHAP,
                                                    flavor.variant,
                                                    forceScreenEffect = true,
                                                )
                                                player.displayClientMessage(
                                                    Component.literal("HexAnime mishap: ${flavor.commandName}"),
                                                    false,
                                                )
                                                1
                                            },
                                    ),
                            )
                            .then(
                                Commands.literal("cast")
                                    .then(
                                        Commands.argument("motion", StringArgumentType.word())
                                            .suggests { _, builder ->
                                                val remaining = builder.remainingLowerCase
                                                CastProbe.entries
                                                    .map(CastProbe::commandName)
                                                    .filter { it.startsWith(remaining) }
                                                    .forEach(builder::suggest)
                                                builder.buildFuture()
                                            }
                                            .executes { context ->
                                                val player = context.source.playerOrException
                                                val name = StringArgumentType.getString(context, "motion")
                                                val probe = CastProbe.entries.firstOrNull {
                                                    it.commandName == name.lowercase()
                                                } ?: return@executes 0
                                                val hand = InteractionHand.entries.firstOrNull { held ->
                                                    CastItems.snapshot(player.getItemInHand(held))?.packagedHex == true
                                                } ?: run {
                                                    player.displayClientMessage(
                                                        Component.literal("Hold a packaged Hex item first."),
                                                        false,
                                                    )
                                                    return@executes 0
                                                }
                                                CastPulseRelay.broadcast(
                                                    player,
                                                    hand,
                                                    CastPulseKind.CAST,
                                                    probe.variant,
                                                )
                                                player.displayClientMessage(
                                                    Component.literal("HexAnime cast: ${probe.commandName}"),
                                                    false,
                                                )
                                                1
                                            },
                                    ),
                            ),
                    ),
            )
        }
    }
}
