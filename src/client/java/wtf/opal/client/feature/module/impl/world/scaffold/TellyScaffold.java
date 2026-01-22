package wtf.opal.client.feature.module.impl.world.scaffold;

import net.minecraft.util.math.*;
import static wtf.opal.client.Constants.mc;

public final class TellyScaffold extends ScaffoldBase {

    // Telly配置
    private boolean telly;
    private float rotateSpeed;
    private float rotateBackSpeed;
    private int tellyTick;
    private int airTick;

    public TellyScaffold(ScaffoldModule module) {
        super(module);
        this.telly = module.getSettings().isTellyEnabled();
        this.rotateSpeed = module.getSettings().getRotateSpeed();
        this.rotateBackSpeed = module.getSettings().getRotateBackSpeed();
        this.tellyTick = module.getSettings().getTellyTick();
    }

    @Override
    public void onPreTick() {
        if (mc.player == null) return;

        // 玩家在地面时重置airTick和blockPos，回退旋转
        if (mc.player.isOnGround()) {
            airTick = 0;
            blockPos = null;
            // 回退旋转逻辑
        } else {
            // 玩家空中时，延迟tellyTick个tick后执行旋转和放置
            airTick++;
            if (airTick >= tellyTick) {
                // 执行旋转和放置
                onAir();
            }
        }
    }

    @Override
    public void onMoveInput() {
        // 自动跳跃逻辑
        if (module.getSettings().isAutoJump() && mc.player.isOnGround()) {
            // 自动跳跃实现
        }
    }

    @Override
    public void onAir() {
        // Telly模式下的空中逻辑
        if (blockPos != null) {
            // 执行旋转
            executeRotation();
            // 执行放置
            place();
        }
    }

    private void executeRotation() {
        // 旋转逻辑实现
        if (rotation != null) {
            // 根据rotateSpeed执行旋转
        }
    }

    @Override
    public String getSuffix() {
        return "Telly";
    }

    @Override
    public Enum<?> getEnumValue() {
        return ScaffoldSettings.Mode.TELLY;
    }
}