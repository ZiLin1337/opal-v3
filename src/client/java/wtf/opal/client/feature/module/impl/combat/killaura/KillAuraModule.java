package wtf.opal.client.feature.module.impl.combat.killaura;

import com.google.common.base.Predicates;
import net.hypixel.data.type.GameType;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.BufferAllocator;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Vec3d;
import wtf.opal.client.OpalClient;
import wtf.opal.client.feature.helper.impl.LocalDataWatch;
import wtf.opal.client.feature.helper.impl.player.mouse.MouseButton;
import wtf.opal.client.feature.helper.impl.player.mouse.MouseHelper;
import wtf.opal.client.feature.helper.impl.player.rotation.RotationHelper;
import wtf.opal.client.feature.helper.impl.player.slot.SlotHelper;
import wtf.opal.client.feature.helper.impl.player.swing.SwingDelay;
import wtf.opal.client.feature.helper.impl.server.impl.HypixelServer;
import wtf.opal.client.feature.module.Module;
import wtf.opal.client.feature.module.ModuleCategory;
import wtf.opal.client.feature.module.impl.combat.BlockModule;
import wtf.opal.client.feature.module.impl.combat.killaura.target.CurrentTarget;
import wtf.opal.client.feature.module.impl.combat.killaura.target.KillAuraTargeting;
import wtf.opal.client.feature.module.impl.combat.velocity.VelocityModule;
import wtf.opal.client.feature.module.impl.combat.velocity.impl.WatchdogVelocity;
import wtf.opal.client.feature.module.impl.world.breaker.BreakerModule;
import wtf.opal.client.feature.module.impl.world.scaffold.ScaffoldModule;
import wtf.opal.client.feature.module.property.impl.mode.ModeProperty;
import wtf.opal.client.feature.module.property.impl.number.NumberProperty;
import wtf.opal.client.renderer.world.WorldRenderer;
import wtf.opal.event.impl.game.PreGameTickEvent;
import wtf.opal.event.impl.game.input.MouseHandleInputEvent;
import wtf.opal.event.impl.game.player.movement.PostMovementPacketEvent;
import wtf.opal.event.impl.game.player.movement.PreMovementPacketEvent;
import wtf.opal.event.impl.render.RenderWorldEvent;
import wtf.opal.event.subscriber.Subscribe;
import wtf.opal.utility.misc.math.MathUtility;
import wtf.opal.utility.misc.math.RandomUtility;
import wtf.opal.utility.player.PlayerUtility;
import wtf.opal.utility.player.RaycastUtility;
import wtf.opal.utility.render.ColorUtility;
import wtf.opal.utility.render.CustomRenderLayers;

import java.util.function.Predicate;

import static wtf.opal.client.Constants.mc;

public final class KillAuraModule extends Module {

    // 1. 定义 Mode 属性
    private final ModeProperty<Mode> mode = new ModeProperty<>("Mode", this, Mode.HYPIXEL);

    // 2. 初始化 Settings (注意：Settings 内部不再自动注册属性)
    private final KillAuraSettings settings = new KillAuraSettings(this);
    private final KillAuraTargeting targeting = new KillAuraTargeting(this.settings);

    // 3. Heypixel 模式的属性
    private final NumberProperty heypixelAimRange = new NumberProperty("Aim Range", 5.0, 1.0, 6.0, 0.1)
            .id("heypixelAimRange")
            .hideIf(() -> this.mode.getValue() != Mode.HEYPIXEL);

    private final NumberProperty heypixelCps = new NumberProperty("CPS", 10.0, 1.0, 20.0, 1.0)
            .id("heypixelCps")
            .hideIf(() -> this.mode.getValue() != Mode.HEYPIXEL);

    private final NumberProperty heypixelRotationSpeed = new NumberProperty("Rotation Speed", 180.0, 1.0, 180.0, 1.0)
            .id("heypixelRotationSpeed")
            .hideIf(() -> this.mode.getValue() != Mode.HEYPIXEL);

    // Heypixel 运行时变量
    private Entity heypixelTarget;
    private long heypixelLastAttackTime = 0;
    private int hypixelAttacks;
    private EntityHitResult hypixelHitResult;

    public KillAuraModule() {
        super(
                "KillAura",
                "Finds and attacks the most relevant nearby entities.",
                ModuleCategory.COMBAT
        );

        // 核心修复：手动控制添加顺序

        // 第一步：添加 Mode，确保它永远在 GUI 最顶端
        addProperties(mode);

        // 第二步：添加 Heypixel 属性
        addProperties(heypixelAimRange, heypixelCps, heypixelRotationSpeed);

        // 第三步：调用 Settings 的注册方法添加 Hypixel 属性
        settings.registerProperties();
    }

    public KillAuraSettings getSettings() {
        return settings;
    }

    public ModeProperty<Mode> getMode() {
        return mode;
    }

    @Override
    public String getSuffix() {
        return mode.getValue().toString();
    }

