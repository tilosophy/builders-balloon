package dev.tilosophy.buildersballoon.test;

import com.mojang.authlib.GameProfile;
import dev.tilosophy.buildersballoon.*;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.Connection;
import net.minecraft.network.PacketSendListener;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.NeoForgeMod;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.level.ChunkEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** Recreated recovery tests. Synthetic connections; no real client or visual claims. */
@GameTestHolder("builders_balloon_tests")
@PrefixGameTestTemplate(false)
public final class StakeGameTests {
    @GameTest(template = "empty")
    public static void shorterAnchorPicking(GameTestHelper h) {
        BlockPos pos=h.absolutePos(new BlockPos(2,2,2));
        for (var direction:net.minecraft.core.Direction.values()) {
            var state=BuildersBalloon.STAKE.get().defaultBlockState().setValue(StakeBlock.DIRECTION,direction);
            var shape=state.getShape(h.getLevel(),pos);
            var axis=direction.getAxis();var bounds=shape.bounds();
            double min=bounds.min(axis),max=bounds.max(axis);
            h.assertTrue(Math.abs(max-min-.75)<1e-9,"Anchor length is 12 pixels: "+direction);
            boolean positive=direction.getAxisDirection()==net.minecraft.core.Direction.AxisDirection.POSITIVE;
            h.assertTrue(positive ? max==1 : min==0,"Mounted base touches support face: "+direction);
            var transverse=axis==net.minecraft.core.Direction.Axis.X ? net.minecraft.core.Direction.Axis.Z : net.minecraft.core.Direction.Axis.X;
            var origin=Vec3.atLowerCornerOf(pos);
            double[] miss={.5,.5,.5};miss[axis.ordinal()]=positive?.125:.875;
            double[] hit={.5,.5,.5};
            var from=new Vec3(miss[0],miss[1],miss[2]).with(transverse,-1).add(origin);
            var to=new Vec3(miss[0],miss[1],miss[2]).with(transverse,2).add(origin);
            h.assertTrue(shape.clip(from,to,pos)==null,"Empty quarter cannot be selected: "+direction);
            from=new Vec3(hit[0],hit[1],hit[2]).with(transverse,-1).add(origin);
            to=new Vec3(hit[0],hit[1],hit[2]).with(transverse,2).add(origin);
            h.assertTrue(shape.clip(from,to,pos)!=null,"Anchor body remains selectable: "+direction);
        }
        h.succeed();
    }

    @GameTest(template="empty")
    public static void placementSpinAndReactivation(GameTestHelper h) {
        var pos=h.absolutePos(new BlockPos(5,3,5));var p=player(h,pos);
        h.assertTrue(BuildersBalloon.STAKE.get().defaultBlockState().getValue(StakeBlock.SPIN)==0,"Legacy default orientation stays zero");
        for (var mount:net.minecraft.core.Direction.values()) {
            for (int spin=0;spin<4;spin++) {
                var front=AnchorOrientation.spinLocal(new Vec3(0,0,3),spin);
                var offset=switch (mount) {
                    case DOWN -> front;
                    case UP -> new Vec3(front.x,0,-front.z);
                    case NORTH -> new Vec3(-front.x,front.z,0);
                    case SOUTH -> new Vec3(front.x,front.z,0);
                    case WEST -> new Vec3(0,front.z,front.x);
                    case EAST -> new Vec3(0,front.z,-front.x);
                };
                var eye=Vec3.atCenterOf(pos).add(offset).add(mount.getOpposite().getStepX()*3,mount.getOpposite().getStepY()*3,mount.getOpposite().getStepZ()*3);
                p.setPos(eye.subtract(0,p.getEyeHeight(),0));
                var support=pos.relative(mount);h.getLevel().setBlock(support,Blocks.STONE.defaultBlockState(),3);
                var hit=new net.minecraft.world.phys.BlockHitResult(Vec3.atCenterOf(support),mount.getOpposite(),support,false);
                var context=new net.minecraft.world.item.context.BlockPlaceContext(p,InteractionHand.MAIN_HAND,StakeItem.stack(net.minecraft.world.item.DyeColor.BLUE),hit);
                var state=BuildersBalloon.STAKE.get().getStateForPlacement(context);
                h.assertTrue(context.getClickedPos().equals(pos) && state.getValue(StakeBlock.DIRECTION)==mount && state.getValue(StakeBlock.SPIN)==spin,"Placement knot faces player: "+mount+"/"+spin);
                h.getLevel().setBlock(pos,state,3);var anchor=(StakeBlockEntity)h.getLevel().getBlockEntity(pos);
                TetherManager.activate(p,anchor);TetherManager.release(p,false);
                p.setPos(Vec3.atCenterOf(pos).subtract(offset));TetherManager.activate(p,anchor);
                h.assertTrue(anchor.getBlockState().getValue(StakeBlock.SPIN)==spin,"Reactivation from another side cannot rotate knot");
                TetherManager.release(p,false);h.getLevel().setBlock(pos,Blocks.AIR.defaultBlockState(),3);
                h.getLevel().setBlock(support,Blocks.AIR.defaultBlockState(),3);
            }
        }
        h.succeed();
    }

    @GameTest(template="empty")
    public static void renamedIdentityAndPersistence(GameTestHelper h) {
        var id=BuildersBalloon.id("balloon_tether");
        h.assertTrue(id.toString().equals("builders_balloon:balloon_tether"),"Final public namespace");
        var oldId=net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("balloon_on_a_stick","balloon_tether");
        h.assertTrue(!net.minecraft.core.registries.BuiltInRegistries.BLOCK.containsKey(oldId)
            && !net.minecraft.core.registries.BuiltInRegistries.ITEM.containsKey(oldId)
            && !net.minecraft.core.registries.BuiltInRegistries.BLOCK_ENTITY_TYPE.containsKey(oldId),"Deliberate break has no legacy registry aliases");
        h.assertTrue(BuildersBalloon.INFLATE.get().getLocation().getNamespace().equals("builders_balloon")
            && BuildersBalloon.POP.get().getLocation().getNamespace().equals("builders_balloon")
            && BalloonNetwork.BindingPayload.TYPE.id().getNamespace().equals("builders_balloon")
            && BalloonNetwork.PopPayload.TYPE.id().getNamespace().equals("builders_balloon"),"Sounds and payloads use final namespace");
        var pos=h.absolutePos(new BlockPos(2,2,2));
        var state=BuildersBalloon.STAKE.get().defaultBlockState().setValue(StakeBlock.COLOR,net.minecraft.world.item.DyeColor.CYAN).setValue(StakeBlock.SPIN,3);
        var stateTag=net.minecraft.nbt.NbtUtils.writeBlockState(state);
        h.assertTrue(stateTag.getString("Name").equals(id.toString()),"Saved palette uses renamed ID");
        var restored=net.minecraft.nbt.NbtUtils.readBlockState(h.getLevel().holderLookup(net.minecraft.core.registries.Registries.BLOCK),stateTag);
        h.assertTrue(restored.equals(state),"Renamed palette roundtrip retains colour and spin");
        h.getLevel().setBlock(pos,state,3);
        var anchor=(StakeBlockEntity)h.getLevel().getBlockEntity(pos);
        var entityTag=anchor.saveWithFullMetadata(h.getLevel().registryAccess());
        h.assertTrue(entityTag.getString("id").equals(id.toString()),"Saved block entity uses renamed ID");
        var stack=StakeItem.stack(net.minecraft.world.item.DyeColor.CYAN);
        var itemTag=(net.minecraft.nbt.CompoundTag)stack.save(h.getLevel().registryAccess());
        h.assertTrue(itemTag.getString("id").equals(id.toString()),"Saved item uses renamed ID");
        h.assertTrue(ItemStack.isSameItemSameComponents(stack,ItemStack.parse(h.getLevel().registryAccess(),itemTag).orElseThrow()),"Renamed item roundtrip retains colour");
        h.succeed();
    }

