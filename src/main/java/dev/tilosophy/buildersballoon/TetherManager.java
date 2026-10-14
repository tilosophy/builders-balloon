package dev.tilosophy.buildersballoon;

import dev.tilosophy.buildersballoon.BalloonConfig;
import dev.tilosophy.buildersballoon.BalloonNetwork;
import dev.tilosophy.buildersballoon.BuildersBalloon;
import dev.tilosophy.buildersballoon.StakeBlockEntity;
import dev.tilosophy.buildersballoon.TetherBoundary;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Position;
import net.minecraft.core.Vec3i;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.RelativeMovement;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.NeoForgeMod;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.level.ChunkEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

@EventBusSubscriber(modid="builders_balloon")
public final class TetherManager {
    private static final Map<UUID, Binding> BINDINGS = new HashMap<UUID, Binding>();
    private static final AttributeModifier FLIGHT = new AttributeModifier(BuildersBalloon.id("tether_flight"), 1.0, AttributeModifier.Operation.ADD_VALUE);
    @Nullable
    private static volatile ClientBinding clientBinding;

    private TetherManager() {
    }

    public enum Activation { BOUND, UNCHANGED, OCCUPIED, OUT_OF_RANGE, UNAVAILABLE }

    public static void bind(ServerPlayer player, StakeBlockEntity stake) {
        bindInternal(player, stake, true);
    }

    /** Activate any inactive in-world anchor without teleporting or taking another player's stake. */
    public static Activation activate(ServerPlayer player, StakeBlockEntity stake) {
        return bindInternal(player, stake, false);
    }

