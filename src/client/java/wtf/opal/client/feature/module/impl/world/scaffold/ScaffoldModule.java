package wtf.opal.client.feature.module.impl.world.scaffold;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.OtherClientPlayerEntity;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.BufferAllocator;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.network.packet.s2c.play.ItemPickupAnimationS2CPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.*;
import net.minecraft.util.shape.VoxelShape;
import wtf.opal.client.OpalClient;
import wtf.opal.client.feature.helper.impl.LocalDataWatch;
import wtf.opal.client.feature.helper.impl.player.mouse.MouseButton;
import wtf.opal.client.feature.helper.impl.player.mouse.MouseHelper;
import wtf.opal.client.feature.helper.impl.player.rotation.RotationHelper;
import wtf.opal.client.feature.helper.impl.player.rotation.handler.RotationMouseHandler;
import wtf.opal.client.feature.helper.impl.player.rotation.model.IRotationModel;
import wtf.opal.client.feature.helper.impl.player.rotation.model.impl.HypixelRotationModel;
import wtf.opal.client.feature.helper.impl.player.rotation.model.impl.InstantRotationModel;
import wtf.opal.client.feature.helper.impl.player.slot.SlotHelper;
import wtf.opal.client.feature.helper.impl.player.swing.SwingDelay;
import wtf.opal.client.feature.helper.impl.render.FadingBlockHelper;
import wtf.opal.client.feature.helper.impl.server.impl.HypixelServer;
import wtf.opal.client.feature.module.Module;
import wtf.opal.client.feature.module.ModuleCategory;
import wtf.opal.client.feature.module.impl.movement.flight.FlightModule;
import wtf.opal.client.feature.module.impl.movement.longjump.LongJumpModule;
import wtf.opal.client.feature.module.impl.visual.overlay.impl.dynamicisland.IslandTrigger;
import wtf.opal.client.feature.module.repository.ModuleRepository;
import wtf.opal.client.feature.simulation.PlayerSimulation;
import wtf.opal.client.renderer.world.WorldRenderer;
import wtf.opal.event.impl.game.PreGameTickEvent;
import wtf.opal.event.impl.game.input.MouseHandleInputEvent;
import wtf.opal.event.impl.game.input.MoveInputEvent;
import wtf.opal.event.impl.game.packet.ReceivePacketEvent;
import wtf.opal.event.impl.game.player.interaction.block.BlockPlacedEvent;
import wtf.opal.event.impl.render.RenderWorldEvent;
import wtf.opal.event.subscriber.Subscribe;
import wtf.opal.mixin.LivingEntityAccessor;
import wtf.opal.utility.misc.chat.ChatUtility;
import wtf.opal.utility.misc.math.RandomUtility;
import wtf.opal.utility.player.*;
import wtf.opal.utility.render.ColorUtility;
import wtf.opal.utility.render.CustomRenderLayers;

import java.awt.*;
import java.util.*;
import java.util.List;

import static wtf.opal.client.Constants.mc;

public final class ScaffoldModule extends Module implements IslandTrigger {

    private final ScaffoldIsland dynamicIsland = new ScaffoldIsland(this);
    private final ScaffoldSettings settings = new ScaffoldSettings(this);

    public BlockData blockCache;
    private int sameYPos;

    private Vec3d preExpandPos;
    private RaytracedRotation rotation;

    private Map<Integer, Integer> realStackSizeMap;

    public ScaffoldModule() {
        super("Scaffold", "Automatically places blocks under you.", ModuleCategory.WORLD);
    }

    @Override
    protected void onDisable() {
        this.dynamicIsland.onDisable();
        this.realStackSizeMap = null;
        this.intelligentRotation = null;
        this.placeTick = 0;

        super.onDisable();
    }

    @Override
    protected void onEnable() {
        super.onEnable();

        blockCache = null;
        rotation = null;

        this.realStackSizeMap = new HashMap<>();

        if (mc.player == null) return;
        sameYPos = MathHelper.floor(mc.player.getY());
    }

