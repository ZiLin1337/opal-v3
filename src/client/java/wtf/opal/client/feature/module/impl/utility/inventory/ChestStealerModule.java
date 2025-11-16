package wtf.opal.client.feature.module.impl.utility.inventory;

import net.minecraft.client.gui.screen.ingame.GenericContainerScreen;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.EquippableComponent;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.*;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.screen.GenericContainerScreenHandler;
import wtf.opal.client.feature.module.Module;
import wtf.opal.client.feature.module.ModuleCategory;
import wtf.opal.client.feature.module.property.impl.bool.BooleanProperty;
import wtf.opal.client.feature.module.property.impl.number.BoundedNumberProperty;
import wtf.opal.event.impl.game.PreGameTickEvent;
import wtf.opal.event.subscriber.Subscribe;
import wtf.opal.utility.misc.chat.ChatUtility;
import wtf.opal.utility.misc.time.Stopwatch;
import wtf.opal.utility.player.InventoryUtility;

import java.util.*;
import java.util.stream.IntStream;

import static wtf.opal.client.Constants.mc;

public final class
ChestStealerModule extends Module {

    private final Stopwatch stopwatch = new Stopwatch();

    private final BooleanProperty smart = new BooleanProperty("Smart", true);
    private final BooleanProperty highlight = new BooleanProperty("Highlight items", true).hideIf(() -> !smart.getValue());

    private final BoundedNumberProperty delay = new BoundedNumberProperty("Delay", 50, 100, 0, 400, 5);

    public ChestStealerModule() {
        super("Chest Stealer", "Steals only useful or upgraded items from chests.", ModuleCategory.UTILITY);
        addProperties(smart, highlight, delay);
    }

    @Subscribe
    public void onPreGameTickEvent(final PreGameTickEvent event) {
        if (!(mc.currentScreen instanceof GenericContainerScreen container)) return;

        final GenericContainerScreenHandler screenHandler = container.getScreenHandler();
        final Inventory chestInventory = screenHandler.getInventory();

        if (!container.getTitle().getString().toLowerCase().contains("chest")) return;
        if (chestInventory.isEmpty() || InventoryUtility.isInventoryFull()) {
            container.close();
            return;
        }

        final Map<EquipmentSlot, ItemStack> bestChestArmor = getBestChestArmor(chestInventory);
        final ItemStack bestChestSword = getBestChestSword(chestInventory);
        final ItemStack bestChestPickaxe = getBestChestTool(chestInventory, ItemTags.PICKAXES);
        final ItemStack bestChestAxe = getBestChestTool(chestInventory, ItemTags.AXES);

        boolean tookItem = false;

        for (int i = 0; i < chestInventory.size(); i++) {
            final ItemStack stack = chestInventory.getStack(i);
            if (stack.isEmpty()) continue;

            if (canMove() && (shouldTake(stack, bestChestArmor, bestChestSword, bestChestPickaxe, bestChestAxe) || !smart.getValue())) {
                InventoryUtility.shiftClick(screenHandler, i, 0);
                stopwatch.reset();
                tookItem = true;
                break;
            }
        }

        if (smart.getValue() && !tookItem) {
            boolean hasValuableLeft = false;
            for (int i = 0; i < chestInventory.size(); i++) {
                final ItemStack stack = chestInventory.getStack(i);
                if (stack.isEmpty()) continue;

                if (shouldTake(stack, bestChestArmor, bestChestSword, bestChestPickaxe, bestChestAxe)) {
                    hasValuableLeft = true;
                    break;
                }
            }

            if (!hasValuableLeft) {
                container.close();
            }
        }
    }

    public BooleanProperty getHighlight() {
        return highlight;
    }

    public BooleanProperty getSmart() {
        return smart;
    }

    public boolean shouldTake(ItemStack stack,
                              Map<EquipmentSlot, ItemStack> bestChestArmor,
                              ItemStack bestChestSword,
                              ItemStack bestChestPickaxe,
                              ItemStack bestChestAxe) {
        if (InventoryUtility.isGoodItem(stack)) {
            return true;
        }

        if (stack.isIn(ItemTags.SWORDS)) {
            final double value = InventoryUtility.getSwordValue(stack);
            final double current = InventoryUtility.getSwordValue(getBestHotbarSword());

            return stack == bestChestSword && value > current;
        }

        if (stack.isIn(ItemTags.PICKAXES)) {
            final double value = InventoryUtility.getToolValue(stack);
            final double current = InventoryUtility.getToolValue(getBestHotbarTool(ItemTags.PICKAXES));

            return stack == bestChestPickaxe && value > current;
        }

        if (stack.isIn(ItemTags.AXES)) {
            final double value = InventoryUtility.getToolValue(stack);
            final double current = InventoryUtility.getToolValue(getBestHotbarAxe());

            return stack == bestChestAxe && value > current;
        }

        if (!InventoryUtility.isArmor(stack)) return false;

        final EquippableComponent equip = stack.getComponents().get(DataComponentTypes.EQUIPPABLE);
        if (equip == null) return false;


        final EquipmentSlot slot = equip.slot();
        final ItemStack currentEquipped = mc.player.getEquippedStack(slot);
        final ItemStack bestInChest = bestChestArmor.getOrDefault(slot, ItemStack.EMPTY);

        if (stack != bestInChest) return false;


        final double stackValue = InventoryUtility.getArmorValue(stack);
        final double equippedValue = InventoryUtility.getArmorValue(currentEquipped);

        return stackValue > equippedValue;

    }

    public Map<EquipmentSlot, ItemStack> getBestChestArmor(Inventory chest) {
        return IntStream.range(0, chest.size())
                .mapToObj(chest::getStack)
                .filter(InventoryUtility::isArmor)
                .map(stack -> {
                    final EquippableComponent equip = stack.getComponents().get(DataComponentTypes.EQUIPPABLE);
                    return equip != null ? Map.entry(equip.slot(), stack) : null;
                })
                .filter(Objects::nonNull)
                .collect(HashMap::new, (map, entry) -> {
                    map.merge(entry.getKey(), entry.getValue(), (existing, replacement) ->
                            InventoryUtility.getArmorValue(replacement) > InventoryUtility.getArmorValue(existing)
                                    ? replacement : existing);
                }, HashMap::putAll);
    }

    public ItemStack getBestChestSword(Inventory chest) {
        return IntStream.range(0, chest.size())
                .mapToObj(chest::getStack)
                .filter(stack -> stack.isIn(ItemTags.SWORDS))
                .max(Comparator.comparingDouble(InventoryUtility::getSwordValue))
                .orElse(ItemStack.EMPTY);
    }

    public ItemStack getBestChestTool(Inventory chest, TagKey<Item> tag) {
        return IntStream.range(0, chest.size())
                .mapToObj(chest::getStack)
                .filter(stack -> stack.isIn(tag))
                .max(Comparator.comparingDouble(InventoryUtility::getToolValue))
                .orElse(ItemStack.EMPTY);
    }

    private ItemStack getBestHotbarSword() {
        return IntStream.range(0, 9)
                .mapToObj(i -> mc.player.getInventory().getStack(i))
                .filter(stack -> stack.isIn(ItemTags.SWORDS))
                .max(Comparator.comparingDouble(InventoryUtility::getSwordValue))
                .orElse(ItemStack.EMPTY);
    }

    private ItemStack getBestHotbarTool(TagKey<Item> tag) {
        return IntStream.range(0, 9)
                .mapToObj(i -> mc.player.getInventory().getStack(i))
                .filter(stack -> stack.isIn(tag))
                .max(Comparator.comparingDouble(InventoryUtility::getToolValue))
                .orElse(ItemStack.EMPTY);
    }

    private ItemStack getBestHotbarAxe() {
        return IntStream.range(0, 9)
                .mapToObj(i -> mc.player.getInventory().getStack(i))
                .filter(stack -> stack.getItem() instanceof AxeItem)
                .max(Comparator.comparingDouble(InventoryUtility::getToolValue))
                .orElse(ItemStack.EMPTY);
    }

    public boolean canMove() {
        final long delayMs = delay.getRandomValue().longValue();
        return delayMs == 0 || stopwatch.hasTimeElapsed(delayMs);
    }
}
