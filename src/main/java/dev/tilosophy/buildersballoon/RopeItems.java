package dev.tilosophy.buildersballoon;

import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;

/** Shared data-pack-extensible ingredient and cosmetic interaction tag. */
public final class RopeItems {
    private RopeItems() {}
    public static final TagKey<Item> SUPPORTED=TagKey.create(Registries.ITEM,BuildersBalloon.id("ropes"));
}
