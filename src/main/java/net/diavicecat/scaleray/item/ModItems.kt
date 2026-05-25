package net.diavicecat.scaleray.item

import net.diavicecat.scaleray.ScaleRay
import net.diavicecat.scaleray.item.custom.PowerCellItem
import net.diavicecat.scaleray.item.custom.ScaleRayItem
import net.minecraft.world.item.Item
import net.neoforged.bus.api.IEventBus
import net.neoforged.neoforge.registries.DeferredItem
import net.neoforged.neoforge.registries.DeferredRegister
import java.util.function.Supplier

object ModItems {
    val ITEMS: DeferredRegister.Items = DeferredRegister.createItems(ScaleRay.MOD_ID)

    @JvmField
    val SCALERAY: DeferredItem<Item?> = ITEMS.register<Item?>(
        "scaleray",
        Supplier { ScaleRayItem(Item.Properties()) })

    @JvmField
    val SCALETECHCASING: DeferredItem<Item?> = ITEMS.register<Item?>(
        "scaletechcasing",
        Supplier { Item(Item.Properties()) })

    @JvmField
    val POWERCELL: DeferredItem<Item?> = ITEMS.register<Item?>(
        "powercell",
        Supplier { PowerCellItem(Item.Properties().component(net.diavicecat.scaleray.component.ModDataComponents.POWER_CHARGES.get(), 0)) })

    @JvmStatic
    fun register(eventBus: IEventBus) {
        ITEMS.register(eventBus)
    }
}
