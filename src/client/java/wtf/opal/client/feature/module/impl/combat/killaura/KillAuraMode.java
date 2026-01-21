package wtf.opal.client.feature.module.impl.combat.killaura;

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
}
