package io.github.xfacthd.framedblocks.client.screen;

import io.github.xfacthd.framedblocks.client.screen.pip.SignBlockPictureInPictureRenderer;
import io.github.xfacthd.framedblocks.common.block.sign.AbstractFramedHangingSignBlock;
import io.github.xfacthd.framedblocks.common.block.sign.AbstractFramedSignBlock;
import io.github.xfacthd.framedblocks.common.blockentity.special.FramedSignBlockEntity;
import io.github.xfacthd.framedblocks.common.data.BlockType;
import io.github.xfacthd.framedblocks.common.net.payload.serverbound.ServerboundSignUpdatePayload;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.font.TextFieldHelper;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.renderer.blockentity.SignRenderer;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.entity.SignText;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import org.jetbrains.annotations.UnknownNullability;
import org.jspecify.annotations.Nullable;

import java.util.Arrays;
import java.util.stream.IntStream;

public class FramedSignScreen extends Screen
{
    private static final Component TITLE_NORMAL = Component.translatable("sign.edit");
    private static final Component TITLE_HANGING = Component.translatable("hanging_sign.edit");
    private static final SignConfig CFG_STANDING = new SignConfig(65, 105, 95F, 89, 10);
    private static final SignConfig CFG_WALL = new SignConfig(60, 110, 95F, 120, 10);
    private static final SignConfig CFG_HANGING = new SignConfig(75, 75, 75F, 127, 9);

    private final AbstractFramedSignBlock signBlock;
    private final FramedSignBlockEntity sign;
    private final boolean front;
    private final SignConfig signConfig;
    private SignText text;
    private final String[] lines;
    private int blinkCounter = 0;
    private int currLine = 0;
    @UnknownNullability
    private TextFieldHelper inputUtil;

    public FramedSignScreen(FramedSignBlockEntity sign, boolean front)
    {
        super(getTitle(sign));
        this.signBlock = (AbstractFramedSignBlock) sign.getBlockState().getBlock();
        this.sign = sign;
        this.front = front;
        this.text = sign.getText(front);
        boolean filtered = minecraft.isTextFilteringEnabled();
        this.lines = IntStream.range(0, 4)
                .mapToObj(idx -> text.getMessage(idx, filtered))
                .map(Component::getString)
                .toArray(String[]::new);
        this.signConfig = switch ((BlockType) sign.getBlockType())
        {
            case FRAMED_SIGN -> CFG_STANDING;
            case FRAMED_WALL_SIGN -> CFG_WALL;
            case FRAMED_HANGING_SIGN, FRAMED_WALL_HANGING_SIGN -> CFG_HANGING;
            default -> throw new IllegalArgumentException("Invalid block type: " + sign.getBlockType());
        };
    }

    private static Component getTitle(FramedSignBlockEntity be)
    {
        boolean hanging = be.getBlockState().getBlock() instanceof AbstractFramedHangingSignBlock;
        return hanging ? TITLE_HANGING : TITLE_NORMAL;
    }

