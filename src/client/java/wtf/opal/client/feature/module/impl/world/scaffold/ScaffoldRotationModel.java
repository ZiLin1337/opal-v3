package wtf.opal.client.feature.module.impl.world.scaffold;

import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec2f;
import wtf.opal.client.feature.helper.impl.player.rotation.model.EnumRotationModel;
import wtf.opal.client.feature.helper.impl.player.rotation.model.IRotationModel;

public final class ScaffoldRotationModel implements IRotationModel {

    private final double forwardSpeed;
    private final double backSpeed;
    private final Vec2f forwardTarget;

    public ScaffoldRotationModel(final double forwardSpeed, final double backSpeed, final Vec2f forwardTarget) {
        this.forwardSpeed = forwardSpeed;
        this.backSpeed = backSpeed;
        this.forwardTarget = forwardTarget;
    }

    @Override
    public Vec2f tick(final Vec2f from, final Vec2f to, final float timeDelta) {
        final double speed = isForwardTarget(to) ? this.forwardSpeed : this.backSpeed;

        final float deltaYaw = MathHelper.wrapDegrees(to.x - from.x) * timeDelta;
        final float deltaPitch = (to.y - from.y) * timeDelta;

        final double distance = Math.sqrt(deltaYaw * deltaYaw + deltaPitch * deltaPitch);
        if (distance == 0.D) {
            return new Vec2f(from.x + deltaYaw, from.y + deltaPitch);
        }

        final double distributionYaw = Math.abs(deltaYaw / distance);
        final double distributionPitch = Math.abs(deltaPitch / distance);

        final double maxYaw = speed * distributionYaw;
        final double maxPitch = speed * distributionPitch;

        final float moveYaw = (float) Math.max(Math.min(deltaYaw, maxYaw), -maxYaw);
        final float movePitch = (float) Math.max(Math.min(deltaPitch, maxPitch), -maxPitch);

        return new Vec2f(from.x + moveYaw, from.y + movePitch);
    }

    private boolean isForwardTarget(final Vec2f to) {
        if (this.forwardTarget == null) {
            return true;
        }
        return MathHelper.angleBetween(to.x, this.forwardTarget.x) + Math.abs(to.y - this.forwardTarget.y) < 1.0E-4F;
    }

    @Override
    public EnumRotationModel getEnum() {
        return EnumRotationModel.LINEAR;
    }
}
