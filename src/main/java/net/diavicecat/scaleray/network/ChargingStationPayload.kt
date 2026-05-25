package net.diavicecat.scaleray.network

import net.diavicecat.scaleray.ScaleRay
import net.minecraft.core.BlockPos
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.network.codec.StreamCodec
import net.minecraft.network.protocol.common.custom.CustomPacketPayload
import net.minecraft.resources.ResourceLocation

data class ChargingStationPayload(val pos: BlockPos) : CustomPacketPayload {

    override fun type(): CustomPacketPayload.Type<ChargingStationPayload> = TYPE

    companion object {
        val TYPE = CustomPacketPayload.Type<ChargingStationPayload>(
            ResourceLocation.fromNamespaceAndPath(ScaleRay.MOD_ID, "charging_station")
        )
        val STREAM_CODEC: StreamCodec<FriendlyByteBuf, ChargingStationPayload> = StreamCodec.of(
            { buf, p -> buf.writeBlockPos(p.pos) },
            { buf -> ChargingStationPayload(buf.readBlockPos()) }
        )
    }
}
