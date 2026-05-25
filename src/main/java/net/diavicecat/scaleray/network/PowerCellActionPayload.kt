package net.diavicecat.scaleray.network

import net.diavicecat.scaleray.ScaleRay
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.network.codec.StreamCodec
import net.minecraft.network.protocol.common.custom.CustomPacketPayload
import net.minecraft.resources.ResourceLocation

data class PowerCellActionPayload(val action: Action) : CustomPacketPayload {

    enum class Action { INSERT, EXTRACT }

    override fun type(): CustomPacketPayload.Type<PowerCellActionPayload> = TYPE

    companion object {
        val TYPE = CustomPacketPayload.Type<PowerCellActionPayload>(
            ResourceLocation.fromNamespaceAndPath(ScaleRay.MOD_ID, "power_cell_action")
        )
        val STREAM_CODEC: StreamCodec<FriendlyByteBuf, PowerCellActionPayload> = StreamCodec.of(
            { buf, p -> buf.writeEnum(p.action) },
            { buf -> PowerCellActionPayload(buf.readEnum(Action::class.java)) }
        )
    }
}
