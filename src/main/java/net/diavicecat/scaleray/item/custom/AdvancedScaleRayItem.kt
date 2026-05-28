package net.diavicecat.scaleray.item.custom

import net.diavicecat.scaleray.menu.AdvancedScaleRayMenu
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResultHolder
import net.minecraft.world.MenuProvider
import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.Level

class AdvancedScaleRayItem(properties: Properties) : ScaleRayItem(properties) {

    override fun use(level: Level, player: Player, hand: InteractionHand): InteractionResultHolder<ItemStack> {
        if (level.isClientSide) return InteractionResultHolder.pass(player.getItemInHand(hand))
        if (player.isShiftKeyDown && player is ServerPlayer) {
            val rayStack = player.getItemInHand(hand)
            player.openMenu(object : MenuProvider {
                override fun getDisplayName() = Component.literal("Advanced Scale Ray")
                override fun createMenu(id: Int, inv: Inventory, p: Player) = AdvancedScaleRayMenu(id, inv, rayStack)
            }) { buf -> buf.writeVarInt(hand.ordinal) }
            return InteractionResultHolder.consume(rayStack)
        }
        return InteractionResultHolder.fail(player.getItemInHand(hand))
    }
}
