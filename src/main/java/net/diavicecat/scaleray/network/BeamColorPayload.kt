package net.diavicecat.scaleray.network

import net.diavicecat.scaleray.ScaleRay
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.network.codec.StreamCodec
import net.minecraft.network.protocol.common.custom.CustomPacketPayload
import net.minecraft.resources.ResourceLocation

data class BeamColorPayload(val color: Int) : CustomPacketPayload {
    override fun type(): CustomPacketPayload.Type<BeamColorPayload> = TYPE

    companion object {
        val TYPE = CustomPacketPayload.Type<BeamColorPayload>(
            ResourceLocation.fromNamespaceAndPath(ScaleRay.MOD_ID, "beam_color")
        )
        val STREAM_CODEC: StreamCodec<FriendlyByteBuf, BeamColorPayload> = StreamCodec.of(
            { buf, p -> buf.writeInt(p.color) },
            { buf -> BeamColorPayload(buf.readInt()) }
        )
    }
}
