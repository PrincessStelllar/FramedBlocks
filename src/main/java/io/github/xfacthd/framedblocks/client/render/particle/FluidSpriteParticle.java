package io.github.xfacthd.framedblocks.client.render.particle;

import io.github.xfacthd.framedblocks.api.util.ClientUtils;
import io.github.xfacthd.framedblocks.common.particle.FluidParticleOptions;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import org.jspecify.annotations.Nullable;

import java.util.Objects;

public final class FluidSpriteParticle extends SingleQuadParticle
{
    private final BlockPos pos;
    private final float uo;
    private final float vo;
    private final int brightness;

    public FluidSpriteParticle(ClientLevel level, double x, double y, double z, double sx, double sy, double sz, Fluid fluid)
    {
        super(level, x, y, z, sx, sy, sz, resolveSprite(fluid));
        this.pos = BlockPos.containing(x, y, z);
        this.gravity = 1F;
        this.quadSize /= 2F;
        this.uo = random.nextFloat() * 3F;
        this.vo = random.nextFloat() * 3F;
        this.brightness = fluid.getFluidType().getLightLevel(fluid.defaultFluidState(), level, pos);

        int tint = ClientUtils.getFluidColor(level, pos, fluid.defaultFluidState());
        this.rCol = .6F * (float)(tint >> 16 & 0xFF) / 255F;
        this.gCol = .6F * (float)(tint >>  8 & 0xFF) / 255F;
        this.bCol = .6F * (float)(tint       & 0xFF) / 255F;
    }

    private static TextureAtlasSprite resolveSprite(Fluid fluid)
    {
        Identifier stillTex = Objects.requireNonNullElse(
                IClientFluidTypeExtensions.of(fluid).getStillTexture(),
                MissingTextureAtlasSprite.getLocation()
        );
        return ClientUtils.getBlockSprite(stillTex);
    }

    @Override
    public Layer getLayer()
    {
        return Layer.TERRAIN;
    }

    @Override
    protected float getU0()
    {
        return sprite.getU((uo + 1.0F) / 4.0F);
    }

    @Override
    protected float getU1()
    {
        return sprite.getU(uo / 4.0F);
    }

    @Override
    protected float getV0()
    {
        return sprite.getV(vo / 4.0F);
    }

    @Override
    protected float getV1()
    {
        return sprite.getV((vo + 1.0F) / 4.0F);
    }

    @Override
    @SuppressWarnings("deprecation")
    protected int getLightCoords(float partialTick)
    {
        int light = level.hasChunkAt(pos) ? LevelRenderer.getLightCoords(level, pos) : 0;
        int block = Math.max(brightness, LightCoordsUtil.block(light));
        return LightCoordsUtil.pack(block, LightCoordsUtil.sky(light));
    }

    public static final class Provider implements ParticleProvider<FluidParticleOptions>
    {
        @Nullable
        @Override
        public Particle createParticle(
                FluidParticleOptions options,
                ClientLevel level,
                double x,
                double y,
                double z,
                double sx,
                double sy,
                double sz,
                RandomSource random
        )
        {
            if (options.fluid() != Fluids.EMPTY)
            {
                return new FluidSpriteParticle(level, x, y, z, sx, sy, sz, options.fluid());
            }
            return null;
        }
    }
}
