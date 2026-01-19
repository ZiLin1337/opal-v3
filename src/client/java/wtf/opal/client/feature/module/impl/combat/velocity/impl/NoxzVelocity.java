package wtf.opal.client.feature.module.impl.combat.velocity.impl;

import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.BufferAllocator;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.listener.ClientPlayPacketListener;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.*;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Vec3d;
import wtf.opal.client.feature.module.impl.combat.velocity.VelocityMode;
import wtf.opal.client.feature.module.impl.combat.velocity.VelocityModule;
import wtf.opal.client.feature.module.property.impl.number.NumberProperty;
import wtf.opal.client.renderer.world.WorldRenderer;
import wtf.opal.event.impl.game.PreGameTickEvent;
import wtf.opal.event.impl.game.input.MoveInputEvent;
import wtf.opal.event.impl.game.packet.InstantaneousReceivePacketEvent;
import wtf.opal.event.impl.render.RenderWorldEvent;
import wtf.opal.event.subscriber.Subscribe;
import wtf.opal.mixin.EntityS2CPacketAccessor;
import wtf.opal.utility.misc.time.Stopwatch;
import wtf.opal.utility.render.ColorUtility;
import wtf.opal.utility.render.CustomRenderLayers;

import java.awt.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import static wtf.opal.client.Constants.mc;

public final class NoxzVelocity extends VelocityMode {

    private final NumberProperty attacks = new NumberProperty("Attack Counts", 3, 1, 5, 1)
            .id("attacksNoxz")
            .hideIf(() -> this.module.getActiveMode() != this);

    private final NumberProperty alinkTime = new NumberProperty("Max Alink Time", 5000, 50, 10000, 50)
            .id("alinkTimeNoxz")
            .hideIf(() -> this.module.getActiveMode() != this);

    private final Queue<Packet<ClientPlayPacketListener>> packetQueue = new ConcurrentLinkedQueue<>();
    private final Map<Integer, Vec3d> entityPositions = new HashMap<>();
    private final Stopwatch stopwatch = new Stopwatch();

    private VelocityStage stage = VelocityStage.NONE;
    private Vec3d storedVelocity = Vec3d.ZERO;
    private Entity targetEntity;
    private boolean lag;

    public NoxzVelocity(VelocityModule module) {
        super(module);
        module.addProperties(this.attacks, this.alinkTime);
    }

    @Override
    public String getSuffix() {
        if (stage == VelocityStage.DELAY) {
            return "Alink " + (stopwatch.getTime() / 50) + "t";
        }
        return "NoXZ";
    }

    @Subscribe
    public void onInstantaneousReceivePacket(final InstantaneousReceivePacketEvent event) {
        if (mc.player == null || mc.world == null) return;

        if (event.getPacket() instanceof PlayerPositionLookS2CPacket) {
            // Lagback detected, force clear
            if (stage == VelocityStage.NONE) {
                this.lag = true;
            } else {
                this.stage = VelocityStage.LAG;
            }
            return;
        }

        if (event.getPacket() instanceof EntityVelocityUpdateS2CPacket velocityPacket) {
            if (velocityPacket.getEntityId() == mc.player.getId()) {
                double x = velocityPacket.getVelocity().x / 8000.0D;
                double y = velocityPacket.getVelocity().y / 8000.0D;
                double z = velocityPacket.getVelocity().z / 8000.0D;

                if (stage == VelocityStage.NONE) {
                    if (!this.lag) {
                        this.stage = VelocityStage.DELAY;
                        this.stopwatch.reset();
                        this.storedVelocity = new Vec3d(x, y, z);
                        event.setCancelled();
                    } else {
                        this.lag = false;
                    }
                } else {
                    // Receive velocity while already delaying - fail safe to LAG state
                    this.storedVelocity = new Vec3d(x, y, z);
                    this.stage = VelocityStage.LAG;
                    event.setCancelled();
                }
            }
            return;
        }

        // Buffer other entity movements while in DELAY/ALINK state to hide the lag visually
        if (stage != VelocityStage.NONE) {
            final Packet<?> packet = event.getPacket();

            if (packet instanceof PlayerPositionLookS2CPacket) {
                this.stage = VelocityStage.LAG;
                return;
            }

            boolean isMovement = packet instanceof EntityS2CPacket; // Entity movement packets

            if (isMovement) {
                EntityS2CPacket entityPacket = (EntityS2CPacket) packet;
                int entityId = ((EntityS2CPacketAccessor) entityPacket).getId();

                if (entityId != mc.player.getId()) {
                    Entity entity = mc.world.getEntityById(entityId);
                    if (entity != null) {
                        // Track entity position for rendering "Ghost" entities
                        Vec3d currentPos = entityPositions.getOrDefault(entityId, new Vec3d(entity.getX(), entity.getY(), entity.getZ()));
                        Vec3d newPos = currentPos.add(
                                entityPacket.getDeltaX() / 4096.0D,
                                entityPacket.getDeltaY() / 4096.0D,
                                entityPacket.getDeltaZ() / 4096.0D
                        );
                        entityPositions.put(entityId, newPos);

                        // Add to queue and cancel so we don't process it yet
                        this.packetQueue.add((Packet<ClientPlayPacketListener>) packet);
                        event.setCancelled();
                    }
                }
            }
        }
    }

