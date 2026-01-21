package wtf.opal.client.feature.helper.impl.render;

import wtf.opal.client.feature.module.property.impl.mode.ModeProperty;

import static wtf.opal.client.Constants.mc;

public final class ScaleProperty {

    private static final ScaleMode[] MINECRAFT_VALUES = new ScaleMode[]{ScaleMode.AUTO, ScaleMode.SMALL, ScaleMode.NORMAL, null, ScaleMode.LARGE};
    private final ModeProperty<ScaleMode> modeProperty;

    private ScaleProperty(final ScaleMode[] values) {
        this.modeProperty = new ModeProperty<>("Scale", ScaleMode.AUTO, values);
    }

    public static ScaleProperty newMinecraftElement() {
        return new ScaleProperty(MINECRAFT_VALUES);
    }

    public static ScaleProperty newNVGElement() {
        return new ScaleProperty(ScaleMode.values());
    }

    public ModeProperty<ScaleMode> get() {
        return modeProperty;
    }

    public float getScale() {
        ScaleMode mode = this.modeProperty.getValue();
        
        switch (mode) {
            case SMALL:
                return 0.5F;
            case NORMAL:
                return 1.0F;
            case MEDIUM:
                return 1.5F;
            case LARGE:
                return 2.0F;
            case AUTO:
            default:
                return 1.0F;
        }
    }

    public enum ScaleMode {
        AUTO("Auto"),
        SMALL("Small (1x)"),
        NORMAL("Normal (2x)"),
        MEDIUM("Medium (2.67x)"),
        LARGE("Large (3x)");

        private final String name;

        ScaleMode(final String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            return name;
        }
    }

}
