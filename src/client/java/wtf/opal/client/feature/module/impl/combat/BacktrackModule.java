package wtf.opal.client.feature.module.impl.combat;//package wtf.opal.client.feature.module.impl.combat;
//
//import com.ibm.icu.impl.Pair;
//import net.minecraft.client.render.VertexConsumerProvider;
//import net.minecraft.client.util.BufferAllocator;
//import net.minecraft.entity.LivingEntity;
//import net.minecraft.entity.TrackedPosition;
//import net.minecraft.entity.player.PlayerEntity;
//import net.minecraft.network.packet.s2c.play.*;
//import net.minecraft.util.math.Direction;
//import net.minecraft.util.math.Vec3d;
//import wtf.opal.client.feature.helper.impl.player.packet.blockage.block.holder.BlockHolder;
//import wtf.opal.client.feature.helper.impl.player.packet.blockage.impl.InboundNetworkBlockage;
//import wtf.opal.client.feature.module.Module;
//import wtf.opal.client.feature.module.ModuleCategory;
//import wtf.opal.client.feature.module.property.impl.bool.BooleanProperty;
//import wtf.opal.client.feature.module.property.impl.number.BoundedNumberProperty;
//import wtf.opal.client.feature.module.property.impl.number.NumberProperty;
//import wtf.opal.client.renderer.world.WorldRenderer;
//import wtf.opal.event.impl.game.PreGameTickEvent;
//import wtf.opal.event.impl.game.packet.InstantaneousReceivePacketEvent;
//import wtf.opal.event.impl.game.player.interaction.AttackEvent;
//import wtf.opal.event.impl.render.RenderWorldEvent;
//import wtf.opal.event.subscriber.Subscribe;
//import wtf.opal.mixin.EntityAccessor;
//import wtf.opal.mixin.EntityS2CPacketAccessor;
//import wtf.opal.utility.misc.chat.ChatUtility;
//import wtf.opal.utility.misc.math.MathUtility;
//import wtf.opal.utility.player.PlayerUtility;
//import wtf.opal.utility.render.ColorUtility;
//import wtf.opal.utility.render.CustomRenderLayers;
//
//import static wtf.opal.client.Constants.mc;
//
//public final class BacktrackModule extends Module {
//
//    private final BlockHolder iBlockHolder = new BlockHolder(InboundNetworkBlockage.get());
//    private boolean blinking;
//
//    private TrackedPlayer trackedPlayer;
//
//    private final BoundedNumberProperty range = new BoundedNumberProperty("Range", 3, 6, 1, 6, 0.1);
//    private final NumberProperty chance = new NumberProperty("Chance", "%", 100, 5, 100, 1).id("chance2");
//
//    private final BooleanProperty resetOnAttack = new BooleanProperty("Reset on attack", false);
//
//    public BacktrackModule() {
//        super("Backtrack", "Gives you more reach using a legit strategy.", ModuleCategory.COMBAT);
//        addProperties(range, chance, resetOnAttack);
//    }
//
//    @Subscribe
//    public void onPreGameTick(final PreGameTickEvent event) {
//        if (trackedPlayer == null) {
//            release();
//        }
//    }
//
//    @Subscribe
//    public void onRenderWorld(final RenderWorldEvent event) {
//        if (trackedPlayer == null) return;
//
//        final Vec3d dimensions = new Vec3d(trackedPlayer.player.getWidth(), trackedPlayer.player.getHeight(), trackedPlayer.player.getWidth());
//
//        VertexConsumerProvider.Immediate vcp = VertexConsumerProvider.immediate(new BufferAllocator(1024));
//        WorldRenderer rc = new WorldRenderer(vcp);
//
//        rc.drawFilledCube(
//                event.matrixStack(),
//                CustomRenderLayers.getPositionColorQuads(true),
//                trackedPlayer.trackedPosition.getPos().subtract(0.25, 0, 0.25), dimensions,
//                ColorUtility.applyOpacity(ColorUtility.getClientTheme().first, 0.25F)
//        );
//
//        vcp.draw();
//    }
//
//    @Subscribe
//    public void onAttack(final AttackEvent event) {
//        if (event.getTarget() instanceof PlayerEntity entity) {
//            if (Math.random() <= (chance.getValue() / 100)) {
//                if (trackedPlayer == null || resetOnAttack.getValue()) {
//                    trackedPlayer = new TrackedPlayer(entity, entity.getPos(), System.currentTimeMillis());
//                    release();
//                }
//            } else {
//                trackedPlayer = null;
//            }
//        }
//    }
//
//    @Subscribe
//    public void onInstantaneousReceivePacket(final InstantaneousReceivePacketEvent event) {
//        if (trackedPlayer == null) return;
//
//        switch (event.getPacket()) {
//            // Update tracked player position
//            case EntityS2CPacket move -> {
//                final EntityS2CPacketAccessor accessor = (EntityS2CPacketAccessor) move;
//                if (accessor.getId() == trackedPlayer.player.getId()) {
//                    final Vec3d trackedPosition = trackedPlayer.trackedPosition.withDelta(move.getDeltaX(), move.getDeltaY(), move.getDeltaZ());
//                    trackedPlayer.trackedPosition.setPos(trackedPosition);
//
//                    runDistanceCheck();
//                }
//            }
//            // Override tracked player position due to teleport
//            case EntityPositionS2CPacket teleport -> {
//                if (teleport.entityId() == trackedPlayer.player.getId()) {
//                    final Vec3d teleportPosition = teleport.change().position();
//                    trackedPlayer.trackedPosition.setPos(teleportPosition);
//
//                    runDistanceCheck();
//                }
//            }
//            // Reset due to tracked player being removed from world
//            case EntitiesDestroyS2CPacket destroyEntities -> {
//                if (destroyEntities.getEntityIds().contains(trackedPlayer.player.getId())) {
//                    trackedPlayer = null;
//                }
//            }
//            // Reset due to local player lag back
//            case PlayerPositionLookS2CPacket posLook -> {
//                trackedPlayer = null;
//            }
//            default -> {
//            }
//        }
//    }
//
//    private void runDistanceCheck() {
//        final Vec3d eyePos = mc.player.getEyePos();
//        final EntityAccessor entityAccessor = (EntityAccessor) mc.player;
//        final Vec3d closestVectorToRealPos = PlayerUtility.getClosestVectorToBox(
//                eyePos,
//                entityAccessor.callCalculateDefaultBoundingBox(trackedPlayer.trackedPosition.getPos()).expand(trackedPlayer.player.getTargetingMargin())
//        );
//        final Vec3d closestVectorToFakePos = PlayerUtility.getClosestVectorToBoundingBox(
//                eyePos,
//                trackedPlayer.player
//        );
//
//        final double distanceToRealPos = eyePos.distanceTo(closestVectorToRealPos);
//        final double distanceToFakePos = eyePos.distanceTo(closestVectorToFakePos);
//
//        if (distanceToFakePos <= mc.player.getEntityInteractionRange() && distanceToRealPos < range.getValue().second) {
//            iBlockHolder.block();
//            blinking = true;
//        } else {
//            trackedPlayer = null;
//        }
//    }
//
//    @Override
//    protected void onEnable() {
//        trackedPlayer = null;
//        super.onEnable();
//    }
//
//    @Override
//    protected void onDisable() {
//        trackedPlayer = null;
//        release();
//        super.onDisable();
//    }
//
//    private void release() {
//        if (blinking) {
//            iBlockHolder.release();
//            blinking = false;
//        }
//    }
//
//    private static class TrackedPlayer {
//        private final PlayerEntity player;
//        private final TrackedPosition trackedPosition;
//        private Vec3d trackedVelocity;
//        private final long attackTime;
//
//        public TrackedPlayer(final PlayerEntity player, final Vec3d trackedPosition, final long attackTime) {
//            this.player = player;
//
//            this.trackedPosition = new TrackedPosition();
//            this.trackedPosition.setPos(trackedPosition);
//
//            this.attackTime = attackTime;
//        }
//
//    }
//
//}
