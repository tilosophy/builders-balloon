package dev.tilosophy.buildersballoon;

import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.companion.ClientSubLevelAccess;
import dev.ryanhcode.sable.companion.math.Pose3dc;
import dev.ryanhcode.sable.sublevel.SubLevel;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3dc;

/** Only resolved when Sable is installed; no copied Sable implementation. */
final class SableAnchorFrames {
    static @Nullable AnchorFrames.Source at(Level level, BlockPos anchor) {
        SubLevel sublevel = Sable.HELPER.getContaining(level, anchor);
        if (sublevel == null) return Sable.HELPER.isInPlotGrid(level, anchor.getX() >> 4, anchor.getZ() >> 4) ? null : AnchorFrames.WORLD;
        return new AnchorFrames.Source() {
            public UUID id() { return sublevel.getUniqueId(); }
            public boolean valid() { return !sublevel.isRemoved() && Sable.HELPER.getContaining(level, anchor) == sublevel; }
            public TetherFrame logical() { return snapshot(sublevel.logicalPose()); }
            public TetherFrame render(float partialTick) {
                return snapshot(sublevel instanceof ClientSubLevelAccess client ? client.renderPose(partialTick) : sublevel.logicalPose());
            }
        };
    }

    private static TetherFrame snapshot(Pose3dc pose) {
        return new TetherFrame(vec(pose.position()), vec(pose.rotationPoint()), pose.orientation(), vec(pose.scale()));
    }

    private static Vec3 vec(Vector3dc v) { return new Vec3(v.x(), v.y(), v.z()); }
}
