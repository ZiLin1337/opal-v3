package wtf.opal.client.feature.module.impl.combat.killaura;

import wtf.opal.client.feature.module.Module;
import wtf.opal.client.feature.module.ModuleCategory;
import wtf.opal.client.feature.module.impl.combat.killaura.impl.HeypixelKillAura;
import wtf.opal.client.feature.module.impl.combat.killaura.impl.HypixelKillAura;
import wtf.opal.client.feature.module.impl.combat.killaura.target.KillAuraTargeting;
import wtf.opal.client.feature.module.property.impl.mode.ModeProperty;

public final class KillAuraModule extends Module {

    private final ModeProperty<Mode> mode = new ModeProperty<>("Mode", this, Mode.HYPIXEL);

    private final HypixelKillAura hypixelMode;
    private final HeypixelKillAura heypixelMode;

    public KillAuraModule() {
        super(
                "KillAura",
                "Finds and attacks the most relevant nearby entities.",
                ModuleCategory.COMBAT
        );

        addProperties(mode);

        this.heypixelMode = new HeypixelKillAura(this);
        this.hypixelMode = new HypixelKillAura(this);

        addModuleModes(mode, hypixelMode, heypixelMode);
    }

    public ModeProperty<Mode> getMode() {
        return mode;
    }

    @Override
    public String getSuffix() {
        final KillAuraMode activeMode = (KillAuraMode) getActiveMode();
        return activeMode != null ? activeMode.getSuffix() : mode.getValue().toString();
    }

    /**
     * Get the settings of the currently active mode.
     * This allows external modules to access settings without knowing which mode is active.
     */
    public KillAuraSettings getSettings() {
        final KillAuraMode activeMode = (KillAuraMode) getActiveMode();
        return activeMode != null ? activeMode.getSettings() : null;
    }

    /**
     * Get the targeting of the currently active mode.
     * This allows external modules to access targeting without knowing which mode is active.
     */
    public KillAuraTargeting getTargeting() {
        final KillAuraMode activeMode = (KillAuraMode) getActiveMode();
        return activeMode != null ? activeMode.getTargeting() : null;
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
