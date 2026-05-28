package net.diavicecat.scaleray.item

import net.diavicecat.scaleray.ScaleRay
import net.diavicecat.scaleray.item.custom.AdvancedScaleRayItem
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
    val ADVANCED_SCALERAY: DeferredItem<Item?> = ITEMS.register<Item?>(
        "advanced_scaleray",
        Supplier { AdvancedScaleRayItem(Item.Properties()) })

    @JvmField
    val SCALETECHCASING: DeferredItem<Item?> = ITEMS.register<Item?>(
        "scaletechcasing",
        Supplier { Item(Item.Properties()) })

    @JvmField
    val POWERCELL: DeferredItem<Item?> = ITEMS.register<Item?>(
        "powercell",
        Supplier { PowerCellItem(Item.Properties().stacksTo(1).component(net.diavicecat.scaleray.component.ModDataComponents.POWER_CHARGES.get(), 0), 10) })

    @JvmField
    val UPGRADED_POWERCELL: DeferredItem<Item?> = ITEMS.register<Item?>(
        "upgraded_powercell",
        Supplier { PowerCellItem(Item.Properties().stacksTo(1).component(net.diavicecat.scaleray.component.ModDataComponents.POWER_CHARGES.get(), 0), 25) })

    @JvmField
    val ADVANCED_POWERCELL: DeferredItem<Item?> = ITEMS.register<Item?>(
        "advanced_powercell",
        Supplier { PowerCellItem(Item.Properties().stacksTo(1).component(net.diavicecat.scaleray.component.ModDataComponents.POWER_CHARGES.get(), 0), 50) })

    @JvmField
    val SUPER_POWERCELL: DeferredItem<Item?> = ITEMS.register<Item?>(
        "super_powercell",
        Supplier { PowerCellItem(Item.Properties().stacksTo(1).component(net.diavicecat.scaleray.component.ModDataComponents.POWER_CHARGES.get(), 0), 100) })

    @JvmField
    val ULTIMATE_POWERCELL: DeferredItem<Item?> = ITEMS.register<Item?>(
        "ultimate_powercell",
        Supplier { PowerCellItem(Item.Properties().stacksTo(1).component(net.diavicecat.scaleray.component.ModDataComponents.POWER_CHARGES.get(), 0), 200) })

    @JvmField
    val CREATIVE_POWERCELL: DeferredItem<Item?> = ITEMS.register<Item?>(
        "creative_powercell",
        Supplier { PowerCellItem(Item.Properties().stacksTo(1), Int.MAX_VALUE) })

    @JvmField
    val SPEED_UPGRADE: DeferredItem<Item?> = ITEMS.register<Item?>(
        "speed_upgrade",
        Supplier { Item(Item.Properties().stacksTo(1)) })

    @JvmField
    val ADVANCED_SPEED_UPGRADE: DeferredItem<Item?> = ITEMS.register<Item?>(
        "advanced_speed_upgrade",
        Supplier { Item(Item.Properties().stacksTo(1)) })

    @JvmField
    val WIRELESS_CHARGE_UPGRADE: DeferredItem<Item?> = ITEMS.register<Item?>(
        "wireless_charge_upgrade",
        Supplier { Item(Item.Properties().stacksTo(1)) })

    @JvmField
    val ADVANCED_WIRELESS_UPGRADE: DeferredItem<Item?> = ITEMS.register<Item?>(
        "advanced_wireless_upgrade",
        Supplier { Item(Item.Properties().stacksTo(1)) })

    @JvmField
    val MODULE_PLATE: DeferredItem<Item?> = ITEMS.register<Item?>(
        "module_plate",
        Supplier { Item(Item.Properties()) })

    @JvmField
    val COLOR_MODULE: DeferredItem<Item?> = ITEMS.register<Item?>(
        "color_module",
        Supplier { Item(Item.Properties().stacksTo(1)) })

    @JvmField
    val EXTENDED_RANGE_MODULE: DeferredItem<Item?> = ITEMS.register<Item?>(
        "extended_range_module",
        Supplier { Item(Item.Properties().stacksTo(1)) })

    @JvmField
    val EXTENDED_POWER_MODULE: DeferredItem<Item?> = ITEMS.register<Item?>(
        "extended_power_module",
        Supplier { Item(Item.Properties().stacksTo(1)) })

    @JvmField
    val SOLAR_CHARGING_MODULE: DeferredItem<Item?> = ITEMS.register<Item?>(
        "solar_charging_module",
        Supplier { Item(Item.Properties().stacksTo(1)) })

    @JvmStatic
    fun register(eventBus: IEventBus) {
        ITEMS.register(eventBus)
    }
}
