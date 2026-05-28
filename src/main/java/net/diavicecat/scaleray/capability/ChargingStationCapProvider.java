package net.diavicecat.scaleray.capability;

import net.diavicecat.scaleray.block.entity.ChargingStationBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.IBlockCapabilityProvider;
import net.neoforged.neoforge.items.IItemHandler;
import org.jetbrains.annotations.Nullable;

public class ChargingStationCapProvider implements IBlockCapabilityProvider<IItemHandler, Direction> {
    public static final ChargingStationCapProvider INSTANCE = new ChargingStationCapProvider();

    @Override
    public @Nullable IItemHandler getCapability(Level level, BlockPos pos, BlockState state, @Nullable BlockEntity blockEntity, Direction context) {
        if (blockEntity instanceof ChargingStationBlockEntity be) {
            return be.getItemHandler();
        }
        return null;
    }
}
