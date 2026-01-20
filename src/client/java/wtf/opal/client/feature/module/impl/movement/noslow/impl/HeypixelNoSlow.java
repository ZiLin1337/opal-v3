package wtf.opal.client.feature.module.impl.movement.noslow.impl;

import wtf.opal.client.feature.module.impl.movement.noslow.NoSlowModule;
import wtf.opal.client.feature.module.property.impl.mode.ModuleMode;
import wtf.opal.event.impl.game.player.movement.SlowdownEvent;
import wtf.opal.event.subscriber.Subscribe;

import static wtf.opal.client.Constants.mc;

public final class HeypixelNoSlow extends ModuleMode<NoSlowModule> {

    public HeypixelNoSlow(final NoSlowModule module) {
        super(module);
    }

    @Subscribe
    public void onSlowdown(final SlowdownEvent event) {
        if (mc.player == null) {
            return;
        }

        // 对应源码中的 checkFood() 逻辑
        // 在 Opal 中，Action.USEABLE 通常对应饮食/药水 (getUseAction 为 EAT 或 DRINK)
        // 源码逻辑: (!checkFood() || mc.player.getUseItemRemainingTicks() <= 30)
        // 如果是食物，且剩余时间大于30tick，则不执行防减速
        if (module.getAction() == NoSlowModule.Action.USEABLE && mc.player.getItemUseTimeLeft() > 30) {
            return;
        }

        // Heypixel 2/3 核心逻辑: mc.player.getUseItemRemainingTicks() % 3 != 0
        if (mc.player.getItemUseTimeLeft() % 3 != 0) {
            event.setCancelled();
        }
    }

    @Override
    public Enum<?> getEnumValue() {
        return NoSlowModule.Mode.HEYPIXEL;
    }
}