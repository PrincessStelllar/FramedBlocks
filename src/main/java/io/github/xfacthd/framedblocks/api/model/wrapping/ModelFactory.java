package io.github.xfacthd.framedblocks.api.model.wrapping;

import net.minecraft.client.renderer.block.model.BlockStateModel;
import net.minecraft.client.renderer.block.model.SingleVariant;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Map;

public interface ModelFactory
{
    BlockStateModel.UnbakedRoot create(ModelFactory.Context ctx);

    default void reset() { }

    record Context(BlockState state, BlockStateModel.UnbakedRoot baseModel, Map<String, SingleVariant.Unbaked> auxModels) { }
}