    static net.minecraft.world.item.crafting.CraftingInput bollardInput(net.minecraft.world.item.Item log) {
        return net.minecraft.world.item.crafting.CraftingInput.of(1,2,java.util.List.of(new ItemStack(log),new ItemStack(Items.IRON_INGOT)));
    }
    static void toggleBollard(GameTestHelper h,BlockPos pos,ServerPlayer player,ItemStack rope,InteractionHand hand) {
        player.setItemInHand(hand,rope);
        var state=h.getLevel().getBlockState(pos);
        var hit=new net.minecraft.world.phys.BlockHitResult(Vec3.atCenterOf(pos),net.minecraft.core.Direction.UP,pos,false);
        var result=player.gameMode.useItemOn(player,h.getLevel(),rope,hand,hit);
        h.assertTrue(result.consumesAction(),"Supported rope consumes the interaction");
    }
    @GameTest(template="empty") public static void decorativePlacementAndDrops(GameTestHelper h) {
        var pos=h.absolutePos(new BlockPos(2,3,2));var player=player(h,pos);
        for(var color:net.minecraft.world.item.DyeColor.values()) {
            for(var mount:net.minecraft.core.Direction.values()) {
                h.getLevel().setBlock(pos,Blocks.AIR.defaultBlockState(),3);
                var support=pos.relative(mount);h.getLevel().setBlock(support,Blocks.STONE.defaultBlockState(),3);
                var item=BollardItem.stack(color);
                var hit=new net.minecraft.world.phys.BlockHitResult(Vec3.atCenterOf(support),mount.getOpposite(),support,false);
                var context=new net.minecraft.world.item.context.BlockPlaceContext(player,InteractionHand.MAIN_HAND,item,hit);
                h.assertTrue(BuildersBalloon.BOLLARD_ITEM.get().place(context).consumesAction(),"Decorative real placement: "+color+"/"+mount);
                var state=h.getLevel().getBlockState(pos);
                h.assertTrue(state.is(BuildersBalloon.BOLLARD.get()) && state.getValue(StakeBlock.COLOR)==color
                    && state.getValue(StakeBlock.DIRECTION)==mount && !state.getValue(BollardBlock.ROPED),"Colour/face/unroped placement");
                h.assertTrue(h.getLevel().getBlockEntity(pos)==null && !player.mayFly() && TetherManager.boundaryFor(player)==null,"Decoration has no block entity or flight");
                for(boolean roped:new boolean[]{false,true}) {
                    var variant=state.setValue(BollardBlock.ROPED,roped).setValue(StakeBlock.SPIN,3).setValue(StakeBlock.WATERLOGGED,true);
                    h.getLevel().setBlock(pos,variant,3);
                    var tag=net.minecraft.nbt.NbtUtils.writeBlockState(variant);
                    h.assertTrue(net.minecraft.nbt.NbtUtils.readBlockState(h.getLevel().holderLookup(net.minecraft.core.registries.Registries.BLOCK),tag).equals(variant),"Decorative state survives save/load");
                    var drops=net.minecraft.world.level.block.Block.getDrops(variant,h.getLevel(),pos,null,player,ItemStack.EMPTY);
                    h.assertTrue(drops.size()==1 && drops.getFirst().is(BuildersBalloon.BOLLARD_ITEM.get()) && drops.getFirst().getCount()==1
                        && StakeItem.color(drops.getFirst())==color,"Decorative drop is one correct-colour bollard, no rope");
                    var props=drops.getFirst().getOrDefault(net.minecraft.core.component.DataComponents.BLOCK_STATE,net.minecraft.world.item.component.BlockItemStateProperties.EMPTY);
                    h.assertTrue(props.get(BollardBlock.ROPED)==null,"Drop does not carry cosmetic rope state");
                    var picked=BuildersBalloon.BOLLARD.get().getCloneItemStack(variant,hit,h.getLevel(),pos,player);
                    h.assertTrue(picked.is(BuildersBalloon.BOLLARD_ITEM.get()) && StakeItem.color(picked)==color,"Pick-block preserves decorative item/colour");
                }
                h.getLevel().setBlock(pos,Blocks.AIR.defaultBlockState(),3);h.getLevel().setBlock(support,Blocks.AIR.defaultBlockState(),3);
            }
        }
        h.succeed();
    }
    @GameTest(template="empty") public static void decorativeToggleAndBindingIsolation(GameTestHelper h) {
        var pos=h.absolutePos(new BlockPos(2,2,2));var player=player(h,pos);
        h.getLevel().setBlock(pos,BuildersBalloon.BOLLARD.get().defaultBlockState().setValue(StakeBlock.COLOR,net.minecraft.world.item.DyeColor.RED).setValue(StakeBlock.SPIN,2),3);
        var stack=new ItemStack(Items.LEAD,3);var initial=stack.copy();
        toggleBollard(h,pos,player,stack,InteractionHand.MAIN_HAND);
        h.assertTrue(h.getLevel().getBlockState(pos).getValue(BollardBlock.ROPED),"First click ropes bollard");
        toggleBollard(h,pos,player,stack,InteractionHand.MAIN_HAND);
        h.assertTrue(!h.getLevel().getBlockState(pos).getValue(BollardBlock.ROPED) && ItemStack.isSameItemSameComponents(stack,initial) && stack.getCount()==3,"Second click unropes, unchanged held lead");
        toggleBollard(h,pos,player,stack,InteractionHand.OFF_HAND);
        h.assertTrue(h.getLevel().getBlockState(pos).getValue(BollardBlock.ROPED) && stack.getCount()==3,"Offhand supported, non-consuming");
        var state=h.getLevel().getBlockState(pos);var hit=new net.minecraft.world.phys.BlockHitResult(Vec3.atCenterOf(pos),net.minecraft.core.Direction.UP,pos,false);
        state.useItemOn(new ItemStack(Items.STICK),h.getLevel(),player,InteractionHand.MAIN_HAND,hit);
        h.assertTrue(h.getLevel().getBlockState(pos).equals(state) && !player.mayFly(),"Unrelated item and decorative clicks grant no flight");
        var anchor=stake(h,pos.offset(3,0,0),player);
        try {
            toggleBollard(h,pos,player,stack,InteractionHand.MAIN_HAND);
            h.assertTrue(player.getUUID().equals(anchor.owner()) && player.mayFly() && TetherManager.boundaryFor(player)!=null,"Cosmetic toggle preserves active functional binding");
            h.assertTrue(h.getLevel().getBlockState(pos).getValue(StakeBlock.COLOR)==net.minecraft.world.item.DyeColor.RED
                && h.getLevel().getBlockState(pos).getValue(StakeBlock.SPIN)==2,"Toggle preserves colour and knot side");
        } finally {TetherManager.release(player,false);}
        h.succeed();
    }
    @GameTest(template="empty") public static void decorativeRopeSounds(GameTestHelper h) {
        var pos=h.absolutePos(new BlockPos(2,2,2));var player=player(h,pos);
        var heard=new java.util.ArrayList<net.minecraft.sounds.SoundEvent>();
        java.util.function.Consumer<net.neoforged.neoforge.event.PlayLevelSoundEvent.AtPosition> listener=event -> {
            if(event.getLevel()==h.getLevel() && event.getPosition().equals(Vec3.atCenterOf(pos))) {
                h.assertTrue(event.getSource()==net.minecraft.sounds.SoundSource.BLOCKS
                    && event.getOriginalVolume()==0.75f && event.getOriginalPitch()==1.0f,"Vanilla knot sounds at 75% volume, unchanged pitch");
                heard.add(event.getSound().value());
            }
        };
        NeoForge.EVENT_BUS.addListener(listener);
        try {
            for(var block:new net.minecraft.world.level.block.Block[]{BuildersBalloon.BOLLARD.get(),BuildersBalloon.METAL_BOLLARD.get()}) {
                h.getLevel().setBlock(pos,block.defaultBlockState(),3);heard.clear();
                var lead=new ItemStack(Items.LEAD,3);
                toggleBollard(h,pos,player,lead,InteractionHand.MAIN_HAND);
                toggleBollard(h,pos,player,lead,InteractionHand.OFF_HAND);
                h.assertTrue(heard.equals(java.util.List.of(net.minecraft.sounds.SoundEvents.LEASH_KNOT_PLACE,
                    net.minecraft.sounds.SoundEvents.LEASH_KNOT_BREAK)),"Exactly one appropriate sound per toggle for both materials");
                h.assertTrue(!h.getLevel().getBlockState(pos).getValue(BollardBlock.ROPED) && lead.getCount()==3,"Sound does not change rope behaviour");
                heard.clear();var state=h.getLevel().getBlockState(pos);
                var hit=new net.minecraft.world.phys.BlockHitResult(Vec3.atCenterOf(pos),net.minecraft.core.Direction.UP,pos,false);
                state.useItemOn(new ItemStack(Items.STICK),h.getLevel(),player,InteractionHand.MAIN_HAND,hit);
                player.getAbilities().mayBuild=false;
                try {state.useItemOn(lead,h.getLevel(),player,InteractionHand.MAIN_HAND,hit);}
                finally {player.getAbilities().mayBuild=true;}
                h.assertTrue(heard.isEmpty() && h.getLevel().getBlockState(pos).equals(state),"Rejected interactions stay silent");
            }
        } finally {NeoForge.EVENT_BUS.unregister(listener);}
        h.succeed();
    }
    @GameTest(template="empty") public static void decorativeRecipeAndWhiteDefaults(GameTestHelper h) {
        h.assertTrue(StakeItem.color(new ItemStack(BuildersBalloon.STAKE_ITEM.get()))==net.minecraft.world.item.DyeColor.WHITE
            && StakeItem.color(new ItemStack(BuildersBalloon.BOLLARD_ITEM.get()))==net.minecraft.world.item.DyeColor.WHITE,"Both default items are white");
        h.assertTrue(BuildersBalloon.STAKE.get().defaultBlockState().getValue(StakeBlock.COLOR)==net.minecraft.world.item.DyeColor.WHITE
            && BuildersBalloon.BOLLARD.get().defaultBlockState().getValue(StakeBlock.COLOR)==net.minecraft.world.item.DyeColor.WHITE,"Both default block palettes are white");
        var componentFree=new ItemStack(BuildersBalloon.STAKE_ITEM.get());componentFree.remove(net.minecraft.core.component.DataComponents.BLOCK_STATE);
        h.assertTrue(StakeItem.color(componentFree)==net.minecraft.world.item.DyeColor.WHITE,"No-component fallback white");
        for(var log:new net.minecraft.world.item.Item[]{Items.OAK_LOG,Items.STRIPPED_OAK_LOG,Items.OAK_WOOD,Items.CRIMSON_STEM,Items.WARPED_HYPHAE}) {
            var result=craft(h,bollardInput(log));
            h.assertTrue(result.is(BuildersBalloon.BOLLARD_ITEM.get()) && result.getCount()==2 && StakeItem.color(result)==net.minecraft.world.item.DyeColor.WHITE,"Log/iron makes two white decorative bollards");
        }
        h.assertTrue(craft(h,bollardInput(Items.OAK_PLANKS)).isEmpty(),"Planks are not logs");
        var wrong=net.minecraft.world.item.crafting.CraftingInput.of(1,2,java.util.List.of(new ItemStack(Items.OAK_LOG),ItemStack.EMPTY));
        h.assertTrue(!craft(h,wrong).is(BuildersBalloon.BOLLARD_ITEM.get()),"Iron required; a log alone may still craft vanilla planks");
        var registry=h.getLevel().registryAccess();
        for(var color:net.minecraft.world.item.DyeColor.values()) {
            var input=net.minecraft.world.item.crafting.CraftingInput.of(2,1,java.util.List.of(BollardItem.stack(net.minecraft.world.item.DyeColor.BLUE),new ItemStack(net.minecraft.world.item.DyeItem.byColor(color))));
            var dyed=craft(h,input);
            h.assertTrue(dyed.is(BuildersBalloon.BOLLARD_ITEM.get()) && dyed.getCount()==1 && StakeItem.color(dyed)==color,"Decorative recolour retains type");
            var item=BollardItem.stack(color);h.assertTrue(ItemStack.isSameItemSameComponents(item,ItemStack.parse(registry,item.save(registry)).orElseThrow()),"All decorative item colours survive persistence");
            var functional=StakeItem.stack(color);h.assertTrue(ItemStack.isSameItemSameComponents(functional,ItemStack.parse(registry,functional.save(registry)).orElseThrow()),"Explicit functional colour remains stored");
        }
        h.succeed();
    }
    @GameTest(template="empty") public static void optionalRopeIngredientAndToggle(GameTestHelper h) {
        var pos=h.absolutePos(new BlockPos(2,2,2));var player=player(h,pos);
        h.assertTrue(new ItemStack(Items.LEAD).is(RopeItems.SUPPORTED),"Vanilla lead always supported");
        for(String namespace:new String[]{"supplementaries","farmersdelight","quark"}) {
            var id=net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(namespace,"rope");
            var exists=net.minecraft.core.registries.BuiltInRegistries.ITEM.containsKey(id);
            if((Boolean.getBoolean("balloon.tests.requireRopes") && !namespace.equals("quark")) || (Boolean.getBoolean("balloon.tests.requireQuark") && namespace.equals("quark")))h.assertTrue(exists,"Real optional rope mod loaded: "+namespace);
            if(!exists)continue;
            var item=net.minecraft.core.registries.BuiltInRegistries.ITEM.get(id);var rope=new ItemStack(item,3);
            h.assertTrue(rope.is(RopeItems.SUPPORTED),"Optional rope in shared tag: "+namespace);
            var input=net.minecraft.world.item.crafting.CraftingInput.of(3,3,java.util.List.of(ItemStack.EMPTY,new ItemStack(Items.RED_WOOL),ItemStack.EMPTY,new ItemStack(item),new ItemStack(Items.OAK_LOG),new ItemStack(Items.LEAD),ItemStack.EMPTY,new ItemStack(Items.IRON_INGOT),ItemStack.EMPTY));
            var result=craft(h,input);h.assertTrue(result.is(BuildersBalloon.STAKE_ITEM.get()) && StakeItem.color(result)==net.minecraft.world.item.DyeColor.RED,"Mixed rope/lead crafting preserves wool colour: "+namespace);
            var metalInput=net.minecraft.world.item.crafting.CraftingInput.of(3,3,java.util.List.of(ItemStack.EMPTY,new ItemStack(Items.RED_WOOL),ItemStack.EMPTY,new ItemStack(item),new ItemStack(Items.IRON_BLOCK),new ItemStack(Items.LEAD),ItemStack.EMPTY,new ItemStack(Items.IRON_INGOT),ItemStack.EMPTY));
            var metalResult=craft(h,metalInput);h.assertTrue(metalResult.is(BuildersBalloon.METAL_STAKE_ITEM.get()) && StakeItem.color(metalResult)==net.minecraft.world.item.DyeColor.RED,"Mixed rope/lead metal crafting: "+namespace);
            for(var block:new net.minecraft.world.level.block.Block[]{BuildersBalloon.BOLLARD.get(),BuildersBalloon.METAL_BOLLARD.get()}) {
                h.getLevel().setBlock(pos,block.defaultBlockState(),3);
                toggleBollard(h,pos,player,rope,InteractionHand.MAIN_HAND);toggleBollard(h,pos,player,rope,InteractionHand.MAIN_HAND);
                h.assertTrue(!h.getLevel().getBlockState(pos).getValue(BollardBlock.ROPED) && rope.getCount()==3,"Optional rope toggles both materials twice, not consumed: "+namespace);
            }
        }
        h.succeed();
    }

