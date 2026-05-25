package net.diavicecat.scaleray.item.custom

import net.minecraft.network.chat.Component
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResultHolder
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.TooltipFlag
import net.minecraft.world.level.Level
import net.neoforged.api.distmarker.Dist
import net.neoforged.api.distmarker.OnlyIn

class ScaleRayItem(properties: Properties) : Item(properties) {

    override fun use(level: Level, player: Player, hand: InteractionHand): InteractionResultHolder<ItemStack> {
        return InteractionResultHolder.fail(player.getItemInHand(hand))
    }

    @OnlyIn(Dist.CLIENT)
    override fun appendHoverText(stack: ItemStack, context: TooltipContext, lines: MutableList<Component>, flag: TooltipFlag) {
        val config = net.diavicecat.scaleray.client.ScaleRayConfig
        lines += Component.literal("Sneak + Right Click to configure").withStyle { it.withColor(0x888888) }
        lines += Component.literal("Mode: ${config.mode.name.lowercase().replaceFirstChar { it.uppercase() }}").withStyle { it.withColor(0xAAAAAA) }
        lines += Component.literal("Target: ${config.target.name.lowercase().replaceFirstChar { it.uppercase() }}").withStyle { it.withColor(0xAAAAAA) }
        lines += Component.literal("Power: ${"%.2f".format(config.power)}").withStyle { it.withColor(0xAAAAAA) }
    }
}
