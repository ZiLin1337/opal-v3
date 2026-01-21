package wtf.opal.client.feature.module.impl.combat.killaura;

import wtf.opal.client.feature.helper.impl.player.rotation.RotationProperty;
import wtf.opal.client.feature.helper.impl.player.rotation.model.IRotationModel;
import wtf.opal.client.feature.helper.impl.player.rotation.model.impl.InstantRotationModel;
import wtf.opal.client.feature.helper.impl.player.swing.CPSProperty;
import wtf.opal.client.feature.helper.impl.target.TargetProperty;
import wtf.opal.client.feature.module.property.impl.GroupProperty;
import wtf.opal.client.feature.module.property.impl.bool.BooleanProperty;
import wtf.opal.client.feature.module.property.impl.bool.MultipleBooleanProperty;
import wtf.opal.client.feature.module.property.impl.mode.ModeProperty;
import wtf.opal.client.feature.module.property.impl.number.NumberProperty;

public final class KillAuraSettings {

    private final RotationProperty rotationProperty;
    private final ModeProperty<Mode> mode;
    private final TargetProperty targetProperty;
    private final CPSProperty cpsProperty, swingCpsProperty;

    private final NumberProperty rotationRange, swingRange;
    private final BooleanProperty hideFakeSwings;

    private final BooleanProperty requireAttackKey, requireWeapon;
    private final BooleanProperty overrideRaycast, tickLookahead;
    private final NumberProperty fov;

    private final MultipleBooleanProperty visuals;

    public KillAuraSettings(final KillAuraModule module) {
        this.rotationProperty = new RotationProperty(InstantRotationModel.INSTANCE);
        this.targetProperty = new TargetProperty(true, false, false, false, false, true);
        this.cpsProperty = new CPSProperty(module, "Attack CPS", true);
        this.swingCpsProperty = new CPSProperty(module, "Swing CPS", false).hideIf(this.cpsProperty::isModernDelay);

        this.rotationRange = new NumberProperty("Rotation range", 5.D, 3.D, 8.D, 0.1D);
        this.swingRange = new NumberProperty("Swing range", 5.D, 3.D, 8.D, 0.1D).hideIf(this.cpsProperty::isModernDelay);
        this.hideFakeSwings = new BooleanProperty("Hide fake swings", true).hideIf(this.cpsProperty::isModernDelay);

        this.requireAttackKey = new BooleanProperty("Require attack key", false);
        this.requireWeapon = new BooleanProperty("Require weapon", false);
        this.overrideRaycast = new BooleanProperty("Override raycast", true);
        this.tickLookahead = new BooleanProperty("Tick lookahead", false).hideIf(() -> !this.isOverrideRaycast());
        this.mode = new ModeProperty<>("Mode", Mode.SWITCH);
        this.fov = new NumberProperty("FOV", 180, 1, 180, 1);

        this.visuals = new MultipleBooleanProperty("Visuals",
                new BooleanProperty("Box", false)
        );

        module.addProperties(
                rotationProperty.get(), new GroupProperty("Requirements", requireWeapon, requireAttackKey),
                mode, rotationRange, swingRange, hideFakeSwings, targetProperty.get(),
                fov, overrideRaycast, tickLookahead, visuals
        );
    }

    public double getSwingRange() {
        return this.swingRange.getValue();
    }

    public boolean isHideFakeSwings() {
        return this.hideFakeSwings.getValue();
    }

    public boolean isOverrideRaycast() {
        return this.overrideRaycast.getValue();
    }

    public boolean isTickLookahead() {
        return this.tickLookahead.getValue();
    }

    public double getRotationRange() {
        return this.rotationRange.getValue();
    }

    public MultipleBooleanProperty getVisuals() {
        return visuals;
    }

    public TargetProperty getTargetProperty() {
        return targetProperty;
    }

    public CPSProperty getCpsProperty() {
        return cpsProperty;
    }

    public CPSProperty getSwingCpsProperty() {
        return swingCpsProperty;
    }

    public boolean isRequireAttackKey() {
        return requireAttackKey.getValue();
    }

    public boolean isRequireWeapon() {
        return requireWeapon.getValue();
    }

    public IRotationModel createRotationModel() {
        return rotationProperty.createModel();
    }

    public Mode getMode() {
        return mode.getValue();
    }

    public float getFov() {
        return this.fov.getValue().floatValue();
    }

    public enum Mode {
        SINGLE("Single"),
        SWITCH("Switch");

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
