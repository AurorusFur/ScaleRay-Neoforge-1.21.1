package net.diavicecat.scaleray.item.custom

import net.diavicecat.scaleray.component.ModDataComponents
import net.minecraft.network.chat.Component
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.TooltipFlag
import net.neoforged.api.distmarker.Dist
import net.neoforged.api.distmarker.OnlyIn

class PowerCellItem(properties: Properties) : Item(properties) {

    @OnlyIn(Dist.CLIENT)
    override fun appendHoverText(stack: ItemStack, context: TooltipContext, lines: MutableList<Component>, flag: TooltipFlag) {
        val charges = stack.get(ModDataComponents.POWER_CHARGES.get()) ?: 0
        lines += Component.literal("Charges: $charges / $MAX_CHARGES").withStyle { it.withColor(0xAAAAAA) }
        if (charges == 0) lines += Component.literal("Depleted").withStyle { it.withColor(0xFF5555) }
    }

    override fun isBarVisible(stack: ItemStack): Boolean {
        val charges = stack.get(ModDataComponents.POWER_CHARGES.get()) ?: 0
        return charges < MAX_CHARGES
    }

    override fun getBarWidth(stack: ItemStack): Int {
        val charges = stack.get(ModDataComponents.POWER_CHARGES.get()) ?: 0
        return (charges.toDouble() / MAX_CHARGES * 13).toInt()
    }

    override fun getBarColor(stack: ItemStack) = 0x00AA00

    companion object {
        const val MAX_CHARGES = 10
    }
}
