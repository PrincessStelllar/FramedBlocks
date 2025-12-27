package io.github.xfacthd.framedblocks.api.util;

import io.github.xfacthd.framedblocks.api.internal.InternalClientAPI;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.data.AtlasIds;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import org.jspecify.annotations.Nullable;

public final class ClientUtils
{
    @SuppressWarnings("deprecation")
    public static final Identifier BLOCK_ATLAS = TextureAtlas.LOCATION_BLOCKS;
    public static final Identifier DUMMY_TEXTURE = Utils.id("neoforge", "white");

    public static void enqueueClientTask(Runnable task)
    {
        enqueueClientTask(0, task);
    }

    public static void enqueueClientTask(int delay, Runnable task)
    {
        InternalClientAPI.INSTANCE.enqueueClientTask(delay, task);
    }

    public static int getBlockColor(@Nullable BlockAndTintGetter level, @Nullable BlockPos pos, BlockState state, int tintIdx)
    {
        return Minecraft.getInstance().getBlockColors().getColor(state, level, pos, tintIdx);
    }

    public static int getFluidColor(BlockAndTintGetter level, BlockPos pos, FluidState fluid)
    {
        return IClientFluidTypeExtensions.of(fluid).getTintColor(fluid, level, pos);
    }

    public static int getFluidColor(FluidState fluid)
    {
        return IClientFluidTypeExtensions.of(fluid).getTintColor();
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public static boolean isDummyTexture(BakedQuad quad)
    {
        return isTexture(quad, DUMMY_TEXTURE);
    }

    public static boolean isTexture(BakedQuad quad, Identifier texture)
    {
        return quad.sprite().contents().name().equals(texture);
    }

    public static void renderTransparentFakeItem(GuiGraphics graphics, ItemStack stack, int x, int y)
    {
        graphics.renderFakeItem(stack, x, y, 0);
        graphics.fill(x, y, x + 16, y + 16, 0x80888888);
    }

    public static TextureAtlasSprite getBlockSprite(Identifier id)
    {
        return Minecraft.getInstance().getAtlasManager().getAtlasOrThrow(AtlasIds.BLOCKS).getSprite(id);
    }

    private ClientUtils() { }
}
