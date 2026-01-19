package wtf.opal.client.feature.module.impl.combat.velocity.impl;

import net.minecraft.network.packet.s2c.common.CommonPingS2CPacket;
import net.minecraft.network.packet.s2c.play.EntityVelocityUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.GameJoinS2CPacket;
import wtf.opal.client.feature.module.impl.combat.velocity.VelocityMode;
import wtf.opal.client.feature.module.impl.combat.velocity.VelocityModule;
import wtf.opal.event.impl.game.packet.ReceivePacketEvent;
import wtf.opal.event.subscriber.Subscribe;

import static wtf.opal.client.Constants.mc;

public final class MushMCVelocity extends VelocityMode {

    public MushMCVelocity(VelocityModule module) {
        super(module);
    }

    private boolean cancel;

    @Subscribe
    public void onReceivePacket(final ReceivePacketEvent event) {
        if (event.getPacket() instanceof EntityVelocityUpdateS2CPacket packet) {
            if (mc.player != null && packet.getEntityId() == mc.player.getId()) {
                event.setCancelled();
                this.cancel = true;
            }
        } else if (event.getPacket() instanceof CommonPingS2CPacket) {
            if (this.cancel) {
                event.setCancelled();
                this.cancel = false;
            }
        } else if (event.getPacket() instanceof GameJoinS2CPacket) {
            this.cancel = false;
        }
    }

    @Override
    public Enum<?> getEnumValue() {
        return VelocityModule.Mode.MUSHMC;
    }
}
