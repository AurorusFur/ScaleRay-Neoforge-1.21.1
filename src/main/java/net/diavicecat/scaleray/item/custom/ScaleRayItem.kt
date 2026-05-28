package net.diavicecat.scaleray.item.custom

import net.diavicecat.scaleray.menu.ScaleRayMenu
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResultHolder
import net.minecraft.world.MenuProvider
import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.TooltipFlag
import net.minecraft.world.level.Level
import net.neoforged.api.distmarker.Dist
import net.neoforged.api.distmarker.OnlyIn

open class ScaleRayItem(properties: Properties) : Item(properties) {

    override fun use(level: Level, player: Player, hand: InteractionHand): InteractionResultHolder<ItemStack> {
        if (level.isClientSide) return InteractionResultHolder.pass(player.getItemInHand(hand))
        if (player.isShiftKeyDown && player is ServerPlayer) {
            val rayStack = player.getItemInHand(hand)
            player.openMenu(object : MenuProvider {
                override fun getDisplayName() = Component.literal("Scale Ray")
                override fun createMenu(id: Int, inv: Inventory, p: Player) = ScaleRayMenu(id, inv, rayStack)
            }) { buf -> buf.writeVarInt(hand.ordinal) }
            return InteractionResultHolder.consume(rayStack)
        }
        return InteractionResultHolder.fail(player.getItemInHand(hand))
    }

    @OnlyIn(Dist.CLIENT)
    override fun appendHoverText(stack: ItemStack, context: TooltipContext, lines: MutableList<Component>, flag: TooltipFlag) {
        val config = net.diavicecat.scaleray.client.ScaleRayConfig
        lines += Component.literal("Sneak + Right Click to configure").withStyle { it.withColor(0x888888) }
        lines += Component.literal("Mode: ${config.mode.name.lowercase().replaceFirstChar { it.uppercase() }}").withStyle { it.withColor(0xAAAAAA) }
        lines += Component.literal("Target: ${config.target.name.lowercase().replaceFirstChar { it.uppercase() }}").withStyle { it.withColor(0xAAAAAA) }
        lines += Component.literal("Power: ${"%.2f".format(config.power)}").withStyle { it.withColor(0xAAAAAA) }
        val cellContents = stack.get(net.diavicecat.scaleray.component.ModDataComponents.INSERTED_CELL.get())
        val cell = cellContents?.getStackInSlot(0) ?: net.minecraft.world.item.ItemStack.EMPTY
        if (cell.isEmpty) {
            lines += Component.literal("No power cell inserted").withStyle { it.withColor(0xFF5555) }
        } else {
            val cellItem = cell.item as? PowerCellItem
            if (cellItem?.isCreative == true) {
                lines += Component.literal("Cell: ∞ charges").withStyle { it.withColor(0x55FF55) }
            } else {
                val maxCharges = cellItem?.maxCharges ?: 0
                val charges = cell.get(net.diavicecat.scaleray.component.ModDataComponents.POWER_CHARGES.get()) ?: 0
                val color = if (charges > 0) 0x55FF55 else 0xFF5555
                lines += Component.literal("Cell: $charges/$maxCharges charges").withStyle { it.withColor(color) }
            }
        }
    }

    override fun isBarVisible(stack: ItemStack): Boolean {
        val cell = stack.get(net.diavicecat.scaleray.component.ModDataComponents.INSERTED_CELL.get())
            ?.getStackInSlot(0) ?: return false
        if (cell.isEmpty) return false
        val cellItem = cell.item as? PowerCellItem ?: return false
        if (cellItem.isCreative) return false
        val charges = cell.get(net.diavicecat.scaleray.component.ModDataComponents.POWER_CHARGES.get()) ?: 0
        return charges < cellItem.maxCharges
    }

    override fun getBarWidth(stack: ItemStack): Int {
        val cell = stack.get(net.diavicecat.scaleray.component.ModDataComponents.INSERTED_CELL.get())
            ?.getStackInSlot(0) ?: return 0
        if (cell.isEmpty) return 0
        val cellItem = cell.item as? PowerCellItem ?: return 0
        if (cellItem.isCreative) return 13
        val charges = cell.get(net.diavicecat.scaleray.component.ModDataComponents.POWER_CHARGES.get()) ?: 0
        return (charges.toDouble() / cellItem.maxCharges * 13).toInt()
    }

    override fun getBarColor(stack: ItemStack): Int {
        val cell = stack.get(net.diavicecat.scaleray.component.ModDataComponents.INSERTED_CELL.get())
            ?.getStackInSlot(0) ?: return 0xFF0000
        if (cell.isEmpty) return 0xFF0000
        val cellItem = cell.item as? PowerCellItem ?: return 0xFF0000
        if (cellItem.isCreative) return 0x00AA00
        val charges = cell.get(net.diavicecat.scaleray.component.ModDataComponents.POWER_CHARGES.get()) ?: 0
        return if (charges > 0) 0x00AA00 else 0xFF0000
    }
}
