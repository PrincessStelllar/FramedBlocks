package io.github.xfacthd.framedblocks.common.block.sign;

import com.google.common.base.Preconditions;
import io.github.xfacthd.framedblocks.api.block.BlockUtils;
import io.github.xfacthd.framedblocks.api.block.blockentity.FramedBlockEntity;
import io.github.xfacthd.framedblocks.api.model.wrapping.WrapHelper;
import io.github.xfacthd.framedblocks.api.model.wrapping.statemerger.StateMerger;
import io.github.xfacthd.framedblocks.api.util.Utils;
import io.github.xfacthd.framedblocks.common.FBContent;
import io.github.xfacthd.framedblocks.common.block.FramedBlock;
import io.github.xfacthd.framedblocks.common.blockentity.special.FramedSignBlockEntity;
import io.github.xfacthd.framedblocks.common.data.BlockType;
import io.github.xfacthd.framedblocks.common.net.payload.clientbound.ClientboundOpenSignScreenPayload;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.contents.PlainTextContents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeItem;
import net.minecraft.world.item.GlowInkSacItem;
import net.minecraft.world.item.HoneycombItem;
import net.minecraft.world.item.InkSacItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LevelEvent;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.SignText;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.ItemAbilities;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jspecify.annotations.Nullable;

import java.util.Arrays;
import java.util.Set;
import java.util.UUID;

public abstract class AbstractFramedSignBlock extends FramedBlock
{
    private static final Vec3 HITBOX_CENTER = new Vec3(.5, .5, .5);

    protected AbstractFramedSignBlock(BlockType type, Properties props)
    {
        super(type, props);
    }

    @Override
    protected InteractionResult useItemOn(
            ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit
    )
    {
        //Makes sure the block can have a camo applied, even when the sign can execute a command
        InteractionResult result = super.useItemOn(stack, state, level, pos, player, hand, hit);
        if (result.consumesAction() || preventUse(state, level, pos, player, stack, hit))
        {
            if (level.isClientSide() && result instanceof InteractionResult.TryEmptyHandInteraction)
            {
                return InteractionResult.PASS;
            }
            return result;
        }

        SignInteraction interaction = SignInteraction.from(stack);
        boolean canInteract = interaction != null && player.mayBuild();

        if (level.getBlockEntity(pos) instanceof FramedSignBlockEntity sign)
        {
            if (level.isClientSide())
            {
                return canInteract || sign.isWaxed() ? InteractionResult.SUCCESS : InteractionResult.CONSUME;
            }

            if (!canInteract || sign.isWaxed() || isBlockedByOtherPlayer(player, sign))
            {
                return InteractionResult.TRY_WITH_EMPTY_HAND;
            }

            boolean front = sign.isFacingFrontText(player);
            if (interaction.interact(level, pos, player, stack, front, sign))
            {
                player.awardStat(Stats.ITEM_USED.get(stack.getItem()));
                level.gameEvent(GameEvent.BLOCK_CHANGE, sign.getBlockPos(), GameEvent.Context.of(player, sign.getBlockState()));
                if (!player.isCreative())
                {
                    stack.shrink(1);
                }

                return InteractionResult.SUCCESS;
            }

            return InteractionResult.TRY_WITH_EMPTY_HAND;
        }

        return InteractionResult.PASS;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit)
    {
        if (level.getBlockEntity(pos) instanceof FramedSignBlockEntity sign)
        {
            Preconditions.checkState(!level.isClientSide(), "Expected to only call this on the server");

            boolean front = sign.isFacingFrontText(player);
            if (sign.isWaxed())
            {
                if (sign.cannotExecuteCommands(front, player) || !sign.tryExecuteCommands(player, level, pos, front))
                {
                    level.playSound(null, pos, getSignInteractionFailedSoundEvent(), SoundSource.BLOCKS);
                }
                return InteractionResult.SUCCESS_SERVER;
            }

            if (!isBlockedByOtherPlayer(player, sign) && canEdit(player, sign, front))
            {
                openEditScreen(player, sign, front);
                return InteractionResult.SUCCESS_SERVER;
            }
        }
        return InteractionResult.PASS;
    }

    protected boolean preventUse(BlockState state, Level level, BlockPos pos, Player player, ItemStack stack, BlockHitResult hit)
    {
        return false;
    }

