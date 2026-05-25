package net.diavicecat.scaleray.block

import net.diavicecat.scaleray.ScaleRay
import net.diavicecat.scaleray.item.ModItems
import net.minecraft.world.item.BlockItem
import net.minecraft.world.item.Item
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.SoundType
import net.minecraft.world.level.block.state.BlockBehaviour
import net.neoforged.bus.api.IEventBus
import net.neoforged.neoforge.registries.DeferredBlock
import net.neoforged.neoforge.registries.DeferredRegister
import java.util.function.Supplier

object ModBlocks {
    val BLOCKS: DeferredRegister.Blocks = DeferredRegister.createBlocks(ScaleRay.MOD_ID)
    @JvmField
    val CHARGINGSTATION: DeferredBlock<Block?> = registerBlock<Block?>(
        "chargingstation",
        Supplier {
            ChargingStationBlock(
                BlockBehaviour.Properties.of()
                    .strength(2.5f, 6.0f).sound(SoundType.METAL)
            )
        })

    @JvmField
    val SCALINGCORE: DeferredBlock<Block?> = registerBlock<Block?>(
        "scalingcore",
        Supplier {
            Block(
                BlockBehaviour.Properties.of()
                    .strength(4f).sound(SoundType.GLASS)
            )
        })


    private fun <T : Block?> registerBlock(name: String, block: Supplier<T?>): DeferredBlock<T?> {
        val toReturn = BLOCKS.register<T?>(name, block)
        registerBlockItem<T?>(name, toReturn)
        return toReturn
    }

    private fun <T : Block?> registerBlockItem(name: String, block: DeferredBlock<T?>) {
        ModItems.ITEMS.register<BlockItem?>(name, Supplier { BlockItem(block.get(), Item.Properties()) })
    }

    @JvmStatic
    fun register(eventBus: IEventBus) {
        BLOCKS.register(eventBus)
    }
}
