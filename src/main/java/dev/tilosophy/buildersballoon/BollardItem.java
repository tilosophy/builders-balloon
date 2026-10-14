package dev.tilosophy.buildersballoon;

import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.BlockItemStateProperties;
import net.minecraft.world.level.block.Block;

/** Coloured decorative item; cosmetic rope state is never stored on its drops. */
public final class BollardItem extends BlockItem {
    public BollardItem(Block block,Properties properties) { super(block,properties); }
    public static ItemStack stack(DyeColor color) {
        return stack(BuildersBalloon.BOLLARD.get(),color);
    }
    public static ItemStack stack(Block block,DyeColor color) {
        ItemStack stack=new ItemStack(block.asItem());
        stack.set(DataComponents.BLOCK_STATE,BlockItemStateProperties.EMPTY.with(StakeBlock.COLOR,color));
        return stack;
    }
    @Override public Component getName(ItemStack stack) {
        return Component.translatable("item.builders_balloon."+net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(this).getPath()+"."+StakeItem.color(stack).getSerializedName());
    }
}
