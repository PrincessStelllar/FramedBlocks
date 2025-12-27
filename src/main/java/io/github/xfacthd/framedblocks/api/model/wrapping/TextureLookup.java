package io.github.xfacthd.framedblocks.api.model.wrapping;

import io.github.xfacthd.framedblocks.api.util.ClientUtils;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.Material;
import net.minecraft.client.resources.model.ModelDebugName;
import net.minecraft.client.resources.model.SpriteGetter;
import net.minecraft.resources.Identifier;

public interface TextureLookup
{
    TextureAtlasSprite get(Identifier id);

    static TextureLookup bindSpriteGetter(SpriteGetter getter, ModelDebugName debugName)
    {
        return id -> getter.get(new Material(ClientUtils.BLOCK_ATLAS, id), debugName);
    }

    /**
     * {@return a lookup that is only usable at the end of or outside of a resource reload}
     */
    static TextureLookup runtime()
    {
        return ClientUtils::getBlockSprite;
    }
}
