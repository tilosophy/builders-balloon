package dev.tilosophy.buildersballoon;

import com.mojang.serialization.MapCodec;
import dev.tilosophy.buildersballoon.StakeBlockEntity;
import dev.tilosophy.buildersballoon.TetherManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.common.util.FakePlayer;
import org.jetbrains.annotations.Nullable;

public final class StakeBlock
extends BaseEntityBlock
implements SimpleWaterloggedBlock {
    public static final MapCodec<StakeBlock> CODEC = StakeBlock.simpleCodec(StakeBlock::new);
    public static final DirectionProperty DIRECTION = DirectionProperty.create((String)"direction");
    public static final net.minecraft.world.level.block.state.properties.IntegerProperty SPIN = net.minecraft.world.level.block.state.properties.IntegerProperty.create("spin", 0, 3);
    public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;
    public static final EnumProperty<DyeColor> COLOR = EnumProperty.create("color", DyeColor.class);
    public static final BooleanProperty CONNECTED = BooleanProperty.create((String)"connected");

    public StakeBlock(BlockBehaviour.Properties properties) {
        super(properties);
        this.registerDefaultState(((BlockState)((BlockState)((BlockState)((BlockState)this.stateDefinition.any()).setValue((Property)DIRECTION, (Comparable)Direction.DOWN)).setValue((Property)WATERLOGGED, (Comparable)Boolean.valueOf(false))).setValue((Property)CONNECTED, (Comparable)Boolean.valueOf(false))).setValue(COLOR, DyeColor.WHITE).setValue(SPIN, 0));
    }

    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new StakeBlockEntity(pos, state);
    }

    @Override public ItemStack getCloneItemStack(BlockState state, net.minecraft.world.phys.HitResult target, LevelReader level, BlockPos pos, Player player) {
        return StakeItem.stack(this,state.getValue(COLOR));
    }

    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(new Property[]{DIRECTION, WATERLOGGED, CONNECTED, COLOR, SPIN});
    }

    public BlockState getStateForPlacement(BlockPlaceContext context) {
        var mount = context.getClickedFace().getOpposite();
        int spin = 0;
        if (context.getPlayer() != null) {
            var source = AnchorFrames.at(context.getLevel(), context.getClickedPos());
            if (source != null && source.valid()) {
                var localEye = source.logical().toLocal(context.getPlayer().getEyePosition());
                spin = AnchorOrientation.towardPlayer(mount, localEye.subtract(net.minecraft.world.phys.Vec3.atCenterOf(context.getClickedPos())));
            }
        }
        return this.defaultBlockState().setValue(DIRECTION, mount).setValue(SPIN, spin)
            .setValue(WATERLOGGED, context.getLevel().getFluidState(context.getClickedPos()).getType() == Fluids.WATER);
    }

    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        BlockEntity blockEntity;
        ServerPlayer player;
        if (placer instanceof ServerPlayer && !((player = (ServerPlayer)placer) instanceof FakePlayer) && (blockEntity = level.getBlockEntity(pos)) instanceof StakeBlockEntity) {
            StakeBlockEntity stake = (StakeBlockEntity)blockEntity;
            TetherManager.bind(player, stake);
        }
    }

    @Override protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        // Vanilla can route an item's default interaction here too: explicitly require an empty main hand.
        if (!player.getMainHandItem().isEmpty() || player instanceof FakePlayer) return InteractionResult.PASS;
        if (level.isClientSide) return InteractionResult.SUCCESS;
        if (player instanceof ServerPlayer server && level.getBlockEntity(pos) instanceof StakeBlockEntity stake) {
            var result = TetherManager.activate(server, stake);
            String message = switch (result) {
                case OCCUPIED -> "message.builders_balloon.occupied";
                case OUT_OF_RANGE -> "message.builders_balloon.out_of_range";
                case UNAVAILABLE -> "message.builders_balloon.unavailable";
                default -> null;
            };
            if (message != null) player.displayClientMessage(Component.translatable(message), true);
            return InteractionResult.CONSUME;
        }
        return InteractionResult.PASS;
    }

    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState replacement, boolean moving) {
        BlockEntity blockEntity;
        if (!state.is(replacement.getBlock()) && (blockEntity = level.getBlockEntity(pos)) instanceof StakeBlockEntity) {
            StakeBlockEntity stake = (StakeBlockEntity)blockEntity;
            TetherManager.releaseStake(stake);
        }
        super.onRemove(state, level, pos, replacement, moving);
    }

    protected FluidState getFluidState(BlockState state) {
        return (Boolean)state.getValue((Property)WATERLOGGED) != false ? Fluids.WATER.getSource(false) : super.getFluidState(state);
    }

    protected BlockState updateShape(BlockState state, Direction direction, BlockState neighbour, LevelAccessor level, BlockPos pos, BlockPos neighbourPos) {
        if (((Boolean)state.getValue((Property)WATERLOGGED)).booleanValue()) {
            level.scheduleTick(pos, (Fluid)Fluids.WATER, Fluids.WATER.getTickDelay((LevelReader)level));
        }
        return super.updateShape(state, direction, neighbour, level, pos, neighbourPos);
    }

    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return switch (state.getValue(DIRECTION)) {
            case DOWN -> StakeBlock.box(4, 0, 4, 12, 12, 12);
            case UP -> StakeBlock.box(4, 4, 4, 12, 16, 12);
            case NORTH -> StakeBlock.box(4, 4, 0, 12, 12, 12);
            case SOUTH -> StakeBlock.box(4, 4, 4, 12, 12, 16);
            case WEST -> StakeBlock.box(0, 4, 4, 12, 12, 12);
            case EAST -> StakeBlock.box(4, 4, 4, 16, 12, 12);
        };
    }
}
