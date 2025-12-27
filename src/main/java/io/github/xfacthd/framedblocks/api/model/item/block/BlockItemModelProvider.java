package io.github.xfacthd.framedblocks.api.model.item.block;

import io.github.xfacthd.framedblocks.api.internal.InternalClientAPI;
import io.github.xfacthd.framedblocks.api.model.wrapping.GeometryFactory;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.model.BlockStateModel;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.world.level.block.state.BlockState;

import java.util.function.Supplier;

public interface BlockItemModelProvider
{
    BlockItemModelProvider DEFAULT = (state, _) -> () -> Minecraft.getInstance().getBlockRenderer().getBlockModel(state);

    Supplier<BlockStateModel> create(BlockState state, ModelBaker baker);

    static Supplier<BlockStateModel> forGeometry(BlockState state, BlockState srcState, GeometryFactory geometry, ModelBaker baker)
    {
        return InternalClientAPI.INSTANCE.createBlockItemModelProviderForGeometry(state, srcState, geometry, baker);
    }
}