    public KillAuraTargeting getTargeting() {
        return targeting;
    }

    @Override
    protected void onDisable() {
        this.targeting.reset();
        heypixelTarget = null;
        heypixelLastAttackTime = 0;
        hypixelHitResult = null;
        hypixelAttacks = 0;
        super.onDisable();
    }

    @Subscribe
    public void onHandleInput(final MouseHandleInputEvent event) {
        if (mode.getValue() == Mode.HEYPIXEL) {
            return; // Heypixel 模式在 PreGameTick 中处理攻击
        }

        final CurrentTarget target = this.targeting.getTarget();
        if (target == null || mc.crosshairTarget == null || mc.crosshairTarget.getType() == HitResult.Type.MISS) {
            if (!this.settings.getCpsProperty().isModernDelay()) {
                final double closestDistance = this.targeting.getClosestDistance();
                if (closestDistance <= this.settings.getSwingRange() && SwingDelay.isSwingAvailable(this.settings.getSwingCpsProperty()) && PlayerUtility.getBlockOver() == null) {
                    final MouseButton leftButton = MouseHelper.getLeftButton();
                    leftButton.setPressed(true, RandomUtility.getRandomInt(2));
                    if (this.settings.isHideFakeSwings() && mc.crosshairTarget.getType() != HitResult.Type.ENTITY) {
                        leftButton.setShowSwings(false);
                    }
                    this.settings.getSwingCpsProperty().resetClick();
                }
            }
            return;
        }

        final BlockModule blockModule = OpalClient.getInstance().getModuleRepository().getModule(BlockModule.class);
        final boolean allowSwingWhenUsing = blockModule.isEnabled() && blockModule.isSwingAllowed();
        if (mc.player.isUsingItem() && !allowSwingWhenUsing) {
            return;
        }

        if (this.settings.isOverrideRaycast()) {
            if (this.settings.isTickLookahead() && (this.hypixelHitResult == null || this.hypixelHitResult.getEntity() != target.getEntity())) {
                return;
            }
            mc.crosshairTarget = target.getRotations().hitResult();
        }

        if (mc.crosshairTarget.getType() == HitResult.Type.ENTITY) {
            if (this.isAttackSwingAvailable(target)) {
                final EntityHitResult hitResult = (EntityHitResult) mc.crosshairTarget;
                if (hitResult.getEntity() == target.getEntity()) {
                    MouseHelper.getLeftButton().setPressed();
                    target.getKillAuraTarget().onAttack(this.hypixelAttacks == 0);

                    this.settings.getCpsProperty().resetClick();
                    SwingDelay.reset();
                    if (this.hypixelAttacks > 0) {
                        this.hypixelAttacks--;
                    } else {
                        this.hypixelAttacks = 2;
                    }
                }
            } else {
                this.hypixelAttacks = 0;
            }
        }
    }

    private boolean isAttackSwingAvailable(final CurrentTarget target) {
        final VelocityModule velocityModule = OpalClient.getInstance().getModuleRepository().getModule(VelocityModule.class);
        if (target.getKillAuraTarget().isAttackAvailable() || this.hypixelAttacks > 0 ||
                velocityModule.isEnabled() && velocityModule.getActiveMode() instanceof WatchdogVelocity watchdogVelocity && watchdogVelocity.isSprintReset()) {
            return true;
        }
        return SwingDelay.isSwingAvailable(this.settings.getCpsProperty(), false);
    }

    @Subscribe
    public void onRenderWorld(final RenderWorldEvent event) {
        if (mode.getValue() == Mode.HEYPIXEL) {
            renderHeypixelWorld(event);
        } else {
            renderHypixelWorld(event);
        }
    }

    private void renderHeypixelWorld(final RenderWorldEvent event) {
        // Heypixel specific rendering (TargetESP, etc.)
    }

    private void renderHypixelWorld(final RenderWorldEvent event) {
        if (!this.targeting.isTargetSelected() || this.targeting.getTarget() == null || !this.settings.getVisuals().getProperty("Box").getValue()) {
            return;
        }

        final LivingEntity target = this.targeting.getTarget().getEntity();

        final Vec3d position = MathUtility.interpolate(target, event.tickDelta()).add(mc.gameRenderer.getCamera().getPos()).subtract(0.25, 0, 0.25);
        final Vec3d dimensions = new Vec3d(target.getWidth(), target.getHeight(), target.getWidth());

        VertexConsumerProvider.Immediate vcp = VertexConsumerProvider.immediate(new BufferAllocator(1024));
        WorldRenderer rc = new WorldRenderer(vcp);

        rc.drawFilledCube(
                event.matrixStack(),
                CustomRenderLayers.getPositionColorQuads(true),
                position, dimensions,
                ColorUtility.applyOpacity(ColorUtility.getClientTheme().first, 0.25F)
        );

        vcp.draw();
    }

    @Subscribe(priority = 2)
    public void onPreGameTick(final PreGameTickEvent event) {
        if (mode.getValue() == Mode.HEYPIXEL) {
            handleHeypixelTick();
        } else {
            handleHypixelTick();
        }
    }

