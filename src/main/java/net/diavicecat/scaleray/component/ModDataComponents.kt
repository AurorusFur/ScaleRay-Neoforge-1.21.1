package net.diavicecat.scaleray.component

import com.mojang.serialization.Codec
import net.diavicecat.scaleray.ScaleRay
import net.minecraft.core.component.DataComponentType
import net.minecraft.core.registries.Registries
import net.minecraft.network.codec.ByteBufCodecs
import net.minecraft.network.codec.StreamCodec
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.world.item.component.ItemContainerContents
import net.neoforged.bus.api.IEventBus
import net.neoforged.neoforge.registries.DeferredHolder
import net.neoforged.neoforge.registries.DeferredRegister
import java.util.function.Supplier

object ModDataComponents {
    private val REGISTRY: DeferredRegister<DataComponentType<*>> =
        DeferredRegister.create(Registries.DATA_COMPONENT_TYPE, ScaleRay.MOD_ID)

    val POWER_CHARGES: DeferredHolder<DataComponentType<*>, DataComponentType<Int>> =
        REGISTRY.register("power_charges", Supplier {
            DataComponentType.builder<Int>()
                .persistent(Codec.INT)
                .networkSynchronized(ByteBufCodecs.VAR_INT)
                .build()
        })

    // ItemContainerContents implements equals/hashCode, unlike raw ItemStack
    val INSERTED_CELL: DeferredHolder<DataComponentType<*>, DataComponentType<ItemContainerContents>> =
        REGISTRY.register("inserted_cell", Supplier {
            DataComponentType.builder<ItemContainerContents>()
                .persistent(ItemContainerContents.CODEC)
                .networkSynchronized(ItemContainerContents.STREAM_CODEC)
                .build()
        })

    val BEAM_COLOR: DeferredHolder<DataComponentType<*>, DataComponentType<Int>> =
        REGISTRY.register("beam_color", Supplier {
            DataComponentType.builder<Int>()
                .persistent(Codec.INT)
                .networkSynchronized(ByteBufCodecs.INT)
                .build()
        })

    fun register(bus: IEventBus) = REGISTRY.register(bus)
}