    @Subscribe
    public void onMoveInput(MoveInputEvent event) {
        if (stage == VelocityStage.DELAY && storedVelocity != null) {
            // If aiming at a player while delaying, force attack phase
            if (mc.crosshairTarget instanceof EntityHitResult hitResult
                    && hitResult.getType() == HitResult.Type.ENTITY
                    && hitResult.getEntity() instanceof PlayerEntity target) {

                // Bot check can be added here if Opal has one

                event.setForward(1.0F);
                event.setSideways(0);
                this.stage = VelocityStage.ATTACK;
                this.targetEntity = target;
            }
        }
    }

    @Subscribe
    public void onPreTick(PreGameTickEvent event) {
        if (mc.player == null) return;

        // Logic for applying velocity or clearing queue
        if (stage == VelocityStage.CLEAR) {
            clear(true);
        } else if (stage == VelocityStage.LAG) {
            mc.player.setVelocity(storedVelocity);
            clear(true);
        } else if (stage == VelocityStage.ATTACK) {
            if (mc.crosshairTarget instanceof EntityHitResult hitResult
                    && hitResult.getEntity() instanceof PlayerEntity
                    && hitResult.getEntity() == this.targetEntity) {

                double motionXZModifier = 1.0D;
                int attackCount = this.attacks.getValue().intValue();

                // Simulate attacks to reduce knockback on the server side
                for (int i = 0; i < attackCount; i++) {
                    if (mc.player.isSprinting()) {
                        mc.player.setSprinting(false);
                    }
                    mc.interactionManager.attackEntity(mc.player, targetEntity);
                    mc.player.swingHand(Hand.MAIN_HAND);
                    motionXZModifier *= 0.6D;
                }

                // Apply reduced velocity
                mc.player.setVelocity(storedVelocity.x * motionXZModifier, storedVelocity.y, storedVelocity.z * motionXZModifier);
                this.stage = VelocityStage.CLEAR;
            }
        } else if (stage == VelocityStage.DELAY) {
            // Timeout check
            if (stopwatch.hasTimeElapsed(this.alinkTime.getValue().longValue())) {
                mc.player.setVelocity(storedVelocity);
                this.stage = VelocityStage.CLEAR;
            }
        }

        if (lag && mc.player.hurtTime == 0) {
            lag = false;
        }
    }

    @Subscribe
    public void onRenderWorld(RenderWorldEvent event) {
        if (stage == VelocityStage.NONE || entityPositions.isEmpty()) return;

        VertexConsumerProvider.Immediate vcp = VertexConsumerProvider.immediate(new BufferAllocator(1024));
        WorldRenderer worldRenderer = new WorldRenderer(vcp);

        // Render "Ghost" entities (Where they actually are vs where they look like they are)
        for (Map.Entry<Integer, Vec3d> entry : entityPositions.entrySet()) {
            Entity entity = mc.world.getEntityById(entry.getKey());
            if (entity instanceof PlayerEntity && entity != mc.player) {
                Vec3d pos = entry.getValue();

                Color color = (entity == targetEntity) ? new Color(200, 0, 0, 60) : new Color(0, 200, 0, 60);
                int intColor = color.getRGB();

                // Render box at the tracked entity position
                worldRenderer.drawFilledCube(
                        event.matrixStack(),
                        CustomRenderLayers.getPositionColorQuads(true),
                        pos,
                        new Vec3d(entity.getWidth(), entity.getHeight(), entity.getWidth()),
                        ColorUtility.applyOpacity(intColor, 0.25F)
                );
            }
        }

        vcp.draw();
    }

    private void clear(boolean processQueue) {
        this.lag = false;
        this.stage = VelocityStage.NONE;
        this.entityPositions.clear();
        this.targetEntity = null;

        if (!processQueue) {
            this.packetQueue.clear();
            return;
        }

        while (!this.packetQueue.isEmpty()) {
            Packet<ClientPlayPacketListener> packet = this.packetQueue.poll();
            if (packet != null && mc.getNetworkHandler() != null) {
                // Re-process the packet
                packet.apply(mc.getNetworkHandler());
            }
        }
    }

    @Override
    public void onEnable() {
        this.stage = VelocityStage.NONE;
        this.lag = false;
        this.entityPositions.clear();
        this.packetQueue.clear();
        this.targetEntity = null;
        super.onEnable();
    }

    @Override
    public void onDisable() {
        this.clear(true);
        super.onDisable();
    }

    @Override
    public Enum<?> getEnumValue() {
        return VelocityModule.Mode.NOXZ; // You will need to add NOXZ to the VelocityModule enum
    }

    private enum VelocityStage {
        NONE, DELAY, ATTACK, CLEAR, LAG
    }
}