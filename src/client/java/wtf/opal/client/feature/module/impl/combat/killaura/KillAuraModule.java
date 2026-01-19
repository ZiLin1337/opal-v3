package wtf.opal.client.feature.module.impl.combat.killaura;

import wtf.opal.client.feature.module.Module;
import wtf.opal.client.feature.module.ModuleCategory;
import wtf.opal.client.feature.module.impl.combat.killaura.impl.HeypixelKillAura;
import wtf.opal.client.feature.module.impl.combat.killaura.impl.HypixelKillAura;
import wtf.opal.client.feature.module.impl.combat.killaura.target.KillAuraTargeting;
import wtf.opal.client.feature.module.property.impl.mode.ModeProperty;

public final class KillAuraModule extends Module {

    private final ModeProperty<Mode> mode = new ModeProperty<>("Mode", this, Mode.HYPIXEL);
    private final KillAuraSettings settings = new KillAuraSettings(this);
    private final KillAuraTargeting targeting = new KillAuraTargeting(this.settings);

    public KillAuraModule() {
        super(
                "KillAura",
                "Finds and attacks the most relevant nearby entities.",
                ModuleCategory.COMBAT
        );
        addProperties(mode);
        addModuleModes(mode, new HypixelKillAura(this), new HeypixelKillAura(this));
    }

    public KillAuraSettings getSettings() {
        return settings;
    }

    @Override
    public String getSuffix() {
        return ((KillAuraMode) this.getActiveMode()).getSuffix();
    }

    public KillAuraTargeting getTargeting() {
        return targeting;
    }

    @Override
    protected void onDisable() {
        this.targeting.reset();
        super.onDisable();
    }

    public enum Mode {
        HYPIXEL("Hypixel"),
        HEYPIXEL("Heypixel");

        private final String name;

        Mode(String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            return name;
        }
    }
}
