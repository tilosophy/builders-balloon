package dev.tilosophy.buildersballoon;

import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.BlockItemStateProperties;
import net.minecraft.world.level.block.Block;

/** One item ID; vanilla block-state components carry the dye colour. */
public final class StakeItem extends BlockItem {
    public StakeItem(Block block, Properties properties) { super(block, properties); }

    public static DyeColor color(ItemStack stack) {
        DyeColor color = stack.getOrDefault(DataComponents.BLOCK_STATE, BlockItemStateProperties.EMPTY).get(StakeBlock.COLOR);
        return color == null ? DyeColor.WHITE : color;
    }

    public static ItemStack stack(DyeColor color) {
        return stack(BuildersBalloon.STAKE.get(), color);
    }

    public static ItemStack stack(Block block, DyeColor color) {
        ItemStack stack = new ItemStack(block.asItem());
        stack.set(DataComponents.BLOCK_STATE, BlockItemStateProperties.EMPTY.with(StakeBlock.COLOR, color));
        return stack;
    }

    @Override public Component getName(ItemStack stack) {
        return Component.translatable("item.builders_balloon." + net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(this).getPath() + "." + color(stack).getSerializedName());
    }
}
