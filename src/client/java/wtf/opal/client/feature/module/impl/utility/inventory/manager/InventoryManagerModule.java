package wtf.opal.client.feature.module.impl.utility.inventory.manager;

import net.hypixel.data.type.GameType;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ToolComponent;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.item.*;
import net.minecraft.network.packet.s2c.play.ItemPickupAnimationS2CPacket;
import net.minecraft.network.packet.s2c.play.ScreenHandlerSlotUpdateS2CPacket;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.screen.PlayerScreenHandler;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import wtf.opal.client.OpalClient;
import wtf.opal.client.feature.helper.impl.LocalDataWatch;
import wtf.opal.client.feature.helper.impl.server.impl.HypixelServer;
import wtf.opal.client.feature.module.Module;
import wtf.opal.client.feature.module.ModuleCategory;
import wtf.opal.client.feature.module.impl.combat.killaura.KillAuraModule;
import wtf.opal.client.feature.module.impl.movement.InventoryMoveModule;
import wtf.opal.client.feature.module.impl.world.scaffold.ScaffoldModule;
import wtf.opal.client.feature.module.repository.ModuleRepository;
import wtf.opal.event.impl.game.PreGameTickEvent;
import wtf.opal.event.impl.game.packet.ReceivePacketEvent;
import wtf.opal.event.subscriber.Subscribe;
import wtf.opal.utility.misc.chat.ChatUtility;
import wtf.opal.utility.misc.time.Stopwatch;
import wtf.opal.utility.player.InventoryUtility;
import wtf.opal.utility.player.PlayerUtility;

import java.util.Comparator;

import static wtf.opal.client.Constants.mc;

public final class InventoryManagerModule extends Module {

    private final InventoryManagerSettings settings = new InventoryManagerSettings(this);

    public final Stopwatch stopwatch = new Stopwatch();

    public InventoryManagerModule() {
        super("Inventory Manager", "Manages your inventory.", ModuleCategory.UTILITY);
    }

    @Subscribe
    public void onPreGameTickEvent(final PreGameTickEvent event) {
        if (mc.player == null) return;

        final ModuleRepository moduleRepository = OpalClient.getInstance().getModuleRepository();

        if (!(mc.currentScreen instanceof InventoryScreen) && !moduleRepository.getModule(InventoryMoveModule.class).isEnabled())
            return;

        final KillAuraModule killAuraModule = moduleRepository.getModule(KillAuraModule.class);
        final ScaffoldModule scaffoldModule = moduleRepository.getModule(ScaffoldModule.class);
        if ((killAuraModule.isEnabled() && killAuraModule.getTargeting().isTargetSelected())
                || scaffoldModule.isEnabled()) {
            return;
        }

        final boolean blitz;
        if (LocalDataWatch.get().getKnownServerManager().getCurrentServer() instanceof HypixelServer) {
            final HypixelServer.ModAPI.Location currentLocation = HypixelServer.ModAPI.get().getCurrentLocation();
            if (currentLocation == null || currentLocation.isLobby()) {
                return;
            }

            if (currentLocation.serverType() == GameType.SURVIVAL_GAMES) {
                blitz = false;
            } else {
                blitz = false;
                if (currentLocation.serverType() != GameType.SKYWARS) {
                    return;
                }
            }
        } else {
            blitz = false;
        }

        final ScreenHandler screenHandler = mc.player.currentScreenHandler;

        if (!(screenHandler instanceof PlayerScreenHandler playerHandler)) {
            return;
        }

        final Slot bestSword = getBestSword(playerHandler);
        final Slot preferredSwordSlot = screenHandler.getSlot(settings.getSwordSlot() + 35);

        final Slot bestPickaxe = getBestPickaxe(playerHandler);
        final Slot preferredPickaxeSlot = screenHandler.getSlot(settings.getPickaxeSlot() + 35);

        final Slot bestAxe = getBestAxe(playerHandler);
        final Slot preferredAxeSlot = screenHandler.getSlot(settings.getAxeSlot() + 35);

        final Slot mostBlocks = getMostBlocks(playerHandler);
        final Slot preferredBlockSlot = screenHandler.getSlot(settings.getBlockSlot() + 35);

        InventoryUtility.filterSlots(playerHandler, slot -> !slot.getStack().isEmpty(), true).forEach(validSlot -> {
            if (!canMove(settings.getDelay().longValue()) || InventoryUtility.isGoodItem(validSlot.getStack())) {
                return;
            }

            if (validSlot.getStack().getItem().getComponents().get(DataComponentTypes.EQUIPPABLE) != null) {
                return;
            }

            if (settings.getSlots().getProperty("Sword").getValue()) {
                arrangeBestSword(screenHandler, preferredSwordSlot, bestSword);
            }
            if (settings.getSlots().getProperty("Pickaxe").getValue()) {
                arrangeBestPickaxe(screenHandler, preferredPickaxeSlot, bestPickaxe);
            }
            if (settings.getSlots().getProperty("Axe").getValue()) {
                arrangeBestAxe(screenHandler, preferredAxeSlot, bestAxe);
            }
            if (settings.getSlots().getProperty("Blocks").getValue()) {
                arrangeMostBlocks(screenHandler, preferredBlockSlot, mostBlocks);
            }

            if (validSlot.getIndex() == preferredSwordSlot.getIndex() && validSlot.getStack().isIn(ItemTags.SWORDS)) {
                return;
            }
            if (validSlot.getIndex() == preferredPickaxeSlot.getIndex() && validSlot.getStack().isIn(ItemTags.PICKAXES)) {
                return;
            }
            if (validSlot.getIndex() == preferredAxeSlot.getIndex() && validSlot.getStack().getItem() instanceof AxeItem) {
                return;
            }
            if (validSlot.getStack().getItem() instanceof BucketItem) {
                return;
            }

            if (validSlot.getStack().getName().getStyle().isEmpty() || blitz && validSlot.getStack().getItem() != Items.NETHER_STAR) { // blitz star
                InventoryUtility.drop(playerHandler, validSlot.id);
                stopwatch.reset();
            }
        });
    }

