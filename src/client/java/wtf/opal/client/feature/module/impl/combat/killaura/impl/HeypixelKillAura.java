package wtf.opal.client.feature.module.impl.combat.killaura.impl;

import wtf.opal.client.feature.helper.impl.player.rotation.RotationHelper;
import wtf.opal.client.feature.module.impl.combat.killaura.KillAuraMode;
import wtf.opal.client.feature.module.impl.combat.killaura.KillAuraModule;
import wtf.opal.client.feature.module.impl.combat.killaura.KillAuraSettings;
import wtf.opal.client.feature.module.impl.combat.killaura.target.CurrentTarget;
import wtf.opal.client.feature.module.impl.combat.killaura.target.KillAuraTargeting;
import wtf.opal.client.feature.module.property.impl.number.NumberProperty;
import wtf.opal.event.impl.game.PreGameTickEvent;
import wtf.opal.event.impl.render.RenderWorldEvent;
import wtf.opal.event.subscriber.Subscribe;

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

    private final KillAuraSettings settings;
    private final KillAuraTargeting targeting;

    private long lastAttackTime = 0;

    public HeypixelKillAura(KillAuraModule module) {
        super(module);
        this.settings = new KillAuraSettings(module);
        this.targeting = new KillAuraTargeting(this.settings);
        
        // Register Heypixel-specific properties
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

    public KillAuraSettings getSettings() {
        return settings;
    }

    public KillAuraTargeting getTargeting() {
        return targeting;
    }

    @Override
    public void onDisable() {
        this.targeting.reset();
        lastAttackTime = 0;
        super.onDisable();
    }

    @Subscribe
    public void onRenderWorld(final RenderWorldEvent event) {
        if (mc.player == null || mc.world == null) {
            return;
        }
        // Heypixel specific rendering (TargetESP, etc.) can be added here
    }

    @Subscribe(priority = 2)
    public void onPreGameTick(final PreGameTickEvent event) {
        if (mc.player == null || mc.world == null) {
            return;
        }

        this.targeting.update();
        final CurrentTarget target = this.targeting.getRotationTarget();
        
        if (target != null) {
            rotateToTarget(target);
            attackTarget(target);
        }
    }

    private void rotateToTarget(final CurrentTarget target) {
        if (target == null) {
            return;
        }

        RotationHelper.getHandler().rotate(
                target.getRotations().rotation(),
                this.settings.createRotationModel()
        );
    }

    private void attackTarget(final CurrentTarget target) {
        if (target == null || target.getEntity() == null) {
            return;
        }

        final long currentTime = System.currentTimeMillis();
        final double cpsValue = this.cps.getValue();
        long delay = (long) (1000.0 / cpsValue);

        // Add random variation
        delay += (long) ((Math.random() - 0.5) * delay * 0.4);

        if (currentTime - lastAttackTime >= delay) {
            mc.interactionManager.attackEntity(mc.player, target.getEntity());
            mc.player.swingHand(mc.player.getActiveHand());
            lastAttackTime = currentTime;
        }
    }
}
