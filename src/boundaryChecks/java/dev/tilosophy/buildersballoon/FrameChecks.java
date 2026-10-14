package dev.tilosophy.buildersballoon;

import java.util.Random;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaterniond;
import org.joml.Matrix4f;
import org.joml.Quaternionf;

/** Deterministic translated/rotated/scaled geometry regression checks. */
public final class FrameChecks {
    public static void run() {
        Random random = new Random(81121);
        int checks = 0;
        Vec3 pivot = new Vec3(25_000_008.5, 128.5, 26_000_008.5);
        for (int range : new int[]{8, 16, 32, 48, 64}) {
            TetherBoundary bounds = new TetherBoundary(pivot.x, pivot.y, pivot.z, range);
            for (int i = 0; i < 20_000; i++) {
                var rotation = new Quaterniond().rotationXYZ(random.nextDouble()*6, random.nextDouble()*6, random.nextDouble()*6);
                Vec3 scale = new Vec3(0.5+random.nextDouble()*2, 0.5+random.nextDouble()*2, 0.5+random.nextDouble()*2);
                TetherFrame frame = new TetherFrame(new Vec3(120, 80, -250), pivot, rotation, scale);
                Vec3 local = pivot.add((random.nextDouble()*2-1)*range, (random.nextDouble()*2-1)*range, (random.nextDouble()*2-1)*range);
                Vec3 world = frame.toWorld(local);
                if (frame.toLocal(world).distanceTo(local) > 1E-7) throw new AssertionError("Frame position roundtrip");
                Vec3 delta = new Vec3((random.nextDouble()*2-1)*range*5, (random.nextDouble()*2-1)*range*5, (random.nextDouble()*2-1)*range*5);
                Vec3 moved = frame.constrain(bounds, world, frame.worldDelta(delta));
                Vec3 end = frame.toLocal(world.add(moved));
                if (Math.abs(end.x-pivot.x)>range+1E-7 || Math.abs(end.y-pivot.y)>range+1E-7 || Math.abs(end.z-pivot.z)>range+1E-7) throw new AssertionError("Rotated movement escapes");
                Vec3 clamped = frame.toLocal(frame.clamp(bounds, world.add(frame.worldDelta(delta))));
                if (Math.abs(clamped.x-pivot.x)>range+1E-7 || Math.abs(clamped.y-pivot.y)>range+1E-7 || Math.abs(clamped.z-pivot.z)>range+1E-7) throw new AssertionError("Rotated packet clamp escapes");
                var basis = new Matrix4f().rotate(new Quaternionf(rotation)).scale((float)scale.x,(float)scale.y,(float)scale.z).mul(frame.inverseLinear());
                if (!basis.equals(new Matrix4f(), 2E-6f)) throw new AssertionError("Balloon orientation cancellation");
                checks++;
            }
        }
        Vec3 invalid = new Vec3(Double.NaN, 0, 0);
        if (TetherFrame.WORLD.clamp(new TetherBoundary(0,0,0,16),invalid)!=invalid) throw new AssertionError("Invalid packet changed");checks++;
        Vec3 delta = new Vec3(0.125,0.25,-0.375);
        if (TetherFrame.WORLD.constrain(new TetherBoundary(0,0,0,16),Vec3.ZERO,delta)!=delta) throw new AssertionError("Accepted world movement changed");checks++;
        System.out.println("Translated/rotated frame checks passed: " + checks);
    }
}
