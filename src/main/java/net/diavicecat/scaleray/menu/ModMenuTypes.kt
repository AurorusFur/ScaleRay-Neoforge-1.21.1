package net.diavicecat.scaleray.menu

import net.diavicecat.scaleray.ScaleRay
import net.minecraft.core.registries.Registries
import net.minecraft.world.inventory.MenuType
import net.neoforged.bus.api.IEventBus
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension
import net.neoforged.neoforge.registries.DeferredHolder
import net.neoforged.neoforge.registries.DeferredRegister
import java.util.function.Supplier

object ModMenuTypes {
    private val REGISTRY: DeferredRegister<MenuType<*>> =
        DeferredRegister.create(Registries.MENU, ScaleRay.MOD_ID)

    val CHARGING_STATION_MENU: DeferredHolder<MenuType<*>, MenuType<ChargingStationMenu>> =
        REGISTRY.register("charging_station_menu", Supplier {
            IMenuTypeExtension.create { windowId, inv, buf ->
                ChargingStationMenu.fromNetwork(windowId, inv, buf)
            }
        })

    val SCALE_RAY_MENU: DeferredHolder<MenuType<*>, MenuType<ScaleRayMenu>> =
        REGISTRY.register("scale_ray_menu", Supplier {
            IMenuTypeExtension.create { windowId, inv, buf ->
                val hand = net.minecraft.world.InteractionHand.entries[buf.readVarInt()]
                ScaleRayMenu(windowId, inv, inv.player.getItemInHand(hand))
            }
        })

    fun register(bus: IEventBus) = REGISTRY.register(bus)
}
