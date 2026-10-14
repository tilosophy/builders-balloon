package dev.tilosophy.buildersballoon.test;
import net.neoforged.fml.common.Mod;
import dev.tilosophy.buildersballoon.BuildersBalloon;
@Mod("builders_balloon_tests")
public final class RuntimeTestsMod {
    public RuntimeTestsMod() {
        var source = BuildersBalloon.class.getProtectionDomain().getCodeSource().getLocation().toString();
        System.out.println("Runtime candidate code source: " + source);
        if (Boolean.getBoolean("balloon.tests.requireJar") && !source.contains(".jar")) throw new IllegalStateException("Packaged JAR required");
    }
}
