package io.github.xfacthd.framedblocks.common.item.block;

import io.github.xfacthd.framedblocks.common.FBContent;
import io.github.xfacthd.framedblocks.common.block.sign.AbstractFramedSignBlock;
import io.github.xfacthd.framedblocks.common.blockentity.special.FramedSignBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

public class FramedSignItem extends FramedStandingAndWallBlockItem
{
    public FramedSignItem(Properties props)
    {
        this(FBContent.BLOCK_FRAMED_SIGN, FBContent.BLOCK_FRAMED_WALL_SIGN, Direction.DOWN, props);
    }

    protected FramedSignItem(Holder<Block> standing, Holder<Block> wall, Direction attachFace, Properties props)
    {
        super(standing.value(), wall.value(), attachFace, props);
    }

    @Override
    protected boolean updateCustomBlockEntityTag(
            BlockPos pos, Level level, @Nullable Player player, ItemStack stack, BlockState state
    )
    {
        boolean hadNBT = super.updateCustomBlockEntityTag(pos, level, player, stack, state);
        if (!level.isClientSide() && !hadNBT && player != null)
        {
            if (level.getBlockEntity(pos) instanceof FramedSignBlockEntity be)
            {
                AbstractFramedSignBlock.openEditScreen(player, be, true);
            }
        }
        return hadNBT;
    }
}
