package net.diavicecat.scaleray.block.entity

import net.diavicecat.scaleray.block.ModBlockEntities
import net.diavicecat.scaleray.component.ModDataComponents
import net.diavicecat.scaleray.item.custom.PowerCellItem
import net.diavicecat.scaleray.menu.ChargingStationMenu
import net.minecraft.core.BlockPos
import net.minecraft.core.HolderLookup
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.ListTag
import net.minecraft.nbt.NbtOps
import net.minecraft.network.chat.Component
import net.minecraft.world.Containers
import net.minecraft.world.MenuProvider
import net.minecraft.world.SimpleContainer
import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.entity.player.Player
import net.minecraft.world.inventory.AbstractContainerMenu
import net.minecraft.world.inventory.ContainerData
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.state.BlockState

class ChargingStationBlockEntity(pos: BlockPos, state: BlockState) :
    BlockEntity(ModBlockEntities.CHARGING_STATION.get(), pos, state), MenuProvider {

    val container = SimpleContainer(2) // slot 0: powercell, slot 1: emerald input
    var powerStored = 0
    private var tickCounter = 0

    val containerData = object : ContainerData {
        override fun get(index: Int) = when (index) {
            0 -> powerStored
            1 -> MAX_POWER
            2 -> tickCounter
            else -> 0
        }
        override fun set(index: Int, value: Int) {
            if (index == 0) powerStored = value
        }
        override fun getCount() = 3
    }

    override fun getDisplayName(): Component = Component.translatable("block.scalerays.chargingstation")

    override fun createMenu(id: Int, inv: Inventory, player: Player): AbstractContainerMenu =
        ChargingStationMenu(id, inv, container, blockPos, containerData)

    fun serverTick() {
        var changed = false

        // Convert all available emeralds to power immediately
        val emerald = container.getItem(1)
        if (!emerald.isEmpty && powerStored < MAX_POWER) {
            val canAdd = MAX_POWER - powerStored
            val toConsume = minOf(emerald.count, canAdd / 3)
            emerald.shrink(toConsume)
            if (emerald.isEmpty) container.setItem(1, ItemStack.EMPTY)
            powerStored += toConsume * 3
            changed = true
        }

        // Reset cycle when no charging can happen (empty slot or fully charged battery)
        val cellForCheck = container.getItem(0)
        val batteryFull = cellForCheck.item is PowerCellItem &&
            (cellForCheck.get(ModDataComponents.POWER_CHARGES.get()) ?: 0) >= PowerCellItem.MAX_CHARGES
        if (cellForCheck.isEmpty || batteryFull) {
            if (tickCounter != 0) { tickCounter = 0; changed = true }
        }

        // Charge battery every CHARGE_INTERVAL ticks
        tickCounter++
        if (tickCounter >= CHARGE_INTERVAL) {
            tickCounter = 0
            if (powerStored > 0) {
                val cell = container.getItem(0)
                if (!cell.isEmpty && cell.item is PowerCellItem) {
                    val current = cell.get(ModDataComponents.POWER_CHARGES.get()) ?: 0
                    if (current < PowerCellItem.MAX_CHARGES) {
                        cell.set(ModDataComponents.POWER_CHARGES.get(), (current + 1).coerceAtMost(PowerCellItem.MAX_CHARGES))
                        powerStored--
                        changed = true
                    }
                }
            }
        }

        if (changed) setChanged()
    }

    fun dropContents(level: Level, pos: BlockPos) {
        Containers.dropContents(level, pos, container)
    }

    override fun saveAdditional(tag: CompoundTag, registries: HolderLookup.Provider) {
        super.saveAdditional(tag, registries)
        tag.putInt("Power", powerStored)
        val context = registries.createSerializationContext(NbtOps.INSTANCE)
        val list = ListTag()
        for (i in 0 until container.containerSize) {
            val stack = container.getItem(i)
            if (!stack.isEmpty) {
                val entry = CompoundTag()
                entry.putByte("Slot", i.toByte())
                ItemStack.CODEC.encodeStart(context, stack).result().ifPresent { entry.put("Item", it) }
                list.add(entry)
            }
        }
        tag.put("Items", list)
    }

    override fun loadAdditional(tag: CompoundTag, registries: HolderLookup.Provider) {
        super.loadAdditional(tag, registries)
        powerStored = tag.getInt("Power").coerceIn(0, MAX_POWER)
        val context = registries.createSerializationContext(NbtOps.INSTANCE)
        val list = tag.getList("Items", 10)
        for (i in 0 until list.size) {
            val entry = list.getCompound(i)
            val slot = (entry.getByte("Slot").toInt()) and 0xFF
            if (slot < container.containerSize && entry.contains("Item")) {
                ItemStack.CODEC.parse(context, entry.get("Item")).result().ifPresent {
                    container.setItem(slot, it)
                }
            }
        }
    }

    companion object {
        const val MAX_POWER = 50
        const val CHARGE_INTERVAL = 200 // 10 seconds
    }
}
