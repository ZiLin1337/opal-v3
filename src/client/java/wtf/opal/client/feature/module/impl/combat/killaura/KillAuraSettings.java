package wtf.opal.client.feature.module.impl.combat.killaura;

import wtf.opal.client.feature.helper.impl.player.rotation.RotationProperty;
import wtf.opal.client.feature.helper.impl.player.rotation.model.IRotationModel;
import wtf.opal.client.feature.helper.impl.player.rotation.model.impl.InstantRotationModel;
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

    // 修改：使用简单的 NumberProperty 代替复杂的 CPSProperty
    private final NumberProperty cps;
    private final NumberProperty swingRange;

    private final BooleanProperty requireAttackKey, requireWeapon;
    private final MultipleBooleanProperty visuals;

    public KillAuraSettings(final KillAuraModule module) {
        this.rotationProperty = new RotationProperty(InstantRotationModel.INSTANCE);
        this.targetProperty = new TargetProperty(true, false, false, false, false, true);

        // HeyPixel Logic: 简单的 CPS 滑块
        this.cps = new NumberProperty("CPS", 10.0D, 1.0D, 20.0D, 1.0D);

        // HeyPixel Logic: Aim Range -> Swing Range
        this.swingRange = new NumberProperty("Range", 5.0D, 3.0D, 6.0D, 0.1D);

        this.requireAttackKey = new BooleanProperty("Require attack key", false);
        this.requireWeapon = new BooleanProperty("Require weapon", false);

        // 保留 Mode 仅作为显示后缀，实际上只跑 Single/Closest 逻辑
        this.mode = new ModeProperty<>("Mode", Mode.SINGLE);

        this.visuals = new MultipleBooleanProperty("Visuals",
                new BooleanProperty("Box", true)
        );

        module.addProperties(
                rotationProperty.get(),
                new GroupProperty("Requirements", requireWeapon, requireAttackKey),
                mode, cps, swingRange, targetProperty.get(),
                visuals
        );
    }

    public double getSwingRange() {
        return this.swingRange.getValue();
    }

    // 获取 CPS 数值
    public double getCps() {
        return this.cps.getValue();
    }

    public TargetProperty getTargetProperty() {
        return targetProperty;
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

    public MultipleBooleanProperty getVisuals() {
        return visuals;
    }

    public enum Mode {
        SINGLE("Single"),
        SWITCH("Switch");

        private final String name;
        Mode(String name) { this.name = name; }
        @Override public String toString() { return name; }
    }
}