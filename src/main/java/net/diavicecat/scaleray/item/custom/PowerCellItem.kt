package net.diavicecat.scaleray.item.custom

import net.diavicecat.scaleray.component.ModDataComponents
import net.minecraft.network.chat.Component
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.TooltipFlag
import net.neoforged.api.distmarker.Dist
import net.neoforged.api.distmarker.OnlyIn

class PowerCellItem(properties: Properties, val maxCharges: Int) : Item(properties) {

    val isCreative = maxCharges == Int.MAX_VALUE

    @OnlyIn(Dist.CLIENT)
    override fun appendHoverText(stack: ItemStack, context: TooltipContext, lines: MutableList<Component>, flag: TooltipFlag) {
        if (isCreative) {
            lines += Component.literal("Charges: ∞").withStyle { it.withColor(0xAAAAAA) }
        } else {
            val charges = stack.get(ModDataComponents.POWER_CHARGES.get()) ?: 0
            lines += Component.literal("Charges: $charges / $maxCharges").withStyle { it.withColor(0xAAAAAA) }
            if (charges == 0) lines += Component.literal("Depleted").withStyle { it.withColor(0xFF5555) }
        }
    }

    override fun isBarVisible(stack: ItemStack): Boolean {
        if (isCreative) return false
        val charges = stack.get(ModDataComponents.POWER_CHARGES.get()) ?: 0
        return charges < maxCharges
    }

    override fun getBarWidth(stack: ItemStack): Int {
        if (isCreative) return 13
        val charges = stack.get(ModDataComponents.POWER_CHARGES.get()) ?: 0
        return (charges.toDouble() / maxCharges * 13).toInt()
    }

    override fun getBarColor(stack: ItemStack) = 0x00AA00
}
