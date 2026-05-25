package net.diavicecat.scaleray.menu

import net.diavicecat.scaleray.component.ModDataComponents
import net.diavicecat.scaleray.item.custom.PowerCellItem
import net.diavicecat.scaleray.item.custom.ScaleRayItem
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.SimpleContainer
import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.entity.player.Player
import net.minecraft.world.inventory.AbstractContainerMenu
import net.minecraft.world.inventory.Slot
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.component.ItemContainerContents

class ScaleRayMenu(containerId: Int, private val playerInv: Inventory, val rayStack: ItemStack) :
    AbstractContainerMenu(ModMenuTypes.SCALE_RAY_MENU.get(), containerId) {

    val cellContainer = SimpleContainer(1).apply {
        val existing = rayStack.get(ModDataComponents.INSERTED_CELL.get())
        if (existing != null) {
            val stack = existing.getStackInSlot(0)
            if (!stack.isEmpty) setItem(0, stack.copy())
        }
    }

    val cellSlot: Slot

    init {
        cellSlot = addSlot(object : Slot(cellContainer, 0, CELL_SLOT_X, CELL_SLOT_Y) {
            override fun mayPlace(stack: ItemStack) = stack.item is PowerCellItem
            override fun getMaxStackSize() = 1
        })

        for (row in 0..2) {
            for (col in 0..8) {
                addSlot(Slot(playerInv, col + row * 9 + 9, INV_X + col * 18, INV_Y + row * 18))
            }
        }
        for (col in 0..8) {
            addSlot(Slot(playerInv, col, INV_X + col * 18, INV_Y + 58))
        }
    }

    override fun removed(player: Player) {
        super.removed(player)
        val cell = cellContainer.getItem(0)
        if (cell.isEmpty) {
            rayStack.remove(ModDataComponents.INSERTED_CELL.get())
        } else {
            rayStack.set(ModDataComponents.INSERTED_CELL.get(), ItemContainerContents.fromItems(listOf(cell.copy())))
        }
        if (player is ServerPlayer) {
            player.inventoryMenu.broadcastChanges()
        }
    }

    override fun quickMoveStack(player: Player, index: Int): ItemStack {
        val slot = slots.getOrNull(index) ?: return ItemStack.EMPTY
        if (!slot.hasItem()) return ItemStack.EMPTY
        val stack = slot.item
        val original = stack.copy()

        if (index == 0) {
            if (!moveItemStackTo(stack, 1, slots.size, true)) return ItemStack.EMPTY
        } else if (stack.item is PowerCellItem) {
            if (!moveItemStackTo(stack, 0, 1, false)) return ItemStack.EMPTY
        } else {
            return ItemStack.EMPTY
        }

        if (stack.isEmpty) slot.set(ItemStack.EMPTY) else slot.setChanged()
        return original
    }

    override fun stillValid(player: Player) =
        player.mainHandItem === rayStack || player.offhandItem === rayStack

    companion object {
        // Cell slot is in the sidebar docked to the right of the controls panel.
        // Sidebar: x=220, width=32, sideRelY=32 → slot at (sideX+7, sideY+14) → (227, 46)
        const val CELL_SLOT_X = 227
        const val CELL_SLOT_Y = 46
        const val INV_X = 29
        const val INV_Y = 175
    }
}
