package dev.tilosophy.buildersballoon.client;

import com.mojang.blaze3d.platform.InputConstants;
import dev.tilosophy.buildersballoon.BalloonNetwork;
import dev.tilosophy.buildersballoon.BuildersBalloon;
import dev.tilosophy.buildersballoon.TetherManager;
import dev.tilosophy.buildersballoon.StakeItem;
import net.minecraft.client.renderer.item.ItemProperties;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import dev.tilosophy.buildersballoon.client.StakeRenderer;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.settings.IKeyConflictContext;
import net.neoforged.neoforge.client.settings.KeyConflictContext;
import net.neoforged.neoforge.network.PacketDistributor;

@EventBusSubscriber(modid="builders_balloon", value={Dist.CLIENT})
public final class BalloonClient {
    public static final ModelResourceLocation BALLOON_MODEL = ModelResourceLocation.standalone((ResourceLocation)BuildersBalloon.id("block/balloon_tether/balloon"));
    public static ModelResourceLocation balloonModel(net.minecraft.world.item.DyeColor color) {
        return color == net.minecraft.world.item.DyeColor.BLUE ? BALLOON_MODEL
            : ModelResourceLocation.standalone(BuildersBalloon.id("block/balloon_tether/balloon_" + color.getSerializedName()));
    }
    private static KeyMapping pop;

    @SubscribeEvent
    public static void setup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            ItemProperties.register(BuildersBalloon.METAL_STAKE_ITEM.get(), BuildersBalloon.id("color"),
                (stack, level, entity, seed) -> StakeItem.color(stack).getId());
            ItemProperties.register(BuildersBalloon.METAL_BOLLARD_ITEM.get(), BuildersBalloon.id("color"),
                (stack, level, entity, seed) -> StakeItem.color(stack).getId());
            ItemProperties.register(BuildersBalloon.STAKE_ITEM.get(), BuildersBalloon.id("color"),
                (stack, level, entity, seed) -> StakeItem.color(stack).getId());
            ItemProperties.register(BuildersBalloon.BOLLARD_ITEM.get(), BuildersBalloon.id("color"),
                (stack, level, entity, seed) -> StakeItem.color(stack).getId());
        });
    }

    @SubscribeEvent
    public static void keys(RegisterKeyMappingsEvent event) {
        pop = new KeyMapping("key.builders_balloon.pop", (IKeyConflictContext)KeyConflictContext.IN_GAME, InputConstants.UNKNOWN, "key.categories.builders_balloon");
        event.register(pop);
    }

    @SubscribeEvent
    public static void models(ModelEvent.RegisterAdditional event) {
        for (var color : net.minecraft.world.item.DyeColor.values()) event.register(balloonModel(color));
    }

    @SubscribeEvent
    public static void renderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer((BlockEntityType)BuildersBalloon.STAKE_ENTITY.get(), StakeRenderer::new);
    }

    @EventBusSubscriber(modid="builders_balloon", value={Dist.CLIENT})
    public static final class GameEvents {
        @SubscribeEvent
        public static void tick(ClientTickEvent.Post event) {
            if (pop == null) {
                return;
            }
            Minecraft minecraft = Minecraft.getInstance();
            boolean clicked = false;
            while (pop.consumeClick()) {
                clicked = true;
            }
            if (clicked && minecraft.screen == null && minecraft.player != null && TetherManager.boundaryFor((Player)minecraft.player) != null) {
                PacketDistributor.sendToServer((CustomPacketPayload)BalloonNetwork.PopPayload.INSTANCE, (CustomPacketPayload[])new CustomPacketPayload[0]);
            }
        }

        @SubscribeEvent
        public static void logout(ClientPlayerNetworkEvent.LoggingOut event) {
            TetherManager.setClientBoundary(null, null, null);
        }
    }
}
