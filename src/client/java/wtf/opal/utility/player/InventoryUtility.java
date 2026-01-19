package wtf.opal.utility.player;

import net.minecraft.block.*;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ToolComponent;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.item.*;
import net.minecraft.registry.*;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.EmptyBlockView;

import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static wtf.opal.client.Constants.mc;

public final class InventoryUtility {

    private InventoryUtility() {
    }

    public static int findItemInHotbar(final Item item) {
        return IntStream.range(0, 9)
                .filter(i -> {
                    final ItemStack itemStack = mc.player.getInventory().getMainStacks().get(i);
                    return itemStack.getItem() == item && itemStack.getCount() > 0;

                }).findFirst()
                .orElse(-1);
    }

    public static boolean isInventoryFull() {
        return mc.player.getInventory().getMainStacks().stream().noneMatch(ItemStack::isEmpty);
    }

    public static boolean isArmor(final ItemStack itemStack) {
        if (itemStack.getItem() == Items.PLAYER_HEAD || itemStack.getItem() == Items.PUMPKIN) {
            return false;
        }

        return itemStack.getComponents().get(DataComponentTypes.EQUIPPABLE) != null;
    }

    public static double getSwordValue(final ItemStack itemStack) {
        if (!(itemStack.isIn(ItemTags.SWORDS))) {
            return 0.0;
        }

        double score = PlayerUtility.getStackAttackDamage(itemStack);

        final int sharpnessLevel = InventoryUtility.calculateEnchantmentLevel(itemStack, Enchantments.SHARPNESS) + 1;
        score *= sharpnessLevel;

        score += InventoryUtility.calculateEnchantmentLevel(itemStack, Enchantments.FIRE_ASPECT);

        final float durabilityRatio = itemStack.getDamage() / (float) itemStack.getMaxDamage();
        score -= durabilityRatio * 0.1;

        return score;
    }

    public static double getArmorValue(final ItemStack itemStack) {
        if (!InventoryUtility.isArmor(itemStack)) {
            return 0.0;
        }

        double score = PlayerUtility.getArmorProtection(itemStack);

        final int protectionLevel = InventoryUtility.calculateEnchantmentLevel(itemStack, Enchantments.PROTECTION) + 1;
        score *= protectionLevel;

        score += InventoryUtility.calculateEnchantmentLevel(itemStack, Enchantments.THORNS);
        score += InventoryUtility.calculateEnchantmentLevel(itemStack, Enchantments.UNBREAKING) * 0.5;
        score += InventoryUtility.calculateEnchantmentLevel(itemStack, Enchantments.PROJECTILE_PROTECTION) * 0.25;

        final float durabilityRatio = itemStack.getDamage() / (float) itemStack.getMaxDamage();
        score -= durabilityRatio * 0.1;

        return score;
    }

    public static double getToolValue(final ItemStack itemStack) {
        final ToolComponent toolComponent = itemStack.get(DataComponentTypes.TOOL);
        if (toolComponent == null) {
            return 0;
        }

        double score = toolComponent.damagePerBlock();

        final int efficiencyLevel = InventoryUtility.calculateEnchantmentLevel(itemStack, Enchantments.EFFICIENCY) + 1;
        score *= efficiencyLevel;

        score += InventoryUtility.calculateEnchantmentLevel(itemStack, Enchantments.UNBREAKING);

        final float durabilityRatio = itemStack.getDamage() / (float) itemStack.getMaxDamage();
        score -= durabilityRatio * 0.1;

        return score;
    }

    public static boolean isGoodItem(final ItemStack itemStack) {
        final Item item = itemStack.getItem();

        if (item instanceof BlockItem blockItem) {
            return isGoodBlock(blockItem.getBlock());
        }

        if (item == Items.PLAYER_HEAD || item == Items.PUMPKIN || item == Items.CARVED_PUMPKIN) {
            return false;
        }

        return item instanceof EnderPearlItem
                || item instanceof PotionItem
                || item instanceof ShieldItem
                || item instanceof FireChargeItem
                || item.getComponents().contains(DataComponentTypes.FOOD);
    }

    public static List<Slot> filterSlots(final ScreenHandler screenHandler, final Predicate<Slot> filterCondition, final boolean shuffle) {
        final List<Slot> filteredSlots = screenHandler.slots.stream().filter(filterCondition).collect(Collectors.toList());

        if (shuffle)
            Collections.shuffle(filteredSlots);

        return filteredSlots;
    }

