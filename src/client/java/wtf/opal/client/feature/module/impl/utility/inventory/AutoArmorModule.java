package wtf.opal.client.feature.module.impl.utility.inventory;

import net.hypixel.data.type.GameType;
import net.minecraft.block.PlayerSkullBlock;
import net.minecraft.block.SkullBlock;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.EquippableComponent;
import net.minecraft.enchantment.Enchantments;
import wtf.opal.client.feature.module.impl.combat.killaura.KillAuraModule;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.PlayerScreenHandler;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import wtf.opal.client.OpalClient;
import wtf.opal.client.feature.helper.impl.LocalDataWatch;
import wtf.opal.client.feature.helper.impl.server.impl.HypixelServer;
import wtf.opal.client.feature.module.Module;
import wtf.opal.client.feature.module.ModuleCategory;
import wtf.opal.client.feature.module.impl.movement.InventoryMoveModule;
import wtf.opal.client.feature.module.impl.utility.inventory.manager.InventoryManagerModule;
import wtf.opal.client.feature.module.property.impl.number.BoundedNumberProperty;
import wtf.opal.client.feature.module.repository.ModuleRepository;
import wtf.opal.event.impl.game.PreGameTickEvent;
import wtf.opal.event.subscriber.Subscribe;
import wtf.opal.utility.player.InventoryUtility;
import wtf.opal.utility.player.PlayerUtility;

import java.util.*;
import java.util.stream.Collectors;

import static wtf.opal.client.Constants.mc;

public final class AutoArmorModule extends Module {

    private final BoundedNumberProperty delay = new BoundedNumberProperty("Delay", 50, 100, 0, 400, 5);

    public AutoArmorModule() {
        super("Auto Armor", "Automatically equips the best armor possible.", ModuleCategory.UTILITY);
        addProperties(delay);
    }

    @Subscribe
    public void onPreGameTickEvent(final PreGameTickEvent event) {
        if (mc.player == null) return;

        final ModuleRepository moduleRepository = OpalClient.getInstance().getModuleRepository();

        if (!(mc.currentScreen instanceof InventoryScreen) && !moduleRepository.getModule(InventoryMoveModule.class).isEnabled())
            return;

        final KillAuraModule killAuraModule = moduleRepository.getModule(KillAuraModule.class);
        if (killAuraModule.isEnabled() && killAuraModule.getTargeting().isTargetSelected()) {
            return;
        }

        if (LocalDataWatch.get().getKnownServerManager().getCurrentServer() instanceof HypixelServer) {
            final HypixelServer.ModAPI.Location currentLocation = HypixelServer.ModAPI.get().getCurrentLocation();
            if (currentLocation != null && (currentLocation.isLobby() || !(currentLocation.serverType() == GameType.SKYWARS || currentLocation.serverType() == GameType.SURVIVAL_GAMES))) {
                return;
            }
        }

        final ScreenHandler screenHandler = mc.player.currentScreenHandler;

        if (!(screenHandler instanceof PlayerScreenHandler playerHandler)) {
            return;
        }

        final InventoryManagerModule managerModule = moduleRepository.getModule(InventoryManagerModule.class);

        final List<Slot> bestArmor = getBestArmor(playerHandler);

        InventoryUtility.filterSlots(playerHandler, slot -> !slot.getStack().isEmpty() && InventoryUtility.isArmor(slot.getStack()), true).forEach(validSlot -> {
            final ItemStack itemStack = validSlot.getStack();

            if (bestArmor.stream().noneMatch(armor -> armor.getStack() == itemStack)) {
                if (!managerModule.canMove(delay.getRandomValue().longValue())) return;

                InventoryUtility.drop(playerHandler, validSlot.id);

                managerModule.stopwatch.reset();
            }
        });

        bestArmor.forEach(equipmentSlotPair -> {
            final List<ItemStack> armorStacks = getArmorStacks();
            Collections.shuffle(armorStacks);

            if (armorStacks.stream().noneMatch(armor -> equipmentSlotPair.getStack() == armor)) {
                if (!managerModule.canMove(delay.getRandomValue().longValue())) return;

                InventoryUtility.shiftClick(playerHandler, equipmentSlotPair.id, 0);

                managerModule.stopwatch.reset();
            }
        });
    }

    private List<Slot> getBestArmor(final PlayerScreenHandler screenHandler) {
        return Arrays.stream(EquipmentSlot.values())
                .map(slotType -> InventoryUtility.filterSlots(screenHandler, slot -> {
                            if (slot.getStack().isEmpty() || !InventoryUtility.isArmor(slot.getStack())) {
                                return false;
                            }
                            final EquippableComponent equippable = slot.getStack().getComponents().get(DataComponentTypes.EQUIPPABLE);
                            return equippable != null && equippable.slot() == slotType;
                        }, false)
                        .stream()
                        .max(Comparator.comparing(slot -> InventoryUtility.getArmorValue(slot.getStack())))
                        .orElse(null))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    private List<ItemStack> getArmorStacks() {
        final List<ItemStack> armorStacks = new ArrayList<>();

        for (final EquipmentSlot slot : EquipmentSlot.values()) {
            final ItemStack equippedStack = mc.player.getEquippedStack(slot);
            if (!equippedStack.isEmpty() && InventoryUtility.isArmor(equippedStack)) {
                armorStacks.add(equippedStack);
            }
        }

        return armorStacks;
    }

}
