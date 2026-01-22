package wtf.opal.client.feature.module.impl.combat.killaura.target;

import wtf.opal.client.feature.helper.impl.target.impl.TargetLivingEntity;

public final class KillAuraTarget {
    private final TargetLivingEntity target;

    public KillAuraTarget(TargetLivingEntity target) {
        this.target = target;
    }

    public TargetLivingEntity getTarget() {
        return target;
    }

    // 移除了 LastAttackData 和 onAttack 逻辑，因为现在由 KillAuraModule 直接控制
}