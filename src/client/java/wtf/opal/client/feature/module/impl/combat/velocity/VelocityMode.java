package wtf.opal.client.feature.module.impl.combat.velocity;

import wtf.opal.client.feature.module.property.impl.mode.ModuleMode;

public abstract class VelocityMode extends ModuleMode<VelocityModule> {
    protected VelocityMode(VelocityModule module) {
        super(module);
    }

    public String getSuffix() {
        return this.getEnumValue().toString();
    }
}
