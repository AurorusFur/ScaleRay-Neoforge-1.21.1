package net.diavicecat.scaleray

import com.mojang.logging.LogUtils
import net.diavicecat.scaleray.block.ModBlockEntities
import net.diavicecat.scaleray.block.ModBlocks
import net.diavicecat.scaleray.block.entity.ChargingStationBlockEntity
import net.diavicecat.scaleray.client.ChargingStationMenuScreen
import net.diavicecat.scaleray.client.ScaleRayMenuScreen
import net.diavicecat.scaleray.item.ModCreativeTab
import net.diavicecat.scaleray.item.ModItems
import net.diavicecat.scaleray.component.ModDataComponents
import net.diavicecat.scaleray.item.custom.PowerCellItem
import net.diavicecat.scaleray.item.custom.ScaleRayItem
import net.diavicecat.scaleray.menu.ModMenuTypes
import net.diavicecat.scaleray.network.LaserBeamPayload
import net.diavicecat.scaleray.network.PowerCellActionPayload
import net.diavicecat.scaleray.network.ScaleRayPayload
import net.minecraft.server.MinecraftServer
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.sounds.SoundEvents
import net.minecraft.sounds.SoundSource
import net.minecraft.world.InteractionHand
import net.minecraft.world.entity.Entity
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.component.ItemContainerContents
import net.minecraft.world.phys.Vec3
import net.neoforged.bus.api.IEventBus
import net.neoforged.neoforge.network.PacketDistributor

