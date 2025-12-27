package io.github.xfacthd.framedblocks.common.util;

import io.github.xfacthd.framedblocks.api.block.IFramedBlock;
import io.github.xfacthd.framedblocks.api.block.blockentity.FramedBlockEntity;
import io.github.xfacthd.framedblocks.api.util.Utils;
import io.github.xfacthd.framedblocks.client.util.ClientAccess;
import io.github.xfacthd.framedblocks.common.config.ServerConfig;
import io.github.xfacthd.framedblocks.common.crafting.saw.FramingSawRecipeCache;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;

public final class EventHandler
{
    public static void onBlockLeftClick(PlayerInteractEvent.LeftClickBlock event)
    {
        Level level = event.getLevel();
        BlockPos pos = event.getPos();
        BlockState state = level.getBlockState(pos);

        if (state.getBlock() instanceof IFramedBlock block)
        {
            if (block.handleBlockLeftClick(state, level, pos, event.getEntity()))
            {
                event.setCanceled(true);

                if (Utils.CLIENT_DIST && level.isClientSide())
                {
                    ClientAccess.resetDestroyDelay();
                }
            }

            if (ServerConfig.VIEW.enableIntangibility() && !event.isCanceled() && block.getBlockType().allowMakingIntangible())
            {
                if (level.getBlockEntity(pos) instanceof FramedBlockEntity be && be.isIntangible(null))
                {
                    event.setCanceled(true);
                }
            }
        }
    }

    public static void onServerShutdown(@SuppressWarnings("unused") ServerStoppedEvent event)
    {
        FramingSawRecipeCache.get(false).clear();
    }

    private EventHandler() { }
}
