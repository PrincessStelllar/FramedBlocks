package io.github.xfacthd.framedblocks.api.block.render;

import net.minecraft.world.level.block.state.BlockState;

import java.util.function.Predicate;

/**
 * Declares whether the "uncullable" quads of either part of a double block should be culled for a particular part state
 * when the other part has an opaque camo applied to it.
 */
public record NullCullPredicate(Predicate<BlockState> leftStateTest, Predicate<BlockState> rightStateTest)
{
    public static final NullCullPredicate NEVER = new NullCullPredicate(_ -> false, _ -> false);
    public static final NullCullPredicate ALWAYS = new NullCullPredicate(_ -> true, _ -> true);
    public static final NullCullPredicate ONLY_LEFT = new NullCullPredicate(_ -> true, _ -> false);
    public static final NullCullPredicate ONLY_RIGHT = new NullCullPredicate(_ -> false, _ -> true);

    public boolean testLeft(BlockState state)
    {
        return leftStateTest.test(state);
    }

    public boolean testRight(BlockState state)
    {
        return rightStateTest.test(state);
    }
}
