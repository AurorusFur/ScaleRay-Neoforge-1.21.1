package net.diavicecat.scaleray.block

import net.diavicecat.scaleray.block.entity.ChargingStationBlockEntity
import com.mojang.serialization.MapCodec
import net.minecraft.core.BlockPos
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.InteractionResult
import net.minecraft.world.entity.player.Player
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.BaseEntityBlock
import net.minecraft.world.level.block.RenderShape
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.entity.BlockEntityTicker
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.phys.BlockHitResult

class ChargingStationBlock(properties: Properties) : BaseEntityBlock(properties) {

    override fun codec(): MapCodec<ChargingStationBlock> = CODEC

    override fun newBlockEntity(pos: BlockPos, state: BlockState): BlockEntity =
        ChargingStationBlockEntity(pos, state)

    override fun getRenderShape(state: BlockState) = RenderShape.MODEL

    override fun <T : BlockEntity?> getTicker(
        level: Level, state: BlockState, type: BlockEntityType<T>
    ): BlockEntityTicker<T>? {
        if (level.isClientSide) return null
        return createTickerHelper(type, ModBlockEntities.CHARGING_STATION.get()!!) { _, _, _, entity ->
            entity.serverTick()
        }
    }

    override fun useWithoutItem(
        state: BlockState, level: Level, pos: BlockPos, player: Player, hit: BlockHitResult
    ): InteractionResult {
        if (level.isClientSide) return InteractionResult.SUCCESS
        val serverPlayer = player as? ServerPlayer ?: return InteractionResult.PASS
        val entity = level.getBlockEntity(pos) as? ChargingStationBlockEntity ?: return InteractionResult.PASS
        serverPlayer.openMenu(entity) { buf -> buf.writeBlockPos(pos) }
        return InteractionResult.CONSUME
    }

    companion object {
        val CODEC: MapCodec<ChargingStationBlock> = simpleCodec(::ChargingStationBlock)
    }

    @Suppress("OVERRIDE_DEPRECATION")
    override fun onRemove(
        state: BlockState, level: Level, pos: BlockPos, newState: BlockState, movedByPiston: Boolean
    ) {
        if (!state.`is`(newState.block)) {
            (level.getBlockEntity(pos) as? ChargingStationBlockEntity)?.dropContents(level, pos)
        }
        super.onRemove(state, level, pos, newState, movedByPiston)
    }
}