    @Override
    protected void init()
    {
        addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, _ -> minecraft.setScreen(null))
                .pos(width / 2 - 100, height / 4 + 144)
                .size(200, 20)
                .build()
        );

        inputUtil = new TextFieldHelper(
                () -> lines[currLine],
                this::setLine,
                TextFieldHelper.createClipboardGetter(minecraft), TextFieldHelper.createClipboardSetter(minecraft),
                (line) -> minecraft.font.width(line) <= signBlock.getMaxTextLineWidth()
        );
    }

    private void setLine(String line)
    {
        lines[currLine] = line;
        text = text.setMessage(currLine, Component.literal(line));
        sign.setText(text, front);
    }

    @Override
    public void removed()
    {
        ClientPacketDistributor.sendToServer(new ServerboundSignUpdatePayload(sign.getBlockPos(), front, Arrays.copyOf(lines, lines.length)));
    }

    @Override
    public void tick()
    {
        blinkCounter++;

        if (minecraft.player != null && (sign.isRemoved() || sign.isTooFarAwayToEdit(minecraft.player)))
        {
            minecraft.setScreen(null);
        }
    }

    @Override
    public boolean isPauseScreen()
    {
        return false;
    }

    @Override
    public boolean charTyped(CharacterEvent event)
    {
        inputUtil.charTyped(event);
        return true;
    }

    @Override
    public boolean keyPressed(KeyEvent event)
    {
        if (event.isUp())
        {
            currLine = currLine - 1 & 3;
            inputUtil.setCursorToEnd();
            return true;
        }
        else if (event.isDown() || event.isConfirmation())
        {
            currLine = currLine + 1 & 3;
            inputUtil.setCursorToEnd();
            return true;
        }
        else
        {
            return inputUtil.keyPressed(event) || super.keyPressed(event);
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks)
    {
        super.render(graphics, mouseX, mouseY, partialTicks);
        graphics.drawCenteredString(font, title, width / 2, 40, 0xFFFFFFFF);
        drawSignBlock(graphics);
        drawLines(graphics, lines);
        drawCursor(graphics, lines);
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick)
    {
        renderTransparentBackground(graphics);
    }

    private void drawSignBlock(GuiGraphics graphics)
    {
        int x0 = width / 2 - 50;
        int y0 = signConfig.pipYOff;
        int x1 = x0 + 100;
        int y1 = y0 + signConfig.pipHeight;
        graphics.submitPictureInPictureRenderState(SignBlockPictureInPictureRenderer.RenderState.create(
                signBlock, sign, x0, y0, x1, y1, signConfig.signScale, graphics.peekScissorStack()
        ));
    }

    private void drawLines(GuiGraphics graphics, @Nullable String[] lines)
    {
        int color = text.hasGlowingText() ? text.getColor().getTextColor() : SignRenderer.getDarkColor(text);
        int lineHeight = signConfig.lineHeight;
        int centerY = 4 * lineHeight / 2;

        int baseX = width / 2;
        int baseY = signConfig.textYOff;
        for (int line = 0; line < lines.length; line++)
        {
            String text = lines[line];
            if (text != null)
            {
                if (font.isBidirectional())
                {
                    text = font.bidirectionalShaping(text);
                }

                int textX = baseX + -font.width(text) / 2;
                int textY = baseY + line * lineHeight - centerY;
                graphics.drawString(font, text, textX, textY, color, false);
            }
        }
    }

    private void drawCursor(GuiGraphics graphics, @Nullable String[] lines)
    {
        int color = text.hasGlowingText() ? text.getColor().getTextColor() : SignRenderer.getDarkColor(text);
        boolean blink = blinkCounter / 6 % 2 == 0;
        int dir = font.isBidirectional() ? -1 : 1;
        int lineHeight = signConfig.lineHeight;
        int baseX = width / 2;
        int centerY = 4 * lineHeight / 2;
        int y = signConfig.textYOff + currLine * lineHeight - centerY;

        for (int i = 0; i < lines.length; ++i)
        {
            String line = lines[i];
            if (line != null && i == currLine && inputUtil.getCursorPos() >= 0)
            {
                int hw = font.width(line) / 2;
                int selectionEnd = font.width(line.substring(0, Math.max(Math.min(inputUtil.getCursorPos(), line.length()), 0)));
                int cursorX = baseX + (selectionEnd - hw) * dir;

                if (blink)
                {
                    if (inputUtil.getCursorPos() < line.length())
                    {
                        graphics.fill(cursorX, y - 1, cursorX + 1, y + lineHeight, 0xFF000000 | color);
                    }
                    else
                    {
                        graphics.drawString(font, "_", cursorX, y, color, false);
                    }
                }

                if (inputUtil.getSelectionPos() != inputUtil.getCursorPos())
                {
                    int x1 = (font.width(line.substring(0, inputUtil.getSelectionPos())) - hw) * dir;
                    int x2 = (font.width(line.substring(0, inputUtil.getCursorPos()   )) - hw) * dir;
                    int xStart = baseX + Math.min(x1, x2);
                    int xEnd = baseX + Math.max(x1, x2);

                    graphics.textHighlight(xStart, y, xEnd, y + lineHeight, true);
                }
            }
        }
    }

    private record SignConfig(int pipYOff, int pipHeight, float signScale, int textYOff, int lineHeight) { }
}
