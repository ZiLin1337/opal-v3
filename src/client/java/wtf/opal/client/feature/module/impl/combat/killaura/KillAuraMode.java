package wtf.opal.client.feature.module.impl.combat.killaura;

import net.minecraft.entity.LivingEntity;
import org.jetbrains.annotations.Nullable;
import wtf.opal.client.feature.module.impl.combat.killaura.target.CurrentTarget;
import wtf.opal.client.feature.module.impl.combat.killaura.target.KillAuraTargeting;
import wtf.opal.client.feature.module.property.impl.mode.ModuleMode;

public abstract class KillAuraMode extends ModuleMode<KillAuraModule> {
    protected KillAuraMode(KillAuraModule module) {
        super(module);
    }

    public String getSuffix() {
        return this.getEnumValue().toString();
    }

    public abstract KillAuraSettings getSettings();

    public abstract KillAuraTargeting getTargeting();

    public @Nullable LivingEntity getTargetEntity() {
        final KillAuraTargeting targeting = this.getTargeting();
        if (targeting == null) {
            return null;
        }

        final CurrentTarget target = targeting.getTarget();
        return target != null ? target.getEntity() : null;
    }

    public boolean isTargetSelected() {
        return this.getTargetEntity() != null;
    }
}
