package wtf.opal.client.feature.module.impl.world.scaffold;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.BufferAllocator;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.s2c.play.ItemPickupAnimationS2CPacket;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.*;
import wtf.opal.client.feature.helper.impl.LocalDataWatch;
import wtf.opal.client.feature.helper.impl.player.mouse.MouseButton;
import wtf.opal.client.feature.helper.impl.player.mouse.MouseHelper;
import wtf.opal.client.feature.helper.impl.player.rotation.RotationHelper;
import wtf.opal.client.feature.helper.impl.player.slot.SlotHelper;
import wtf.opal.client.feature.helper.impl.player.swing.SwingDelay;
import wtf.opal.client.feature.helper.impl.render.FadingBlockHelper;
import wtf.opal.client.feature.module.Module;
import wtf.opal.client.feature.module.ModuleCategory;
import wtf.opal.client.feature.module.impl.visual.overlay.impl.dynamicisland.IslandTrigger;
import wtf.opal.client.renderer.world.WorldRenderer;
import wtf.opal.event.impl.game.PreGameTickEvent;
import wtf.opal.event.impl.game.input.MouseHandleInputEvent;
import wtf.opal.event.impl.game.input.MoveInputEvent;
import wtf.opal.event.impl.game.packet.ReceivePacketEvent;
import wtf.opal.event.impl.game.player.interaction.block.BlockPlacedEvent;
import wtf.opal.event.impl.game.player.movement.ClipAtLedgeEvent;
import wtf.opal.event.impl.render.RenderWorldEvent;
import wtf.opal.event.subscriber.Subscribe;
import wtf.opal.mixin.LivingEntityAccessor;
import wtf.opal.utility.player.InventoryUtility;
import wtf.opal.utility.player.PlayerUtility;
import wtf.opal.utility.player.RaytracedRotation;
import wtf.opal.utility.player.RotationUtility;
import wtf.opal.utility.render.ColorUtility;
import wtf.opal.utility.render.CustomRenderLayers;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static wtf.opal.client.Constants.mc;

public final class ScaffoldModule extends Module implements IslandTrigger {

    private final ScaffoldIsland dynamicIsland = new ScaffoldIsland(this);
    private final ScaffoldSettings settings = new ScaffoldSettings(this);

    private PlaceData placeData;
    private RaytracedRotation rotation;

    private int sameYPos;
    private int tellyAirTicks;

    private Map<Integer, Integer> realStackSizeMap;

    private int placeTick;

    public ScaffoldModule() {
        super("Scaffold", "Automatically places blocks under you.", ModuleCategory.WORLD);
    }

    @Override
    protected void onEnable() {
        super.onEnable();

        this.placeData = null;
        this.rotation = null;
        this.placeTick = 0;
        this.tellyAirTicks = 0;

        this.realStackSizeMap = new HashMap<>();

        if (mc.player != null) {
            this.sameYPos = MathHelper.floor(mc.player.getY());
        }
    }

    @Override
    protected void onDisable() {
        this.dynamicIsland.onDisable();
        this.placeData = null;
        this.rotation = null;
        this.realStackSizeMap = null;
        this.tellyAirTicks = 0;
        this.placeTick = 0;

        super.onDisable();
    }

    @Override
    public String getSuffix() {
        return this.settings.getMode().toString();
    }

    public ScaffoldSettings getSettings() {
        return settings;
    }

    @Subscribe
    public void onBlockPlaced(final BlockPlacedEvent event) {
        if (mc.player == null || this.realStackSizeMap == null || mc.interactionManager == null) {
            return;
        }
        if (!mc.interactionManager.getCurrentGameMode().isCreative()) {
            final int selectedSlot = mc.player.getInventory().getSelectedSlot();
            this.realStackSizeMap.put(
                    selectedSlot,
                    this.realStackSizeMap.getOrDefault(selectedSlot, mc.player.getMainHandStack().getCount() + 1) - 1
            );
        }
    }

