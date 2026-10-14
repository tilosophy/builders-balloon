package dev.tilosophy.buildersballoon;

import dev.tilosophy.buildersballoon.BuildersBalloon;
import dev.tilosophy.buildersballoon.TetherBoundary;
import dev.tilosophy.buildersballoon.TetherManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public final class BalloonNetwork {
    private BalloonNetwork() {
    }

    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("2");
        registrar.playToServer(PopPayload.TYPE, PopPayload.CODEC, (payload, context) -> {
            Player patt0$temp = context.player();
            if (patt0$temp instanceof ServerPlayer) {
                ServerPlayer player = (ServerPlayer)patt0$temp;
                TetherManager.release(player, true);
            }
        });
        registrar.playToClient(BindingPayload.TYPE, BindingPayload.CODEC, (payload, context) -> {
            if (payload.active && payload.range >= 8 && payload.range <= 64) {
                BlockPos pos = payload.stake;
                TetherManager.setClientBoundary(context.player().getUUID(), (ResourceKey<Level>)ResourceKey.create((ResourceKey)Registries.DIMENSION, (ResourceLocation)payload.dimension), new TetherBoundary((double)pos.getX() + 0.5, (double)pos.getY() + 0.5, (double)pos.getZ() + 0.5, payload.range), payload.sublevel);
            } else {
                TetherManager.setClientBoundary(null, null, null);
            }
        });
    }

    public record PopPayload() implements CustomPacketPayload
    {
        public static final PopPayload INSTANCE = new PopPayload();
        public static final CustomPacketPayload.Type<PopPayload> TYPE = new CustomPacketPayload.Type(BuildersBalloon.id("pop"));
        public static final StreamCodec<RegistryFriendlyByteBuf, PopPayload> CODEC = StreamCodec.unit(INSTANCE);

        public CustomPacketPayload.Type<PopPayload> type() {
            return TYPE;
        }
    }

    public record BindingPayload(boolean active, ResourceLocation dimension, BlockPos stake, int range, @org.jetbrains.annotations.Nullable java.util.UUID sublevel) implements CustomPacketPayload
    {
        public static final CustomPacketPayload.Type<BindingPayload> TYPE = new CustomPacketPayload.Type(BuildersBalloon.id("binding"));
        public static final StreamCodec<RegistryFriendlyByteBuf, BindingPayload> CODEC = new StreamCodec<RegistryFriendlyByteBuf, BindingPayload>(){

            public BindingPayload decode(RegistryFriendlyByteBuf buffer) {
                return new BindingPayload(buffer.readBoolean(), buffer.readResourceLocation(), buffer.readBlockPos(), buffer.readVarInt(), buffer.readBoolean() ? buffer.readUUID() : null);
            }

            public void encode(RegistryFriendlyByteBuf buffer, BindingPayload value) {
                buffer.writeBoolean(value.active);
                buffer.writeResourceLocation(value.dimension);
                buffer.writeBlockPos(value.stake);
                buffer.writeVarInt(value.range);
                buffer.writeBoolean(value.sublevel != null);
                if (value.sublevel != null) buffer.writeUUID(value.sublevel);
            }
        };

        public CustomPacketPayload.Type<BindingPayload> type() {
            return TYPE;
        }
    }
}
