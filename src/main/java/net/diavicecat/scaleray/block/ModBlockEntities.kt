package net.diavicecat.scaleray.block

import net.diavicecat.scaleray.ScaleRay
import net.diavicecat.scaleray.block.entity.ChargingStationBlockEntity
import net.minecraft.core.registries.Registries
import net.minecraft.world.level.block.entity.BlockEntityType
import net.neoforged.bus.api.IEventBus
import net.neoforged.neoforge.registries.DeferredHolder
import net.neoforged.neoforge.registries.DeferredRegister
import java.util.function.Supplier

object ModBlockEntities {
    private val REGISTRY: DeferredRegister<BlockEntityType<*>> =
        DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, ScaleRay.MOD_ID)

    val CHARGING_STATION: DeferredHolder<BlockEntityType<*>, BlockEntityType<ChargingStationBlockEntity>> =
        REGISTRY.register("charging_station", Supplier {
            @Suppress("NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")
            BlockEntityType.Builder.of(
                { pos, state -> ChargingStationBlockEntity(pos, state) },
                ModBlocks.CHARGINGSTATION.get()
            ).build(null)
        })

    fun register(bus: IEventBus) = REGISTRY.register(bus)
}
