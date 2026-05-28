package net.diavicecat.scaleray.client

import net.diavicecat.scaleray.ScaleRay
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.RenderType
import net.minecraft.client.renderer.blockentity.BeaconRenderer
import net.minecraft.world.phys.Vec3
import net.neoforged.api.distmarker.Dist
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.fml.common.EventBusSubscriber
import net.neoforged.neoforge.client.event.RenderLevelStageEvent
import org.joml.Quaternionf
import org.joml.Vector3f

@EventBusSubscriber(modid = ScaleRay.MOD_ID, bus = EventBusSubscriber.Bus.GAME, value = [Dist.CLIENT])
object ClientBeamRenderer {

    private data class ActiveBeam(val start: Vec3, val end: Vec3, val color: Int, val expiresAt: Long)

    private val activeBeams = mutableListOf<ActiveBeam>()

    fun addBeam(start: Vec3, end: Vec3, color: Int = 0x00E63C) {
        synchronized(activeBeams) {
            activeBeams.add(ActiveBeam(start, end, color, System.currentTimeMillis() + 500))
        }
    }

    @JvmStatic
    @SubscribeEvent
    fun onRenderLevel(event: RenderLevelStageEvent) {
        if (event.stage != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) return

        val now = System.currentTimeMillis()
        val beams: List<ActiveBeam>
        synchronized(activeBeams) {
            activeBeams.removeIf { it.expiresAt < now }
            beams = activeBeams.toList()
        }
        if (beams.isEmpty()) return

        val mc = Minecraft.getInstance()
        val level = mc.level ?: return
        val camPos = event.camera.position
        val poseStack = event.poseStack
        val bufferSource = mc.renderBuffers().bufferSource()
        val partialTick = event.partialTick.getGameTimeDeltaPartialTick(false)
        val gameTime = level.gameTime
        val up = Vector3f(0f, 1f, 0f)

        for (beam in beams) {
            val beamVec = beam.end.subtract(beam.start)
            val length = beamVec.length()
            if (length < 0.01) continue

            val dir = Vector3f(beamVec.x.toFloat(), beamVec.y.toFloat(), beamVec.z.toFloat()).normalize()

            poseStack.pushPose()
            poseStack.translate(beam.start.x - camPos.x, beam.start.y - camPos.y, beam.start.z - camPos.z)

            val dot = dir.dot(up)
            when {
                dot > 0.9999f -> {}
                dot < -0.9999f -> poseStack.mulPose(com.mojang.math.Axis.XP.rotationDegrees(180f))
                else -> poseStack.mulPose(Quaternionf().rotationTo(up, dir))
            }

            val height = Math.ceil(length).toInt().coerceAtLeast(1)
            BeaconRenderer.renderBeaconBeam(
                poseStack, bufferSource, BeaconRenderer.BEAM_LOCATION,
                partialTick, 1.0f, gameTime,
                0, height,
                beam.color, 0.08f, 0.15f
            )

            poseStack.popPose()
        }

        bufferSource.endBatch(RenderType.beaconBeam(BeaconRenderer.BEAM_LOCATION, false))
        bufferSource.endBatch(RenderType.beaconBeam(BeaconRenderer.BEAM_LOCATION, true))
    }
}
