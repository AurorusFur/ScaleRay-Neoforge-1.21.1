package net.diavicecat.scaleray.network

import net.diavicecat.scaleray.ScaleRay
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.network.codec.StreamCodec
import net.minecraft.network.protocol.common.custom.CustomPacketPayload
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.phys.Vec3

data class ScaleRayPayload(
    val action: Action,
    val beamStartX: Double, val beamStartY: Double, val beamStartZ: Double,
    val beamEndX: Double,   val beamEndY: Double,   val beamEndZ: Double
) : CustomPacketPayload {
    enum class Action { SHRINK, GROW, RESET }

    val beamStart get() = Vec3(beamStartX, beamStartY, beamStartZ)
    val beamEnd   get() = Vec3(beamEndX,   beamEndY,   beamEndZ)

    override fun type(): CustomPacketPayload.Type<ScaleRayPayload> = TYPE

    companion object {
        val TYPE = CustomPacketPayload.Type<ScaleRayPayload>(
            ResourceLocation.fromNamespaceAndPath(ScaleRay.MOD_ID, "scale_action")
        )
        val STREAM_CODEC: StreamCodec<FriendlyByteBuf, ScaleRayPayload> = StreamCodec.of(
            { buf, p ->
                buf.writeEnum(p.action)
                buf.writeDouble(p.beamStartX); buf.writeDouble(p.beamStartY); buf.writeDouble(p.beamStartZ)
                buf.writeDouble(p.beamEndX);   buf.writeDouble(p.beamEndY);   buf.writeDouble(p.beamEndZ)
            },
            { buf -> ScaleRayPayload(
                buf.readEnum(Action::class.java),
                buf.readDouble(), buf.readDouble(), buf.readDouble(),
                buf.readDouble(), buf.readDouble(), buf.readDouble()
            )}
        )
    }
}
