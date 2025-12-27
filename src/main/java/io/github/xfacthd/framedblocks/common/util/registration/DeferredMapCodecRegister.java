package io.github.xfacthd.framedblocks.common.util.registration;

import com.mojang.serialization.MapCodec;
import io.github.xfacthd.framedblocks.api.util.registration.DeferredMapCodec;
import net.minecraft.core.Registry;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class DeferredMapCodecRegister<B> extends DeferredRegister<MapCodec<? extends B>>
{
    private DeferredMapCodecRegister(ResourceKey<? extends Registry<MapCodec<? extends B>>> registryKey, String namespace)
    {
        super(registryKey, namespace);
    }

    @Override
    @SuppressWarnings("unchecked")
    protected <I extends MapCodec<? extends B>> DeferredHolder<MapCodec<? extends B>, I> createHolder(ResourceKey<? extends Registry<MapCodec<? extends B>>> registryKey, Identifier key)
    {
        return (DeferredHolder<MapCodec<? extends B>, I>) DeferredMapCodec.createCodec(ResourceKey.create(registryKey, key));
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    public <T extends B> DeferredMapCodec<T> registerCodec(String name, MapCodec<T> codec)
    {
        return (DeferredMapCodec) register(name, () -> codec);
    }

    public static <T> DeferredMapCodecRegister<T> createMapCodecs(ResourceKey<Registry<MapCodec<? extends T>>> key, String namespace)
    {
        return new DeferredMapCodecRegister<>(key, namespace);
    }
}