    @Override
    protected BlockState updateShape(
            BlockState state,
            LevelReader level,
            ScheduledTickAccess tickAccess,
            BlockPos pos, Direction side,
            BlockPos adjPos,
            BlockState adjState,
            RandomSource random
    )
    {
        if (state.getValue(BlockStateProperties.WATERLOGGED))
        {
            tickAccess.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(level));
        }
        return super.updateShape(state, level, tickAccess, pos, side, adjPos, adjState, random);
    }

    @Override
    protected boolean isPathfindable(BlockState state, PathComputationType type)
    {
        return type != PathComputationType.WATER || state.getFluidState().is(FluidTags.WATER);
    }

    @Override
    public boolean isPossibleToRespawnInThis(BlockState state)
    {
        return true;
    }

    public abstract float getYRotationDegrees(BlockState state);

    public Vec3 getSignHitboxCenterPosition(BlockState state)
    {
        return HITBOX_CENTER;
    }

    public int getTextLineHeight()
    {
        return 10;
    }

    public int getMaxTextLineWidth()
    {
        return 90;
    }

    protected SoundEvent getSignInteractionFailedSoundEvent()
    {
        return SoundEvents.WAXED_SIGN_INTERACT_FAIL;
    }

    @Override
    public FramedBlockEntity newBlockEntity(BlockPos pos, BlockState state)
    {
        return FramedSignBlockEntity.normalSign(pos, state);
    }

    @Override
    @Nullable
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type)
    {
        return BlockUtils.createBlockEntityTicker(type, FBContent.BE_TYPE_FRAMED_SIGN.value(), FramedSignBlockEntity::tick);
    }

    private static boolean isBlockedByOtherPlayer(Player player, FramedSignBlockEntity sign)
    {
        UUID uuid = sign.getEditingPlayer();
        return uuid != null && !uuid.equals(player.getUUID());
    }

    private static boolean canEdit(Player player, FramedSignBlockEntity sign, boolean frontText)
    {
        SignText text = sign.getText(frontText);
        return Arrays.stream(text.getMessages(player.isTextFilteringEnabled())).allMatch(line ->
                line.equals(CommonComponents.EMPTY) || line.getContents() instanceof PlainTextContents
        );
    }

    public static void openEditScreen(Player player, FramedSignBlockEntity sign, boolean frontText)
    {
        if (player instanceof ServerPlayer serverPlayer)
        {
            sign.setEditingPlayer(player.getUUID());
            PacketDistributor.sendToPlayer(serverPlayer, new ClientboundOpenSignScreenPayload(sign.getBlockPos(), frontText));
        }
    }

    private enum SignInteraction
    {
        APPLY_DYE(SignText::hasMessage, (level, pos, _, stack, front, sign) ->
        {
            level.playSound(null, pos, SoundEvents.DYE_USE, SoundSource.BLOCKS, 1.0F, 1.0F);
            return sign.updateText(text -> text.setColor(((DyeItem) stack.getItem()).getDyeColor()), front);
        }),
        APPLY_INK(SignText::hasMessage, (level, pos, _, _, front, sign) ->
        {
            level.playSound(null, pos, SoundEvents.INK_SAC_USE, SoundSource.BLOCKS, 1.0F, 1.0F);
            return sign.updateText(text -> text.setHasGlowingText(false), front);
        }),
        APPLY_GLOW_INK(SignText::hasMessage, (level, pos, player, stack, front, sign) ->
        {
            level.playSound(null, pos, SoundEvents.GLOW_INK_SAC_USE, SoundSource.BLOCKS, 1.0F, 1.0F);
            if (player instanceof ServerPlayer)
            {
                CriteriaTriggers.ITEM_USED_ON_BLOCK.trigger((ServerPlayer)player, pos, stack);
            }
            return sign.updateText(text -> text.setHasGlowingText(true), front);
        }),
        APPLY_WAX((_, _) -> true, (level, _, _, _, _, sign) ->
        {
            if (sign.setWaxed(true))
            {
                level.levelEvent(LevelEvent.PARTICLES_AND_SOUND_WAX_ON, sign.getBlockPos(), 0);
                return true;
            }
            return false;
        }),
        REMOVE_WAX(SignText::hasMessage, (level, _, _, _, _, sign) ->
        {
            if (sign.setWaxed(false))
            {
                level.levelEvent(LevelEvent.PARTICLES_WAX_OFF, sign.getBlockPos(), 0);
                return true;
            }
            return false;
        });

        private final InteractionPredicate predicate;
        private final Action action;

        SignInteraction(InteractionPredicate predicate, Action action)
        {
            this.predicate = predicate;
            this.action = action;
        }

        public boolean interact(
                Level level, BlockPos pos, Player player, ItemStack stack, boolean front, FramedSignBlockEntity sign
        )
        {
            return predicate.canInteract(sign.getText(front), player) && action.apply(level, pos, player, stack, front, sign);
        }

        @Nullable
        public static SignInteraction from(ItemStack stack)
        {
            return switch (stack.getItem())
            {
                case DyeItem ignored -> APPLY_DYE;
                case InkSacItem ignored -> APPLY_INK;
                case GlowInkSacItem ignored -> APPLY_GLOW_INK;
                case HoneycombItem ignored -> APPLY_WAX;
                default -> stack.canPerformAction(ItemAbilities.AXE_WAX_OFF) ? REMOVE_WAX : null;
            };
        }
    }

    private interface Action
    {
        boolean apply(Level level, BlockPos pos, Player player, ItemStack stack, boolean front, FramedSignBlockEntity sign);
    }

    private interface InteractionPredicate
    {
        boolean canInteract(SignText text, Player player);
    }

    public static final class RotatingSignStateMerger implements StateMerger
    {
        public static final RotatingSignStateMerger INSTANCE = new RotatingSignStateMerger();

        private final StateMerger ignoringMerger = StateMerger.ignoring(WrapHelper.IGNORE_WATERLOGGED);

        private RotatingSignStateMerger() { }

        @Override
        public BlockState apply(BlockState state)
        {
            state = ignoringMerger.apply(state);
            int rot = state.getValue(BlockStateProperties.ROTATION_16);
            if (rot > 7)
            {
                state = state.setValue(BlockStateProperties.ROTATION_16, rot - 8);
            }
            return state;
        }

        @Override
        public Set<Property<?>> getHandledProperties(Holder<Block> block)
        {
            return Utils.concat(
                    ignoringMerger.getHandledProperties(block),
                    Set.of(BlockStateProperties.ROTATION_16)
            );
        }
    }
}
