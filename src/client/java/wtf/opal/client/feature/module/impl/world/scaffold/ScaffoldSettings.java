package wtf.opal.client.feature.module.impl.world.scaffold;

import wtf.opal.client.feature.helper.impl.player.swing.CPSProperty;
import wtf.opal.client.feature.module.property.impl.GroupProperty;
import wtf.opal.client.feature.module.property.impl.bool.BooleanProperty;
import wtf.opal.client.feature.module.property.impl.mode.ModeProperty;
import wtf.opal.client.feature.module.property.impl.number.NumberProperty;

public final class ScaffoldSettings {

    private final ModeProperty<Mode> mode;

    private final CPSProperty placeCps;

    private final BooleanProperty sameY;
    private final BooleanProperty autoJump;

    private final BooleanProperty safeWalk;
    private final BooleanProperty tower;

    private final ModeProperty<SwitchMode> switchMode;
    private final ModeProperty<SwingMode> swingMode;

    private final BooleanProperty overrideRaycast;
    private final BooleanProperty blockOverlay;

    private final NumberProperty rotateSpeed;
    private final NumberProperty rotateBackSpeed;

    private final NumberProperty tellyTick;

    public ScaffoldSettings(final ScaffoldModule module) {
        this.mode = new ModeProperty<>("Mode", Mode.SNAP);

        this.placeCps = new CPSProperty(module, "Place CPS", false);

        this.sameY = new BooleanProperty("Same Y", true);
        this.autoJump = new BooleanProperty("Auto jump", true).hideIf(() -> !this.isSameYEnabled());

        this.safeWalk = new BooleanProperty("Safe walk", true);
        this.tower = new BooleanProperty("Tower", false);

        this.switchMode = new ModeProperty<>("Switch mode", SwitchMode.HOTBAR);
        this.swingMode = new ModeProperty<>("Swing mode", SwingMode.CLIENT);

        this.overrideRaycast = new BooleanProperty("Override raycast", true);
        this.blockOverlay = new BooleanProperty("Block overlay", true);

        this.rotateSpeed = new NumberProperty("Rotate speed", 12.0, 1.0, 30.0, 0.5);
        this.rotateBackSpeed = new NumberProperty("Rotate back speed", 18.0, 1.0, 40.0, 0.5);

        this.tellyTick = new NumberProperty("Telly tick", 2, 0, 10, 1).hideIf(() -> this.getMode() != Mode.TELLY);

        module.addProperties(
                mode,
                new GroupProperty("Rotations", rotateSpeed, rotateBackSpeed, overrideRaycast),
                new GroupProperty("Placement", switchMode, swingMode),
                new GroupProperty("Movement", sameY, autoJump, safeWalk, tower),
                blockOverlay,
                tellyTick
        );
    }

    public Mode getMode() {
        return mode.getValue();
    }

    public CPSProperty getPlaceCps() {
        return placeCps;
    }

    public boolean isSameYEnabled() {
        return sameY.getValue();
    }

    public boolean isAutoJumpEnabled() {
        return autoJump.getValue();
    }

    public boolean isSafeWalkEnabled() {
        return safeWalk.getValue();
    }

    public boolean isTowerEnabled() {
        return tower.getValue();
    }

    public SwitchMode getSwitchMode() {
        return switchMode.getValue();
    }

    public SwingMode getSwingMode() {
        return swingMode.getValue();
    }

    public boolean isOverrideRaycast() {
        return overrideRaycast.getValue();
    }

    public boolean isBlockOverlayEnabled() {
        return blockOverlay.getValue();
    }

    public double getRotateSpeed() {
        return rotateSpeed.getValue();
    }

    public double getRotateBackSpeed() {
        return rotateBackSpeed.getValue();
    }

    public int getTellyTick() {
        return tellyTick.getValue().intValue();
    }

    public enum Mode {
        SNAP("Snap"),
        TELLY("Telly");

        private final String name;

        Mode(final String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    public enum SwitchMode {
        NORMAL("Normal"),
        HOTBAR("Hotbar"),
        FULL("Full");

        private final String name;

        SwitchMode(final String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    public enum SwingMode {
        CLIENT("Client"),
        SERVER("Server");

        private final String name;

        SwingMode(final String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            return name;
        }
    }
}
