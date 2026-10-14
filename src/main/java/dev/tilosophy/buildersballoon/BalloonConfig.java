package dev.tilosophy.buildersballoon;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class BalloonConfig {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.IntValue RANGE;
    public static final ModConfigSpec.IntValue METAL_RANGE;

    private BalloonConfig() {
    }

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
        RANGE = builder.comment(new String[]{"Maximum distance from the stake centre along each axis, in blocks.", "A cube-shaped tether boundary; captured when an anchor is activated."}).defineInRange("range", 16, 8, 32);
        METAL_RANGE = builder.comment("Metal tether distance along each axis; captured when activated.").defineInRange("metalRange", 32, 8, 64);
        SPEC = builder.build();
    }
}