    @Subscribe
    public void onBlockPlaced(BlockPlacedEvent event) {
        if (!mc.interactionManager.getCurrentGameMode().isCreative()) {
            int selectedSlot = mc.player.getInventory().getSelectedSlot();
            this.realStackSizeMap.put(selectedSlot, this.realStackSizeMap.getOrDefault(selectedSlot, mc.player.getMainHandStack().getCount() + 1) - 1);
        }
    }

    @Subscribe
    public void onRenderWorld(final RenderWorldEvent event) {
        if (mc.crosshairTarget instanceof BlockHitResult blockHitResult && rotation != null && settings.isBlockOverlayEnabled() && !mc.world.getBlockState(blockHitResult.getBlockPos()).isAir()) {
            final Vec3d startVec = new Vec3d(blockHitResult.getBlockPos().getX(), blockHitResult.getBlockPos().getY(), blockHitResult.getBlockPos().getZ());
            final Vec3d dimensions = new Vec3d(1, 1, 1);

            VertexConsumerProvider.Immediate vcp = VertexConsumerProvider.immediate(new BufferAllocator(1024));
            WorldRenderer rc = new WorldRenderer(vcp);

            rc.drawFilledCube(event.matrixStack(), CustomRenderLayers.getPositionColorQuads(true), startVec, dimensions, ColorUtility.applyOpacity(ColorUtility.getClientTheme().first, 0.25F));

            vcp.draw();
        }
    }

    @Subscribe(priority = 1)
    public void onMoveInput(final MoveInputEvent event) {
        if (this.settings.isSameYEnabled() && this.settings.isAutoJump() && mc.player.isOnGround() && LocalDataWatch.get().groundTicks > 0) {
            final PlayerSimulation simulation = new PlayerSimulation(mc.player);
            final OtherClientPlayerEntity entity = simulation.getSimulatedEntity();
            boolean flag = false;
            for (int i = 0; i < 2; i++) {
                simulation.simulateTick();
                if(!entity.isOnGround()) {
                    flag = true;
                    break;
                }
            }
            if(flag) {
                ((LivingEntityAccessor) mc.player).setJumpingCooldown(0);
                event.setJump(true);
            }
        }
    }

    @Subscribe(priority = 1)
    public void onHandleInput(final MouseHandleInputEvent event) {
        final boolean isBlock = mc.player.getMainHandStack().getItem() instanceof BlockItem || mc.player.getOffHandStack().getItem() instanceof BlockItem;
        if (this.blockCache != null) {
            if (rotation != null && settings.isOverrideRaycast()) {
                mc.crosshairTarget = this.rotation.hitResult();
            }

            final Block blockOver = PlayerUtility.getBlockOver();

            if (!InventoryUtility.isBlockInteractable(blockOver) && isBlock) {
                final MouseButton rightButton = MouseHelper.getRightButton();
                rightButton.setPressed();
                if (this.settings.getSwingMode().getValue() == ScaffoldSettings.SwingMode.SERVER) {
                    rightButton.setShowSwings(false);
                }

                this.placeTick = mc.player.age;

                if (settings.isBlockOverlayEnabled()) {
                    FadingBlockHelper.getInstance().addFadingBlock(
                            new FadingBlockHelper.FadingBlock(
                                    blockCache.blockWithDirection.blockPos,
                                    Color.BITMASK,
                                    ColorUtility.applyOpacity(ColorUtility.getClientTheme().first, 0.25F),
                                    300
                            )
                    );
                }
            }
        } else {
            if (!isBlock || !this.simulateClick()) {
                MouseHelper.getRightButton().setDisabled();
            }
        }

        if (preExpandPos != null) {
            mc.player.setPos(preExpandPos.x, preExpandPos.y, preExpandPos.z);
            preExpandPos = null;
        }
    }

