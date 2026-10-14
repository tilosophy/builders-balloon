package dev.tilosophy.buildersballoon.mixin;

import dev.tilosophy.buildersballoon.TetherManager;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(value={Entity.class}, remap=false)
public abstract class EntityMovementMixin {
    @ModifyVariable(method={"move"}, at=@At(value="HEAD"), argsOnly=true, require=1)
    private Vec3 balloonOnAStick$constrain(Vec3 movement) {
        Vec3 vec3;
        Object entityMovementMixin = this;
        if (entityMovementMixin instanceof Player) {
            Player player = (Player)entityMovementMixin;
            vec3 = TetherManager.constrainMovement(player, movement);
        } else {
            vec3 = movement;
        }
        return vec3;
    }
}
