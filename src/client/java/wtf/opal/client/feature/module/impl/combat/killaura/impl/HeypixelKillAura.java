package wtf.opal.client.feature.module.impl.combat.killaura.impl;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Vec2f;
import wtf.opal.client.feature.helper.impl.LocalDataWatch;
import wtf.opal.client.feature.helper.impl.player.rotation.RotationHelper;
import wtf.opal.client.feature.helper.impl.player.rotation.model.impl.LinearRotationModel;
import wtf.opal.client.feature.helper.impl.target.TargetFlags;
import wtf.opal.client.feature.helper.impl.target.TargetList;
import wtf.opal.client.feature.helper.impl.target.impl.TargetLivingEntity;
import wtf.opal.client.feature.module.impl.combat.killaura.KillAuraMode;
import wtf.opal.client.feature.module.impl.combat.killaura.KillAuraModule;
import wtf.opal.client.feature.module.impl.combat.killaura.KillAuraSettings;
import wtf.opal.client.feature.module.impl.combat.killaura.target.KillAuraTargeting;
import wtf.opal.client.feature.module.property.impl.number.NumberProperty;
import wtf.opal.event.impl.game.PreGameTickEvent;
import wtf.opal.event.subscriber.Subscribe;
import wtf.opal.utility.player.PlayerUtility;
import wtf.opal.utility.player.RaycastUtility;
import wtf.opal.utility.player.RotationUtility;

import static wtf.opal.client.Constants.mc;

public final class HeypixelKillAura extends KillAuraMode {

    private final NumberProperty aimRange = new NumberProperty("Aim Range", 5.0, 1.0, 6.0, 0.1)
            .id("heypixelAimRange")
            .hideIf(() -> this.module.getActiveMode() != this);

    private final NumberProperty cps = new NumberProperty("CPS", 10.0, 1.0, 20.0, 1.0)
            .id("heypixelCps")
            .hideIf(() -> this.module.getActiveMode() != this);

    private final NumberProperty rotationSpeed = new NumberProperty("Rotation Speed", 180.0, 1.0, 180.0, 1.0)
            .id("heypixelRotationSpeed")
            .hideIf(() -> this.module.getActiveMode() != this);

    private long lastAttackTime;
    private LivingEntity target;

    public HeypixelKillAura(KillAuraModule module) {
        super(module);
        module.addProperties(aimRange, cps, rotationSpeed);
    }

    @Override
    public String getSuffix() {
        return "Heypixel";
    }

    @Override
    public Enum<?> getEnumValue() {
        return KillAuraModule.Mode.HEYPIXEL;
    }

    @Override
    public KillAuraSettings getSettings() {
        return null;
    }

    @Override
    public KillAuraTargeting getTargeting() {
        return null;
    }

    @Override
    public LivingEntity getTargetEntity() {
        return target;
    }

    @Override
    public boolean isTargetSelected() {
        return target != null;
    }

    @Override
    public void onDisable() {
        this.target = null;
        this.lastAttackTime = 0;
        RotationHelper.getHandler().reverse();
        super.onDisable();
    }

    @Subscribe(priority = 2)
    public void onPreGameTick(final PreGameTickEvent event) {
        if (mc.player == null || mc.world == null) {
            return;
        }

        this.target = this.findTarget();
        if (this.target == null) {
            return;
        }

        final Vec2f rotation = RotationUtility.getVanillaRotation(
                RotationUtility.getRotationFromPosition(PlayerUtility.getClosestVectorToBoundingBox(mc.player.getEyePos(), this.target))
        );

        RotationHelper.getHandler().rotate(rotation, new LinearRotationModel(this.rotationSpeed.getValue()));

        if (!this.isRotationValid(this.target)) {
            return;
        }

        this.attackTarget(this.target);
    }

    private LivingEntity findTarget() {
        final TargetList targetList = LocalDataWatch.getTargetList();
        if (targetList == null || mc.player == null) {
            return null;
        }

        final double range = this.aimRange.getValue();
        final double rangeSq = range * range;

        LivingEntity closest = null;
        double closestDistSq = Double.MAX_VALUE;

        final int flags = TargetFlags.get(true, true, true, false);
        for (final TargetLivingEntity candidate : targetList.collectTargets(flags, TargetLivingEntity.class)) {
            if (candidate.isLocal()) {
                continue;
            }

            final LivingEntity entity = candidate.getEntity();
            if (entity == null || !entity.isAlive() || entity.isRemoved() || entity.isSpectator()) {
                continue;
            }

            if (entity instanceof PlayerEntity playerEntity) {
                final String nameUpper = playerEntity.getName().getString().toUpperCase();
                if (LocalDataWatch.getFriendList().contains(nameUpper)) {
                    continue;
                }
            }

            final double distSq = mc.player.squaredDistanceTo(entity);
            if (distSq <= rangeSq && distSq < closestDistSq) {
                closestDistSq = distSq;
                closest = entity;
            }
        }

        return closest;
    }

    private boolean isRotationValid(final LivingEntity target) {
        final double attackRange = mc.player.getEntityInteractionRange();

        final float yaw = RotationHelper.getClientHandler().getYawOr(mc.player.getYaw());
        final float pitch = RotationHelper.getClientHandler().getPitchOr(mc.player.getPitch());

        final var hitResult = RaycastUtility.raycastEntity(attackRange, 1.0F, yaw, pitch, entity -> entity == target);
        return hitResult != null && hitResult.getEntity() == target;
    }

    private void attackTarget(final LivingEntity target) {
        if (target == null || !target.isAlive()) {
            return;
        }

        final long time = System.currentTimeMillis();

        final double baseDelay = 1000.0 / this.cps.getValue();
        long delay = (long) (baseDelay + (Math.random() - 0.5) * baseDelay * 0.4);

        if (time - lastAttackTime >= delay) {
            mc.interactionManager.attackEntity(mc.player, target);
            mc.player.swingHand(Hand.MAIN_HAND);
            lastAttackTime = time;
        }
    }
}
