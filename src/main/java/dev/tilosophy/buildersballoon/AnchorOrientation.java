package dev.tilosophy.buildersballoon;

import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;

/** Fixed quarter-turns about the mounting axis; zero preserves legacy placement. */
public final class AnchorOrientation {
    private AnchorOrientation() {}

    public static int towardPlayer(Direction mount, Vec3 offset) {
        double x = switch (mount) {
            case DOWN, UP, SOUTH -> offset.x;
            case NORTH -> -offset.x;
            case WEST -> offset.z;
            case EAST -> -offset.z;
        };
        double z = switch (mount) {
            case DOWN -> offset.z;
            case UP -> -offset.z;
            default -> offset.y;
        };
        if (!Double.isFinite(x) || !Double.isFinite(z) || x*x+z*z < 1e-10) return 0;
        return Math.floorMod((int)Math.round(Math.atan2(-x,z)/(Math.PI/2)),4);
    }

    public static Vec3 spinLocal(Vec3 point, int spin) {
        return switch (spin) {
            case 0 -> point;
            case 1 -> new Vec3(-point.z,point.y,point.x);
            case 2 -> new Vec3(-point.x,point.y,-point.z);
            case 3 -> new Vec3(point.z,point.y,-point.x);
            default -> throw new IllegalArgumentException("Invalid anchor spin");
        };
    }
}
