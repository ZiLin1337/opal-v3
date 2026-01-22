package wtf.opal.client.feature.module.impl.combat.killaura.target;

import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.Nullable;
import wtf.opal.client.feature.helper.impl.LocalDataWatch;
import wtf.opal.client.feature.helper.impl.target.TargetList;
import wtf.opal.client.feature.helper.impl.target.TargetProperty;
import wtf.opal.client.feature.helper.impl.target.impl.TargetLivingEntity;
import wtf.opal.client.feature.module.impl.combat.killaura.KillAuraSettings;
import wtf.opal.utility.player.PlayerUtility;
import wtf.opal.utility.player.RaytracedRotation;
import wtf.opal.utility.player.RotationUtility;

import java.util.Comparator;
import java.util.List;

import static wtf.opal.client.Constants.mc;

public final class KillAuraTargeting {

    private final KillAuraSettings settings;
    private KillAuraTarget cachedWrapper;

    public KillAuraTargeting(KillAuraSettings settings) {
        this.settings = settings;
    }

    private @Nullable CurrentTarget target;

    public void update() {
        this.findTarget();
    }

    private void findTarget() {
        this.target = null;
        if (mc.player == null) return;

        double range = this.settings.getSwingRange();

        final TargetList targetList = LocalDataWatch.getTargetList();
        final TargetProperty targetProperty = settings.getTargetProperty();

        if (targetList == null) return;

        List<TargetLivingEntity> candidates = targetList.collectTargets(
                targetProperty.getTargetFlags(),
                TargetLivingEntity.class
        );

        TargetLivingEntity bestTarget = candidates.stream()
                .filter(t -> t.getEntity().distanceTo(mc.player) <= range)
                .min(Comparator.comparingDouble(t -> t.getEntity().distanceTo(mc.player)))
                .orElse(null);

        if (bestTarget != null) {
            LivingEntity entity = bestTarget.getEntity();
            Vec3d closestVector = PlayerUtility.getClosestVectorToBoundingBox(mc.player.getEyePos(), entity);
            RaytracedRotation rotations = RotationUtility.getRotationFromRaycastedEntity(entity, closestVector, range);

            if (rotations != null) {
                if (cachedWrapper == null || cachedWrapper.getTarget().getEntity() != entity) {
                    cachedWrapper = new KillAuraTarget(bestTarget);
                }
                this.target = new CurrentTarget(cachedWrapper, rotations);
            }
        }
    }

    public void reset() {
        this.target = null;
        this.cachedWrapper = null;
    }

    public @Nullable CurrentTarget getTarget() {
        return target;
    }

    public boolean isTargetSelected() {
        return getTarget() != null;
    }
}