package io.github.xfacthd.framedblocks.client.util;

import io.github.xfacthd.framedblocks.api.model.AbstractFramedBlockModel;
import io.github.xfacthd.framedblocks.api.model.item.AbstractFramedBlockItemModel;
import io.github.xfacthd.framedblocks.client.model.unbaked.UnbakedStandaloneFramedBlockModel;
import io.github.xfacthd.framedblocks.client.render.special.ModelBasedOutlineRenderer;
import io.github.xfacthd.framedblocks.common.data.camo.fluid.FluidCamoClientHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.model.ModelManager;

public final class FramedClientUtils
{
    public static void clearModelCaches()
    {
        ModelManager modelManager = Minecraft.getInstance().getModelManager();

        modelManager.getBlockModelShaper()
                .framedblocks$getModelByStateCache()
                .values()
                .stream()
                .filter(AbstractFramedBlockModel.class::isInstance)
                .map(AbstractFramedBlockModel.class::cast)
                .forEach(AbstractFramedBlockModel::clearCache);

        modelManager.framedblocks$getBakedItemStackModels()
                .values()
                .stream()
                .filter(AbstractFramedBlockItemModel.class::isInstance)
                .map(AbstractFramedBlockItemModel.class::cast)
                .forEach(AbstractFramedBlockItemModel::clearCache);

        UnbakedStandaloneFramedBlockModel.clearCaches();

        FluidCamoClientHandler.clearModelCache();

        ModelBasedOutlineRenderer.clearCaches();
    }

    private FramedClientUtils() { }
}
