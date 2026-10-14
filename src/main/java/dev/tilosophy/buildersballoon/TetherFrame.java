package dev.tilosophy.buildersballoon;

import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Quaterniond;
import org.joml.Quaterniondc;
import org.joml.Quaternionf;
import org.joml.Vector3d;

/** An immutable snapshot of an anchor's local-to-world transform. */
public final class TetherFrame {
    public static final TetherFrame WORLD = new TetherFrame(Vec3.ZERO, Vec3.ZERO, new Quaterniond(), new Vec3(1, 1, 1));
    private final Vec3 position, pivot, scale;
    private final Quaterniond rotation;

    public TetherFrame(Vec3 position, Vec3 pivot, Quaterniondc rotation, Vec3 scale) {
        this.position = position;
        this.pivot = pivot;
        this.rotation = new Quaterniond(rotation);
        this.scale = scale;
    }

    public Vec3 toWorld(Vec3 local) {
        return this == WORLD ? local : worldDelta(local.subtract(pivot)).add(position);
    }

    public Vec3 toLocal(Vec3 world) {
        return this == WORLD ? world : localDelta(world.subtract(position)).add(pivot);
    }

    public Vec3 worldDelta(Vec3 local) {
        if (this == WORLD) return local;
        Vector3d v = rotation.transform(new Vector3d(local.x * scale.x, local.y * scale.y, local.z * scale.z));
        return new Vec3(v.x, v.y, v.z);
    }

    public Vec3 localDelta(Vec3 world) {
        if (this == WORLD) return world;
        Vector3d v = rotation.transformInverse(new Vector3d(world.x, world.y, world.z));
        return new Vec3(v.x / scale.x, v.y / scale.y, v.z / scale.z);
    }

    public boolean contains(TetherBoundary bounds, Vec3 world) {
        Vec3 local = toLocal(world);
        return bounds.contains(local.x, local.y, local.z);
    }

    public Vec3 clamp(TetherBoundary bounds, Vec3 world) {
        if (!finite(world)) return world; // Preserve vanilla's invalid-packet rejection.
        Vec3 local = toLocal(world);
        if (bounds.contains(local.x, local.y, local.z)) return world;
        return toWorld(new Vec3(bounds.clampX(local.x), bounds.clampY(local.y), bounds.clampZ(local.z)));
    }

    public Vec3 constrain(TetherBoundary bounds, Vec3 worldPosition, Vec3 movement) {
        if (!finite(worldPosition) || !finite(movement)) return movement;
        Vec3 local = toLocal(worldPosition), delta = localDelta(movement);
        Vec3 clipped = new Vec3(bounds.movementX(local.x, delta.x), bounds.movementY(local.y, delta.y), bounds.movementZ(local.z, delta.z));
        // Avoid roundtrip precision drift for accepted movement.
        return clipped.equals(delta) ? movement : worldDelta(clipped);
    }

    /** Cancel a sublevel renderer's rotation/scale; keep its anchor translation. */
    public Matrix4f inverseLinear() {
        return new Matrix4f().scale((float)(1 / scale.x), (float)(1 / scale.y), (float)(1 / scale.z))
            .rotate(new Quaternionf(rotation).conjugate());
    }

    private static boolean finite(Vec3 v) {
        return Double.isFinite(v.x) && Double.isFinite(v.y) && Double.isFinite(v.z);
    }
}
