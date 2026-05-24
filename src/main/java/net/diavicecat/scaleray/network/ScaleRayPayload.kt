package net.diavicecat.scaleray.network

import net.diavicecat.scaleray.ScaleRay
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.network.codec.StreamCodec
import net.minecraft.network.protocol.common.custom.CustomPacketPayload
import net.minecraft.resources.ResourceLocation

data class ScaleRayPayload(val action: Action) : CustomPacketPayload {
    enum class Action { SHRINK, GROW, RESET }

    override fun type(): CustomPacketPayload.Type<ScaleRayPayload> = TYPE

    companion object {
        val TYPE = CustomPacketPayload.Type<ScaleRayPayload>(
            ResourceLocation.fromNamespaceAndPath(ScaleRay.MOD_ID, "scale_action")
        )
        val STREAM_CODEC: StreamCodec<FriendlyByteBuf, ScaleRayPayload> = StreamCodec.of(
            { buf, payload -> buf.writeEnum(payload.action) },
            { buf -> ScaleRayPayload(buf.readEnum(Action::class.java)) }
        )
    }
}
