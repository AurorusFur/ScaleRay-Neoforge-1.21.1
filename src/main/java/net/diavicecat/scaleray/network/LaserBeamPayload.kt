package net.diavicecat.scaleray.network

import net.diavicecat.scaleray.ScaleRay
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.network.codec.StreamCodec
import net.minecraft.network.protocol.common.custom.CustomPacketPayload
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.phys.Vec3

data class LaserBeamPayload(val start: Vec3, val end: Vec3) : CustomPacketPayload {
    override fun type(): CustomPacketPayload.Type<LaserBeamPayload> = TYPE

    companion object {
        val TYPE = CustomPacketPayload.Type<LaserBeamPayload>(
            ResourceLocation.fromNamespaceAndPath(ScaleRay.MOD_ID, "laser_beam")
        )
        val STREAM_CODEC: StreamCodec<FriendlyByteBuf, LaserBeamPayload> = StreamCodec.of(
            { buf, p ->
                buf.writeDouble(p.start.x); buf.writeDouble(p.start.y); buf.writeDouble(p.start.z)
                buf.writeDouble(p.end.x); buf.writeDouble(p.end.y); buf.writeDouble(p.end.z)
            },
            { buf -> LaserBeamPayload(
                Vec3(buf.readDouble(), buf.readDouble(), buf.readDouble()),
                Vec3(buf.readDouble(), buf.readDouble(), buf.readDouble())
            )}
        )
    }
}
