package dev.tilosophy.buildersballoon.sabletest;

import dev.tilosophy.buildersballoon.test.StakeGameTests;

import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.api.sublevel.ServerSubLevelContainer;
import dev.ryanhcode.sable.companion.math.Pose3d;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.tilosophy.buildersballoon.*;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** Runs only in the isolated fixture with the released Sable mod loaded. */
@GameTestHolder("builders_balloon_sable_tests")
@PrefixGameTestTemplate(false)
public final class SableGameTests {
    static ServerSubLevel sublevel(GameTestHelper h, Vec3 world) {
        var container=(ServerSubLevelContainer)SubLevelContainer.getContainer(h.getLevel());
        var pose=new Pose3d();pose.position().set(world.x,world.y,world.z);
        var sub=(ServerSubLevel)container.allocateNewSubLevel(pose);
        BlockPos local=sub.getPlot().getCenterBlock();
        sub.logicalPose().rotationPoint().set(local.getX()+0.5,local.getY()+0.5,local.getZ()+0.5);
        sub.getPlot().newEmptyChunk(new ChunkPos(local));
        return sub;
    }
    static StakeBlockEntity anchor(GameTestHelper h, BlockPos pos) {
        h.getLevel().setBlock(pos, BuildersBalloon.STAKE.get().defaultBlockState(),3);
        return (StakeBlockEntity)h.getLevel().getBlockEntity(pos);
    }
    @GameTest(template="empty") public static void stationaryPlacementHoppingAndCleanup(GameTestHelper h) {
        Vec3 world=Vec3.atCenterOf(h.absolutePos(new BlockPos(3,3,3)));
        var sub=sublevel(h,world);var pos=sub.getPlot().getCenterBlock();var p=StakeGameTests.player(h,h.absolutePos(new BlockPos(3,3,3)));
        try {
            var a=anchor(h,pos);p.setPos(world.add(2,0,0));
            BuildersBalloon.STAKE.get().setPlacedBy(h.getLevel(),pos,a.getBlockState(),p,new ItemStack(BuildersBalloon.STAKE_ITEM.get()));
            h.assertTrue(p.getUUID().equals(a.owner()) && p.mayFly() && a.getBlockState().getValue(StakeBlock.CONNECTED),"Stationary Sable placement binds instead of no-space error");
            TetherManager.release(p,true);
            h.assertTrue(TetherManager.activate(p,a)==TetherManager.Activation.BOUND,"Stationary sublevel reactivation accepts nearby world-space player");
            p.getAbilities().flying=true;var b=StakeGameTests.inactive(h,h.absolutePos(new BlockPos(5,3,3)));
            var before=p.position();TetherManager.activate(p,b);
            h.assertTrue(a.owner()==null && p.getUUID().equals(b.owner()) && p.getAbilities().flying && p.position().equals(before),"Sublevel to world hop retains flight/position");
            TetherManager.activate(p,a);
            h.assertTrue(b.owner()==null && p.getUUID().equals(a.owner()) && p.position().equals(before),"World to sublevel hop retains flight/position");
            sub.markRemoved();NeoForge.EVENT_BUS.post(new PlayerTickEvent.Post(p));
            h.assertTrue(a.owner()==null && !p.mayFly(),"Removed sublevel releases without teleporting to plot storage");
            h.assertTrue(p.position().equals(before),"Removed plot cannot teleport player");h.succeed();
        } finally {TetherManager.release(p,false);sub.markRemoved();}
    }
    @GameTest(template="empty") public static void rotatingRangeAndPacketCoordinates(GameTestHelper h) {
        int old=BalloonConfig.RANGE.get();Vec3 world=Vec3.atCenterOf(h.absolutePos(new BlockPos(3,3,3)));
        var sub=sublevel(h,world);var pos=sub.getPlot().getCenterBlock();var p=StakeGameTests.player(h,h.absolutePos(new BlockPos(3,3,3)));
        try {
            BalloonConfig.RANGE.set(8);var a=anchor(h,pos);p.setPos(world.add(1,0,0));
            h.assertTrue(TetherManager.activate(p,a)==TetherManager.Activation.BOUND,"Sublevel range captures 8");
            sub.logicalPose().orientation().rotationY(Math.PI/4);
            var frame=AnchorFrames.at(h.getLevel(),pos).logical();var center=Vec3.atCenterOf(pos);
            p.noPhysics=true;p.setPos(frame.toWorld(center.add(7,0,0)));
            var outward=frame.worldDelta(new Vec3(2,0,1));p.move(MoverType.SELF,outward);
            var local=frame.toLocal(p.position());
            h.assertTrue(Math.abs(local.x-center.x-8)<1E-6 && Math.abs(local.z-center.z-1)<1E-6,"Entity movement clips rotated face, retains tangent");
            var clamped=TetherManager.constrainPosition(p,frame.toWorld(center.add(12,2,1)));
            local=frame.toLocal(clamped);
            h.assertTrue(Math.abs(local.x-center.x-8)<1E-6 && Math.abs(local.y-center.y-2)<1E-6 && Math.abs(local.z-center.z-1)<1E-6,"Full packet point is clamped in rotated local axes");
            p.setPos(frame.toWorld(center.add(7,0,0)));p.getAbilities().flying=true;
            h.getLevel().addNewPlayer(p); // Real packet path needs ChunkMap tracking, even with a synthetic connection.
            p.connection.resetPosition();
            Vec3 requested=frame.toWorld(center.add(12,2,1));
            p.connection.handleMovePlayer(new net.minecraft.network.protocol.game.ServerboundMovePlayerPacket.Pos(requested.x,requested.y,requested.z,false));
            local=frame.toLocal(p.position());
            h.assertTrue(Math.abs(local.x-center.x-8)<1E-6 && Math.abs(local.y-center.y-2)<1E-6 && Math.abs(local.z-center.z-1)<1E-6,"Actual transformed server packet listener clips all three coupled axes");
            p.setPos(frame.toWorld(center.add(1,0,0)));NeoForge.EVENT_BUS.post(new PlayerTickEvent.Post(p));
            var savedLocal=frame.toLocal(p.position());sub.logicalPose().position().add(40,0,0);
            frame=AnchorFrames.at(h.getLevel(),pos).logical();NeoForge.EVENT_BUS.post(new PlayerTickEvent.Post(p));
            h.assertTrue(p.position().distanceTo(frame.toWorld(savedLocal))<1E-6 && p.getUUID().equals(a.owner()),"Moving range recovers to transformed safe position, never storage coordinates");
            h.succeed();
        } finally {TetherManager.release(p,false);p.remove(net.minecraft.world.entity.Entity.RemovalReason.DISCARDED);sub.markRemoved();BalloonConfig.RANGE.set(old);}
    }
    @GameTest(template="empty") public static void movingRecoveryPreservesLook(GameTestHelper h) {
        int old=BalloonConfig.RANGE.get();
        Vec3 world=Vec3.atCenterOf(h.absolutePos(new BlockPos(3,3,3)));
        var sub=sublevel(h,world);var pos=sub.getPlot().getCenterBlock();
        var packets=new java.util.ArrayList<net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket>();
        var p=StakeGameTests.player(h,h.absolutePos(new BlockPos(3,3,3)),packet -> {
            if(packet instanceof net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket correction) packets.add(correction);
        });
        try {
            BalloonConfig.RANGE.set(8);var a=anchor(h,pos);p.setPos(world.add(1,0,0));
            TetherManager.activate(p,a);p.getAbilities().flying=true;p.noPhysics=true;
            h.getLevel().addNewPlayer(p);p.connection.resetPosition();
            NeoForge.EVENT_BUS.post(new PlayerTickEvent.Post(p));
            packets.clear(); // Ignore initial player-registration synchronization.
            // Simulate view changing client-side before an older server correction arrives.
            float clientYaw=75,clientPitch=-20;
            for(int n=0;n<8;n++) {
                // Keep recovery destinations inside the fixture's loaded chunks.
                sub.logicalPose().position().add(n%2==0 ? 16 : -16,0,0);
                sub.logicalPose().orientation().rotationY(n*0.1);
                NeoForge.EVENT_BUS.post(new PlayerTickEvent.Post(p));
                h.assertTrue(packets.size()==n+1,"One recovery packet per boundary crossing: iteration="+n+", packets="+packets.size()+", bound="+(TetherManager.boundaryFor(p)!=null)+", position="+p.position());
                var packet=packets.get(n);
                h.assertTrue(packet.getRelativeArguments().equals(java.util.Set.of(
                    net.minecraft.world.entity.RelativeMovement.Y_ROT,net.minecraft.world.entity.RelativeMovement.X_ROT)),
                    "Recovery corrects absolute position and preserves both client view axes");
                h.assertTrue(packet.getYRot()==0 && packet.getXRot()==0,"Recovery applies zero rotation delta");
                // Vanilla client applies these relative angles to its current view.
                clientYaw+=packet.getYRot();clientPitch+=packet.getXRot();
                h.assertTrue(clientYaw==75+n*11 && clientPitch==-20+n*2,"An arriving correction cannot rewind mouse input");
                clientYaw+=11;clientPitch+=2;
                if(n%2==0) {
                    float serverYaw=p.getYRot(),serverPitch=p.getXRot();
                    p.connection.handleMovePlayer(new net.minecraft.network.protocol.game.ServerboundMovePlayerPacket.Rot(clientYaw,clientPitch,false));
                    h.assertTrue(p.getYRot()==serverYaw && p.getXRot()==serverPitch,"Pending correction still uses vanilla acknowledgement gate");
                } else {
                    p.connection.handleAcceptTeleportPacket(new net.minecraft.network.protocol.game.ServerboundAcceptTeleportationPacket(packet.getId()));
                    p.connection.handleMovePlayer(new net.minecraft.network.protocol.game.ServerboundMovePlayerPacket.Rot(clientYaw,clientPitch,false));
                    h.assertTrue(p.getYRot()==clientYaw && p.getXRot()==clientPitch,"Latest look is accepted after acknowledging the newest correction");
                }
                var frame=AnchorFrames.at(h.getLevel(),pos).logical();
                h.assertTrue(frame.contains(TetherManager.boundaryFor(p),p.position()) && p.getUUID().equals(a.owner()),"Repeated corrections retain moving range and binding");
            }
            h.succeed();
        } finally {TetherManager.release(p,false);p.remove(net.minecraft.world.entity.Entity.RemovalReason.DISCARDED);sub.markRemoved();BalloonConfig.RANGE.set(old);}
    }


