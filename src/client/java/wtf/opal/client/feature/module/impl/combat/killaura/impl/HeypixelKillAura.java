package wtf.opal.client.feature.module.impl.combat.killaura.impl;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.EntityHitResult;
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
// 假设 AnimationsModule 可以被访问到
import wtf.opal.client.feature.module.impl.visual.AnimationsModule;
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

    // 【需要根据你的框架调整】AnimationsModule 的实例
    // *** 请将下面这行替换成你框架中获取 AnimationsModule 的正确代码 ***
    private final AnimationsModule animationsModule;


    public HeypixelKillAura(KillAuraModule module) {
        super(module);
        module.addProperties(aimRange, cps, rotationSpeed);

        // 临时初始化为 null，你需要在这里替换成实际的获取逻辑
        this.animationsModule = null;
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

        // 1. 计算看向目标的理论角度 (这是解决视角锁定的关键步骤)
        final Vec2f rotation = RotationUtility.getVanillaRotation(
                RotationUtility.getRotationFromPosition(PlayerUtility.getClosestVectorToBoundingBox(mc.player.getEyePos(), this.target))
        );

        // 2. 提交旋转给 RotationHelper 处理
        RotationHelper.getHandler().rotate(rotation, new LinearRotationModel(this.rotationSpeed.getValue()));

        // 3. 使用计算出的 rotation 进行射线检测 (解决“只有瞄准才打”的问题)
        if (!this.canHitTarget(this.target, rotation)) {
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

    private boolean canHitTarget(final LivingEntity target, final Vec2f rotation) {
        final double attackRange = this.aimRange.getValue();

        // 使用计算出的 rotation (目标角度) 进行射线检测
        final EntityHitResult hitResult = RaycastUtility.raycastEntity(
                attackRange,
                1.0F,
                rotation.x, // Yaw
                rotation.y, // Pitch
                entity -> entity == target
        );

        return hitResult != null && hitResult.getEntity() == target;
    }

    private void attackTarget(final LivingEntity target) {
        if (target == null || !target.isAlive()) {
            return;
        }

        // **【Fake Block 关联与抖动】**
        // 检查 animationsModule 是否可用且处于 Fake Blocking 状态 (即 KA 正在触发自动格挡)
        boolean isFakeBlocking = animationsModule != null
                && animationsModule.isEnabled()
                && animationsModule.isAuraFakeBlocking(); // 假设这是 AnimationsModule 中判断 KA 自动格挡的方法

        final long time = System.currentTimeMillis();
        final double baseDelay = 1000.0 / this.cps.getValue();

        long delay = (long) (baseDelay + (Math.random() - 0.5) * baseDelay * 0.4);

        if (isFakeBlocking) {
            // 假防砍逻辑：在满足 CPS 限制的情况下进行攻击，此时依赖 AnimationsModule
            // 来处理包发送和渲染，以模拟快速的攻击/格挡动作。
            if (time - lastAttackTime >= delay) {
                mc.interactionManager.attackEntity(mc.player, target);
                mc.player.swingHand(Hand.MAIN_HAND); // 触发客户端挥手
                lastAttackTime = time;
            }
        } else {
            // 普通逻辑
            if (time - lastAttackTime >= delay) {
                mc.interactionManager.attackEntity(mc.player, target);
                mc.player.swingHand(Hand.MAIN_HAND);
                lastAttackTime = time;
            }
        }
    }
}