    @Subscribe
    public void onReceivePacket(final ReceivePacketEvent event) {
        if (event.getPacket() instanceof ScreenHandlerSlotUpdateS2CPacket slotUpdate
                && slotUpdate.getStack().getItem() != Items.AIR
                && mc.player != null
                && slotUpdate.getSyncId() == mc.player.playerScreenHandler.syncId) {
            stopwatch.reset();
        }
    }

    private void arrangeBestSword(final ScreenHandler screenHandler, final Slot preferredSwordSlot, final Slot bestSwordSlot) {
        if (bestSwordSlot != null && bestSwordSlot.getIndex() != preferredSwordSlot.getIndex()) {
            double bestSwordValue = InventoryUtility.getSwordValue(bestSwordSlot.getStack());
            double preferredSwordValue = InventoryUtility.getSwordValue(preferredSwordSlot.getStack());

            if (bestSwordValue > preferredSwordValue) {
                InventoryUtility.swap(screenHandler, bestSwordSlot.id, preferredSwordSlot.id - 36);
                stopwatch.reset();
            }
        }
    }

    private Slot getBestSword(final ScreenHandler screenHandler) {
        return InventoryUtility.filterSlots(screenHandler, slot -> slot.getStack().isIn(ItemTags.SWORDS), false)
                .stream()
                .max(Comparator.comparing(swordSlot -> InventoryUtility.getSwordValue(swordSlot.getStack())))
                .orElse(null);
    }

    private void arrangeBestPickaxe(final ScreenHandler screenHandler, final Slot preferredPickaxeSlot, final Slot bestPickaxeSlot) {
        if (bestPickaxeSlot != null && bestPickaxeSlot.getIndex() != preferredPickaxeSlot.getIndex()) {
            double bestPickaxeValue = InventoryUtility.getToolValue(bestPickaxeSlot.getStack());
            double preferredPickaxeValue = InventoryUtility.getToolValue(preferredPickaxeSlot.getStack());

            if (bestPickaxeValue > preferredPickaxeValue) {
                InventoryUtility.swap(screenHandler, bestPickaxeSlot.id, preferredPickaxeSlot.id - 36);
                stopwatch.reset();
            }
        }
    }

    private Slot getBestPickaxe(final ScreenHandler screenHandler) {
        return InventoryUtility.filterSlots(screenHandler, slot -> slot.getStack().isIn(ItemTags.PICKAXES), false)
                .stream()
                .max(Comparator.comparing(pickaxeSlot -> InventoryUtility.getToolValue(pickaxeSlot.getStack())))
                .orElse(null);
    }

    private void arrangeBestAxe(final ScreenHandler screenHandler, final Slot preferredAxeSlot, final Slot bestAxeSlot) {
        if (bestAxeSlot != null && bestAxeSlot.getIndex() != preferredAxeSlot.getIndex()) {
            double bestAxeValue = InventoryUtility.getToolValue(bestAxeSlot.getStack());
            double preferredAxeValue = InventoryUtility.getToolValue(preferredAxeSlot.getStack());

            if (bestAxeValue > preferredAxeValue) {
                InventoryUtility.swap(screenHandler, bestAxeSlot.id, preferredAxeSlot.id - 36);
                stopwatch.reset();
            }
        }
    }

    private Slot getBestAxe(final ScreenHandler screenHandler) {
        return InventoryUtility.filterSlots(screenHandler, slot -> slot.getStack().getItem() instanceof AxeItem, false)
                .stream()
                .max(Comparator.comparing(axeSlot -> InventoryUtility.getToolValue(axeSlot.getStack())))
                .orElse(null);
    }

    private Slot getMostBlocks(final ScreenHandler screenHandler) {
        return InventoryUtility.filterSlots(screenHandler, slot ->
                                slot.getStack().getItem() instanceof BlockItem blockItem &&
                                        slot.getStack().getCount() > 0 &&
                                        InventoryUtility.isGoodBlock(blockItem.getBlock())
                        , false)
                .stream()
                .max(Comparator.comparing(blockSlot -> blockSlot.getStack().getCount()))
                .orElse(null);
    }

    private void arrangeMostBlocks(final ScreenHandler screenHandler, final Slot preferredBlockSlot, final Slot mostBlockSlot) {
        if (mostBlockSlot != null && mostBlockSlot.getIndex() != preferredBlockSlot.getIndex()) {
            double mostBlockCount = mostBlockSlot.getStack().getCount();
            double preferredBlockValue = preferredBlockSlot.getStack().getCount();

            if (mostBlockCount > preferredBlockValue) {
                InventoryUtility.swap(screenHandler, mostBlockSlot.id, preferredBlockSlot.id - 36);
                stopwatch.reset();
            }
        }
    }

    public boolean canMove(final long delay) {
        if (delay == 0) return true;

        return stopwatch.hasTimeElapsed(delay);
    }

}
