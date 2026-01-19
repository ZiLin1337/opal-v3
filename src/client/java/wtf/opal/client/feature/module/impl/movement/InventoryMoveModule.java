package wtf.opal.client.feature.module.impl.movement;

import net.minecraft.client.gui.screen.ChatScreen;
import wtf.opal.client.feature.module.Module;
import wtf.opal.client.feature.module.ModuleCategory;
import wtf.opal.client.screen.click.dropdown.DropdownClickGUI;
import wtf.opal.event.impl.game.PreGameTickEvent;
import wtf.opal.event.subscriber.Subscribe;
import wtf.opal.utility.player.PlayerUtility;

import static wtf.opal.client.Constants.mc;

public final class InventoryMoveModule extends Module {

    public InventoryMoveModule() {
        super("Inventory Move", "Allows you to move while in inventories.", ModuleCategory.MOVEMENT);
    }

    @Subscribe
    public void onPreGameTick(final PreGameTickEvent event) {
        if (this.isBlocked()) {
            return;
        }
        PlayerUtility.updateMovementKeyStates();
    }

    public boolean isBlocked() {
        return mc.currentScreen instanceof ChatScreen || mc.currentScreen instanceof DropdownClickGUI;
    }
}
