package io.github.xfacthd.framedblocks.api.util.registration;

import com.mojang.serialization.MapCodec;
import net.minecraft.resources.ResourceKey;
import net.neoforged.neoforge.registries.DeferredHolder;

public final class DeferredMapCodec<T> extends DeferredHolder<MapCodec<? super T>, MapCodec<T>>
{
    private DeferredMapCodec(ResourceKey<MapCodec<? super T>> key)
    {
        super(key);
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    public static <B, T extends B> DeferredMapCodec<T> createCodec(ResourceKey<MapCodec<? extends B>> key)
    {
        return new DeferredMapCodec<>((ResourceKey) key);
    }
}
