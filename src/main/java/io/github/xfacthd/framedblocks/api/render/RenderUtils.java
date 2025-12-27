package io.github.xfacthd.framedblocks.api.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.color.block.BlockColors;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.BlockModelPart;
import net.minecraft.client.renderer.block.model.BlockStateModel;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.ARGB;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

public final class RenderUtils
{
    private static final Direction[] DIRECTIONS = Direction.values();

    public static RenderType getEntityRenderType(ChunkSectionLayer chunkLayer)
    {
        return switch (chunkLayer)
        {
            case SOLID -> Sheets.solidBlockSheet();
            case CUTOUT -> Sheets.cutoutBlockSheet();
            case TRANSLUCENT -> Sheets.translucentBlockItemSheet();
        };
    }

    public static void submitModel(
            BlockState state,
            BlockAndTintGetter level,
            BlockPos pos,
            PoseStack poseStack,
            SubmitNodeCollector submitNodeCollector,
            BlockStateModel model,
            RandomSource random,
            int light,
            int overlay
    )
    {
        for (BlockModelPart part : model.collectParts(level, pos, state, random))
        {
            RenderType renderType = getEntityRenderType(part.getRenderType(state));
            submitNodeCollector.submitCustomGeometry(poseStack, renderType, (pose, buffer) ->
            {
                for (Direction side : DIRECTIONS)
                {
                    renderQuadList(state, level, pos, pose, buffer, part.getQuads(side), light, overlay);
                }

                renderQuadList(state, level, pos, pose, buffer, part.getQuads(null), light, overlay);
            });
        }
    }

    public static void renderModel(
            BlockState state,
            BlockAndTintGetter level,
            BlockPos pos,
            PoseStack.Pose pose,
            MultiBufferSource bufferSource,
            BlockStateModel model,
            RandomSource random,
            int light,
            int overlay
    )
    {
        for (BlockModelPart part : model.collectParts(level, pos, state, random))
        {
            VertexConsumer buffer = bufferSource.getBuffer(getEntityRenderType(part.getRenderType(state)));
            for (Direction side : DIRECTIONS)
            {
                renderQuadList(state, level, pos, pose, buffer, part.getQuads(side), light, overlay);
            }

            renderQuadList(state, level, pos, pose, buffer, part.getQuads(null), light, overlay);
        }
    }

    public static void renderQuadList(
            BlockState state,
            BlockAndTintGetter level,
            BlockPos pos,
            PoseStack.Pose pose,
            VertexConsumer consumer,
            List<BakedQuad> quads,
            int light,
            int overlay
    )
    {
        BlockColors blockColors = Minecraft.getInstance().getBlockColors();
        int lastTintIndex = -1;
        int lastTintValue = 0xFFFFFFFF;

        for (BakedQuad quad : quads)
        {
            float redF = 1.0F;
            float greenF = 1.0F;
            float blueF = 1.0F;
            if (quad.isTinted())
            {
                int color = lastTintValue;
                if (lastTintIndex != quad.tintIndex())
                {
                    lastTintIndex = quad.tintIndex();
                    color = lastTintValue = blockColors.getColor(state, level, pos, lastTintIndex);
                }
                redF = ARGB.redFloat(color);
                greenF = ARGB.greenFloat(color);
                blueF = ARGB.blueFloat(color);
            }

            consumer.putBulkData(pose, quad, redF, greenF, blueF, 1F, light, overlay);
        }
    }

    private RenderUtils() { }
}
