package wtf.opal.client.feature.module.impl.combat.killaura.impl;

import net.minecraft.entity.LivingEntity;
import wtf.opal.client.feature.helper.impl.player.rotation.RotationHelper;
import wtf.opal.client.feature.helper.impl.player.swing.SwingDelay;
import wtf.opal.client.feature.module.impl.combat.killaura.KillAuraMode;
import wtf.opal.client.feature.module.impl.combat.killaura.KillAuraModule;
import wtf.opal.client.feature.module.impl.combat.killaura.target.CurrentTarget;
import wtf.opal.client.feature.module.property.impl.number.NumberProperty;
import wtf.opal.event.impl.game.PreGameTickEvent;
import wtf.opal.event.impl.render.RenderWorldEvent;
import wtf.opal.event.subscriber.Subscribe;

import static wtf.opal.client.Constants.mc;

public final class HeypixelKillAura extends KillAuraMode {

    private final NumberProperty range = new NumberProperty("Range", this, 3.0, 3.0, 6.0, 0.1)
            .hideIf(() -> this.module.getActiveMode() != this);

    public HeypixelKillAura(KillAuraModule module) {
        super(module);
        module.addProperties(range);
    }

    @Override
    public Enum<?> getEnumValue() {
        return KillAuraModule.Mode.HEYPIXEL;
    }

    @Subscribe
    public void onPreGameTick(PreGameTickEvent event) {
        this.module.getTargeting().update();

        final CurrentTarget target = this.module.getTargeting().getTarget();
        if (target == null) {
            return;
        }

        final LivingEntity entity = target.getEntity();
        if (mc.player.distanceTo(entity) > range.getValue()) {
            return;
        }

        RotationHelper.getHandler().rotate(
                target.getRotations().rotation(),
                this.module.getSettings().createRotationModel()
        );

        if (SwingDelay.isSwingAvailable(this.module.getSettings().getCpsProperty())) {
            mc.interactionManager.attackEntity(mc.player, entity);
            mc.player.swingHand(mc.player.getActiveHand());
        }
    }

    @Subscribe
    public void onRenderWorld(RenderWorldEvent event) {
        // Heypixel specific rendering
    }
}