    private static Activation bindInternal(ServerPlayer player, StakeBlockEntity stake, boolean allowReposition) {
        AttributeInstance flight = player.getAttribute(NeoForgeMod.CREATIVE_FLIGHT);
        if (flight == null || !player.isAlive() || stake.getLevel() != player.level() || player instanceof net.neoforged.neoforge.common.util.FakePlayer) {
            return Activation.UNAVAILABLE;
        }
        if (stake.owner() != null) {
            return stake.owner().equals(player.getUUID()) ? Activation.UNCHANGED : Activation.OCCUPIED;
        }
        AnchorFrames.Source source = AnchorFrames.at(player.level(), stake.getBlockPos());
        if (source == null || !source.valid()) return Activation.UNAVAILABLE;
        Binding next = new Binding(player, stake, source);
        TetherFrame frame = source.logical();
        Vec3 start = player.position();
        if (!frame.contains(next.boundary, start)) {
            if (!allowReposition) return Activation.OUT_OF_RANGE;
            start = findSafeInside(player, next.boundary, frame, start);
            if (start == null) {
                player.displayClientMessage(Component.translatable("message.builders_balloon.no_space"), true);
                return Activation.UNAVAILABLE;
            }
        }
        boolean wasFlying = player.getAbilities().flying;
        TetherManager.release(player, true);
        BINDINGS.put(player.getUUID(), next);
        stake.setOwner(player.getUUID());
        flight.addTransientModifier(FLIGHT);
        if (wasFlying) {
            player.getAbilities().flying = true;
            player.onUpdateAbilities();
        }
        if (!frame.contains(next.boundary, player.position())) {
            next.lastSafePosition = frame.toLocal(start);
            player.connection.teleport(start.x, start.y, start.z, player.getYRot(), player.getXRot());
        }
        PacketDistributor.sendToPlayer((ServerPlayer)player, (CustomPacketPayload)new BalloonNetwork.BindingPayload(true, player.level().dimension().location(), stake.getBlockPos(), next.boundary.range(), source.id()), (CustomPacketPayload[])new CustomPacketPayload[0]);
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(), (SoundEvent)BuildersBalloon.INFLATE.get(), SoundSource.PLAYERS, 0.3f, 1.0f);
        return Activation.BOUND;
    }

    public static void release(ServerPlayer player, boolean notify) {
        Binding binding = BINDINGS.remove(player.getUUID());
        if (binding == null) {
            return;
        }
        binding.stake.setOwner(null);
        AttributeInstance flight = player.getAttribute(NeoForgeMod.CREATIVE_FLIGHT);
        if (flight != null) {
            flight.removeModifier(FLIGHT.id());
        }
        if (!player.mayFly()) {
            player.getAbilities().flying = false;
            player.onUpdateAbilities();
        }
        player.resetFallDistance();
        if (notify) {
            PacketDistributor.sendToPlayer((ServerPlayer)player, (CustomPacketPayload)new BalloonNetwork.BindingPayload(false, binding.level.dimension().location(), binding.stake.getBlockPos(), binding.boundary.range(), binding.source.id()), (CustomPacketPayload[])new CustomPacketPayload[0]);
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(), (SoundEvent)BuildersBalloon.POP.get(), SoundSource.PLAYERS, 0.5f, 1.0f);
        }
    }

    public static void releaseStake(StakeBlockEntity stake) {
        if (stake.getLevel() == null || stake.getLevel().isClientSide || stake.owner() == null) {
            return;
        }
        Binding binding = BINDINGS.get(stake.owner());
        if (binding != null && binding.stake == stake) {
            TetherManager.release(binding.player, true);
        }
    }

    @Nullable
    public static TetherBoundary boundaryFor(Player player) {
        if (player.level().isClientSide) {
            ClientBinding binding = clientBinding;
            return binding != null && binding.player.equals(player.getUUID()) && binding.dimension.equals((Object)player.level().dimension()) ? binding.boundary : null;
        }
        Binding binding = BINDINGS.get(player.getUUID());
        return binding != null && binding.level == player.level() ? binding.boundary : null;
    }

    public static void setClientBoundary(@Nullable UUID player, @Nullable ResourceKey<Level> dimension, @Nullable TetherBoundary boundary) {
        setClientBoundary(player, dimension, boundary, null);
    }

    public static void setClientBoundary(@Nullable UUID player, @Nullable ResourceKey<Level> dimension, @Nullable TetherBoundary boundary, @Nullable UUID sublevel) {
        clientBinding = player != null && dimension != null && boundary != null ? new ClientBinding(player, dimension, boundary, sublevel) : null;
    }

    @Nullable
    private static TetherFrame frameFor(Player player) {
        if (!player.level().isClientSide) {
            Binding binding = BINDINGS.get(player.getUUID());
            return binding != null && binding.level == player.level() && binding.source.valid() ? binding.source.logical() : null;
        }
        ClientBinding binding = clientBinding;
        if (binding == null || !binding.player.equals(player.getUUID()) || !binding.dimension.equals(player.level().dimension())) return null;
        BlockPos anchor = BlockPos.containing(binding.boundary.x(), binding.boundary.y(), binding.boundary.z());
        AnchorFrames.Source source = AnchorFrames.at(player.level(), anchor);
        return source != null && source.valid() && java.util.Objects.equals(source.id(), binding.sublevel) ? source.logical() : null;
    }

    /** Clamp a full packet position: rotated frames couple the world axes. */
    public static Vec3 constrainPosition(Player player, Vec3 position) {
        TetherBoundary bounds = boundaryFor(player);
        TetherFrame frame = frameFor(player);
        return bounds == null || frame == null ? position : frame.clamp(bounds, position);
    }

    public static Vec3 constrainMovement(Player player, Vec3 movement) {
        TetherBoundary boundary = TetherManager.boundaryFor(player);
        TetherFrame frame = frameFor(player);
        return boundary == null || frame == null ? movement : frame.constrain(boundary, player.position(), movement);
    }

    @Nullable
    private static Vec3 findSafeInside(ServerPlayer player, TetherBoundary boundary, TetherFrame frame, Vec3 preferred) {
        Vec3 bounded = frame.clamp(boundary, preferred);
        if (TetherManager.safePosition(player, bounded)) {
            return bounded;
        }
        Vec3 closest = null;
        double distance = Double.POSITIVE_INFINITY;
        for (int dx = -2; dx <= 2; ++dx) {
            for (int dy = -2; dy <= 3; ++dy) {
                for (int dz = -2; dz <= 2; ++dz) {
                    Vec3 candidate = bounded.add((double)dx, (double)dy, (double)dz);
                    double candidateDistance = candidate.distanceToSqr(preferred);
                    if (!(candidateDistance < distance) || !frame.contains(boundary, candidate) || !TetherManager.safePosition(player, candidate)) continue;
                    closest = candidate;
                    distance = candidateDistance;
                }
            }
        }
        return closest;
    }

    private static boolean safePosition(ServerPlayer player, Vec3 position) {
        BlockPos pos = BlockPos.containing((Position)position);
        return player.serverLevel().hasChunkAt(pos) && player.serverLevel().noCollision((Entity)player, player.getBoundingBox().move(position.subtract(player.position())));
    }

    @SubscribeEvent
    public static void tick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (!(player instanceof ServerPlayer)) {
            return;
        }
        ServerPlayer player2 = (ServerPlayer)player;
        Binding binding = BINDINGS.get(player2.getUUID());
        if (binding == null) {
            return;
        }
        if (!player2.isAlive() || player2.level() != binding.level || binding.stake.isRemoved() || !binding.source.valid() || !binding.level.hasChunkAt(binding.stake.getBlockPos()) || binding.level.getBlockEntity(binding.stake.getBlockPos()) != binding.stake) {
            TetherManager.release(player2, true);
            return;
        }
        TetherBoundary b = binding.boundary;
        TetherFrame frame = binding.source.logical();
        if (!frame.contains(b, player2.position())) {
            player2.stopRiding();
            Vec3 safe = frame.toWorld(binding.lastSafePosition);
            if (!TetherManager.safePosition(player2, safe)) {
                if ((safe = TetherManager.findSafeInside(player2, b, frame, safe)) == null) {
                    TetherManager.release(player2, true);
                    return;
                }
                binding.lastSafePosition = frame.toLocal(safe);
            }
            // Zero relative rotation preserves the client's current view during repeated recovery.
            player2.connection.teleport(safe.x, safe.y, safe.z, player2.getYRot(), player2.getXRot(),
                    Set.of(RelativeMovement.Y_ROT, RelativeMovement.X_ROT));
        } else {
            binding.lastSafePosition = frame.toLocal(player2.position());
        }
        Vec3 velocity = player2.getDeltaMovement();
        Vec3 clipped = TetherManager.constrainMovement((Player)player2, velocity);
        if (!velocity.equals((Object)clipped)) {
            player2.setDeltaMovement(clipped);
            player2.hurtMarked = true;
        }
    }

    @SubscribeEvent
    public static void miningSpeed(PlayerEvent.BreakSpeed event) {
        Player player = event.getEntity();
        if (!player.onGround() && player.getAbilities().flying && TetherManager.boundaryFor(player) != null) {
            event.setNewSpeed(event.getNewSpeed() * 5.0f);
        }
    }

    @SubscribeEvent(priority=EventPriority.LOWEST)
    public static void death(LivingDeathEvent event) {
        LivingEntity livingEntity = event.getEntity();
        if (livingEntity instanceof ServerPlayer) {
            ServerPlayer player = (ServerPlayer)livingEntity;
            TetherManager.release(player, true);
        }
    }

    @SubscribeEvent
    public static void logout(PlayerEvent.PlayerLoggedOutEvent event) {
        Player player = event.getEntity();
        if (player instanceof ServerPlayer) {
            ServerPlayer player2 = (ServerPlayer)player;
            TetherManager.release(player2, false);
        }
    }

    @SubscribeEvent
    public static void dimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        Player player = event.getEntity();
        if (player instanceof ServerPlayer) {
            ServerPlayer player2 = (ServerPlayer)player;
            TetherManager.release(player2, true);
        }
    }

    @SubscribeEvent
    public static void unload(ChunkEvent.Unload event) {
        LevelAccessor levelAccessor = event.getLevel();
        if (!(levelAccessor instanceof ServerLevel)) {
            return;
        }
        ServerLevel level = (ServerLevel)levelAccessor;
        for (Binding binding : List.copyOf(BINDINGS.values())) {
            if (binding.level != level || binding.stake.getBlockPos().getX() >> 4 != event.getChunk().getPos().x || binding.stake.getBlockPos().getZ() >> 4 != event.getChunk().getPos().z) continue;
            TetherManager.release(binding.player, true);
        }
    }

    @SubscribeEvent
    public static void stopped(ServerStoppedEvent event) {
        BINDINGS.clear();
    }

    private static final class Binding {
        final ServerPlayer player;
        final StakeBlockEntity stake;
        final ServerLevel level;
        final TetherBoundary boundary;
        final AnchorFrames.Source source;
        Vec3 lastSafePosition;

        Binding(ServerPlayer player, StakeBlockEntity stake, AnchorFrames.Source source) {
            this.player = player;
            this.stake = stake;
            this.source = source;
            this.level = player.serverLevel();
            Vec3 centre = Vec3.atCenterOf((Vec3i)stake.getBlockPos());
            this.boundary = new TetherBoundary(centre.x, centre.y, centre.z, stake.getBlockState().is(BuildersBalloon.METAL_STAKE.get()) ? BalloonConfig.METAL_RANGE.get() : BalloonConfig.RANGE.get());
            this.lastSafePosition = source.logical().toLocal(player.position());
        }
    }

    private record ClientBinding(UUID player, ResourceKey<Level> dimension, TetherBoundary boundary, @Nullable UUID sublevel) {
    }
}
