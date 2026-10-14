package dev.tilosophy.buildersballoon.mixin;

import net.minecraft.world.phys.Vec3;
import dev.tilosophy.buildersballoon.TetherManager;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value={ServerGamePacketListenerImpl.class}, remap=false)
public abstract class PlayerPacketMixin {
    @Shadow
    public ServerPlayer player;

    @Redirect(method={"handleMovePlayer"}, at=@At(value="INVOKE", target="Lnet/minecraft/network/protocol/game/ServerboundMovePlayerPacket;getX(D)D", ordinal=1), require=1)
    private double balloonOnAStick$x(ServerboundMovePlayerPacket packet, double fallback) {
        return balloonOnAStick$position(packet).x;
    }

    @Redirect(method={"handleMovePlayer"}, at=@At(value="INVOKE", target="Lnet/minecraft/network/protocol/game/ServerboundMovePlayerPacket;getY(D)D", ordinal=1), require=1)
    private double balloonOnAStick$y(ServerboundMovePlayerPacket packet, double fallback) {
        return balloonOnAStick$position(packet).y;
    }

    @Redirect(method={"handleMovePlayer"}, at=@At(value="INVOKE", target="Lnet/minecraft/network/protocol/game/ServerboundMovePlayerPacket;getZ(D)D", ordinal=1), require=1)
    private double balloonOnAStick$z(ServerboundMovePlayerPacket packet, double fallback) {
        return balloonOnAStick$position(packet).z;
    }
    @org.spongepowered.asm.mixin.Unique
    private Vec3 balloonOnAStick$position(ServerboundMovePlayerPacket packet) {
        return TetherManager.constrainPosition(player, new Vec3(packet.getX(player.getX()), packet.getY(player.getY()), packet.getZ(player.getZ())));
    }

}
