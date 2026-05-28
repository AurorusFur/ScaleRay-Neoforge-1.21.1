package net.diavicecat.scaleray.client

import net.diavicecat.scaleray.ScaleRay
import net.diavicecat.scaleray.component.ModDataComponents
import net.diavicecat.scaleray.item.ModItems
import net.diavicecat.scaleray.item.custom.ScaleRayItem
import net.diavicecat.scaleray.network.LaserBeamPayload
import net.diavicecat.scaleray.network.ScaleRayPayload
import net.minecraft.client.Minecraft
import net.minecraft.world.phys.Vec3
import net.neoforged.api.distmarker.Dist
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.fml.common.EventBusSubscriber
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent
import net.neoforged.neoforge.network.PacketDistributor

@EventBusSubscriber(modid = ScaleRay.MOD_ID, bus = EventBusSubscriber.Bus.GAME, value = [Dist.CLIENT])
object ScaleRayClientEvents {

    @JvmStatic
    @SubscribeEvent
    fun onRightClick(event: PlayerInteractEvent.RightClickItem) {
        if (event.itemStack.item !is ScaleRayItem) return

        val mc = Minecraft.getInstance()
        val player = mc.player ?: return

        // Sneak + right-click → let server handle via ScaleRayItem.use() (opens inventory GUI)
        if (player.isShiftKeyDown) return

        // Normal right-click → cancel default and fire the ray
        event.isCanceled = true
        val forward = player.lookAngle
        val up = Vec3(0.0, 1.0, 0.0)
        val right = forward.cross(up).normalize()
        val mainArmSign = if (player.mainArm.name == "RIGHT") 1.0 else -1.0
        val handPos = player.getEyePosition()
            .add(right.scale(0.4 * mainArmSign))
            .add(forward.scale(0.5))
            .subtract(0.0, 0.25, 0.0)

        val crosshairPos = mc.hitResult?.location
            ?: player.getEyePosition().add(forward.scale(50.0))

        PacketDistributor.sendToServer(ScaleRayPayload(
            ScaleRayConfig.mode,
            ScaleRayConfig.target,
            ScaleRayConfig.power,
            handPos.x, handPos.y, handPos.z,
            crosshairPos.x, crosshairPos.y, crosshairPos.z
        ))
    }
}
