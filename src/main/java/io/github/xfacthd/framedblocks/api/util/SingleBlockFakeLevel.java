package io.github.xfacthd.framedblocks.api.util;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.ColorResolver;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.lighting.LevelLightEngine;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.model.data.ModelData;
import org.jspecify.annotations.Nullable;

public record SingleBlockFakeLevel(Level realLevel, BlockPos realPos, BlockPos fakePos, BlockState state, @Nullable BlockEntity blockEntity, ModelData modelData) implements BlockAndTintGetter
{
    public SingleBlockFakeLevel(Level realLevel, BlockPos realPos, BlockState state, @Nullable BlockEntity blockEntity, ModelData modelData)
    {
        this(realLevel, realPos, BlockPos.ZERO, state, blockEntity, modelData);
    }

    @Override
    public float getShade(Direction side, boolean shade)
    {
        return realLevel.getShade(side, shade);
    }

    @Override
    public float getShade(float normalX, float normalY, float normalZ, boolean shade)
    {
        return realLevel.getShade(normalX, normalY, normalZ, shade);
    }

    @Override
    public LevelLightEngine getLightEngine()
    {
        return realLevel.getLightEngine();
    }

    @Override
    public int getBrightness(LightLayer layer, BlockPos pos)
    {
        return 15;
    }

    @Override
    public int getBlockTint(BlockPos pos, ColorResolver resolver)
    {
        return realLevel.getBlockTint(realPos, resolver);
    }

    @Nullable
    @Override
    public BlockEntity getBlockEntity(BlockPos pos)
    {
        if (pos.equals(fakePos))
        {
            return blockEntity;
        }
        return null;
    }

    @Override
    public BlockState getBlockState(BlockPos pos)
    {
        if (pos.equals(fakePos))
        {
            return state;
        }
        return Blocks.AIR.defaultBlockState();
    }

    @Override
    public FluidState getFluidState(BlockPos pos)
    {
        return Fluids.EMPTY.defaultFluidState();
    }

    @Override
    public ModelData getModelData(BlockPos pos)
    {
        if (pos.equals(fakePos))
        {
            return modelData;
        }
        return ModelData.EMPTY;
    }

    @Override
    public int getHeight()
    {
        return realLevel.getHeight();
    }

    @Override
    public int getMinY()
    {
        return realLevel.getMinY();
    }
}
