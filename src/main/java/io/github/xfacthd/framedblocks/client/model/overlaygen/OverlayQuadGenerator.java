package io.github.xfacthd.framedblocks.client.model.overlaygen;

import io.github.xfacthd.framedblocks.api.util.Utils;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import net.neoforged.neoforge.client.model.quad.MutableQuad;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Predicate;

public final class OverlayQuadGenerator
{
    public static final Identifier LISTENER_ID = Utils.id("overlay_quad_gen");
    private static final Map<OverlayCacheKey, BakedQuad> OVERLAY_CACHE = new ConcurrentHashMap<>();

    public static void generate(
            List<BakedQuad> srcQuads,
            ArrayList<BakedQuad> outQuads,
            Function<Direction, TextureAtlasSprite> spriteGetter,
            Predicate<Direction> filter
    )
    {
        outQuads.ensureCapacity(outQuads.size() + srcQuads.size());
        Set<OverlayCacheKey> uniqueKeys = new HashSet<>(srcQuads.size());
        for (BakedQuad quad : srcQuads)
        {
            if (!filter.test(quad.direction())) continue;

            TextureAtlasSprite sprite = spriteGetter.apply(quad.direction());
            OverlayCacheKey key = buildCacheKey(quad, sprite);
            if (uniqueKeys.add(key))
            {
                outQuads.add(OVERLAY_CACHE.computeIfAbsent(key, OverlayQuadGenerator::generateOverlayQuad));
            }
        }
    }

    private static BakedQuad generateOverlayQuad(OverlayCacheKey key)
    {
        MutableQuad quad = new MutableQuad();

        quad.setSprite(key.sprite());
        quad.setDirection(key.face());
        for (int i = 0; i < 4; i++)
        {
            quad.setPosition(i, key.pos(i));
        }
        quad.setNormal(key.normals());
        quad.bakeUvsFromPosition();

        return quad.toBakedQuad();
    }

    private static OverlayCacheKey buildCacheKey(BakedQuad quad, TextureAtlasSprite sprite)
    {
        return new OverlayCacheKey(quad.direction(), quad.position0(), quad.position1(), quad.position2(), quad.position3(), quad.bakedNormals(), sprite);
    }

    public static void onResourceReload(@SuppressWarnings("unused") ResourceManager resourceManager)
    {
        OVERLAY_CACHE.clear();
    }

    private OverlayQuadGenerator() { }
}
