package dev.tilosophy.buildersballoon.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.tilosophy.buildersballoon.StakeBlock;
import dev.tilosophy.buildersballoon.AnchorFrames;
import dev.tilosophy.buildersballoon.TetherFrame;
import dev.tilosophy.buildersballoon.StakeBlockEntity;
import dev.tilosophy.buildersballoon.client.BalloonClient;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.BlockModelRotation;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Position;
import net.minecraft.core.Vec3i;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.RenderTypeHelper;
import net.neoforged.neoforge.client.model.data.ModelData;
import org.joml.Matrix4f;
import org.joml.Vector3f;

public final class StakeRenderer
implements BlockEntityRenderer<StakeBlockEntity> {
    private static final float BALLOON_SCALE = 1.2f; // 1.5 times the former 0.8 scale.
    private final BlockRenderDispatcher dispatcher;

    public StakeRenderer(BlockEntityRendererProvider.Context context) {
        this.dispatcher = context.getBlockRenderDispatcher();
    }

    public boolean shouldRenderOffScreen(StakeBlockEntity stake) {
        return (Boolean)stake.getBlockState().getValue((Property)StakeBlock.CONNECTED) != false && stake.owner() != null;
    }

    public int getViewDistance() {
        return 256;
    }

    public AABB getRenderBoundingBox(StakeBlockEntity stake) {
        return new AABB(stake.getBlockPos()).inflate(68.0);
    }

    public void render(StakeBlockEntity stake, float partialTick, PoseStack poses, MultiBufferSource buffers, int light, int overlay) {
        Level level = stake.getLevel();
        if (level == null || !((Boolean)stake.getBlockState().getValue((Property)StakeBlock.CONNECTED)).booleanValue() || stake.owner() == null) {
            return;
        }
        Player player = level.getPlayerByUUID(stake.owner());
        if (player == null) {
            return;
        }
        AnchorFrames.Source source = AnchorFrames.at(level, stake.getBlockPos());
        if (source == null || !source.valid()) return;
        TetherFrame frame = source.render(partialTick);
        Vec3 origin = frame.toWorld(Vec3.atLowerCornerOf((Vec3i)stake.getBlockPos()));
        // Match the painted anchor's model rotation, then apply the sublevel frame.
        BlockModelRotation rotation = switch (stake.getBlockState().getValue(StakeBlock.DIRECTION)) {
            case DOWN -> BlockModelRotation.X0_Y0;
            case UP -> BlockModelRotation.X180_Y0;
            case NORTH -> BlockModelRotation.X90_Y180;
            case SOUTH -> BlockModelRotation.X90_Y0;
            case WEST -> BlockModelRotation.X90_Y90;
            case EAST -> BlockModelRotation.X90_Y270;
        };
        Vec3 localKnot = dev.tilosophy.buildersballoon.AnchorOrientation.spinLocal(
            new Vec3(0, 7.05 / 16 - 0.5, 11.65 / 16 - 0.5), stake.getBlockState().getValue(StakeBlock.SPIN));
        Vector3f knot = new Vector3f((float)localKnot.x, (float)localKnot.y, (float)localKnot.z);
        rotation.getRotation().getMatrix().transformPosition(knot);
        Vec3 anchor = frame.toWorld(Vec3.atCenterOf(stake.getBlockPos()).add(knot.x, knot.y, knot.z));
        poses.pushPose();
        if (source.id() != null) poses.mulPose(frame.inverseLinear());
        Vec3 body = StakeRenderer.bodyCentre(player, partialTick);
        double theta = (double)(Mth.rotLerp((float)partialTick, (float)player.yBodyRotO, (float)player.yBodyRot) * ((float)Math.PI / 180)) + 1.5707963267948966;
        double bob = Math.sin((double)((float)level.getGameTime() + partialTick) * 0.05) * 0.05;
        Vec3 balloon = body.add(-Math.cos(theta) * 0.5, 1.5 + bob, -Math.sin(theta) * 0.5);
        int playerLight = LevelRenderer.getLightColor((BlockAndTintGetter)level, (BlockPos)BlockPos.containing((Position)body));
        StakeRenderer.rope(poses, buffers, origin, anchor, body, light, playerLight);
        StakeRenderer.rope(poses, buffers, origin, body, balloon, playerLight, playerLight);
        poses.pushPose();
        Vec3 offset = balloon.subtract(origin).subtract(BALLOON_SCALE * 0.5, 0.0, BALLOON_SCALE * 0.5);
        poses.translate(offset.x, offset.y, offset.z);
        poses.scale(BALLOON_SCALE, BALLOON_SCALE, BALLOON_SCALE);
        BakedModel model = Minecraft.getInstance().getModelManager().getModel(BalloonClient.balloonModel(stake.getBlockState().getValue(StakeBlock.COLOR)));
        BlockState state = stake.getBlockState();
        for (RenderType type : model.getRenderTypes(state, RandomSource.create((long)42L), ModelData.EMPTY)) {
            VertexConsumer buffer = buffers.getBuffer(RenderTypeHelper.getEntityRenderType((RenderType)type, (boolean)false));
            this.dispatcher.getModelRenderer().renderModel(poses.last(), buffer, state, model, 1.0f, 1.0f, 1.0f, playerLight, overlay, ModelData.EMPTY, type);
        }
        poses.popPose();
        poses.popPose();
    }

    private static Vec3 bodyCentre(Player player, float partialTick) {
        float rotY = Mth.rotLerp((float)partialTick, (float)player.yBodyRotO, (float)player.yBodyRot) * ((float)Math.PI / 180);
        float rotX = Mth.lerp((float)partialTick, (float)player.xRotO, (float)player.getXRot()) * ((float)Math.PI / 180);
        if (player.isFallFlying() || player.isAutoSpinAttack()) {
            return player.getPosition(partialTick).add(new Vec3(0.0, -0.11, 0.85).xRot(-rotX).yRot(-rotY));
        }
        if (player.isVisuallySwimming()) {
            return player.getPosition(partialTick).add(new Vec3(0.0, 0.2, -0.15).xRot(-rotX).yRot(-rotY));
        }
        return player.getPosition(partialTick).add(new Vec3(0.0, player.getBoundingBox().getYsize() - 0.75, player.isCrouching() ? -0.2 : 0.07).yRot(-rotY));
    }

    private static void rope(PoseStack poses, MultiBufferSource buffers, Vec3 origin, Vec3 start, Vec3 end, int lightA, int lightB) {
        int i;
        poses.pushPose();
        Vec3 offset = start.subtract(origin);
        poses.translate(offset.x, offset.y, offset.z);
        Vec3 delta = end.subtract(start);
        double horizontal = Math.hypot(delta.x, delta.z);
        float crossX = horizontal < 1.0E-6 ? 0.0125f : (float)(delta.z / horizontal * 0.0125);
        float crossZ = horizontal < 1.0E-6 ? 0.0f : (float)(delta.x / horizontal * 0.0125);
        VertexConsumer consumer = buffers.getBuffer(RenderType.leash());
        Matrix4f matrix = poses.last().pose();
        for (i = 0; i <= 24; ++i) {
            StakeRenderer.pair(consumer, matrix, delta, crossX, crossZ, i, false, lightA, lightB);
        }
        for (i = 24; i >= 0; --i) {
            StakeRenderer.pair(consumer, matrix, delta, crossX, crossZ, i, true, lightA, lightB);
        }
        poses.popPose();
    }

    private static void pair(VertexConsumer consumer, Matrix4f matrix, Vec3 delta, float cx, float cz, int index, boolean reverse, int lightA, int lightB) {
        float t = (float)index / 24.0f;
        float x = (float)delta.x * t;
        float z = (float)delta.z * t;
        float y = (float)(delta.y > 0.0 ? delta.y * (double)t * (double)t : delta.y - delta.y * (double)(1.0f - t) * (double)(1.0f - t));
        float shade = index % 2 == (reverse ? 1 : 0) ? 0.7f : 1.0f;
        int light = (int)Mth.lerp((float)t, (float)(lightA & 0xFFFF), (float)(lightB & 0xFFFF)) | (int)Mth.lerp((float)t, (float)(lightA >>> 16), (float)(lightB >>> 16)) << 16;
        float lower = reverse ? 0.0f : 0.025f;
        consumer.addVertex(matrix, x - cx, y + lower, z + cz).setColor(128 / 255f * shade, 112 / 255f * shade, 98 / 255f * shade, 1.0f).setLight(light);
        consumer.addVertex(matrix, x + cx, y + 0.025f - lower, z - cz).setColor(128 / 255f * shade, 112 / 255f * shade, 98 / 255f * shade, 1.0f).setLight(light);
    }
}
