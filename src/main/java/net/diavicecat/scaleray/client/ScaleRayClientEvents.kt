package net.diavicecat.scaleray.client

import net.diavicecat.scaleray.Events.Keybinds
import net.diavicecat.scaleray.ScaleRay
import net.diavicecat.scaleray.item.ModItems
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
        if (event.itemStack.item != ModItems.SCALERAY.get()) return

        // Cancelling the event prevents the vanilla use() call and the arm swing.
        event.isCanceled = true

        val mc = Minecraft.getInstance()
        val player = mc.player ?: return

        // Hand position: offset from eye toward the player's right side and slightly forward/down.
        val forward = player.lookAngle
        val up = Vec3(0.0, 1.0, 0.0)
        val right = forward.cross(up).normalize()
        val mainArmSign = if (player.mainArm.name == "RIGHT") 1.0 else -1.0
        val handPos = player.getEyePosition()
            .add(right.scale(0.4 * mainArmSign))
            .add(forward.scale(0.5))
            .subtract(0.0, 0.25, 0.0)

        // Crosshair hit position — where the player is actually pointing.
        val crosshairPos = mc.hitResult?.location
            ?: player.getEyePosition().add(forward.scale(50.0))

        val action = when {
            Keybinds.ShrinkReset.isDown -> ScaleRayPayload.Action.RESET
            Keybinds.ShrinkMode.isDown  -> ScaleRayPayload.Action.GROW
            else                        -> ScaleRayPayload.Action.SHRINK
        }

        PacketDistributor.sendToServer(ScaleRayPayload(
            action,
            handPos.x, handPos.y, handPos.z,
            crosshairPos.x, crosshairPos.y, crosshairPos.z
        ))
    }
}