    private boolean simulateClick() {
        if (!SwingDelay.isSwingAvailable(this.settings.getSimulationCps(), false)) {
            return false;
        }
        if (mc.crosshairTarget != null) {
            if (mc.crosshairTarget instanceof BlockHitResult blockHitResult) {
//                if(!this.settings.isOverrideRaycast() && (this.blockCache.blockWithDirection.blockPos() != blockHitResult.getBlockPos() || this.blockCache.blockWithDirection.direction() != blockHitResult.getSide())) {
//                    return false;
//                }
                final BlockPos blockPos = blockHitResult.getBlockPos();
                final Block block = mc.world.getBlockState(blockPos).getBlock();
                if (InventoryUtility.isBlockInteractable(block)) {
                    return false;
                }
                final Hand hand;
                final BlockItem blockItem;
                if (mc.player.getMainHandStack().getItem() instanceof BlockItem item) {
                    hand = Hand.MAIN_HAND;
                    blockItem = item;
                } else if (mc.player.getOffHandStack().getItem() instanceof BlockItem item) {
                    hand = Hand.OFF_HAND;
                    blockItem = item;
                } else {
                    ChatUtility.debug("???");
                    return false;
                }
                final ItemUsageContext itemUsageContext = new ItemUsageContext(mc.player, hand, blockHitResult);
                final ItemPlacementContext placementContext = blockItem.getPlacementContext(new ItemPlacementContext(itemUsageContext));
                if (placementContext == null) {
                    return false;
                }
                final BlockPos offsetPos = blockPos.offset(blockHitResult.getSide());
                final BlockState placementState = block.getPlacementState(placementContext);
                final Block heldBlock = blockItem.getBlock();
                final VoxelShape collisionShape = heldBlock.getDefaultState().getCollisionShape(mc.world, offsetPos);
                if (collisionShape.isEmpty()) {
                    return false;
                }
                final Box blockBox = collisionShape.getBoundingBox().offset(offsetPos);
                if (placementState == null || placementState.canPlaceAt(mc.world, blockPos) && !mc.player.getBoundingBox().intersects(blockBox)) {
                    return false;
                }



                MouseHelper.getRightButton().setPressed();
                this.settings.getSimulationCps().resetClick();
                SwingDelay.reset();
                return true;
            }
        }
        return false;
    }

    private Vec2f intelligentRotation;