    @Subscribe
    public void onReceivePacket(final ReceivePacketEvent event) {
        if (mc.player == null || this.realStackSizeMap == null) {
            return;
        }
        if (event.getPacket() instanceof ItemPickupAnimationS2CPacket pickup
                && pickup.getCollectorEntityId() == mc.player.getId()) {
            final int selectedSlot = mc.player.getInventory().getSelectedSlot();
            this.realStackSizeMap.put(
                    selectedSlot,
                    this.realStackSizeMap.getOrDefault(selectedSlot, mc.player.getMainHandStack().getCount() - pickup.getStackAmount()) + pickup.getStackAmount()
            );
        }
    }

    @Subscribe(priority = 1)
    public void onClipAtLedge(final ClipAtLedgeEvent event) {
        if (!this.settings.isSafeWalkEnabled() || mc.player == null) {
            return;
        }

        final boolean holdingBlock = mc.player.getMainHandStack().getItem() instanceof BlockItem
                || mc.player.getOffHandStack().getItem() instanceof BlockItem;

        if (holdingBlock) {
            event.setClip(true);
        }
    }

    @Subscribe(priority = 1)
    public void onMoveInput(final MoveInputEvent event) {
        if (mc.player == null) {
            return;
        }

        if (this.settings.isSameYEnabled() && this.settings.isAutoJumpEnabled() && mc.player.isOnGround() && LocalDataWatch.get().groundTicks > 0) {
            final wtf.opal.client.feature.simulation.PlayerSimulation simulation = new wtf.opal.client.feature.simulation.PlayerSimulation(mc.player);
            final net.minecraft.client.network.OtherClientPlayerEntity entity = simulation.getSimulatedEntity();

            boolean shouldJump = false;
            for (int i = 0; i < 2; i++) {
                simulation.simulateTick();
                if (!entity.isOnGround()) {
                    shouldJump = true;
                    break;
                }
            }

            if (shouldJump) {
                ((LivingEntityAccessor) mc.player).setJumpingCooldown(0);
                event.setJump(true);
            }
        }

        if (this.settings.isTowerEnabled() && PlayerUtility.isKeyPressed(mc.options.jumpKey)) {
            event.setJump(true);
        }
    }

    @Subscribe(priority = 1)
    public void onPreGameTick(final PreGameTickEvent event) {
        if (mc.player == null || mc.world == null) {
            this.placeData = null;
            this.rotation = null;
            return;
        }

        final int hotbarSlot = getPlaceableBlockSlot();
        final boolean offhandBlock = isGoodBlockStack(mc.player.getOffHandStack());
        if (hotbarSlot == -1 && !offhandBlock) {
            this.placeData = null;
            this.rotation = null;
            return;
        }

        if (hotbarSlot != -1) {
            final SlotHelper.Silence silence = switch (this.settings.getSwitchMode()) {
                case NORMAL -> SlotHelper.Silence.NONE;
                case FULL -> SlotHelper.Silence.FULL;
                case HOTBAR -> SlotHelper.Silence.DEFAULT;
            };
            SlotHelper.setCurrentItem(hotbarSlot).silence(silence);
        }

        final boolean shouldUpdateY = !this.settings.isSameYEnabled()
                || this.settings.isTowerEnabled() && PlayerUtility.isKeyPressed(mc.options.jumpKey)
                || (this.settings.isAutoJumpEnabled() && PlayerUtility.isKeyPressed(mc.options.jumpKey))
                || mc.player.isOnGround()
                || Math.abs(MathHelper.floor(mc.player.getY()) - this.sameYPos) > 3;

        if (shouldUpdateY) {
            this.sameYPos = MathHelper.floor(mc.player.getY());
        }

        final BlockPos targetPos = mc.player.getBlockPos().withY(this.sameYPos).down();
        PlaceData data = findPlaceData(targetPos, mc.player);

        if (this.settings.getMode() == ScaffoldSettings.Mode.TELLY) {
            if (mc.player.isOnGround()) {
                this.tellyAirTicks = 0;
            } else {
                this.tellyAirTicks++;
            }

            if (!mc.player.isOnGround() && this.tellyAirTicks < this.settings.getTellyTick()) {
                data = null;
            }
        }

        this.placeData = data;
        this.rotation = data != null ? data.rotations : null;

        if (this.rotation != null) {
            RotationHelper.getHandler().rotate(
                    this.rotation.rotation(),
                    new ScaffoldRotationModel(this.settings.getRotateSpeed(), this.settings.getRotateBackSpeed(), this.rotation.rotation())
            );
        }
    }

