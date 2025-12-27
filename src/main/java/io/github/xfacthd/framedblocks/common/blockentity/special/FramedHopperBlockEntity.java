package io.github.xfacthd.framedblocks.common.blockentity.special;

import io.github.xfacthd.framedblocks.api.block.blockentity.FramedBlockEntity;
import io.github.xfacthd.framedblocks.api.util.Utils;
import io.github.xfacthd.framedblocks.common.FBContent;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.HopperMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.HopperBlock;
import net.minecraft.world.level.block.entity.Hopper;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.ResourceHandlerUtil;
import net.neoforged.neoforge.transfer.item.ContainerOrHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.SnapshotJournal;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

import java.util.function.BooleanSupplier;

public class FramedHopperBlockEntity extends FramedBlockEntity implements Hopper, MenuProvider
{
    public static final Component TITLE = Utils.translate("title", "framed_hopper");
    public static final String COOLDOWN_NBT_KEY = "TransferCooldown";

    private final NonNullList<ItemStack> items = NonNullList.withSize(HopperBlockEntity.HOPPER_CONTAINER_SIZE, ItemStack.EMPTY);
    private int cooldownTime = -1;
    private long tickedGameTime;
    private Direction facing;
    private final SnapshotJournal<Integer> cooldownTimeJournal = new SnapshotJournal<>()
    {
        @Override
        protected Integer createSnapshot() {
            return cooldownTime;
        }

        @Override
        protected void revertToSnapshot(Integer snapshot) {
            cooldownTime = snapshot;
        }
    };

    public FramedHopperBlockEntity(BlockPos pos, BlockState state)
    {
        super(FBContent.BE_TYPE_FRAMED_HOPPER.value(), pos, state);
        this.facing = state.getValue(HopperBlock.FACING);
    }

    public static void tick(Level level, BlockPos ignoredPos, BlockState ignoredState, FramedHopperBlockEntity hopper)
    {
        hopper.cooldownTime--;
        hopper.tickedGameTime = level.getGameTime();
        if (!hopper.isOnCooldown())
        {
            hopper.setCooldown(0);
            hopper.tryMoveItems(() -> HopperBlockEntity.suckInItems(level, hopper));
        }
    }

    @Override
    public void onTransfer(int slot, int amountChange, TransactionContext transaction)
    {
        if (amountChange > 0 && !isOnCustomCooldown() && wasEmpty(slot, amountChange))
        {
            cooldownTimeJournal.updateSnapshots(transaction);
            setCooldown(HopperBlockEntity.MOVE_ITEM_SPEED);
        }
    }

    private boolean wasEmpty(int slot, int amountChange)
    {
        if (items.get(slot).getCount() != amountChange)
        {
            return false;
        }

        for (int i = 0; i < items.size(); ++i)
        {
            if (i != slot && !items.get(i).isEmpty())
            {
                return false;
            }
        }
        return true;
    }

    private void tryMoveItems(BooleanSupplier validator)
    {
        if (!level().isClientSide() && !isOnCooldown() && getBlockState().getValue(HopperBlock.ENABLED))
        {
            boolean success = false;
            if (!isEmpty())
            {
                success = ejectItems();
            }
            if (!isFull())
            {
                success |= validator.getAsBoolean();
            }
            if (success)
            {
                setCooldown(HopperBlockEntity.MOVE_ITEM_SPEED);
                setChanged();
            }
        }
    }

    private boolean ejectItems()
    {
        Direction side = facing.getOpposite();
        ContainerOrHandler containerOrHandler = HopperBlockEntity.getContainerOrHandlerAt(level(), worldPosition.relative(facing), side);

        Container container = containerOrHandler.container();
        if (container != null && !HopperBlockEntity.isFullContainer(container, side))
        {
            return ejectItemsInto(container, side, HopperBlockEntity::addItem);
        }

        ResourceHandler<ItemResource> itemHandler = containerOrHandler.itemHandler();
        if (itemHandler != null && !ResourceHandlerUtil.isFull(itemHandler))
        {
            return ejectItemsInto(itemHandler, side, (_, dest, stack, _) ->
            {
                try (Transaction tx = Transaction.open(null))
                {
                    if (dest.insert(ItemResource.of(stack), 1, tx) == 1)
                    {
                        tx.commit();
                        return ItemStack.EMPTY;
                    }
                    return stack;
                }
            });
        }

        return false;
    }

