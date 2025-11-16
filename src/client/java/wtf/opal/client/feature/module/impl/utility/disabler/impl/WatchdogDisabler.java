package wtf.opal.client.feature.module.impl.utility.disabler.impl;

import net.minecraft.entity.data.DataTracker;
import net.minecraft.network.packet.c2s.common.CommonPongC2SPacket;
import net.minecraft.network.packet.c2s.common.KeepAliveC2SPacket;
import net.minecraft.network.packet.c2s.play.ClickSlotC2SPacket;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
import net.minecraft.network.packet.c2s.play.CloseHandledScreenC2SPacket;
import net.minecraft.network.packet.s2c.play.EntityTrackerUpdateS2CPacket;
import net.minecraft.screen.slot.SlotActionType;
import wtf.opal.client.feature.helper.impl.LocalDataWatch;
import wtf.opal.client.feature.helper.impl.player.mouse.MouseHelper;
import wtf.opal.client.feature.helper.impl.player.packet.blockage.block.holder.BlockHolder;
import wtf.opal.client.feature.helper.impl.player.packet.blockage.impl.OutboundNetworkBlockage;
import wtf.opal.client.feature.helper.impl.server.impl.HypixelServer;
import wtf.opal.client.feature.module.impl.utility.disabler.DisablerModule;
import wtf.opal.client.feature.module.property.impl.bool.BooleanProperty;
import wtf.opal.client.feature.module.property.impl.bool.MultipleBooleanProperty;
import wtf.opal.client.feature.module.property.impl.mode.ModuleMode;
import wtf.opal.event.impl.game.JoinWorldEvent;
import wtf.opal.event.impl.game.PreGameTickEvent;
import wtf.opal.event.impl.game.packet.InstantaneousSendPacketEvent;
import wtf.opal.event.impl.game.packet.ReceivePacketEvent;
import wtf.opal.event.subscriber.Subscribe;
import wtf.opal.mixin.EntityAccessor;
import wtf.opal.utility.misc.chat.ChatUtility;

import static wtf.opal.client.Constants.mc;

public final class WatchdogDisabler extends ModuleMode<DisablerModule> {

    private final MultipleBooleanProperty options = new MultipleBooleanProperty("Options",
            new BooleanProperty("Inventory Move", true)
    ).hideIf(() -> this.module.getActiveMode() != this);

    public WatchdogDisabler(DisablerModule module) {
        super(module);
        module.addProperties(options);
    }

    @Override
    public Enum<?> getEnumValue() {
        return DisablerModule.Mode.WATCHDOG;
    }

    private boolean shouldBlink;

    public boolean isInventoryMoveDisabler() {
        return this.options.getProperty("Inventory Move").getValue() && LocalDataWatch.get().getKnownServerManager().getCurrentServer() instanceof HypixelServer;
    }

    @Subscribe
    public void onInstantaneousSendPacketEvent(final InstantaneousSendPacketEvent event) {
        if (event.getPacket() instanceof ClickSlotC2SPacket clickSlot) {
            if (!isInventoryMoveDisabler()) return;

            final boolean allowedAction = clickSlot.actionType() == SlotActionType.QUICK_MOVE
                    || clickSlot.actionType() == SlotActionType.SWAP
                    || clickSlot.actionType() == SlotActionType.THROW;

            if (LocalDataWatch.get().getKnownServerManager().getCurrentServer() instanceof HypixelServer) {
                final HypixelServer.ModAPI.Location currentLocation = HypixelServer.ModAPI.get().getCurrentLocation();
                if (currentLocation != null && currentLocation.isLobby()) {
                    shouldBlink = false;
                    return;
                }
            }

            if (clickSlot.syncId() == mc.player.playerScreenHandler.syncId && allowedAction) {
                mc.getNetworkHandler().sendPacket(new CloseHandledScreenC2SPacket(clickSlot.syncId()));
            } else {
                shouldBlink = true;
            }
        } else if (event.getPacket() instanceof CloseHandledScreenC2SPacket closeScreen && mc.player != null && closeScreen.getSyncId() == mc.player.playerScreenHandler.syncId) {
            if (!isInventoryMoveDisabler()) return;

            shouldBlink = false;
        }
    }

    @Override
    public void onDisable() {
        this.blockHolder.release();
        super.onDisable();
    }

    private final BlockHolder blockHolder = new BlockHolder(OutboundNetworkBlockage.get());

    @Subscribe(priority = 2)
    public void onPreGameTick(final PreGameTickEvent event) {
        if (isInventoryMoveDisabler()) {
            if (mc.currentScreen == null) {
                shouldBlink = false;
            }

            if (shouldBlink) {
                this.blockHolder.block(p -> p, p -> !(p instanceof ClickSlotC2SPacket || p instanceof CloseHandledScreenC2SPacket || p instanceof CommonPongC2SPacket || p instanceof KeepAliveC2SPacket));
            } else {
                this.blockHolder.release();
            }
        }
    }

    @Subscribe
    public void onJoinWorld(final JoinWorldEvent event) {
        shouldBlink = false;
    }

}
