package dev.tilosophy.buildersballoon;

import dev.tilosophy.buildersballoon.BalloonConfig;
import dev.tilosophy.buildersballoon.BalloonNetwork;
import dev.tilosophy.buildersballoon.StakeBlock;
import dev.tilosophy.buildersballoon.StakeBlockEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.IConfigSpec;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

@Mod(value="builders_balloon")
public final class BuildersBalloon {
    public static final String MOD_ID = "builders_balloon";
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks((String)"builders_balloon");
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems((String)"builders_balloon");
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create((ResourceKey)Registries.BLOCK_ENTITY_TYPE, (String)"builders_balloon");
    public static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create((ResourceKey)Registries.SOUND_EVENT, (String)"builders_balloon");
    public static final DeferredBlock<StakeBlock> STAKE = BLOCKS.registerBlock("balloon_tether", StakeBlock::new, net.minecraft.world.level.block.state.BlockBehaviour.Properties.of().strength(0.3f, 2.0f).sound(net.minecraft.world.level.block.SoundType.WOOD).noOcclusion());
    public static final DeferredItem<StakeItem> STAKE_ITEM = ITEMS.register("balloon_tether", () -> new StakeItem(STAKE.get(), new Item.Properties().stacksTo(64).component(net.minecraft.core.component.DataComponents.BLOCK_STATE, net.minecraft.world.item.component.BlockItemStateProperties.EMPTY.with(StakeBlock.COLOR, net.minecraft.world.item.DyeColor.WHITE))));
    public static final DeferredBlock<BollardBlock> BOLLARD = BLOCKS.registerBlock("bollard", BollardBlock::new, net.minecraft.world.level.block.state.BlockBehaviour.Properties.of().strength(0.3f, 2.0f).sound(net.minecraft.world.level.block.SoundType.WOOD).noOcclusion());
    public static final DeferredItem<BollardItem> BOLLARD_ITEM = ITEMS.register("bollard", () -> new BollardItem(BOLLARD.get(), new Item.Properties().stacksTo(64).component(net.minecraft.core.component.DataComponents.BLOCK_STATE, net.minecraft.world.item.component.BlockItemStateProperties.EMPTY.with(StakeBlock.COLOR, net.minecraft.world.item.DyeColor.WHITE))));
    public static final DeferredBlock<StakeBlock> METAL_STAKE = BLOCKS.registerBlock("metal_balloon_tether", StakeBlock::new, net.minecraft.world.level.block.state.BlockBehaviour.Properties.of().strength(0.8f, 1200.0f).sound(net.minecraft.world.level.block.SoundType.METAL).noOcclusion());
    public static final DeferredItem<StakeItem> METAL_STAKE_ITEM = ITEMS.register("metal_balloon_tether", () -> new StakeItem(METAL_STAKE.get(), new Item.Properties().stacksTo(64).component(net.minecraft.core.component.DataComponents.BLOCK_STATE, net.minecraft.world.item.component.BlockItemStateProperties.EMPTY.with(StakeBlock.COLOR, net.minecraft.world.item.DyeColor.WHITE))));
    public static final DeferredBlock<BollardBlock> METAL_BOLLARD = BLOCKS.registerBlock("metal_bollard", BollardBlock::new, net.minecraft.world.level.block.state.BlockBehaviour.Properties.of().strength(0.8f, 1200.0f).sound(net.minecraft.world.level.block.SoundType.METAL).noOcclusion());
    public static final DeferredItem<BollardItem> METAL_BOLLARD_ITEM = ITEMS.register("metal_bollard", () -> new BollardItem(METAL_BOLLARD.get(), new Item.Properties().stacksTo(64).component(net.minecraft.core.component.DataComponents.BLOCK_STATE, net.minecraft.world.item.component.BlockItemStateProperties.EMPTY.with(StakeBlock.COLOR, net.minecraft.world.item.DyeColor.WHITE))));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<StakeBlockEntity>> STAKE_ENTITY = BLOCK_ENTITIES.register("balloon_tether", () -> BlockEntityType.Builder.of(StakeBlockEntity::new, (Block[])new Block[]{STAKE.get(), METAL_STAKE.get()}).build(null));
    public static final DeferredHolder<SoundEvent, SoundEvent> INFLATE = SOUNDS.register("balloon_inflate", () -> SoundEvent.createVariableRangeEvent((ResourceLocation)BuildersBalloon.id("balloon_inflate")));
    public static final DeferredHolder<SoundEvent, SoundEvent> POP = SOUNDS.register("balloon_pop", () -> SoundEvent.createVariableRangeEvent((ResourceLocation)BuildersBalloon.id("balloon_pop")));

    public BuildersBalloon(IEventBus bus, ModContainer container) {
        BLOCKS.register(bus);
        ITEMS.register(bus);
        BLOCK_ENTITIES.register(bus);
        SOUNDS.register(bus);
        bus.addListener(BalloonNetwork::register);
        bus.addListener(BuildersBalloon::creativeTab);
        container.registerConfig(ModConfig.Type.SERVER, (IConfigSpec)BalloonConfig.SPEC);
    }

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath((String)MOD_ID, (String)path);
    }

    private static void creativeTab(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.FUNCTIONAL_BLOCKS) {
            for (var color : net.minecraft.world.item.DyeColor.values()) { event.accept(StakeItem.stack(color)); event.accept(StakeItem.stack(METAL_STAKE.get(), color)); }
        }
        if (event.getTabKey() == CreativeModeTabs.BUILDING_BLOCKS) {
            for (var color : net.minecraft.world.item.DyeColor.values()) { event.accept(BollardItem.stack(color)); event.accept(BollardItem.stack(METAL_BOLLARD.get(), color)); }
        }
    }
}