    public static ServerPlayer player(GameTestHelper h, BlockPos pos) {
        return player(h, pos, packet -> {});
    }
    public static ServerPlayer player(GameTestHelper h, BlockPos pos, java.util.function.Consumer<Packet<?>> outgoing) {
        var profile = new GameProfile(UUID.randomUUID(), "balloon-test");
        var p = new ServerPlayer(h.getLevel().getServer(), h.getLevel(), profile, ClientInformation.createDefault());
        new ServerGamePacketListenerImpl(h.getLevel().getServer(), new Connection(PacketFlow.SERVERBOUND), p, CommonListenerCookie.createInitial(profile, false)) {
            @Override public void send(Packet<?> packet, PacketSendListener listener) { outgoing.accept(packet); }
        };
        p.gameMode.changeGameModeForPlayer(GameType.SURVIVAL);
        p.setPos(Vec3.atCenterOf(pos).add(2, 1, 0));
        p.setNoGravity(true);
        return p;
    }
    public static StakeBlockEntity inactive(GameTestHelper h, BlockPos pos) {
        h.getLevel().setBlock(pos, BuildersBalloon.STAKE.get().defaultBlockState(), 3);
        return (StakeBlockEntity) h.getLevel().getBlockEntity(pos);
    }
    static StakeBlockEntity stake(GameTestHelper h, BlockPos pos, ServerPlayer p) {
        var s = inactive(h, pos);
        BuildersBalloon.STAKE.get().setPlacedBy(h.getLevel(), pos, s.getBlockState(), p, new ItemStack(BuildersBalloon.STAKE_ITEM.get()));
        return s;
    }
    @GameTest(template="empty") public static void lifecycle(GameTestHelper h) {
        var pos = h.absolutePos(new BlockPos(2,2,2)); var p = player(h,pos);
        var a = stake(h,pos,p);
        try {
            h.assertTrue(p.mayFly() && p.getUUID().equals(a.owner()), "Placement binds and grants flight");
            p.getAbilities().flying=true;
            var b = stake(h,pos.offset(3,0,0),p);
            h.assertTrue(a.owner()==null && p.getUUID().equals(b.owner()) && p.getAbilities().flying, "Replacement preserves flight and clears old owner");
            h.getLevel().removeBlock(a.getBlockPos(),false);
            h.assertTrue(p.getUUID().equals(b.owner()), "Removing old anchor cannot release current one");
            p.fallDistance=12;
            TetherManager.release(p,true);
            h.assertTrue(b.owner()==null && !p.mayFly() && p.fallDistance==0, "Pop clears binding, flight and fall distance once");
            p.fallDistance=7; TetherManager.release(p,true);
            h.assertTrue(p.fallDistance==7, "Repeated release cannot grant immunity");
            h.assertTrue(h.getLevel().getBlockState(b.getBlockPos()).is(BuildersBalloon.STAKE.get()), "Pop leaves reusable stake");
            h.succeed();
        } finally {TetherManager.release(p,false);}
    }
    static final class OwnerProbe extends BlockEntity {
        UUID owner;
        OwnerProbe(StakeBlockEntity s) {super(BuildersBalloon.STAKE_ENTITY.get(),s.getBlockPos(),s.getBlockState());}
        @Override protected void loadAdditional(net.minecraft.nbt.CompoundTag t, net.minecraft.core.HolderLookup.Provider r) {
            super.loadAdditional(t,r); owner=t.hasUUID("Owner")?t.getUUID("Owner"):null;
        }
    }
    @GameTest(template="empty") public static void ownerPacketsAndDisk(GameTestHelper h) {
        var pos=h.absolutePos(new BlockPos(2,2,2));var p=player(h,pos);var a=stake(h,pos,p);
        var r=h.getLevel().registryAccess();var probe=new OwnerProbe(a);
        probe.onDataPacket(null,a.getUpdatePacket(),r);
        h.assertTrue(p.getUUID().equals(probe.owner), "Active packet reaches default consumer");
        h.assertTrue(!a.saveWithFullMetadata(r).hasUUID("Owner"), "Disk never stores active ownership");
        TetherManager.release(p,true);
        h.assertTrue(!a.getUpdateTag(r).isEmpty(), "Inactive update is not empty");
        probe.onDataPacket(null,a.getUpdatePacket(),r);
        h.assertTrue(probe.owner==null, "Inactive packet clears cached owner");h.succeed();
    }
    @GameTest(template="empty") public static void movement(GameTestHelper h) {
        var pos=h.absolutePos(new BlockPos(2,2,2));var p=player(h,pos);stake(h,pos,p);
        try {
            p.noPhysics=true;var b=TetherManager.boundaryFor(p);
            p.setPos(b.x()+b.range(),b.y(),b.z());p.move(MoverType.SELF,new Vec3(3,0,1));
            h.assertTrue(p.getX()==b.x()+b.range() && p.getZ()==b.z()+1,"Entity.move clips outward and preserves tangent");
            p.move(MoverType.SELF,new Vec3(-1,0,0));h.assertTrue(p.getX()==b.x()+b.range()-1,"Inward motion retained");h.succeed();
        } finally {TetherManager.release(p,false);}
    }
    @GameTest(template="empty") public static void miningAndIndependentFlight(GameTestHelper h) {
        var pos=h.absolutePos(new BlockPos(2,2,2));var p=player(h,pos);var state=Blocks.OAK_LOG.defaultBlockState();
        p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.DIAMOND_AXE));
        p.getAttribute(Attributes.BLOCK_BREAK_SPEED).setBaseValue(1.75);
        p.setOnGround(true);float ground=p.getDigSpeed(state,pos);p.setOnGround(false);
        h.assertTrue(Math.abs(p.getDigSpeed(state,pos)-ground/5)<1E-5,"Unbound airborne penalty retained");
        stake(h,pos,p);
        try {
            h.assertTrue(Math.abs(p.getDigSpeed(state,pos)-ground/5)<1E-5,"Bound non-flying player keeps penalty");
            p.getAbilities().flying=true;
            h.assertTrue(Math.abs(p.getDigSpeed(state,pos)-ground)<1E-5,"Balloon flight restores ground speed");
            var other=new AttributeModifier(BuildersBalloon.id("test_other_flight"),1,AttributeModifier.Operation.ADD_VALUE);
            p.getAttribute(NeoForgeMod.CREATIVE_FLIGHT).addTransientModifier(other);
            TetherManager.release(p,true);
            h.assertTrue(p.getAbilities().flying && Math.abs(p.getDigSpeed(state,pos)-ground/5)<1E-5,"Other flight survives but mining bonus ends");
            p.getAttribute(NeoForgeMod.CREATIVE_FLIGHT).removeModifier(other.id());
            p.gameMode.changeGameModeForPlayer(GameType.CREATIVE);stake(h,pos.offset(3,0,0),p);TetherManager.release(p,true);
            h.assertTrue(p.mayFly(),"Creative flight retained");h.succeed();
        } finally {TetherManager.release(p,false);}
    }
    @GameTest(template="empty") public static void cleanupEvents(GameTestHelper h) {
        var pos=h.absolutePos(new BlockPos(2,2,2));var p=player(h,pos);var a=stake(h,pos,p);
        NeoForge.EVENT_BUS.post(new PlayerEvent.PlayerLoggedOutEvent(p));h.assertTrue(a.owner()==null&&!p.mayFly(),"Logout cleanup");
        TetherManager.bind(p,a);NeoForge.EVENT_BUS.post(new LivingDeathEvent(p,p.damageSources().generic()));h.assertTrue(a.owner()==null,"Death cleanup");
        TetherManager.bind(p,a);NeoForge.EVENT_BUS.post(new PlayerEvent.PlayerChangedDimensionEvent(p,net.minecraft.world.level.Level.OVERWORLD,net.minecraft.world.level.Level.NETHER));h.assertTrue(a.owner()==null,"Dimension cleanup");
        TetherManager.bind(p,a);NeoForge.EVENT_BUS.post(new ChunkEvent.Unload(h.getLevel().getChunkAt(pos)));h.assertTrue(a.owner()==null,"Unload cleanup");h.succeed();
    }
    static net.minecraft.world.item.crafting.CraftingInput shapedInput(net.minecraft.world.item.Item log,net.minecraft.world.item.Item wool) {
        return net.minecraft.world.item.crafting.CraftingInput.of(3,3,java.util.List.of(
            ItemStack.EMPTY,new ItemStack(wool),ItemStack.EMPTY,
            new ItemStack(Items.LEAD),new ItemStack(log),new ItemStack(Items.LEAD),
            ItemStack.EMPTY,new ItemStack(Items.IRON_INGOT),ItemStack.EMPTY));
    }
    static ItemStack craft(GameTestHelper h,net.minecraft.world.item.crafting.CraftingInput input) {
        return h.getLevel().getRecipeManager().getRecipeFor(net.minecraft.world.item.crafting.RecipeType.CRAFTING,input,h.getLevel())
            .map(recipe -> recipe.value().assemble(input,h.getLevel().registryAccess())).orElse(ItemStack.EMPTY);
    }
    @GameTest(template="empty") public static void shapedColoursAndStacking(GameTestHelper h) {
        var id=BuildersBalloon.id("balloon_tether");var oldId=BuildersBalloon.id("stake");
        h.assertTrue(net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(BuildersBalloon.STAKE_ITEM.get()).equals(id)
            && net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(BuildersBalloon.STAKE.get()).equals(id)
            && net.minecraft.core.registries.BuiltInRegistries.BLOCK_ENTITY_TYPE.getKey(BuildersBalloon.STAKE_ENTITY.get()).equals(id),"All three registrations use balloon_tether");
        h.assertTrue(!net.minecraft.core.registries.BuiltInRegistries.ITEM.containsKey(oldId)
            && !net.minecraft.core.registries.BuiltInRegistries.BLOCK.containsKey(oldId)
            && !net.minecraft.core.registries.BuiltInRegistries.BLOCK_ENTITY_TYPE.containsKey(oldId),"Old stake registrations intentionally absent");
        var p=player(h,h.absolutePos(new BlockPos(2,2,2)));
        for(var color:net.minecraft.world.item.DyeColor.values()) {
            var wool=net.minecraft.core.registries.BuiltInRegistries.ITEM.get(net.minecraft.resources.ResourceLocation.withDefaultNamespace(color.getSerializedName()+"_wool"));
            var crafted=craft(h,shapedInput(Items.STRIPPED_DARK_OAK_LOG,wool));
            h.assertTrue(crafted.is(BuildersBalloon.STAKE_ITEM.get()) && crafted.getCount()==1 && StakeItem.color(crafted)==color,"Wool determines crafted colour: "+color);
            h.assertTrue(crafted.getMaxStackSize()==64 && ItemStack.isSameItemSameComponents(crafted,StakeItem.stack(color)),"Crafted and creative variants can stack: "+color);
            p.getInventory().clearContent();var first=crafted.copy();first.setCount(63);p.getInventory().add(first);
            var second=StakeItem.stack(color);second.setCount(2);p.getInventory().add(second);
            h.assertTrue(p.getInventory().getItem(0).getCount()==64 && p.getInventory().getItem(1).getCount()==1,"Actual inventory merge stops at 64: "+color);
            var other=StakeItem.stack(color==net.minecraft.world.item.DyeColor.RED?net.minecraft.world.item.DyeColor.BLUE:net.minecraft.world.item.DyeColor.RED);
            p.getInventory().add(other.copy());
            h.assertTrue(p.getInventory().getItem(1).getCount()==1 && ItemStack.isSameItemSameComponents(p.getInventory().getItem(2),other),"Different colours occupy separate inventory stacks");
            var save=p.getInventory().getItem(0).save(h.getLevel().registryAccess());
            var loaded=ItemStack.parse(h.getLevel().registryAccess(),save).orElseThrow();
            h.assertTrue(loaded.getCount()==64 && ItemStack.isSameItemSameComponents(loaded,crafted),"Full coloured stack saves/reloads");
        }
        h.assertTrue(ItemStack.isSameItemSameComponents(new ItemStack(BuildersBalloon.STAKE_ITEM.get()),StakeItem.stack(net.minecraft.world.item.DyeColor.WHITE)),"Default and explicit white merge");
        for (var log:java.util.List.of(Items.OAK_LOG,Items.OAK_WOOD,Items.STRIPPED_OAK_LOG,Items.STRIPPED_OAK_WOOD,Items.CRIMSON_STEM,Items.WARPED_HYPHAE,Items.STRIPPED_CRIMSON_HYPHAE)) {
            h.assertTrue(StakeItem.color(craft(h,shapedInput(log,Items.BLUE_WOOL)))==net.minecraft.world.item.DyeColor.BLUE
                && !craft(h,shapedInput(log,Items.BLUE_WOOL)).isEmpty(),"Logs tag member accepted: "+log);
        }
        h.assertTrue(craft(h,shapedInput(Items.OAK_PLANKS,Items.BLUE_WOOL)).isEmpty(),"Planks are not logs");
        h.assertTrue(craft(h,shapedInput(Items.OAK_FENCE,Items.BLUE_WOOL)).isEmpty(),"Old fence ingredient rejected");
        for (int slot:new int[]{3,5,7}) {
            var missing=shapedInput(Items.OAK_LOG,Items.BLUE_WOOL).items().stream().map(ItemStack::copy).collect(java.util.stream.Collectors.toCollection(java.util.ArrayList::new));
            missing.set(slot,ItemStack.EMPTY);
            h.assertTrue(craft(h,net.minecraft.world.item.crafting.CraftingInput.of(3,3,missing)).isEmpty(),"Both leads and iron are required: "+slot);
        }
        var rotated=net.minecraft.world.item.crafting.CraftingInput.of(3,3,java.util.List.of(
            ItemStack.EMPTY,new ItemStack(Items.LEAD),ItemStack.EMPTY,
            new ItemStack(Items.IRON_INGOT),new ItemStack(Items.OAK_LOG),new ItemStack(Items.BLUE_WOOL),
            ItemStack.EMPTY,new ItemStack(Items.LEAD),ItemStack.EMPTY));
        h.assertTrue(craft(h,rotated).isEmpty(),"Recipe cannot rotate");
        var oldRecipe=net.minecraft.world.item.crafting.CraftingInput.of(3,3,java.util.List.of(
            ItemStack.EMPTY,new ItemStack(Items.OAK_SLAB),ItemStack.EMPTY,
            new ItemStack(Items.LEAD),new ItemStack(Items.OAK_FENCE),new ItemStack(Items.BLUE_WOOL),
            ItemStack.EMPTY,new ItemStack(Items.OAK_SLAB),ItemStack.EMPTY));
        h.assertTrue(craft(h,oldRecipe).isEmpty(),"Old slab/fence recipe removed");
        h.succeed();
    }
    @GameTest(template="empty") public static void recipeAndLoot(GameTestHelper h) {
        var pos=h.absolutePos(new BlockPos(2,2,2));var p=player(h,pos);var a=stake(h,pos,p);
        try {
            var input=shapedInput(Items.SPRUCE_LOG,Items.BLUE_WOOL);
            var result=h.getLevel().getRecipeManager().getRecipeFor(net.minecraft.world.item.crafting.RecipeType.CRAFTING,input,h.getLevel()).orElseThrow().value().assemble(input,h.getLevel().registryAccess());
            h.assertTrue(result.is(BuildersBalloon.STAKE_ITEM.get())&&result.getCount()==1&&result.getMaxStackSize()==64,"Shaped wooden recipe yields one tether with stack limit 64");
            var drops=net.minecraft.world.level.block.Block.getDrops(a.getBlockState(),h.getLevel(),pos,a);
            h.assertTrue(drops.size()==1&&drops.getFirst().is(BuildersBalloon.STAKE_ITEM.get())&&drops.getFirst().getCount()==1,"One stake drop");h.succeed();
        } finally {TetherManager.release(p,false);}
    }
    static net.minecraft.world.InteractionResult click(GameTestHelper h, StakeBlockEntity s, ServerPlayer p) {
        var hit=new net.minecraft.world.phys.BlockHitResult(Vec3.atCenterOf(s.getBlockPos()),net.minecraft.core.Direction.UP,s.getBlockPos(),false);
        return s.getBlockState().useWithoutItem(h.getLevel(),p,hit);
    }
    @GameTest(template="empty") public static void hopAndReactivate(GameTestHelper h) {
        var pos=h.absolutePos(new BlockPos(2,2,2));var p=player(h,pos);var a=stake(h,pos,p);
        p.getAbilities().flying=true;var b=stake(h,pos.offset(3,0,0),p);var before=p.position();
        try {
            click(h,a,p);
            h.assertTrue(p.getUUID().equals(a.owner())&&b.owner()==null&&p.getAbilities().flying&&p.position().equals(before),"Hop switches owner without teleporting or losing flight");
            var boundary=TetherManager.boundaryFor(p);click(h,a,p);
            h.assertTrue(TetherManager.boundaryFor(p)==boundary,"Clicking your active anchor does nothing");
            TetherManager.release(p,true);click(h,a,p);
            h.assertTrue(p.getUUID().equals(a.owner())&&p.mayFly(),"Popped stake reactivates");
            TetherManager.release(p,true);
            // A fresh ownerless block entity represents the inactive state restored from disk.
            var restored=inactive(h,pos.offset(6,0,0));click(h,restored,p);
            h.assertTrue(p.getUUID().equals(restored.owner()),"Inactive/reloaded stake can activate");
            h.succeed();
        } finally {TetherManager.release(p,false);}
    }
    @GameTest(template="empty") public static void occupiedAndEmptyHand(GameTestHelper h) {
        var pos=h.absolutePos(new BlockPos(2,2,2));var p=player(h,pos);var q=player(h,pos);var a=stake(h,pos,p);var b=stake(h,pos.offset(3,0,0),q);
        try {
            var before=TetherManager.boundaryFor(q);click(h,a,q);
            h.assertTrue(p.getUUID().equals(a.owner())&&q.getUUID().equals(b.owner())&&TetherManager.boundaryFor(q)==before,"Cannot take another player's occupied anchor");
            var target=inactive(h,pos.offset(6,0,0));q.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.LEAD));
            h.assertTrue(click(h,target,q)==net.minecraft.world.InteractionResult.PASS&&target.owner()==null&&q.getUUID().equals(b.owner()),"Held item must not activate or release");
            q.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);q.setItemInHand(InteractionHand.OFF_HAND,new ItemStack(Items.TORCH));
            TetherManager.release(p,true);click(h,a,q);
            h.assertTrue(q.getUUID().equals(a.owner())&&b.owner()==null,"Empty main hand can reuse another player's inactive anchor even with an offhand item");
            var r=h.getLevel().registryAccess();var probe=new OwnerProbe(a);probe.onDataPacket(null,a.getUpdatePacket(),r);click(h,target,q);probe.onDataPacket(null,a.getUpdatePacket(),r);
            h.assertTrue(probe.owner==null&&q.getUUID().equals(target.owner()),"Hop publishes inactive ownership for old renderer");
            h.succeed();
        } finally {TetherManager.release(p,false);TetherManager.release(q,false);}
    }
    @GameTest(template="empty") public static void rangeEndpointsAndPayload(GameTestHelper h) {
        int old=BalloonConfig.RANGE.get();var pos=h.absolutePos(new BlockPos(2,2,2));var p=player(h,pos);
        try {
            for (int range:new int[]{8,32}) {
                BalloonConfig.RANGE.set(range);var anchor=stake(h,pos,p);var b=TetherManager.boundaryFor(p);
                h.assertTrue(b.range()==range && b.contains(b.x()+range,b.y(),b.z()) && !b.contains(b.x()+range+.01,b.y(),b.z()),"Server binds exact configured range: "+range);
                var payload=new BalloonNetwork.BindingPayload(true,p.level().dimension().location(),pos,range,null);
                var buffer=new net.minecraft.network.RegistryFriendlyByteBuf(io.netty.buffer.Unpooled.buffer(),h.getLevel().registryAccess());
                try {
                    BalloonNetwork.BindingPayload.CODEC.encode(buffer,payload);
                    h.assertTrue(BalloonNetwork.BindingPayload.CODEC.decode(buffer).equals(payload),"Range endpoint survives client payload codec: "+range);
                } finally {buffer.release();}
                TetherManager.release(p,false);
            }
            h.succeed();
        } finally {TetherManager.release(p,false);BalloonConfig.RANGE.set(old);}
    }

    @GameTest(template="empty") public static void hopRangeAndHardness(GameTestHelper h) {
        int old=BalloonConfig.RANGE.get();var pos=h.absolutePos(new BlockPos(2,2,2));var p=player(h,pos);
        try {
            BalloonConfig.RANGE.set(8);var a=stake(h,pos,p);var target=inactive(h,pos.offset(8,0,0));p.getAbilities().flying=true;
            p.setPos(Vec3.atCenterOf(pos).add(-0.31,0,0));var before=p.position();var boundary=TetherManager.boundaryFor(p);
            click(h,target,p);
            h.assertTrue(target.owner()==null&&p.getUUID().equals(a.owner())&&p.position().equals(before)&&TetherManager.boundaryFor(p)==boundary&&p.getAbilities().flying,"Out-of-range hop rejects without teleporting or changing existing binding");
            p.setPos(Vec3.atCenterOf(pos));before=p.position();click(h,target,p);
            h.assertTrue(p.getUUID().equals(target.owner())&&a.owner()==null&&p.position().equals(before)&&TetherManager.boundaryFor(p).range()==8,"Exact target boundary accepts hop and captures server range");
            var state=target.getBlockState();h.assertTrue(Math.abs(state.getDestroySpeed(h.getLevel(),target.getBlockPos())-0.3F)<1E-6,"Stake hardness is 0.30");
            p.setOnGround(true);p.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);float hand=p.getDigSpeed(state,pos);p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.IRON_AXE));
            h.assertTrue(p.getDigSpeed(state,pos)>hand,"Axe tag still speeds mining");h.succeed();
        } finally {TetherManager.release(p,false);BalloonConfig.RANGE.set(old);}
    }

    @GameTest(template="empty") public static void dyeRecipesPlacementAndDrops(GameTestHelper h) {
        var pos=h.absolutePos(new BlockPos(2,2,2));var p=player(h,pos);
        var r=h.getLevel().registryAccess();
        try {
            for (var color : net.minecraft.world.item.DyeColor.values()) {
                // The same item ID matches regardless of its existing colour component.
                var dye=net.minecraft.core.registries.BuiltInRegistries.ITEM.get(net.minecraft.resources.ResourceLocation.withDefaultNamespace(color.getSerializedName()+"_dye"));
                var input=net.minecraft.world.item.crafting.CraftingInput.of(2,1,java.util.List.of(StakeItem.stack(net.minecraft.world.item.DyeColor.RED),new ItemStack(dye)));
                var result=h.getLevel().getRecipeManager().getRecipeFor(net.minecraft.world.item.crafting.RecipeType.CRAFTING,input,h.getLevel()).orElseThrow().value().assemble(input,r);
                h.assertTrue(result.getCount()==1 && StakeItem.color(result)==color,"Dye recipe produces "+color);
                h.getLevel().removeBlock(pos,false);h.getLevel().setBlock(pos.below(),Blocks.STONE.defaultBlockState(),3);
                p.setPos(Vec3.atCenterOf(pos).add(2,1,0));var held=result.copy();held.setCount(2);p.setItemInHand(InteractionHand.MAIN_HAND,held);
                var hit=new net.minecraft.world.phys.BlockHitResult(Vec3.atCenterOf(pos.below()).add(0,0.5,0),net.minecraft.core.Direction.UP,pos.below(),false);
                var context=new net.minecraft.world.item.context.BlockPlaceContext(new net.minecraft.world.item.context.UseOnContext(p,InteractionHand.MAIN_HAND,hit));
                h.assertTrue(BuildersBalloon.STAKE_ITEM.get().place(context).consumesAction(),"Real BlockItem placement succeeds for "+color);
                h.assertTrue(p.getMainHandItem().getCount()==1 && StakeItem.color(p.getMainHandItem())==color,"Placement consumes exactly one coloured item from a stack");
                var a=(StakeBlockEntity)h.getLevel().getBlockEntity(pos);
                h.assertTrue(a!=null && a.getBlockState().getValue(StakeBlock.COLOR)==color && p.getUUID().equals(a.owner()),"Placement transfers colour before binding");
                var drops=net.minecraft.world.level.block.Block.getDrops(a.getBlockState(),h.getLevel(),pos,a);
                h.assertTrue(drops.size()==1 && drops.getFirst().getCount()==1 && StakeItem.color(drops.getFirst())==color,"Loot preserves "+color);
                h.assertTrue(ItemStack.isSameItemSameComponents(drops.getFirst(),result),"Drop can merge with crafted/recoloured items of the same colour");
                var picked=BuildersBalloon.STAKE.get().getCloneItemStack(a.getBlockState(),hit,h.getLevel(),pos,p);
                h.assertTrue(StakeItem.color(picked)==color,"Pick block preserves "+color);
                h.assertTrue(ItemStack.isSameItemSameComponents(picked,result),"Pick-block colour components are canonical");
                TetherManager.release(p,true);h.assertTrue(a.getBlockState().getValue(StakeBlock.COLOR)==color,"Pop retains colour");
                p.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);click(h,a,p);
                h.assertTrue(p.getUUID().equals(a.owner()) && a.getBlockState().getValue(StakeBlock.COLOR)==color,"Reactivation retains colour");
                TetherManager.release(p,false);
            }
            h.succeed();
        } finally {TetherManager.release(p,false);}
    }
    @GameTest(template="empty") public static void colourSavesAndDefaultBlue(GameTestHelper h) {
        var r=h.getLevel().registryAccess();var pos=h.absolutePos(new BlockPos(2,2,2));var p=player(h,pos);
        // New-ID palette without a colour field defaults white; the old ID is deliberately not migrated.
        var legacy=new net.minecraft.nbt.CompoundTag();legacy.putString("Name","builders_balloon:balloon_tether");
        var properties=new net.minecraft.nbt.CompoundTag();properties.putString("direction","east");properties.putString("waterlogged","true");properties.putString("connected","false");legacy.put("Properties",properties);
        var decoded=net.minecraft.nbt.NbtUtils.readBlockState(h.getLevel().holderLookup(net.minecraft.core.registries.Registries.BLOCK),legacy);
        h.assertTrue(decoded.getValue(StakeBlock.COLOR)==net.minecraft.world.item.DyeColor.WHITE && decoded.getValue(StakeBlock.DIRECTION)==net.minecraft.core.Direction.EAST && decoded.getValue(StakeBlock.WATERLOGGED),"New-ID palette defaults to white, preserves face/waterlogging");
        h.assertTrue(StakeItem.color(new ItemStack(BuildersBalloon.STAKE_ITEM.get()))==net.minecraft.world.item.DyeColor.WHITE,"Default item colour is white");
        try {
            var a=stake(h,pos,p);h.getLevel().setBlock(pos,a.getBlockState().setValue(StakeBlock.COLOR,net.minecraft.world.item.DyeColor.RED),3);
            var b=inactive(h,pos.offset(3,0,0));h.getLevel().setBlock(b.getBlockPos(),b.getBlockState().setValue(StakeBlock.COLOR,net.minecraft.world.item.DyeColor.LIME),3);
            p.getAbilities().flying=true;click(h,b,p);
            h.assertTrue(a.owner()==null && p.getUUID().equals(b.owner()) && p.getAbilities().flying && a.getBlockState().getValue(StakeBlock.COLOR)==net.minecraft.world.item.DyeColor.RED && b.getBlockState().getValue(StakeBlock.COLOR)==net.minecraft.world.item.DyeColor.LIME,"Hopping retains each anchor's own colour and flight");
            var diskState=net.minecraft.nbt.NbtUtils.writeBlockState(b.getBlockState());
            var restored=net.minecraft.nbt.NbtUtils.readBlockState(h.getLevel().holderLookup(net.minecraft.core.registries.Registries.BLOCK),diskState);
            h.assertTrue(restored.getValue(StakeBlock.COLOR)==net.minecraft.world.item.DyeColor.LIME,"World block-state save/load retains colour");
            var stack=StakeItem.stack(net.minecraft.world.item.DyeColor.PURPLE);
            var saved=stack.save(r);var loaded=ItemStack.parse(r,saved).orElseThrow();
            h.assertTrue(StakeItem.color(loaded)==net.minecraft.world.item.DyeColor.PURPLE,"Item save/load retains colour");h.succeed();
        } finally {TetherManager.release(p,false);}
    }

    @GameTest(template="empty") public static void bindingPacketRoundtrip(GameTestHelper h) {
        for (UUID sublevel : new UUID[]{null,UUID.randomUUID()}) {
            var packet=new BalloonNetwork.BindingPayload(true,h.getLevel().dimension().location(),new BlockPos(25_000_008,128,26_000_008),16,sublevel);
            var buf=new net.minecraft.network.RegistryFriendlyByteBuf(io.netty.buffer.Unpooled.buffer(),h.getLevel().registryAccess());
            try {
                BalloonNetwork.BindingPayload.CODEC.encode(buf,packet);
                h.assertTrue(BalloonNetwork.BindingPayload.CODEC.decode(buf).equals(packet),"World/sublevel identity survives binding synchronization");
            } finally {buf.release();}
        }
        h.succeed();
    }

    @GameTest(template="empty") public static void metalRecipesAndColours(GameTestHelper h) {
        var pos=h.absolutePos(new BlockPos(2,2,2));var player=player(h,pos);
        for(var color:net.minecraft.world.item.DyeColor.values()) {
            var input=net.minecraft.world.item.crafting.CraftingInput.of(3,3,java.util.List.of(ItemStack.EMPTY,new ItemStack(net.minecraft.core.registries.BuiltInRegistries.ITEM.get(net.minecraft.resources.ResourceLocation.withDefaultNamespace(color.getSerializedName()+"_wool"))),ItemStack.EMPTY,new ItemStack(Items.LEAD),new ItemStack(Items.IRON_BLOCK),new ItemStack(Items.LEAD),ItemStack.EMPTY,new ItemStack(Items.IRON_INGOT),ItemStack.EMPTY));
            var result=craft(h,input);h.assertTrue(result.is(BuildersBalloon.METAL_STAKE_ITEM.get()) && result.getCount()==1 && StakeItem.color(result)==color,"Metal functional crafting selects wool colour");
            for(var block:new net.minecraft.world.level.block.Block[]{BuildersBalloon.METAL_STAKE.get(),BuildersBalloon.METAL_BOLLARD.get()}) {
                var original=StakeItem.stack(block,color);
                var dyed=craft(h,net.minecraft.world.item.crafting.CraftingInput.of(2,1,java.util.List.of(original,new ItemStack(net.minecraft.world.item.DyeItem.byColor(color)))));
                h.assertTrue(dyed.is(block.asItem()) && dyed.getCount()==1 && StakeItem.color(dyed)==color,"Metal recolouring retains correct item type");
                var state=block.defaultBlockState().setValue(StakeBlock.COLOR,color).setValue(StakeBlock.SPIN,3);
                if(state.hasProperty(BollardBlock.ROPED))state=state.setValue(BollardBlock.ROPED,true);
                h.getLevel().setBlock(pos,state,3);
                var drops=net.minecraft.world.level.block.Block.getDrops(state,h.getLevel(),pos,h.getLevel().getBlockEntity(pos),player,ItemStack.EMPTY);
                h.assertTrue(drops.size()==1 && drops.getFirst().is(block.asItem()) && StakeItem.color(drops.getFirst())==color,"Hand-removable metal drops preserve type and colour");
                h.assertTrue(!drops.getFirst().getOrDefault(net.minecraft.core.component.DataComponents.BLOCK_STATE,net.minecraft.world.item.component.BlockItemStateProperties.EMPTY).properties().containsKey("roped"),"No rope state on drops");
                var picked=block.getCloneItemStack(state,new net.minecraft.world.phys.BlockHitResult(Vec3.atCenterOf(pos),net.minecraft.core.Direction.UP,pos,false),h.getLevel(),pos,player);
                h.assertTrue(picked.is(block.asItem()) && StakeItem.color(picked)==color,"Metal pick-block retains type and colour");
                h.assertTrue(ItemStack.isSameItemSameComponents(original,ItemStack.parse(h.getLevel().registryAccess(),original.save(h.getLevel().registryAccess())).orElseThrow()),"Metal colour persists in item data");
                var restored=net.minecraft.nbt.NbtUtils.readBlockState(h.getLevel().holderLookup(net.minecraft.core.registries.Registries.BLOCK),net.minecraft.nbt.NbtUtils.writeBlockState(state));
                h.assertTrue(restored.equals(state),"Metal palette persistence retains direction, colour, knot and rope");
            }
        }
        var decor=craft(h,net.minecraft.world.item.crafting.CraftingInput.of(1,2,java.util.List.of(new ItemStack(Items.IRON_BLOCK),new ItemStack(Items.IRON_INGOT))));
        h.assertTrue(decor.is(BuildersBalloon.METAL_BOLLARD_ITEM.get()) && decor.getCount()==4 && StakeItem.color(decor)==net.minecraft.world.item.DyeColor.WHITE,"Vanilla metal decoration yields four white items");
        h.succeed();
    }
    @GameTest(template="empty") public static void separateMetalRangeAndHopping(GameTestHelper h) {
        var pos=h.absolutePos(new BlockPos(2,2,2));var player=player(h,pos);int saved=BalloonConfig.METAL_RANGE.get();
        try {
            h.assertTrue(BalloonConfig.METAL_RANGE.getDefault()==32 && BalloonConfig.RANGE.getDefault()==16,"Independent defaults");
            BalloonConfig.METAL_RANGE.set(64);
            h.getLevel().setBlock(pos,BuildersBalloon.METAL_STAKE.get().defaultBlockState(),3);
            var metal=(StakeBlockEntity)h.getLevel().getBlockEntity(pos);TetherManager.bind(player,metal);
            h.assertTrue(TetherManager.boundaryFor(player).range()==64 && metal.getBlockState().getValue(StakeBlock.CONNECTED),"Metal binding uses 64 and shows rope");
            BalloonConfig.METAL_RANGE.set(8);h.assertTrue(TetherManager.boundaryFor(player).range()==64,"Existing binding captures its range");
            var wood=inactive(h,pos.offset(3,0,0));TetherManager.bind(player,wood);
            h.assertTrue(TetherManager.boundaryFor(player).range()==BalloonConfig.RANGE.get() && metal.owner()==null,"Hopping to wood restores wooden range");
            TetherManager.bind(player,metal);h.assertTrue(TetherManager.boundaryFor(player).range()==8 && wood.owner()==null,"Reactivation captures changed metal range");
            var decorPos=pos.offset(0,0,3);h.getLevel().setBlock(decorPos,BuildersBalloon.METAL_BOLLARD.get().defaultBlockState(),3);
            toggleBollard(h,decorPos,player,new ItemStack(Items.LEAD,3),InteractionHand.MAIN_HAND);
            h.assertTrue(h.getLevel().getBlockEntity(decorPos)==null && player.getUUID().equals(metal.owner()),"Metal decoration never takes flight ownership");
            h.getLevel().removeBlock(pos,false);h.assertTrue(TetherManager.boundaryFor(player)==null && !player.mayFly(),"Removing active metal tether releases flight");
        } finally {TetherManager.release(player,false);BalloonConfig.METAL_RANGE.set(saved);}
        h.succeed();
    }
    @GameTest(template="empty") public static void metalHardnessAndExplosionSurvival(GameTestHelper h) {
        var level=h.getLevel();var pos=h.absolutePos(new BlockPos(5,3,5));
        for(var block:new net.minecraft.world.level.block.Block[]{BuildersBalloon.METAL_STAKE.get(),BuildersBalloon.METAL_BOLLARD.get()}) {
            for(float power:new float[]{3,4}) {
                var state=block.defaultBlockState();level.setBlock(pos.below(),Blocks.DIRT.defaultBlockState(),3);level.setBlock(pos,state,3);
                h.assertTrue(Math.abs(state.getDestroySpeed(level,pos)-.8f)<.00001,"Metal hardness 0.80");
                h.assertTrue(block.getExplosionResistance()==1200f,"Metal blast resistance 1200");
                level.explode(null,pos.getX()+.5,pos.getY()+.5,pos.getZ()+.5,power, power==4?net.minecraft.world.level.Level.ExplosionInteraction.TNT:net.minecraft.world.level.Level.ExplosionInteraction.MOB);
                h.assertTrue(level.getBlockState(pos).is(block),"Metal survives normal creeper/TNT strength at direct contact");
            }
            level.setBlock(pos.below(),Blocks.AIR.defaultBlockState(),3);h.assertTrue(level.getBlockState(pos).is(block),"Removal of support does not remove metal block");
            level.removeBlock(pos,false);
        }
        h.succeed();
    }
    @GameTest(template="empty") public static void optionalCreateRecipesAndWrench(GameTestHelper h) {
        var registry=net.minecraft.core.registries.BuiltInRegistries.ITEM;
        var industrialId=net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("create","industrial_iron_block");
        var wrenchId=net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("create","wrench");
        var wrenchTag=net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.BLOCK,net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("create","wrench_pickup"));
        for(var block:new net.minecraft.world.level.block.Block[]{BuildersBalloon.STAKE.get(),BuildersBalloon.BOLLARD.get(),BuildersBalloon.METAL_STAKE.get(),BuildersBalloon.METAL_BOLLARD.get()})h.assertTrue(block.defaultBlockState().is(wrenchTag),"All four blocks in Create pickup tag");
        if(Boolean.getBoolean("balloon.tests.requireCreate"))h.assertTrue(registry.containsKey(industrialId) && registry.containsKey(wrenchId),"Actual Create fixtures required");
        if(!registry.containsKey(industrialId)){h.succeed();return;}
        var industrial=registry.get(industrialId);
        var decor=craft(h,net.minecraft.world.item.crafting.CraftingInput.of(1,2,java.util.List.of(new ItemStack(industrial),new ItemStack(Items.IRON_INGOT))));
        h.assertTrue(decor.is(BuildersBalloon.METAL_BOLLARD_ITEM.get()) && decor.getCount()==2,"Create metal decoration yields two");
        var functional=craft(h,net.minecraft.world.item.crafting.CraftingInput.of(3,3,java.util.List.of(ItemStack.EMPTY,new ItemStack(Items.RED_WOOL),ItemStack.EMPTY,new ItemStack(Items.LEAD),new ItemStack(industrial),new ItemStack(Items.LEAD),ItemStack.EMPTY,new ItemStack(Items.IRON_INGOT),ItemStack.EMPTY)));
        h.assertTrue(functional.is(BuildersBalloon.METAL_STAKE_ITEM.get()) && StakeItem.color(functional)==net.minecraft.world.item.DyeColor.RED,"Create functional recipe retains colour");
        var pos=h.absolutePos(new BlockPos(3,3,3));var player=player(h,pos);player.setShiftKeyDown(true);
        for(var block:new net.minecraft.world.level.block.Block[]{BuildersBalloon.STAKE.get(),BuildersBalloon.BOLLARD.get(),BuildersBalloon.METAL_STAKE.get(),BuildersBalloon.METAL_BOLLARD.get()}) {
            var state=block.defaultBlockState().setValue(StakeBlock.COLOR,net.minecraft.world.item.DyeColor.CYAN);
            if(state.hasProperty(BollardBlock.ROPED))state=state.setValue(BollardBlock.ROPED,true);
            h.getLevel().setBlock(pos,state,3);if(block instanceof StakeBlock)TetherManager.bind(player,(StakeBlockEntity)h.getLevel().getBlockEntity(pos));
            player.getInventory().clearContent();var wrench=new ItemStack(registry.get(wrenchId));player.setItemInHand(InteractionHand.MAIN_HAND,wrench);
            var hit=new net.minecraft.world.phys.BlockHitResult(Vec3.atCenterOf(pos),net.minecraft.core.Direction.UP,pos,false);
            var used=player.gameMode.useItemOn(player,h.getLevel(),wrench,InteractionHand.MAIN_HAND,hit);
            h.assertTrue(used.consumesAction() && h.getLevel().getBlockState(pos).isAir(),"Real Create sneak-wrench picks up every variant");
            var found=player.getInventory().items.stream().filter(s->s.is(block.asItem())).toList();
            h.assertTrue(found.size()==1 && found.getFirst().getCount()==1 && StakeItem.color(found.getFirst())==net.minecraft.world.item.DyeColor.CYAN,"Wrench returns exactly one correct coloured item");
            h.assertTrue(TetherManager.boundaryFor(player)==null && wrench.getCount()==1 && wrench.getDamageValue()==0,"Wrench releases binding without consumption/damage");
        }
        h.succeed();
    }

    @GameTest(template="empty") public static void metalPlacementFacesAndWaterlogging(GameTestHelper h) {
        var pos=h.absolutePos(new BlockPos(5,3,5));var player=player(h,pos);
        for(var block:new net.minecraft.world.level.block.Block[]{BuildersBalloon.METAL_STAKE.get(),BuildersBalloon.METAL_BOLLARD.get()}) {
            for(var color:net.minecraft.world.item.DyeColor.values())for(var mount:net.minecraft.core.Direction.values()) {
                TetherManager.release(player,false);h.getLevel().setBlock(pos,Blocks.AIR.defaultBlockState(),3);
                var support=pos.relative(mount);h.getLevel().setBlock(support,Blocks.STONE.defaultBlockState(),3);
                var hit=new net.minecraft.world.phys.BlockHitResult(Vec3.atCenterOf(support),mount.getOpposite(),support,false);
                var context=new net.minecraft.world.item.context.BlockPlaceContext(player,InteractionHand.MAIN_HAND,StakeItem.stack(block,color),hit);
                var item=(net.minecraft.world.item.BlockItem)block.asItem();h.assertTrue(item.place(context).consumesAction(),"Actual metal placement on every mounting face");
                var state=h.getLevel().getBlockState(pos);h.assertTrue(state.is(block) && state.getValue(StakeBlock.COLOR)==color && state.getValue(StakeBlock.DIRECTION)==mount,"Metal placement retains type, face and colour");
                int spin=state.getValue(StakeBlock.SPIN);
                if(block instanceof StakeBlock) {
                    var anchor=(StakeBlockEntity)h.getLevel().getBlockEntity(pos);TetherManager.release(player,false);TetherManager.activate(player,anchor);
                    h.assertTrue(h.getLevel().getBlockState(pos).getValue(StakeBlock.SPIN)==spin,"Metal reactivation retains knot orientation");
                } else {
                    var rope=new ItemStack(Items.LEAD,4);toggleBollard(h,pos,player,rope,InteractionHand.MAIN_HAND);toggleBollard(h,pos,player,rope,InteractionHand.MAIN_HAND);
                    h.assertTrue(!h.getLevel().getBlockState(pos).getValue(BollardBlock.ROPED) && rope.getCount()==4 && h.getLevel().getBlockState(pos).getValue(StakeBlock.SPIN)==spin,"Metal cosmetic toggle is non-consuming and cannot rotate knot");
                }
                TetherManager.release(player,false);h.getLevel().setBlock(pos,Blocks.WATER.defaultBlockState(),3);
                var water=block.getStateForPlacement(new net.minecraft.world.item.context.BlockPlaceContext(player,InteractionHand.MAIN_HAND,StakeItem.stack(block,color),hit));
                h.assertTrue(water.getValue(StakeBlock.WATERLOGGED),"Waterlogged metal placement");
                h.getLevel().setBlock(pos,Blocks.AIR.defaultBlockState(),3);h.getLevel().setBlock(support,Blocks.AIR.defaultBlockState(),3);
            }
        }
        h.succeed();
    }
}
