package dev.tilosophy.buildersballoon;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.material.*;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.*;

/** Cosmetic state only: no block entity, binding, flight or entity-leash support. */
public final class BollardBlock extends Block implements SimpleWaterloggedBlock {
    public static final MapCodec<BollardBlock> CODEC = simpleCodec(BollardBlock::new);
    public static final BooleanProperty ROPED = BooleanProperty.create("roped");

    public BollardBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(StakeBlock.DIRECTION, Direction.DOWN)
            .setValue(StakeBlock.SPIN,0).setValue(StakeBlock.COLOR,DyeColor.WHITE)
            .setValue(StakeBlock.WATERLOGGED,false).setValue(ROPED,false));
    }
    @Override protected MapCodec<? extends Block> codec() { return CODEC; }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> builder) {
        builder.add(StakeBlock.DIRECTION,StakeBlock.SPIN,StakeBlock.COLOR,StakeBlock.WATERLOGGED,ROPED);
    }
    @Override public BlockState getStateForPlacement(BlockPlaceContext context) {
        var mount=context.getClickedFace().getOpposite();int spin=0;
        if(context.getPlayer()!=null) {
            var source=AnchorFrames.at(context.getLevel(),context.getClickedPos());
            if(source!=null && source.valid())spin=AnchorOrientation.towardPlayer(mount,
                source.logical().toLocal(context.getPlayer().getEyePosition()).subtract(Vec3.atCenterOf(context.getClickedPos())));
        }
        return defaultBlockState().setValue(StakeBlock.DIRECTION,mount).setValue(StakeBlock.SPIN,spin)
            .setValue(StakeBlock.WATERLOGGED,context.getLevel().getFluidState(context.getClickedPos()).getType()==Fluids.WATER);
    }
    @Override protected ItemInteractionResult useItemOn(ItemStack stack,BlockState state,Level level,BlockPos pos,Player player,InteractionHand hand,BlockHitResult hit) {
        if(!stack.is(RopeItems.SUPPORTED))return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        if(!player.mayBuild())return ItemInteractionResult.FAIL;
        if(!level.isClientSide && level.setBlock(pos,state.cycle(ROPED),3)) {
            // One server broadcast includes the acting player and nearby listeners.
            level.playSound(null,pos,state.getValue(ROPED) ? SoundEvents.LEASH_KNOT_BREAK : SoundEvents.LEASH_KNOT_PLACE,
                SoundSource.BLOCKS,0.75f,1.0f);
        }
        return ItemInteractionResult.sidedSuccess(level.isClientSide);
    }
    @Override public ItemStack getCloneItemStack(BlockState state,net.minecraft.world.phys.HitResult hit,LevelReader level,BlockPos pos,Player player) {
        return BollardItem.stack(this,state.getValue(StakeBlock.COLOR));
    }
    @Override protected FluidState getFluidState(BlockState state) {
        return state.getValue(StakeBlock.WATERLOGGED)?Fluids.WATER.getSource(false):super.getFluidState(state);
    }
    @Override protected BlockState updateShape(BlockState state,Direction direction,BlockState neighbour,LevelAccessor level,BlockPos pos,BlockPos neighbourPos) {
        if(state.getValue(StakeBlock.WATERLOGGED))level.scheduleTick(pos,Fluids.WATER,Fluids.WATER.getTickDelay(level));
        return super.updateShape(state,direction,neighbour,level,pos,neighbourPos);
    }
    @Override protected VoxelShape getShape(BlockState state,BlockGetter level,BlockPos pos,CollisionContext context) {
        return switch(state.getValue(StakeBlock.DIRECTION)) {
            case DOWN -> box(4,0,4,12,12,12);
            case UP -> box(4,4,4,12,16,12);
            case NORTH -> box(4,4,0,12,12,12);
            case SOUTH -> box(4,4,4,12,12,16);
            case WEST -> box(0,4,4,12,12,12);
            case EAST -> box(4,4,4,16,12,12);
        };
    }
}