    @Subscribe(priority = 1)
    public void onPreGameTick(final PreGameTickEvent event) {
        if (mc.player == null) {
            blockCache = null;
            return;
        }
        if (this.settings.isSameYEnabled() && this.settings.isAutoJump() && LocalDataWatch.get().getKnownServerManager().getCurrentServer() instanceof HypixelServer && (mc.player.isOnGround())) {
            RotationMouseHandler handler = RotationHelper.getHandler();
            if(mc.player != null) {
                if(rotation != null)
                handler.rotate(new Vec2f(mc.gameRenderer.getCamera().getYaw() + (44f * Math.signum(rotation.rotation().x)), mc.gameRenderer.getCamera().getPitch()), InstantRotationModel.INSTANCE);
            }
            this.rotation = null;
            return;
        }
            // Expand
        Vec3d expandOffset = null;
//        if (LocalDataWatch.get().getKnownServerManager().getCurrentServer() instanceof HypixelServer) {
//            expandOffset = mc.player.getVelocity().withAxis(Direction.Axis.Y, 0.0D);
//        }

        if (expandOffset != null) {
            preExpandPos = mc.player.getEntityPos();

            mc.player.setPos(mc.player.getX() + expandOffset.getX(), mc.player.getY() + expandOffset.getY(), mc.player.getZ() + expandOffset.getZ());
        }

        final int slot = getPlaceableBlock();
        if (slot == -1) {
            if (!(mc.player.getOffHandStack().getItem() instanceof BlockItem blockItem && InventoryUtility.isGoodBlock(blockItem.getBlock()))) {
                return;
            }
        }
        final SlotHelper.Silence silence;
        switch (settings.getSwitchMode().getValue()) {
            case NORMAL -> silence = SlotHelper.Silence.NONE;
            case FULL -> silence = SlotHelper.Silence.FULL;
            default -> silence = SlotHelper.Silence.DEFAULT;
        }
        SlotHelper.setCurrentItem(slot).silence(silence);

        final ModuleRepository moduleRepository = OpalClient.getInstance().getModuleRepository();
        final boolean updateY = !settings.isSameYEnabled()
              //  || mc.options.useKey.isPressed()
                || (this.settings.isAutoJump() && PlayerUtility.isKeyPressed(mc.options.jumpKey))
                || mc.player.isOnGround()
                || Math.abs(Math.floor(mc.player.getY() - sameYPos)) > 3
                || moduleRepository.getModule(LongJumpModule.class).isEnabled()
                || moduleRepository.getModule(FlightModule.class).isEnabled();

        if (updateY) {
            sameYPos = MathHelper.floor(mc.player.getY());
        }

        this.intelligentRotation = null;

        final boolean watchdog = this.getSettings().getMode().is(ScaffoldSettings.Mode.WATCHDOG);
        if (!watchdog || !mc.player.input.playerInput.jump() ||
                !mc.player.isOnGround() && (mc.player.getVelocity().getY() >= 0.0D || PlayerUtility.isBoxEmpty(mc.player.getBoundingBox().offset(0.0D, mc.player.getVelocity().getY(), 0.0D)))) {
            this.updateMovementIntelligence();
            updateData();
            this.updateMovementIntelligence();
            // TODO: when for sneak
//            if ((mc.player.input.playerInput.sneak()) &&
//                    (int) (mc.player.getY() + mc.player.getVelocity().getY()) == (int) mc.player.getY() && (mc.player.getVelocity().getY() >= 0.2D || !updateY)) { // telly check bypass, doesn't run jumping else you fall off lol
//                this.blockCache = null;
//            }
        } else {
            this.blockCache = null;
            this.rotation = null;
        }

        if (rotation != null) {
            if (!settings.isSnapRotationsEnabled() || blockCache != null) {
                final IRotationModel model = (LocalDataWatch.get().getKnownServerManager().getCurrentServer() instanceof HypixelServer && this.settings.getMode().is(ScaffoldSettings.Mode.WATCHDOG)) ? new HypixelRotationModel() : settings.createRotationModel();
                RotationHelper.getHandler().rotate(
                        rotation.rotation(),
                        model
                );
            }
        }
    }

    private boolean isYawDiagonal() {
        final float direction = Math.abs(MoveUtility.getDirectionDegrees() % 90);
        final int range = 30;
        return direction > 45 - range && direction < 45 + range;
    }

    private int placeTick;

    private void updateMovementIntelligence() {
        if (this.settings.isMovementIntelligence()) {
            if (!this.settings.getMode().is(ScaffoldSettings.Mode.WATCHDOG) || mc.player.isOnGround() ||
                    !PlayerUtility.isBoxEmpty(mc.player.getBoundingBox().offset(0.0D, mc.player.getVelocity().getY(), 0.0D))) {
                final Vec2f currentRotation = rotation != null ? rotation.rotation() : RotationUtility.getRotation();
                this.intelligentRotation = RotationUtility.getPriorityAngle(currentRotation, this.settings.getMovementIntelligenceSteps(), this.settings.isMovementSnapping(), this.settings.isDiagonalMovement());
            }
        }
    }

    @Subscribe
    public void onReceivePacket(final ReceivePacketEvent event) {
        if (event.getPacket() instanceof ItemPickupAnimationS2CPacket pickup
                && mc.player != null
                && pickup.getCollectorEntityId() == mc.player.getId()) {
            int selectedSlot = mc.player.getInventory().getSelectedSlot();
            this.realStackSizeMap.put(
                    selectedSlot,
                    this.realStackSizeMap.getOrDefault(selectedSlot, mc.player.getMainHandStack().getCount() - pickup.getStackAmount()) + pickup.getStackAmount()
            );
        }
    }