    public static void drop(final ScreenHandler screenHandler, final int slot) {
        mc.interactionManager.clickSlot(screenHandler.syncId, slot, 1, SlotActionType.THROW, mc.player);
    }

    public static void shiftClick(final ScreenHandler screenHandler, final int slot, final int mouseButton) {
        mc.interactionManager.clickSlot(screenHandler.syncId, slot, mouseButton, SlotActionType.QUICK_MOVE, mc.player);
    }

    public static void swap(final ScreenHandler screenHandler, final int originalSlot, final int newSlot) {
        mc.interactionManager.clickSlot(screenHandler.syncId, originalSlot, newSlot, SlotActionType.SWAP, mc.player);
    }

    public static int calculateEnchantmentLevel(final ItemStack itemStack, final RegistryKey<Enchantment> enchantment) {
        final DynamicRegistryManager drm = mc.world.getRegistryManager();
        final RegistryWrapper.Impl<Enchantment> registryWrapper = drm.getOrThrow(RegistryKeys.ENCHANTMENT);
        return EnchantmentHelper.getLevel(registryWrapper.getOrThrow(enchantment), itemStack);
    }

    public static boolean isGoodBlock(final Block block) {
        return !isBlockInteractable(block)
                && block.getDefaultState().getOutlineShape(EmptyBlockView.INSTANCE, mc.player.getBlockPos(), ShapeContext.of(mc.player)) == VoxelShapes.fullCube()
                && !(block instanceof TntBlock)
                && !(block instanceof FallingBlock);
    }

    public static boolean isBlockInteractable(final Block block) {
        return interactableBlocks.contains(block);
    }

    private static final List<Block> interactableBlocks = Registries.BLOCK.stream()
            .filter(block ->
                    block instanceof TrapdoorBlock ||
                            block instanceof SweetBerryBushBlock ||
                            block instanceof AbstractFurnaceBlock ||
                            block instanceof AbstractSignBlock ||
                            block instanceof AnvilBlock ||
                            block instanceof BarrelBlock ||
                            block instanceof BeaconBlock ||
                            block instanceof BedBlock ||
                            block instanceof BellBlock ||
                            block instanceof BrewingStandBlock ||
                            block instanceof ButtonBlock ||
                            block instanceof CakeBlock ||
                            block instanceof CandleCakeBlock ||
                            block instanceof CartographyTableBlock ||
                            block instanceof CaveVinesBodyBlock ||
                            block instanceof CaveVinesHeadBlock ||
                            block instanceof ChestBlock ||
                            block instanceof ChiseledBookshelfBlock ||
                            block instanceof CommandBlock ||
                            block instanceof ComparatorBlock ||
                            block instanceof ComposterBlock ||
                            block instanceof CraftingTableBlock ||
                            block instanceof DaylightDetectorBlock ||
                            block instanceof DecoratedPotBlock ||
                            block instanceof DispenserBlock ||
                            block instanceof DoorBlock ||
                            block instanceof DragonEggBlock ||
                            block instanceof EnchantingTableBlock ||
                            block instanceof EnderChestBlock ||
                            block instanceof FenceBlock ||
                            block instanceof FenceGateBlock ||
//                            block instanceof TableBloc ||
                            block instanceof FlowerPotBlock ||
                            block instanceof GrindstoneBlock ||
                            block instanceof HopperBlock ||
                            block instanceof JigsawBlock ||
                            block instanceof JukeboxBlock ||
                            block instanceof LecternBlock ||
                            block instanceof LeverBlock ||
                            block instanceof LightBlock ||
                            block instanceof LoomBlock ||
                            block instanceof NoteBlock ||
                            block instanceof PistonExtensionBlock ||
                            block instanceof RedstoneWireBlock ||
                            block instanceof RepeaterBlock ||
                            block instanceof RespawnAnchorBlock ||
                            block instanceof ShulkerBoxBlock ||
                            block instanceof SmithingTableBlock ||
                            block instanceof StonecutterBlock ||
                            block instanceof FlowerBlock ||
                            block instanceof StructureBlock ||
                            block instanceof SlimeBlock ||
                            block instanceof CobwebBlock)
            .toList();

}
