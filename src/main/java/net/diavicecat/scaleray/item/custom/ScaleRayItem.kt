package net.diavicecat.scaleray.item.custom

import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResultHolder
import net.minecraft.world.level.Level
import net.minecraft.world.entity.player.Player

class ScaleRayItem(properties: Properties) : Item(properties) {

    override fun use(level: Level, player: Player, hand: InteractionHand): InteractionResultHolder<ItemStack> {
        return InteractionResultHolder.fail(player.getItemInHand(hand))
    }
}
