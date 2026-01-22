package wtf.opal.client.feature.module.impl.world.scaffold;

import net.minecraft.util.math.*;
import static wtf.opal.client.Constants.mc;

public final class SnapScaffold extends ScaffoldBase {

    public SnapScaffold(ScaffoldModule module) {
        super(module);
    }

    @Override
    public void onPreTick() {
        if (mc.player == null) return;

        // 动态获取配置
        boolean snap = module.getSettings().isSnapEnabled();

        // 当blockPos为null时，设置向后180度的旋转
        if (blockPos == null) {
            setBackwardRotation();
        }

        // 根据snap值选择不同旋转策略
        if (snap) {
            executeSnapRotation();
        }

        // 立即执行place()放置方块
        place();
    }

    @Override
    public void onMoveInput() {
        // 动态获取配置
        boolean safeWalk = module.getSettings().isSafeWalkEnabled();

        // SafeWalk防坠逻辑
        if (safeWalk) {
            executeSafeWalk();
        }
    }

    private void setBackwardRotation() {
        // 设置向后180度旋转的逻辑
        if (rotation != null) {
            Vec2f currentRotation = rotation.rotation();
            Vec2f backwardRotation = new Vec2f(currentRotation.x + 180, currentRotation.y);
            // 应用旋转
        }
    }

    private void executeSnapRotation() {
        // 根据snap值选择不同旋转策略
        if (rotation != null) {
            // 快速旋转逻辑
            executeRotation();
        }
    }

    private void executeSafeWalk() {
        // SafeWalk防坠逻辑
        if (!mc.player.isOnGround() && mc.player.getVelocity().getY() <= 0) {
            // 检查下方是否有方块，如果没有则防止掉落
            BlockPos belowPos = mc.player.getBlockPos().down();
            if (mc.world.getBlockState(belowPos).isAir()) {
                // 执行防坠逻辑
                preventFalling();
            }
        }
    }

    private void preventFalling() {
        // 防止掉落的具体实现
        if (mc.player.getVelocity().getY() < 0) {
            // 设置向上的速度或执行其他防坠操作
        }
    }

    private void executeRotation() {
        // 旋转逻辑实现
        if (rotation != null) {
            // 动态获取rotateSpeed配置
            float rotateSpeed = module.getSettings().getRotateSpeed();
            // 根据rotateSpeed执行旋转
        }
    }

    @Override
    public void place() {
        // Snap模式下的放置逻辑
        super.place();
        // 立即放置的额外逻辑
    }

    @Override
    public String getSuffix() {
        // 动态获取snap配置
        boolean snap = module.getSettings().isSnapEnabled();
        return snap ? "Snap" : "Normal";
    }

    @Override
    public Enum<?> getEnumValue() {
        return ScaffoldSettings.Mode.SNAP;
    }
}