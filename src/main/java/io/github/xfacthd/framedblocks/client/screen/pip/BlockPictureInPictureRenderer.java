package io.github.xfacthd.framedblocks.client.screen.pip;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import io.github.xfacthd.framedblocks.api.render.RenderUtils;
import io.github.xfacthd.framedblocks.api.util.SingleBlockFakeLevel;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.pip.PictureInPictureRenderer;
import net.minecraft.client.gui.render.state.pip.PictureInPictureRenderState;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.block.model.BlockModelPart;
import net.minecraft.client.renderer.block.model.BlockStateModel;
import net.minecraft.client.renderer.block.model.ItemTransform;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Quaternionfc;
import org.joml.Vector3f;

import javax.annotation.Nullable;
import java.util.List;

public final class BlockPictureInPictureRenderer extends PictureInPictureRenderer<BlockPictureInPictureRenderer.RenderState>
{
    private static final float RENDER_SIZE = 16F;
    private static final ItemTransform DEFAULT_TRANSFORM = new ItemTransform(
            new Vector3f(30, 225, 0), new Vector3f(), new Vector3f(0.625F, 0.625F, 0.625F)
    );
    private static final Quaternionfc LIGHT_FIX_ROT = Axis.YP.rotationDegrees(285);
    private static final RandomSource RANDOM = RandomSource.create();

    public BlockPictureInPictureRenderer(MultiBufferSource.BufferSource bufferSource)
    {
        super(bufferSource);
    }

    @Override
    protected void renderToTexture(RenderState renderState, PoseStack poseStack)
    {
        float scale = renderState.scale;
        BlockState state = renderState.state;
        SingleBlockFakeLevel fakeLevel = renderState.fakeLevel;

        poseStack.scale(RENDER_SIZE * scale, -RENDER_SIZE * scale, -RENDER_SIZE * scale);
        DEFAULT_TRANSFORM.apply(false, poseStack.last());
        poseStack.translate(.5, .5, .5);
        poseStack.last().normal().rotate(LIGHT_FIX_ROT);
        poseStack.translate(-.5, -.5, -.5);

        BlockRenderDispatcher blockRenderer = Minecraft.getInstance().getBlockRenderer();
        BlockStateModel model = blockRenderer.getBlockModel(state);
        List<BlockModelPart> modelParts = model.collectParts(fakeLevel, BlockPos.ZERO, state, RANDOM);
        RANDOM.setSeed(state.getSeed(BlockPos.ZERO));
        blockRenderer.getModelRenderer().tesselateBlock(
                fakeLevel,
                modelParts,
                state,
                BlockPos.ZERO,
                poseStack,
                this::getBuffer,
                false,
                OverlayTexture.NO_OVERLAY
        );
    }

    private VertexConsumer getBuffer(ChunkSectionLayer chunkLayer)
    {
        return bufferSource.getBuffer(RenderUtils.getEntityRenderType(chunkLayer));
    }

    @Override
    protected float getTranslateY(int height, int guiScale)
    {
        return height / 2F;
    }

    @Override
    protected String getTextureLabel()
    {
        return "framedblocks block-in-ui";
    }

    @Override
    public Class<RenderState> getRenderStateClass()
    {
        return RenderState.class;
    }

    public record RenderState(
            BlockState state,
            SingleBlockFakeLevel fakeLevel,
            int x0,
            int y0,
            int x1,
            int y1,
            float scale,
            @Nullable ScreenRectangle scissorArea,
            @Nullable ScreenRectangle bounds
    ) implements PictureInPictureRenderState
    {
        public static RenderState of(
                BlockState state,
                SingleBlockFakeLevel fakeLevel,
                int x0,
                int y0,
                int x1,
                int y1,
                float scale,
                @Nullable ScreenRectangle scissorArea
        )
        {
            ScreenRectangle bounds = PictureInPictureRenderState.getBounds(x0, y0, x1, y1, scissorArea);
            return new RenderState(state, fakeLevel, x0, y0, x1, y1, scale, scissorArea, bounds);
        }
    }
}