    private int getPlaceableBlock() {
        for (int i = 0; i < 9; i++) {
            final ItemStack itemStack = mc.player.getInventory().getMainStacks().get(i);
            if (itemStack.getItem() instanceof BlockItem blockItem
                    && this.realStackSizeMap.getOrDefault(i, itemStack.getCount()) > 0 &&
                    InventoryUtility.isGoodBlock(blockItem.getBlock())) {
                return i;
            }
        }
        return -1;
    }

    private boolean updateData() {
        blockCache = getBlockData();

        if (blockCache != null) {
            this.rotation = blockCache.rotation;
            return true;
        }

        final PlayerSimulation simulation = new PlayerSimulation(mc.player);
        final OtherClientPlayerEntity entity = simulation.getSimulatedEntity();
        for (int i = 0; i < 10; i++) {
            simulation.simulateTick();
            final BlockData simulatedData = getBlockData(entity.getBlockPos().down(), entity);
            if (simulatedData != null) {
                rotation = simulatedData.rotation;
                break;
            }
        }

        return blockCache != null;
    }

    private RaytracedRotation getRotation(BlockWithDirection data, Vec3d start) {
        final Vec2f sortingAngle;
        if (this.intelligentRotation != null) {
            sortingAngle = this.intelligentRotation;
        } else {
            sortingAngle = rotation != null ? rotation.rotation() : RotationUtility.getRotation();
        }
        return RotationUtility.getRotationFromRaycastedBlock(data.blockPos, data.direction, sortingAngle, start);
    }

    private BlockData getBlockData() {
        return getBlockData(mc.player.getBlockPos().withY(sameYPos).down(), mc.player);
    }

    private BlockData getBlockData(final BlockPos targetBlockPos, final PlayerEntity entity) {
        if (mc.world.getBlockState(targetBlockPos).isReplaceable()) {
            final BlockPos.Mutable blockPos = new BlockPos.Mutable();
            final List<BlockWithDirection> blockList = new ArrayList<>();

            int range = 2;
            for (int y = 0; y > -range; y--) {
                for (int x = 0; x < range; x++) {
                    for (int z = 0; z < range; z++) {
                        for (int sign : new int[]{1, -1}) {
                            blockPos.set(targetBlockPos.getX() + (x * sign), targetBlockPos.getY() + (y * sign), targetBlockPos.getZ() + (z * sign));
                            if (!mc.world.getBlockState(blockPos).isReplaceable()) continue;

                            for (Direction direction : Direction.values()) {
                                final BlockPos block = blockPos.offset(direction);

                                if (!mc.world.getBlockState(block).isReplaceable()) {
                                    blockList.add(new BlockWithDirection(block, direction.getOpposite()));
                                }
                            }
                        }
                    }
                }
            }

            if (blockList.isEmpty()) {
                return null;
            }

            blockList.sort(Comparator.comparingDouble(data -> data.blockPos.offset(data.direction).getSquaredDistance(targetBlockPos)));

            final Vec3d eyePos = entity.getEntityPos().add(0.0D, entity.getStandingEyeHeight(), 0.0D);
            for (final BlockWithDirection block : blockList) {
                final RaytracedRotation rotation = this.getRotation(block, eyePos);
                if (rotation != null) {
                    return new BlockData(block, rotation);
                }
            }
        }
        return null;
    }

    public record BlockWithDirection(BlockPos blockPos, Direction direction) {
    }

    public record BlockData(BlockWithDirection blockWithDirection, RaytracedRotation rotation) {
    }

    @Override
    public void renderIsland(DrawContext context, float posX, float posY, float width, float height, float progress) {
        this.dynamicIsland.render(context, posX, posY);
    }

    public ScaffoldSettings getSettings() {
        return settings;
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
