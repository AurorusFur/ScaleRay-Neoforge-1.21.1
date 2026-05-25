package net.diavicecat.scaleray.item

import net.diavicecat.scaleray.ScaleRay
import net.diavicecat.scaleray.block.ModBlocks
import net.minecraft.core.registries.Registries
import net.minecraft.network.chat.Component
import net.minecraft.world.item.CreativeModeTab
import net.minecraft.world.item.ItemStack
import net.neoforged.bus.api.IEventBus
import net.neoforged.neoforge.registries.DeferredRegister
import java.util.function.Supplier

object ModCreativeTab {
    val CREATIVE_MODE_TABS: DeferredRegister<CreativeModeTab> =
        DeferredRegister.create(Registries.CREATIVE_MODE_TAB, ScaleRay.MOD_ID)

    val SCALERAY_TAB = CREATIVE_MODE_TABS.register("main", Supplier {
        CreativeModeTab.builder()
            .icon { ItemStack(ModItems.SCALERAY.get()) }
            .title(Component.translatable("itemGroup.scalerays.main"))
            .displayItems { _, output ->
                output.accept(ModItems.SCALERAY.get())
                output.accept(ModItems.POWERCELL.get())
                output.accept(ModItems.SCALETECHCASING.get())
                output.accept(ModBlocks.CHARGINGSTATION.get())
                output.accept(ModBlocks.SCALINGCORE.get())
            }
            .build()
    })

    fun register(eventBus: IEventBus) {
        CREATIVE_MODE_TABS.register(eventBus)
    }
}