    private void handleHeypixelTick() {
        findHeypixelTarget();
        if (heypixelTarget != null) {
            heypixelRotateToTarget();
            attackHeypixelTarget();
        }
    }

    private void handleHypixelTick() {
        if (!shouldRunHypixel()) {
            this.targeting.reset();
            return;
        }

        this.targeting.update();

        final CurrentTarget target = this.targeting.getRotationTarget();
        if (target == null) {
            return;
        }

        RotationHelper.getHandler().rotate(
                target.getRotations().rotation(),
                this.settings.createRotationModel()
        );
    }

    @Subscribe
    public void onPreMovementPacket(final PreMovementPacketEvent event) {
        if (mode.getValue() == Mode.HEYPIXEL || !this.settings.isTickLookahead() || this.targeting.getRotationTarget() == null || !shouldRunHypixel()) {
            return;
        }

        this.targeting.update();

        final CurrentTarget target = this.targeting.getRotationTarget();
        if (target == null) {
            return;
        }

        final BreakerModule breakerModule = OpalClient.getInstance().getModuleRepository().getModule(BreakerModule.class);
        if (breakerModule.isEnabled() && breakerModule.isBreaking()) {
            return;
        }

        event.setYaw(mc.player.getYaw());
        event.setPitch(mc.player.getPitch());
    }

    @Subscribe
    public void onPostMovementPacket(final PostMovementPacketEvent event) {
        if (mode.getValue() != Mode.HYPIXEL || !this.settings.isTickLookahead()) {
            return;
        }
        final CurrentTarget target = this.targeting.getTarget();
        Predicate<Entity> entityPredicate = target == null ? Predicates.alwaysTrue() : e -> e == target.getEntity();
        this.hypixelHitResult = RaycastUtility.raycastEntity(mc.player.getEntityInteractionRange(), 1.0F, mc.player.getYaw(), mc.player.getPitch(), entityPredicate);
    }

    private boolean shouldRunHypixel() {
        if (mc.player == null) {
            return false;
        }

        if (this.settings.isRequireAttackKey() && !mc.options.attackKey.isPressed()) {
            return false;
        }

        final ItemStack heldItem = SlotHelper.getInstance().getMainHandStack(mc.player);
        if (this.settings.isRequireWeapon() &&
                !(heldItem.isIn(ItemTags.SWORDS) || heldItem.isIn(ItemTags.AXES) || heldItem.isIn(ItemTags.PICKAXES))) {
            return false;
        }

        if (OpalClient.getInstance().getModuleRepository().getModule(ScaffoldModule.class).isEnabled()) {
            return false;
        }

        if (LocalDataWatch.get().getKnownServerManager().getCurrentServer() instanceof HypixelServer) {
            final HypixelServer.ModAPI.Location currentLocation = HypixelServer.ModAPI.get().getCurrentLocation();
            return currentLocation == null || (!currentLocation.isLobby() && currentLocation.serverType() != GameType.REPLAY);
        }

        return true;
    }

    private void findHeypixelTarget() {
        final double range = heypixelAimRange.getValue();
        final CurrentTarget currentTarget = this.targeting.getRotationTarget();

        if (currentTarget != null) {
            final LivingEntity entity = currentTarget.getEntity();
            if (entity != null && !entity.isDead() && entity.isAttackable() && mc.player.distanceTo(entity) <= range) {
                heypixelTarget = entity;
                return;
            }
        }

        this.targeting.update();
        final CurrentTarget target = this.targeting.getRotationTarget();
        if (target != null && target.getEntity() != null && mc.player.distanceTo(target.getEntity()) <= range) {
            heypixelTarget = target.getEntity();
        } else {
            heypixelTarget = null;
        }
    }

    private void attackHeypixelTarget() {
        if (heypixelTarget == null) {
            return;
        }

        final long currentTime = System.currentTimeMillis();
        final double cps = heypixelCps.getValue();
        long delay = (long) (1000.0 / cps);

        // Add random variation
        delay += (long) ((Math.random() - 0.5) * delay * 0.4);

        if (currentTime - heypixelLastAttackTime >= delay) {
            mc.interactionManager.attackEntity(mc.player, heypixelTarget);
            mc.player.swingHand(mc.player.getActiveHand());
            heypixelLastAttackTime = currentTime;
        }
    }

    private void heypixelRotateToTarget() {
        if (heypixelTarget == null) {
            return;
        }

        this.targeting.update();
        final CurrentTarget target = this.targeting.getRotationTarget();
        if (target != null && target.getEntity() == heypixelTarget) {
            RotationHelper.getHandler().rotate(
                    target.getRotations().rotation(),
                    this.settings.createRotationModel()
            );
        }
    }

    public enum Mode {
        HYPIXEL("Hypixel"),
        HEYPIXEL("Heypixel");

        private final String name;

        Mode(String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            return name;
        }
    }
}