package dev.tilosophy.buildersballoon;

import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.neoforged.fml.ModList;
import org.jetbrains.annotations.Nullable;

/** Optional integration boundary: ordinary installs never load Sable API classes. */
public final class AnchorFrames {
    private AnchorFrames() {}

    public interface Source {
        @Nullable UUID id();
        boolean valid();
        TetherFrame logical();
        TetherFrame render(float partialTick);
    }

    public static final Source WORLD = new Source() {
        public UUID id() { return null; }
        public boolean valid() { return true; }
        public TetherFrame logical() { return TetherFrame.WORLD; }
        public TetherFrame render(float partialTick) { return TetherFrame.WORLD; }
    };

    /** Null means an unavailable plot, not an ordinary-world anchor. */
    public static @Nullable Source at(Level level, BlockPos anchor) {
        return ModList.get().isLoaded("sable") ? SableAnchorFrames.at(level, anchor) : WORLD;
    }
}
