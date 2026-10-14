package dev.tilosophy.buildersballoon;

import dev.tilosophy.buildersballoon.BuildersBalloon;
import dev.tilosophy.buildersballoon.StakeBlock;
import dev.tilosophy.buildersballoon.TetherManager;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import org.jetbrains.annotations.Nullable;

public final class StakeBlockEntity
extends BlockEntity {
    @Nullable
    private UUID owner;

    public StakeBlockEntity(BlockPos pos, BlockState state) {
        super((BlockEntityType)BuildersBalloon.STAKE_ENTITY.get(), pos, state);
    }

    @Nullable
    public UUID owner() {
        return this.owner;
    }

    public void setOwner(@Nullable UUID owner) {
        this.owner = owner;
        if (this.level != null && this.level.hasChunkAt(this.worldPosition)) {
            BlockState state = this.level.getBlockState(this.worldPosition);
            if (state.getBlock() instanceof StakeBlock) {
                this.level.setBlock(this.worldPosition, (BlockState)state.setValue((Property)StakeBlock.CONNECTED, (Comparable)Boolean.valueOf(owner != null)), 3);
            }
            this.level.sendBlockUpdated(this.worldPosition, this.getBlockState(), this.getBlockState(), 3);
        }
    }

    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        tag.putBoolean("Active", this.owner != null);
        if (this.owner != null) {
            tag.putUUID("Owner", this.owner);
        }
        return tag;
    }

    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        this.owner = this.level != null && this.level.isClientSide && tag.hasUUID("Owner") ? tag.getUUID("Owner") : null;
    }

    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create((BlockEntity)this);
    }

    public void onLoad() {
        super.onLoad();
        if (this.level != null && !this.level.isClientSide && this.owner == null && ((Boolean)this.getBlockState().getValue((Property)StakeBlock.CONNECTED)).booleanValue()) {
            this.level.setBlock(this.worldPosition, (BlockState)this.getBlockState().setValue((Property)StakeBlock.CONNECTED, (Comparable)Boolean.valueOf(false)), 3);
        }
    }

    public void setRemoved() {
        TetherManager.releaseStake(this);
        super.setRemoved();
    }
}
