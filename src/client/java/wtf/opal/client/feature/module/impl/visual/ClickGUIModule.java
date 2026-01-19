package wtf.opal.client.feature.module.impl.visual;

import org.lwjgl.glfw.GLFW;
import wtf.opal.client.OpalClient;
import wtf.opal.client.binding.type.InputType;
import wtf.opal.client.feature.module.Module;
import wtf.opal.client.feature.module.ModuleCategory;
import wtf.opal.client.feature.module.property.impl.bool.BooleanProperty;
import wtf.opal.client.screen.click.dropdown.DropdownClickGUI;
import wtf.opal.event.impl.game.PreGameTickEvent;
import wtf.opal.event.impl.render.RenderBloomEvent;
import wtf.opal.event.subscriber.Subscribe;
import wtf.opal.utility.player.PlayerUtility;

import static wtf.opal.client.Constants.mc;

public final class ClickGUIModule extends Module {

    private final BooleanProperty allowMovement = new BooleanProperty("Allow movement", true);
    private final DropdownClickGUI dropdownClickGUI = new DropdownClickGUI();

    public ClickGUIModule() {
        super("Click GUI", "A display for interacting with client features.", ModuleCategory.VISUAL);
        addProperties(allowMovement);
        OpalClient.getInstance().getBindRepository().getBindingService().register(GLFW.GLFW_KEY_RIGHT_SHIFT, this, InputType.KEYBOARD);
    }

    @Override
    protected void onEnable() {
        mc.setScreen(dropdownClickGUI);
    }

    @Override
    protected void onDisable() {
        if (mc.currentScreen == dropdownClickGUI) {
            dropdownClickGUI.close();
        }
    }

    @Subscribe
    public void onPreGameTick(final PreGameTickEvent event) {
        if (!allowMovement.getValue() || mc.currentScreen != dropdownClickGUI) {
            return;
        }
        if (DropdownClickGUI.selectingBind || DropdownClickGUI.typingString) {
            PlayerUtility.unpressMovementKeyStates();
        } else {
            PlayerUtility.updateMovementKeyStates();
        }
    }

    @Subscribe
    public void onBloomRender(final RenderBloomEvent event) {
        if (mc.currentScreen == dropdownClickGUI) {
            dropdownClickGUI.render(event.drawContext(), -1, -1, event.tickDelta());
        }
    }

}