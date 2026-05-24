package net.diavicecat.scaleray

import com.mojang.logging.LogUtils
import net.diavicecat.scaleray.block.ModBlocks
import net.diavicecat.scaleray.item.ModItems
import net.diavicecat.scaleray.network.ScaleRayPayload
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.item.CreativeModeTabs
import net.neoforged.bus.api.IEventBus
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.fml.ModContainer
import net.neoforged.fml.common.EventBusSubscriber
import net.neoforged.fml.common.Mod
import net.neoforged.fml.config.ModConfig
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent
import net.neoforged.neoforge.common.NeoForge
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent
import net.neoforged.neoforge.event.server.ServerStartingEvent
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent
import net.neoforged.neoforge.network.handling.IPayloadContext
import org.slf4j.Logger
import java.util.function.Consumer

// The value here should match an entry in the META-INF/neoforge.mods.toml file
@Mod(ScaleRay.Companion.MOD_ID)
class ScaleRay(modEventBus: IEventBus, modContainer: ModContainer) {
    // The constructor for the mod class is the first code that is run when your mod is loaded.
    // FML will recognize some parameter types like IEventBus or ModContainer and pass them in automatically.
    init {
        // Register the commonSetup method for modloading
        modEventBus.addListener<FMLCommonSetupEvent?>(Consumer { event: FMLCommonSetupEvent? -> this.commonSetup(event) })
        modEventBus.addListener<RegisterPayloadHandlersEvent> { event ->
            val registrar = event.registrar(MOD_ID)
            registrar.playToServer(
                ScaleRayPayload.TYPE,
                ScaleRayPayload.STREAM_CODEC,
                ::handleScaleRayPayload
            )
        }

        // Register ourselves for server and other game events we are interested in.
        // Note that this is necessary if and only if we want *this* class (ExampleMod) to respond directly to events.
        // Do not add this line if there are no @SubscribeEvent-annotated functions in this class, like onServerStarting() below.
        NeoForge.EVENT_BUS.register(this)

        ModItems.register(modEventBus)
        ModBlocks.register(modEventBus)

        // Register the item to a creative tab
        modEventBus.addListener<BuildCreativeModeTabContentsEvent?>(Consumer { event: BuildCreativeModeTabContentsEvent? ->
            this.addCreative(
                event!!
            )
        })

        // Register our mod's ModConfigSpec so that FML can create and load the config file for us
        modContainer.registerConfig(ModConfig.Type.COMMON, Config.SPEC)
    }

    private fun commonSetup(event: FMLCommonSetupEvent?) {
    }

    private fun handleScaleRayPayload(payload: ScaleRayPayload, context: IPayloadContext) {
        context.enqueueWork {
            val player = context.player() as? ServerPlayer ?: return@enqueueWork
            val server = player.server
            val source = player.createCommandSourceStack()
            val command = when (payload.action) {
                ScaleRayPayload.Action.SHRINK -> "scale add pehkui:base -0.1 @s"
                ScaleRayPayload.Action.GROW -> "scale add pehkui:base 0.1 @s"
                ScaleRayPayload.Action.RESET -> "scale set pehkui:base 1 @s"
            }
            server.commands.performPrefixedCommand(source, command)
        }
    }

    // Add the example block item to the building blocks tab
    private fun addCreative(event: BuildCreativeModeTabContentsEvent) {
        if (event.getTabKey() === CreativeModeTabs.INGREDIENTS) {
            event.accept(ModItems.SCALETECHCASING)
            event.accept(ModBlocks.SCALINGCORE)
        }
        if (event.getTabKey() === CreativeModeTabs.TOOLS_AND_UTILITIES) {
            event.accept(ModItems.SCALERAY)
        }
    }

    // You can use SubscribeEvent and let the Event Bus discover methods to call
    @SubscribeEvent
    fun onServerStarting(event: ServerStartingEvent?) {
    }

    companion object {
        const val MOD_ID: String = "scalerays"
        private val LOGGER: Logger = LogUtils.getLogger()
    }
}