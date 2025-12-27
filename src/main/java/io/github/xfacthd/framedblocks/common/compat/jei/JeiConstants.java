package io.github.xfacthd.framedblocks.common.compat.jei;

import io.github.xfacthd.framedblocks.api.util.Utils;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

public final class JeiConstants
{
    public static final Component MSG_INVALID_RECIPE = Utils.translate("msg", "framing_saw.transfer.invalid_recipe");
    public static final Component MSG_TRANSFER_NOT_IMPLEMENTED = Utils.translate("msg", "framing_saw.transfer.not_implemented");
    public static final Component MSG_SUPPORTS_MOST_CAMOS = Utils.translate("msg", "camo_application.camo.most_supported")
            .withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC);

    private JeiConstants() { }
}
