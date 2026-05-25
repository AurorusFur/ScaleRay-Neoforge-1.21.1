package net.diavicecat.scaleray

import com.mojang.logging.LogUtils
import net.diavicecat.scaleray.block.ModBlocks
import net.diavicecat.scaleray.item.ModItems
import net.diavicecat.scaleray.network.LaserBeamPayload
import net.diavicecat.scaleray.network.ScaleRayPayload
import net.minecraft.server.MinecraftServer
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.sounds.SoundEvents
import net.minecraft.sounds.SoundSource
import net.minecraft.world.entity.Entity
import net.minecraft.world.item.CreativeModeTabs
import net.minecraft.world.phys.Vec3
import net.neoforged.bus.api.IEventBus
import net.neoforged.neoforge.network.PacketDistributor

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
            registrar.playToClient(LaserBeamPayload.TYPE, LaserBeamPayload.STREAM_CODEC) { payload, context ->
                context.enqueueWork {
                    net.diavicecat.scaleray.client.ClientBeamRenderer.addBeam(payload.start, payload.end)
                }
            }
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
            val target = getLookedAtEntity(player, 10.0) ?: return@enqueueWork
            val server = player.server

            applyScale(target, payload.action, server)
            spawnLaserEffects(player, payload.beamStart, payload.beamEnd, player.level() as ServerLevel)
        }
    }

    private fun applyScale(target: Entity, action: ScaleRayPayload.Action, server: MinecraftServer) {
        if (tryPehkuiApi(target, action)) {
            LOGGER.debug("ScaleRay: applied scale via Pehkui API")
            return
        }
        LOGGER.debug("ScaleRay: Pehkui API unavailable, falling back to command")
        // Pehkui API unavailable — fall back to command executed as server (permission level 4).
        val source = server.createCommandSourceStack()
            .withPermission(4)
            .withEntity(target)
            .withPosition(target.position())
            .withLevel(target.level() as ServerLevel)
        val command = when (action) {
            ScaleRayPayload.Action.SHRINK -> "scale add pehkui:base -0.1 @s"
            ScaleRayPayload.Action.GROW   -> "scale add pehkui:base 0.1 @s"
            ScaleRayPayload.Action.RESET  -> "scale set pehkui:base 1 @s"
        }
        server.commands.performPrefixedCommand(source, command)
    }

    private fun tryPehkuiApi(target: Entity, action: ScaleRayPayload.Action): Boolean {
        return try {
            val scaleTypesClass = Class.forName("virtuoel.pehkui.api.ScaleTypes")
            val baseScaleType   = scaleTypesClass.getField("BASE").get(null)
            val getScaleData    = baseScaleType.javaClass.getMethod("getScaleData", Entity::class.java)
            val scaleData       = getScaleData.invoke(baseScaleType, target)
            val currentScale    = scaleData.javaClass.getMethod("getScale").invoke(scaleData) as Float
            val newScale = when (action) {
                ScaleRayPayload.Action.SHRINK -> (currentScale - 0.1f).coerceAtLeast(0.1f)
                ScaleRayPayload.Action.GROW   -> currentScale + 0.1f
                ScaleRayPayload.Action.RESET  -> 1.0f
            }
            scaleData.javaClass.getMethod("setTargetScale", Float::class.javaPrimitiveType).invoke(scaleData, newScale)
            true
        } catch (_: Exception) {
            false
        }
    }

    private fun getLookedAtEntity(player: ServerPlayer, range: Double): Entity? {
        val eyePos: Vec3 = player.getEyePosition()
        val lookVec: Vec3 = player.lookAngle
        val endPos: Vec3 = eyePos.add(lookVec.scale(range))
        val searchBox = player.boundingBox.expandTowards(lookVec.scale(range)).inflate(1.0)

        var closestEntity: Entity? = null
        var closestDistSq = range * range

        for (entity in player.level().getEntities(player, searchBox)) {
            if (entity.isSpectator) continue
            val hit = entity.boundingBox.inflate(entity.pickRadius.toDouble()).clip(eyePos, endPos)
            if (hit.isPresent) {
                val distSq = eyePos.distanceToSqr(hit.get())
                if (distSq < closestDistSq) {
                    closestDistSq = distSq
                    closestEntity = entity
                }
            }
        }

        return closestEntity
    }

    private fun spawnLaserEffects(player: ServerPlayer, beamStart: Vec3, beamEnd: Vec3, level: ServerLevel) {
        val beamPayload = LaserBeamPayload(beamStart, beamEnd)

        for (nearby in level.players()) {
            if (nearby.position().distanceTo(player.position()) <= 64.0) {
                PacketDistributor.sendToPlayer(nearby as ServerPlayer, beamPayload)
            }
        }

        level.playSound(null, player.x, player.y, player.z, SoundEvents.EVOKER_CAST_SPELL, SoundSource.PLAYERS, 1.0f, 1.8f)
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