    @Subscribe(priority = 1)
    public void onHandleInput(final MouseHandleInputEvent event) {
        if (mc.player == null || mc.world == null) {
            return;
        }

        final boolean holdingBlock = isGoodBlockStack(mc.player.getMainHandStack()) || isGoodBlockStack(mc.player.getOffHandStack());
        if (!holdingBlock) {
            MouseHelper.getRightButton().setDisabled();
            return;
        }

        if (this.placeData == null || this.rotation == null) {
            if (!this.simulateClick()) {
                MouseHelper.getRightButton().setDisabled();
            }
            return;
        }

        if (this.settings.isOverrideRaycast()) {
            mc.crosshairTarget = this.rotation.hitResult();
        }

        final Block blockOver = PlayerUtility.getBlockOver();
        if (blockOver != null && InventoryUtility.isBlockInteractable(blockOver)) {
            return;
        }

        final MouseButton rightButton = MouseHelper.getRightButton();
        rightButton.setPressed();
        if (this.settings.getSwingMode() == ScaffoldSettings.SwingMode.SERVER) {
            rightButton.setShowSwings(false);
        }

        this.placeTick = mc.player.age;

        if (this.settings.isBlockOverlayEnabled()) {
            final BlockPos placedPos = this.placeData.target.blockPos.offset(this.placeData.target.side);
            FadingBlockHelper.getInstance().addFadingBlock(
                    new FadingBlockHelper.FadingBlock(
                            placedPos,
                            0xFFFFFFFF,
                            ColorUtility.applyOpacity(ColorUtility.getClientTheme().first, 0.25F),
                            300
                    )
            );
        }
    }

    private boolean simulateClick() {
        if (!SwingDelay.isSwingAvailable(this.settings.getPlaceCps(), false)) {
            return false;
        }

        if (!(mc.crosshairTarget instanceof BlockHitResult blockHitResult)) {
            return false;
        }

        final BlockPos blockPos = blockHitResult.getBlockPos();
        final Block block = mc.world.getBlockState(blockPos).getBlock();
        if (InventoryUtility.isBlockInteractable(block)) {
            return false;
        }

        final ItemStack stack = mc.player.getMainHandStack().getItem() instanceof BlockItem ? mc.player.getMainHandStack() : mc.player.getOffHandStack();
        if (!(stack.getItem() instanceof BlockItem)) {
            return false;
        }

        MouseHelper.getRightButton().setPressed();
        this.settings.getPlaceCps().resetClick();
        SwingDelay.reset();
        return true;
    }

    @Subscribe
    public void onRenderWorld(final RenderWorldEvent event) {
        if (!(mc.crosshairTarget instanceof BlockHitResult blockHitResult) || rotation == null || !settings.isBlockOverlayEnabled() || mc.world == null) {
            return;
        }

        if (mc.world.getBlockState(blockHitResult.getBlockPos()).isAir()) {
            return;
        }

        final Vec3d startVec = new Vec3d(blockHitResult.getBlockPos().getX(), blockHitResult.getBlockPos().getY(), blockHitResult.getBlockPos().getZ());
        final Vec3d dimensions = new Vec3d(1, 1, 1);

        final VertexConsumerProvider.Immediate vcp = VertexConsumerProvider.immediate(new BufferAllocator(1024));
        final WorldRenderer rc = new WorldRenderer(vcp);

        rc.drawFilledCube(
                event.matrixStack(),
                CustomRenderLayers.getPositionColorQuads(true),
                startVec,
                dimensions,
                ColorUtility.applyOpacity(ColorUtility.getClientTheme().first, 0.25F)
        );

        vcp.draw();
    }