    @GameTest(template="empty") public static void metalRotatedRange64(GameTestHelper h) {
        int saved=BalloonConfig.METAL_RANGE.get();var world=Vec3.atCenterOf(h.absolutePos(new BlockPos(3,3,3)));
        var sub=sublevel(h,world);var pos=sub.getPlot().getCenterBlock();var p=StakeGameTests.player(h,h.absolutePos(new BlockPos(3,3,3)));
        try {
            BalloonConfig.METAL_RANGE.set(64);h.getLevel().setBlock(pos,BuildersBalloon.METAL_STAKE.get().defaultBlockState(),3);
            var metal=(StakeBlockEntity)h.getLevel().getBlockEntity(pos);p.setPos(world.add(1,0,0));
            h.assertTrue(TetherManager.activate(p,metal)==TetherManager.Activation.BOUND && TetherManager.boundaryFor(p).range()==64,"Metal Sable anchor binds with full configurable range");
            sub.logicalPose().orientation().rotationY(Math.PI/4);var frame=AnchorFrames.at(h.getLevel(),pos).logical();var centre=Vec3.atCenterOf(pos);
            var clamped=TetherManager.constrainPosition(p,frame.toWorld(centre.add(80,5,12)));var local=frame.toLocal(clamped);
            h.assertTrue(Math.abs(local.x-centre.x-64)<1E-6 && Math.abs(local.y-centre.y-5)<1E-6 && Math.abs(local.z-centre.z-12)<1E-6,"Metal 64-range packet clamp respects rotated local axes");
            var wooden=StakeGameTests.inactive(h,h.absolutePos(new BlockPos(5,3,3)));p.setPos(world.add(1,0,0));
            h.assertTrue(TetherManager.activate(p,wooden)==TetherManager.Activation.BOUND && TetherManager.boundaryFor(p).range()==BalloonConfig.RANGE.get() && metal.owner()==null,"Metal sublevel to wooden world hop uses wooden range");
            TetherManager.activate(p,metal);h.assertTrue(TetherManager.boundaryFor(p).range()==64 && wooden.owner()==null,"Hop back restores metal sublevel range");
            h.getLevel().removeBlock(pos,false);h.assertTrue(TetherManager.boundaryFor(p)==null && !p.mayFly(),"Metal Sable removal releases flight");h.succeed();
        } finally {TetherManager.release(p,false);sub.markRemoved();BalloonConfig.METAL_RANGE.set(saved);}
    }
}
