package net.diavicecat.scaleray.block

import net.diavicecat.scaleray.block.entity.ChargingStationBlockEntity
import com.mojang.serialization.MapCodec
import net.minecraft.core.BlockPos
import net.minecraft.server.level.ServerPlayer
import net.minecraft.stats.Stats
import net.minecraft.world.InteractionResult
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.PickaxeItem
import net.minecraft.world.item.TieredItem
import net.minecraft.world.level.BlockGetter
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.BaseEntityBlock
import net.minecraft.world.level.block.Block
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

    override fun getDestroyProgress(state: BlockState, player: Player, level: BlockGetter, pos: BlockPos): Float {
        val f = state.getDestroySpeed(level, pos)
        if (f == -1.0f) return 0.0f
        val tool = player.mainHandItem
        return if (tool.item is PickaxeItem) {
            val tierSpeed = (tool.item as TieredItem).tier.speed
            tierSpeed / f / 30f
        } else {
            1.0f / f / 100f
        }
    }

    override fun playerDestroy(level: Level, player: Player, pos: BlockPos, state: BlockState, blockEntity: BlockEntity?, tool: ItemStack) {
        player.awardStat(Stats.BLOCK_MINED.get(this))
        player.causeFoodExhaustion(0.005f)
        if (!level.isClientSide) {
            popResource(level, pos, ItemStack(this))
        }
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