    private <T> boolean ejectItemsInto(T target, Direction side, InsertionFunction<T> inserter)
    {
        for (int i = 0; i < getContainerSize(); ++i)
        {
            ItemStack stack = getItem(i);
            if (!stack.isEmpty())
            {
                int count = stack.getCount();
                ItemStack remainder = inserter.insert(this, target, removeItem(i, 1), side);
                if (remainder.isEmpty())
                {
                    return true;
                }

                stack.setCount(count);
                if (count == 1)
                {
                    setItem(i, stack);
                }
            }
        }
        return false;
    }

    public void setCooldown(int cooldownTime)
    {
        this.cooldownTime = cooldownTime;
    }

    private boolean isOnCooldown()
    {
        return cooldownTime > 0;
    }

    public boolean isOnCustomCooldown()
    {
        return cooldownTime > HopperBlockEntity.MOVE_ITEM_SPEED;
    }

    public void entityInside(Entity entity)
    {
        if (entity instanceof ItemEntity itemEntity && !itemEntity.getItem().isEmpty())
        {
            AABB entityBounds = entity.getBoundingBox().move(-worldPosition.getX(), -worldPosition.getY(), -worldPosition.getZ());
            if (entityBounds.intersects(getSuckAabb()))
            {
                tryMoveItems(() -> HopperBlockEntity.addItem(this, itemEntity));
            }
        }
    }

    public long getLastUpdateTime()
    {
        return tickedGameTime;
    }

    @Override
    public double getLevelX()
    {
        return worldPosition.getX() + .5;
    }

    @Override
    public double getLevelY()
    {
        return worldPosition.getY() + .5;
    }

    @Override
    public double getLevelZ()
    {
        return worldPosition.getZ() + .5;
    }

    @Override
    public boolean isGridAligned()
    {
        return true;
    }

    @Override
    public void setItem(int index, ItemStack stack)
    {
        items.set(index, stack);
        stack.limitSize(getMaxStackSize(stack));
    }

    @Override
    public ItemStack getItem(int slot)
    {
        return items.get(slot);
    }

    @Override
    public ItemStack removeItem(int index, int count)
    {
        return ContainerHelper.removeItem(items, index, count);
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot)
    {
        return ContainerHelper.takeItem(items, slot);
    }

    @Override
    public boolean isEmpty()
    {
        for (ItemStack stack : items)
        {
            if (!stack.isEmpty())
            {
                return false;
            }
        }
        return true;
    }

    private boolean isFull()
    {
        for (ItemStack stack : items)
        {
            if (stack.isEmpty() || stack.getCount() < stack.getMaxStackSize())
            {
                return false;
            }
        }
        return true;
    }

    @Override
    public int getContainerSize()
    {
        return items.size();
    }

    @Override
    public void clearContent()
    {
        items.clear();
    }

    @Override
    public boolean stillValid(Player player)
    {
        return Container.stillValidBlockEntity(this, player);
    }

    @Override
    public void setBlockState(BlockState state)
    {
        super.setBlockState(state);
        facing = state.getValue(HopperBlock.FACING);
    }

    @Override
    public void loadAdditional(ValueInput valueInput)
    {
        super.loadAdditional(valueInput);
        ContainerHelper.loadAllItems(valueInput, items);
        cooldownTime = valueInput.getIntOr(COOLDOWN_NBT_KEY, 0);
    }

    @Override
    public void saveAdditional(ValueOutput valueOutput)
    {
        super.saveAdditional(valueOutput);
        ContainerHelper.saveAllItems(valueOutput, items);
        valueOutput.putInt(COOLDOWN_NBT_KEY, cooldownTime);
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player)
    {
        return new HopperMenu(containerId, inventory, this);
    }

    @Override
    public Component getDisplayName()
    {
        return TITLE;
    }

    @FunctionalInterface
    private interface InsertionFunction<T>
    {
        ItemStack insert(Container source, T target, ItemStack stack, Direction side);
    }
}
