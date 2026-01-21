package wtf.opal.client.feature.module.impl.utility.inventory;

import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.EquippableComponent;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.PlayerScreenHandler;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import wtf.opal.client.OpalClient;
import wtf.opal.client.feature.module.Module;
import wtf.opal.client.feature.module.ModuleCategory;
import wtf.opal.client.feature.module.impl.combat.killaura.KillAuraModule;
import wtf.opal.client.feature.module.impl.movement.InventoryMoveModule;
import wtf.opal.client.feature.module.impl.utility.inventory.manager.InventoryManagerModule;
import wtf.opal.client.feature.module.property.impl.number.BoundedNumberProperty;
import wtf.opal.client.feature.module.repository.ModuleRepository;
import wtf.opal.event.impl.game.PreGameTickEvent;
import wtf.opal.event.subscriber.Subscribe;
import wtf.opal.utility.player.InventoryUtility;

import java.util.List;

import static wtf.opal.client.Constants.mc;

public final class AutoArmorModule extends Module {

    private final BoundedNumberProperty delay = new BoundedNumberProperty("Delay", 100, 200, 0, 1000, 10);

    public AutoArmorModule() {
        super("Auto Armor", "Automatically equips the best armor possible.", ModuleCategory.UTILITY);
        addProperties(delay);
    }

    @Subscribe
    public void onPreGameTickEvent(final PreGameTickEvent event) {
        if (mc.player == null || mc.world == null) return;

        final ModuleRepository moduleRepository = OpalClient.getInstance().getModuleRepository();

        if (!(mc.currentScreen instanceof InventoryScreen) && !moduleRepository.getModule(InventoryMoveModule.class).isEnabled())
            return;

        final KillAuraModule killAuraModule = moduleRepository.getModule(KillAuraModule.class);
        if (killAuraModule.isEnabled() && killAuraModule.getTargeting().isTargetSelected()) {
            return;
        }

        final ScreenHandler screenHandler = mc.player.currentScreenHandler;
        if (!(screenHandler instanceof PlayerScreenHandler playerHandler)) {
            return;
        }

        final InventoryManagerModule managerModule = moduleRepository.getModule(InventoryManagerModule.class);

        // ==========================================
        //         HeyPixel 逻辑移植
        // ==========================================

        // 第一步：检查身上的装备
        for (int i = 5; i <= 8; i++) {
            ItemStack equippedStack = playerHandler.getSlot(i).getStack();
            EquipmentSlot slotType = getSlotType(i);

            if (slotType != null && !equippedStack.isEmpty() && InventoryUtility.isArmor(equippedStack)) {
                // 修复：使用 double 接收返回值
                double currentScore = InventoryUtility.getArmorValue(equippedStack);
                double bestScoreInInventory = getBestArmorScoreInInventory(playerHandler, slotType);

                if (bestScoreInInventory > currentScore) {
                    if (managerModule.canMove(delay.getRandomValue().longValue())) {
                        // 修复：调用 Opal 原有的 drop 方法替代 click(THROW)
                        InventoryUtility.drop(playerHandler, i);
                        managerModule.stopwatch.reset();
                        return;
                    }
                }
            }
        }

        // 第二步：检查背包里的装备
        List<Slot> inventorySlots = playerHandler.slots;
        for (int i = 9; i < 45; i++) {
            if (i >= inventorySlots.size()) break;

            Slot slot = inventorySlots.get(i);
            ItemStack stack = slot.getStack();

            if (!stack.isEmpty() && InventoryUtility.isArmor(stack)) {
                EquipmentSlot itemSlotType = getEquipmentSlot(stack);
                if (itemSlotType == null) continue;

                // 修复：使用 double 接收返回值
                double itemScore = InventoryUtility.getArmorValue(stack);
                double bestScoreForType = getBestArmorScore(playerHandler, itemSlotType);
                double currentEquippedScore = getCurrentArmorScore(itemSlotType);

                if (itemScore == bestScoreForType && itemScore > currentEquippedScore) {
                    if (managerModule.canMove(delay.getRandomValue().longValue())) {
                        // 修复：调用 Opal 原有的 shiftClick 方法替代 click(QUICK_MOVE)
                        InventoryUtility.shiftClick(playerHandler, i, 0);
                        managerModule.stopwatch.reset();
                        return;
                    }
                }
            }
        }
    }

    // 修复：返回类型改为 double
    private double getCurrentArmorScore(EquipmentSlot slotType) {
        ItemStack equipped = mc.player.getEquippedStack(slotType);
        if (equipped.isEmpty() || !InventoryUtility.isArmor(equipped)) {
            return -1;
        }
        return InventoryUtility.getArmorValue(equipped);
    }

    // 修复：返回类型改为 double
    private double getBestArmorScoreInInventory(PlayerScreenHandler handler, EquipmentSlot slotType) {
        double bestScore = -1;
        for (int i = 9; i < 45; i++) {
            if (i >= handler.slots.size()) break;
            ItemStack stack = handler.slots.get(i).getStack();
            if (!stack.isEmpty() && InventoryUtility.isArmor(stack)) {
                if (getEquipmentSlot(stack) == slotType) {
                    double score = InventoryUtility.getArmorValue(stack);
                    if (score > bestScore) {
                        bestScore = score;
                    }
                }
            }
        }
        return bestScore;
    }

    // 修复：返回类型改为 double
    private double getBestArmorScore(PlayerScreenHandler handler, EquipmentSlot slotType) {
        double bestScore = -1;
        for (Slot slot : handler.slots) {
            ItemStack stack = slot.getStack();
            if (!stack.isEmpty() && InventoryUtility.isArmor(stack)) {
                if (getEquipmentSlot(stack) == slotType) {
                    double score = InventoryUtility.getArmorValue(stack);
                    if (score > bestScore) {
                        bestScore = score;
                    }
                }
            }
        }
        return bestScore;
    }

    private EquipmentSlot getEquipmentSlot(ItemStack stack) {
        EquippableComponent equippable = stack.getComponents().get(DataComponentTypes.EQUIPPABLE);
        if (equippable != null) {
            return equippable.slot();
        }

        if (stack.getItem() == Items.ELYTRA) {
            return EquipmentSlot.CHEST;
        }

        return null;
    }

    private EquipmentSlot getSlotType(int slotId) {
        return switch (slotId) {
            case 5 -> EquipmentSlot.HEAD;
            case 6 -> EquipmentSlot.CHEST;
            case 7 -> EquipmentSlot.LEGS;
            case 8 -> EquipmentSlot.FEET;
            default -> null;
        };
    }
}