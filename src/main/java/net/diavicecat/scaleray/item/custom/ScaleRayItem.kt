package net.diavicecat.scaleray.item.custom

import net.diavicecat.scaleray.Events.Keybinds
import net.diavicecat.scaleray.network.ScaleRayPayload
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResultHolder
import net.minecraft.world.level.Level
import net.minecraft.world.entity.player.Player
import net.neoforged.neoforge.network.PacketDistributor

class ScaleRayItem(properties: Properties) : Item(properties) {

    override fun use(
        level: Level,
        player: Player,
        hand: InteractionHand
    ): InteractionResultHolder<ItemStack> {
        if (level.isClientSide) {
            // Keybinds references KeyMapping (client-only); this block never runs on the
            // dedicated server, so that class is never loaded there.
            val action = when {
                Keybinds.ShrinkReset.isDown -> ScaleRayPayload.Action.RESET
                Keybinds.ShrinkMode.isDown -> ScaleRayPayload.Action.GROW
                else -> ScaleRayPayload.Action.SHRINK
            }
            PacketDistributor.sendToServer(ScaleRayPayload(action))
        }
        return InteractionResultHolder.success(player.getItemInHand(hand))
    }
}
