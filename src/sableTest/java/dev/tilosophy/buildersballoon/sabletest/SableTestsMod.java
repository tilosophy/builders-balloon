package dev.tilosophy.buildersballoon.sabletest;

import dev.tilosophy.buildersballoon.test.StakeGameTests;

import net.neoforged.fml.common.Mod;
import net.neoforged.fml.ModList;

@Mod("builders_balloon_sable_tests")
public final class SableTestsMod {
    public SableTestsMod() {
        if (!ModList.get().isLoaded("sable")) throw new IllegalStateException("Real Sable fixture is required");
    }
}
