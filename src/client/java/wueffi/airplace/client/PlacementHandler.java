package wueffi.airplace.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3i;

import static wueffi.airplace.client.AirPlaceClient.placeKey;

public class PlacementHandler {

    public static BlockPos targetPos = new BlockPos(0,0,0);
    public static long lastPlaceTick = -20;

    public static void tick(MinecraftClient client) {
        if (!AirPlaceConfig.active || client == null || client.player == null || client.world == null) return;

        long currentTick = client.world.getTime();

        Vec3d eyePos = client.player.getCameraPosVec(1.0F);
        Vec3d lookVec = client.player.getRotationVec(1.0F);
        Vec3d end = eyePos.add(lookVec.multiply(5.0D));
        BlockHitResult hitResult = client.world.raycast(
                new net.minecraft.world.RaycastContext(
                        eyePos, end,
                        net.minecraft.world.RaycastContext.ShapeType.OUTLINE,
                        net.minecraft.world.RaycastContext.FluidHandling.NONE,
                        client.player
                )
        );

        Direction face;

        if (hitResult.getType() == HitResult.Type.BLOCK) {
            face = hitResult.getSide();
            targetPos = hitResult.getBlockPos().offset(face);
        } else {
            Vec3i vec3i = new Vec3i((int)Math.floor(end.x), (int)Math.floor(end.y), (int)Math.floor(end.z));
            targetPos = new BlockPos(vec3i);
            face = Direction.getFacing(lookVec.x, lookVec.y, lookVec.z);
        }


        if (placeKey.isPressed()  && currentTick > lastPlaceTick + 5) {
            lastPlaceTick = client.world.getTime();
            if (!client.world.getBlockState(targetPos).isAir()) return;

            ItemStack stack = client.player.getMainHandStack();
            if (!(stack.getItem() instanceof BlockItem)) return;

            BlockHitResult placeHit = new BlockHitResult(
                    Vec3d.ofCenter(targetPos),
                    face,
                    targetPos,
                    false
            );
            assert client.interactionManager != null;
            client.interactionManager.interactBlock(client.player, Hand.MAIN_HAND, placeHit);
        }
    }
}