    private int getPlaceableBlockSlot() {
        if (mc.player == null) {
            return -1;
        }

        for (int i = 0; i < 9; i++) {
            final ItemStack stack = mc.player.getInventory().getMainStacks().get(i);
            if (!isGoodBlockStack(stack)) {
                continue;
            }
            final int count = this.realStackSizeMap != null
                    ? this.realStackSizeMap.getOrDefault(i, stack.getCount())
                    : stack.getCount();

            if (count > 0) {
                return i;
            }
        }

        return -1;
    }

    private boolean isGoodBlockStack(final ItemStack stack) {
        return stack.getItem() instanceof BlockItem blockItem && InventoryUtility.isGoodBlock(blockItem.getBlock());
    }

    private PlaceData findPlaceData(final BlockPos targetBlockPos, final PlayerEntity entity) {
        if (!mc.world.getBlockState(targetBlockPos).isReplaceable()) {
            return null;
        }

        final BlockPos.Mutable mutable = new BlockPos.Mutable();
        final List<BlockTarget> possible = new ArrayList<>();

        final int range = 2;
        for (int dy = 0; dy >= -range; dy--) {
            for (int dx = -range; dx <= range; dx++) {
                for (int dz = -range; dz <= range; dz++) {
                    mutable.set(targetBlockPos.getX() + dx, targetBlockPos.getY() + dy, targetBlockPos.getZ() + dz);

                    if (!mc.world.getBlockState(mutable).isReplaceable()) {
                        continue;
                    }

                    for (final Direction dir : Direction.values()) {
                        final BlockPos neighbour = new BlockPos(
                                mutable.getX() + dir.getOffsetX(),
                                mutable.getY() + dir.getOffsetY(),
                                mutable.getZ() + dir.getOffsetZ()
                        );
                        if (isSolidSupport(neighbour)) {
                            possible.add(new BlockTarget(neighbour, dir.getOpposite()));
                        }
                    }
                }
            }
        }

        if (possible.isEmpty()) {
            return null;
        }

        possible.sort(Comparator.comparingDouble(target -> target.blockPos.offset(target.side).getSquaredDistance(targetBlockPos)));

        final Vec3d eyePos = entity.getEntityPos().add(0.0D, entity.getStandingEyeHeight(), 0.0D);
        final Vec2f priority = RotationUtility.getRotation();

        for (final BlockTarget target : possible) {
            final RaytracedRotation rotations = RotationUtility.getRotationFromRaycastedBlock(target.blockPos, target.side, priority, eyePos);
            if (rotations != null) {
                return new PlaceData(target, rotations);
            }
        }

        return null;
    }

    private boolean isSolidSupport(final BlockPos pos) {
        final BlockState state = mc.world.getBlockState(pos);
        return !state.isReplaceable() && state.getFluidState().isEmpty();
    }

    private record BlockTarget(BlockPos blockPos, Direction side) {
    }

    private record PlaceData(BlockTarget target, RaytracedRotation rotations) {
    }

    @Override
    public void renderIsland(final DrawContext context, final float posX, final float posY, final float width, final float height, final float progress) {
        this.dynamicIsland.render(context, posX, posY);
    }

    @Override
    public float getIslandWidth() {
        return this.dynamicIsland.getWidth();
    }

    @Override
    public float getIslandHeight() {
        return this.dynamicIsland.getHeight();
    }

    @Override
    public int getIslandPriority() {
        return 1;
    }
}
