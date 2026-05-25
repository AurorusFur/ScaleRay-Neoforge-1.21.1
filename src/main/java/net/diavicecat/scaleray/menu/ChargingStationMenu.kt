package net.diavicecat.scaleray.menu

import net.diavicecat.scaleray.block.entity.ChargingStationBlockEntity
import net.diavicecat.scaleray.item.custom.PowerCellItem
import net.minecraft.core.BlockPos
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.world.Container
import net.minecraft.world.SimpleContainer
import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.entity.player.Player
import net.minecraft.world.inventory.AbstractContainerMenu
import net.minecraft.world.inventory.ContainerData
import net.minecraft.world.inventory.SimpleContainerData
import net.minecraft.world.inventory.Slot
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items

class ChargingStationMenu(
    containerId: Int,
    private val playerInv: Inventory,
    private val stationContainer: Container,
    val blockPos: BlockPos,
    private val containerData: ContainerData = SimpleContainerData(3)
) : AbstractContainerMenu(ModMenuTypes.CHARGING_STATION_MENU.get(), containerId) {

    val powerStored: Int    get() = containerData.get(0)
    val maxPower: Int       get() = containerData.get(1)
    val chargeProgress: Int get() = containerData.get(2)

    init {
        addSlot(object : Slot(stationContainer, 0, CELL_SLOT_X, CELL_SLOT_Y) {
            override fun mayPlace(stack: ItemStack) = stack.item is PowerCellItem
            override fun getMaxStackSize() = 1
        })
        addSlot(object : Slot(stationContainer, 1, EMERALD_SLOT_X, EMERALD_SLOT_Y) {
            override fun mayPlace(stack: ItemStack) = stack.item == Items.EMERALD
        })

        for (row in 0..2) {
            for (col in 0..8) {
                addSlot(Slot(playerInv, col + row * 9 + 9, INV_X + col * 18, INV_Y + row * 18))
            }
        }
        for (col in 0..8) {
            addSlot(Slot(playerInv, col, INV_X + col * 18, INV_Y + 58))
        }

        addDataSlots(containerData)
    }

    override fun quickMoveStack(player: Player, index: Int): ItemStack {
        val slot = slots.getOrNull(index) ?: return ItemStack.EMPTY
        if (!slot.hasItem()) return ItemStack.EMPTY
        val stack = slot.item
        val original = stack.copy()

        when {
            index == 0 -> if (!moveItemStackTo(stack, 2, slots.size, true)) return ItemStack.EMPTY
            index == 1 -> if (!moveItemStackTo(stack, 2, slots.size, true)) return ItemStack.EMPTY
            stack.item is PowerCellItem -> if (!moveItemStackTo(stack, 0, 1, false)) return ItemStack.EMPTY
            stack.item == Items.EMERALD -> if (!moveItemStackTo(stack, 1, 2, false)) return ItemStack.EMPTY
            else -> return ItemStack.EMPTY
        }

        if (stack.isEmpty) slot.set(ItemStack.EMPTY) else slot.setChanged()
        return original
    }

    override fun stillValid(player: Player): Boolean {
        if (player.level().getBlockEntity(blockPos) !is ChargingStationBlockEntity) return false
        return player.distanceToSqr(blockPos.x + 0.5, blockPos.y + 0.5, blockPos.z + 0.5) <= 64.0
    }

    companion object {
        const val CELL_SLOT_X    = 53
        const val CELL_SLOT_Y    = 30
        const val EMERALD_SLOT_X = 109
        const val EMERALD_SLOT_Y = 30
        const val INV_X          = 8
        const val INV_Y          = 97

        fun fromNetwork(containerId: Int, playerInv: Inventory, buf: FriendlyByteBuf): ChargingStationMenu {
            val pos = buf.readBlockPos()
            val entity = playerInv.player.level().getBlockEntity(pos) as? ChargingStationBlockEntity
                ?: return ChargingStationMenu(containerId, playerInv, SimpleContainer(2), pos)
            // Client uses default SimpleContainerData; server-side createMenu passes entity.containerData
            return ChargingStationMenu(containerId, playerInv, entity.container, pos)
        }
    }
}
