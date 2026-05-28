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
import net.neoforged.neoforge.items.IItemHandler

class ChargingStationBlockEntity(pos: BlockPos, state: BlockState) :
    BlockEntity(ModBlockEntities.CHARGING_STATION.get(), pos, state), MenuProvider {

    val container = SimpleContainer(6) // slot 0: powercell input, slot 1: emerald input, slots 2-4: speed upgrades, slot 5: powercell output
    var powerStored = 0
    private var tickCounter = 0
    private var effectiveInterval = CHARGE_INTERVAL

    val containerData = object : ContainerData {
        override fun get(index: Int) = when (index) {
            0 -> powerStored
            1 -> MAX_POWER
            2 -> tickCounter
            3 -> effectiveInterval
            else -> 0
        }
        override fun set(index: Int, value: Int) {
            if (index == 0) powerStored = value
        }
        override fun getCount() = 4
    }

    // Virtual slot mapping for hoppers:
    //   virtual 0 → container 0: battery input  (insert only)
    //   virtual 1 → container 1: emerald input  (insert only)
    //   virtual 2 → container 5: battery output (extract only)
    val itemHandler: IItemHandler = object : IItemHandler {
        private fun containerSlot(slot: Int) = if (slot == 2) 5 else slot
        override fun getSlots() = 3
        override fun getStackInSlot(slot: Int): ItemStack = container.getItem(containerSlot(slot))
        override fun isItemValid(slot: Int, stack: ItemStack) = when (slot) {
            0 -> stack.item is PowerCellItem
            1 -> stack.item == Items.EMERALD
            else -> false  // output slot: no insertion
        }
        override fun getSlotLimit(slot: Int) = if (slot == 0) 1 else 64
        override fun insertItem(slot: Int, stack: ItemStack, simulate: Boolean): ItemStack {
            if (slot == 2 || !isItemValid(slot, stack)) return stack
            val cSlot = containerSlot(slot)
            val limit = getSlotLimit(slot)
            val existing = container.getItem(cSlot)
            val space = if (existing.isEmpty) limit
                        else if (ItemStack.isSameItemSameComponents(existing, stack)) limit - existing.count
                        else 0
            if (space <= 0) return stack
            val add = minOf(stack.count, space)
            if (!simulate) {
                if (existing.isEmpty) container.setItem(cSlot, stack.copyWithCount(add))
                else existing.grow(add)
                setChanged()
            }
            val remainder = stack.count - add
            return if (remainder <= 0) ItemStack.EMPTY else stack.copyWithCount(remainder)
        }
        override fun extractItem(slot: Int, amount: Int, simulate: Boolean): ItemStack {
            if (slot != 2) return ItemStack.EMPTY  // only extract from output slot
            val stack = container.getItem(5)
            if (stack.isEmpty) return ItemStack.EMPTY
            val extract = minOf(amount, stack.count)
            val out = stack.copyWithCount(extract)
            if (!simulate) {
                stack.shrink(extract)
                if (stack.isEmpty) container.setItem(5, ItemStack.EMPTY)
                setChanged()
            }
            return out
        }
    }

    override fun getDisplayName(): Component = Component.translatable("block.scalerays.chargingstation")

    override fun createMenu(id: Int, inv: Inventory, player: Player): AbstractContainerMenu =
        ChargingStationMenu(id, inv, container, blockPos, containerData)

    fun serverTick() {
        var changed = false
        val upgradeCount = (2..4).count { !container.getItem(it).isEmpty }
        val newInterval = (CHARGE_INTERVAL - upgradeCount * 50).coerceAtLeast(10)
        if (newInterval != effectiveInterval) {
            tickCounter = (tickCounter.toLong() * newInterval / effectiveInterval).toInt()
            effectiveInterval = newInterval
        }

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

        // Auto-move fully charged battery from input slot to output slot
        val cellIn = container.getItem(0)
        val cellInItem = cellIn.item as? PowerCellItem
        if (cellInItem != null && !cellInItem.isCreative) {
            val charges = cellIn.get(ModDataComponents.POWER_CHARGES.get()) ?: 0
            if (charges >= cellInItem.maxCharges && container.getItem(5).isEmpty) {
                container.setItem(5, cellIn.copy())
                container.setItem(0, ItemStack.EMPTY)
                changed = true
            }
        }

        // Reset cycle when no charging can happen (empty slot, creative cell, or fully charged battery)
        val cellForCheck = container.getItem(0)
        val cellItemForCheck = cellForCheck.item as? PowerCellItem
        val batteryFull = cellItemForCheck != null &&
            (cellItemForCheck.isCreative || (cellForCheck.get(ModDataComponents.POWER_CHARGES.get()) ?: 0) >= cellItemForCheck.maxCharges)
        if (cellForCheck.isEmpty || batteryFull) {
            if (tickCounter != 0) { tickCounter = 0; changed = true }
        }

        // Charge battery every effectiveInterval ticks (reduced by speed upgrades)
        tickCounter++
        if (tickCounter >= effectiveInterval) {
            tickCounter = 0
            if (powerStored > 0) {
                val cell = container.getItem(0)
                val cellItem = cell.item as? PowerCellItem
                if (!cell.isEmpty && cellItem != null && !cellItem.isCreative) {
                    val current = cell.get(ModDataComponents.POWER_CHARGES.get()) ?: 0
                    if (current < cellItem.maxCharges) {
                        cell.set(ModDataComponents.POWER_CHARGES.get(), (current + 1).coerceAtMost(cellItem.maxCharges))
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