import net.neoforged.api.distmarker.Dist
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.fml.ModContainer
import net.neoforged.fml.common.EventBusSubscriber
import net.neoforged.fml.common.Mod
import net.neoforged.fml.config.ModConfig
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent
import net.neoforged.fml.loading.FMLEnvironment
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent
import net.neoforged.neoforge.capabilities.Capabilities
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent
import net.neoforged.neoforge.common.NeoForge
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
                PowerCellActionPayload.TYPE,
                PowerCellActionPayload.STREAM_CODEC,
                ::handlePowerCellAction
            )
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

        modEventBus.addListener<RegisterCapabilitiesEvent> { event ->
            event.registerBlockEntity(
                Capabilities.ItemHandler.BLOCK,
                ModBlockEntities.CHARGING_STATION.get()
            ) { be, _ -> be.itemHandler }
        }

        ModItems.register(modEventBus)
        ModBlocks.register(modEventBus)
        ModBlockEntities.register(modEventBus)
        ModDataComponents.register(modEventBus)
        ModMenuTypes.register(modEventBus)
        ModCreativeTab.register(modEventBus)

        if (FMLEnvironment.dist == Dist.CLIENT) {
            modEventBus.addListener<RegisterMenuScreensEvent> { event ->
                event.register(ModMenuTypes.SCALE_RAY_MENU.get(), ::ScaleRayMenuScreen)
                event.register(ModMenuTypes.CHARGING_STATION_MENU.get(), ::ChargingStationMenuScreen)
            }
        }


        // Register our mod's ModConfigSpec so that FML can create and load the config file for us
        modContainer.registerConfig(ModConfig.Type.COMMON, Config.SPEC)
    }

    private fun commonSetup(event: FMLCommonSetupEvent?) {
    }

    private fun handleScaleRayPayload(payload: ScaleRayPayload, context: IPayloadContext) {
        context.enqueueWork {
            val player = context.player() as? ServerPlayer ?: return@enqueueWork
            val level  = player.level() as ServerLevel
            val server = player.server

            val target: Entity = when (payload.target) {
                ScaleRayPayload.Target.OBSERVED -> getLookedAtEntity(player, 10.0) ?: return@enqueueWork
                ScaleRayPayload.Target.SELF     -> player
            }

            if (payload.mode == ScaleRayPayload.Mode.SHRINK &&
                !canShrink(target, payload.power)) return@enqueueWork

            if (!consumeCharge(player.mainHandItem)) return@enqueueWork

            applyScale(target, payload.mode, payload.power, server)

            // Only show the laser beam when targeting an observed entity.
            if (payload.target == ScaleRayPayload.Target.OBSERVED) {
                spawnLaserEffects(player, payload.beamStart, payload.beamEnd, level)
            } else {
                level.playSound(null, player.x, player.y, player.z,
                    SoundEvents.EVOKER_CAST_SPELL, SoundSource.PLAYERS, 1.0f, 1.8f)
            }
        }
    }

    private fun applyScale(target: Entity, mode: ScaleRayPayload.Mode, power: Float, server: MinecraftServer) {
        if (tryPehkuiApi(target, mode, power)) {
            LOGGER.debug("ScaleRay: applied scale via Pehkui API")
            return
        }
        LOGGER.debug("ScaleRay: Pehkui API unavailable, falling back to command")
        val source = server.createCommandSourceStack()
            .withPermission(4)
            .withEntity(target)
            .withPosition(target.position())
            .withLevel(target.level() as ServerLevel)
        val command = when (mode) {
            ScaleRayPayload.Mode.SHRINK -> "scale add pehkui:base -$power @s"
            ScaleRayPayload.Mode.GROW   -> "scale add pehkui:base $power @s"
            ScaleRayPayload.Mode.RESET  -> "scale set pehkui:base 1 @s"
        }
        server.commands.performPrefixedCommand(source, command)
    }

    private fun canShrink(target: Entity, power: Float): Boolean {
        return try {
            val scaleTypesClass = Class.forName("virtuoel.pehkui.api.ScaleTypes")
            val baseScaleType   = scaleTypesClass.getField("BASE").get(null)
            val getScaleData    = baseScaleType.javaClass.getMethod("getScaleData", Entity::class.java)
            val scaleData       = getScaleData.invoke(baseScaleType, target)
            val currentScale    = scaleData.javaClass.getMethod("getScale").invoke(scaleData) as Float
            (currentScale - power) >= 0.03f
        } catch (_: Exception) {
            true  // Pehkui unavailable — allow, command fallback has no floor
        }
    }

    private fun tryPehkuiApi(target: Entity, mode: ScaleRayPayload.Mode, power: Float): Boolean {
        return try {
            val scaleTypesClass = Class.forName("virtuoel.pehkui.api.ScaleTypes")
            val baseScaleType   = scaleTypesClass.getField("BASE").get(null)
            val getScaleData    = baseScaleType.javaClass.getMethod("getScaleData", Entity::class.java)
            val scaleData       = getScaleData.invoke(baseScaleType, target)
            val currentScale    = scaleData.javaClass.getMethod("getScale").invoke(scaleData) as Float
            val newScale = when (mode) {
                ScaleRayPayload.Mode.SHRINK -> currentScale - power
                ScaleRayPayload.Mode.GROW   -> currentScale + power
                ScaleRayPayload.Mode.RESET  -> 1.0f
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

    private fun consumeCharge(rayStack: ItemStack): Boolean {
        if (rayStack.item !is ScaleRayItem) return false
        val cellContents = rayStack.get(ModDataComponents.INSERTED_CELL.get()) ?: return false
        val cell = cellContents.getStackInSlot(0)
        if (cell.isEmpty) return false
        val cellItem = cell.item as? PowerCellItem ?: return false
        if (cellItem.isCreative) return true
        val charges = cell.get(ModDataComponents.POWER_CHARGES.get()) ?: 0
        if (charges <= 0) return false
        val newCell = cell.copy()
        newCell.set(ModDataComponents.POWER_CHARGES.get(), charges - 1)
        rayStack.set(ModDataComponents.INSERTED_CELL.get(), ItemContainerContents.fromItems(listOf(newCell)))
        return true
    }

    private fun handlePowerCellAction(payload: PowerCellActionPayload, context: IPayloadContext) {
        context.enqueueWork {
            val player = context.player() as? ServerPlayer ?: return@enqueueWork
            val rayStack = player.mainHandItem
            if (rayStack.item !is ScaleRayItem) return@enqueueWork

            when (payload.action) {
                PowerCellActionPayload.Action.INSERT -> {
                    val offhand = player.offhandItem
                    if (offhand.item !is PowerCellItem) return@enqueueWork
                    if (rayStack.has(ModDataComponents.INSERTED_CELL.get())) return@enqueueWork
                    rayStack.set(ModDataComponents.INSERTED_CELL.get(), ItemContainerContents.fromItems(listOf(offhand.copy())))
                    player.setItemInHand(InteractionHand.OFF_HAND, ItemStack.EMPTY)
                }
                PowerCellActionPayload.Action.EXTRACT -> {
                    val cellContents = rayStack.get(ModDataComponents.INSERTED_CELL.get()) ?: return@enqueueWork
                    val cell = cellContents.getStackInSlot(0)
                    if (cell.isEmpty) return@enqueueWork
                    rayStack.remove(ModDataComponents.INSERTED_CELL.get())
                    if (player.offhandItem.isEmpty) {
                        player.setItemInHand(InteractionHand.OFF_HAND, cell)
                    } else {
                        player.inventory.add(cell)
                    }
                }
            }
            player.inventoryMenu.broadcastChanges()
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