package wtf.opal.client.feature.module.impl.combat.killaura.impl;

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
import wtf.opal.client.feature.module.impl.combat.BlockModule;
import wtf.opal.client.feature.module.impl.combat.killaura.KillAuraMode;
import wtf.opal.client.feature.module.impl.combat.killaura.KillAuraModule;
import wtf.opal.client.feature.module.impl.combat.killaura.target.CurrentTarget;
import wtf.opal.client.feature.module.impl.combat.velocity.VelocityModule;
import wtf.opal.client.feature.module.impl.combat.velocity.impl.WatchdogVelocity;
import wtf.opal.client.feature.module.impl.world.breaker.BreakerModule;
import wtf.opal.client.feature.module.impl.world.scaffold.ScaffoldModule;
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

public class HypixelKillAura extends KillAuraMode {

    private int attacks;
    private EntityHitResult hitResult;

    public HypixelKillAura(KillAuraModule module) {
        super(module);
    }

    @Override
    public Enum<?> getEnumValue() {
        return KillAuraModule.Mode.HYPIXEL;
    }

    @Subscribe
    public void onHandleInput(final MouseHandleInputEvent event) {
        final CurrentTarget target = this.module.getTargeting().getTarget();
        if (target == null || mc.crosshairTarget == null || mc.crosshairTarget.getType() == HitResult.Type.MISS) {
            if (!this.module.getSettings().getCpsProperty().isModernDelay()) {
                final double closestDistance = this.module.getTargeting().getClosestDistance();
                if (closestDistance <= this.module.getSettings().getSwingRange() && SwingDelay.isSwingAvailable(this.module.getSettings().getSwingCpsProperty()) && PlayerUtility.getBlockOver() == null) {
                    final MouseButton leftButton = MouseHelper.getLeftButton();
                    leftButton.setPressed(true, RandomUtility.getRandomInt(2));
                    if (this.module.getSettings().isHideFakeSwings() && mc.crosshairTarget.getType() != HitResult.Type.ENTITY) {
                        leftButton.setShowSwings(false);
                    }
                    this.module.getSettings().getSwingCpsProperty().resetClick();
                }
            }
            return;
        }

        final BlockModule blockModule = OpalClient.getInstance().getModuleRepository().getModule(BlockModule.class);
        final boolean allowSwingWhenUsing = blockModule.isEnabled() && blockModule.isSwingAllowed();
        if (mc.player.isUsingItem() && !allowSwingWhenUsing) {
            return;
        }

        if (this.module.getSettings().isOverrideRaycast()) {
            if (this.module.getSettings().isTickLookahead() && (this.hitResult == null || this.hitResult.getEntity() != target.getEntity())) {
                return;
            }
            mc.crosshairTarget = target.getRotations().hitResult();
        }

        if (mc.crosshairTarget.getType() == HitResult.Type.ENTITY) {
            if (this.isAttackSwingAvailable(target)) {
                final EntityHitResult hitResult = (EntityHitResult) mc.crosshairTarget;
                if (hitResult.getEntity() == target.getEntity()) {
                    MouseHelper.getLeftButton().setPressed();
                    target.getKillAuraTarget().onAttack(this.attacks == 0);

                    this.module.getSettings().getCpsProperty().resetClick();
                    SwingDelay.reset();
                    if (this.attacks > 0) {
                        this.attacks--;
                    } else {
                        this.attacks = 2;
                    }
                }
            } else {
                this.attacks = 0;
            }
        }
    }

    private boolean isAttackSwingAvailable(final CurrentTarget target) {
        final VelocityModule velocityModule = OpalClient.getInstance().getModuleRepository().getModule(VelocityModule.class);
        if (target.getKillAuraTarget().isAttackAvailable() || this.attacks > 0 ||
                velocityModule.isEnabled() && velocityModule.getActiveMode() instanceof WatchdogVelocity watchdogVelocity && watchdogVelocity.isSprintReset()) {
            return true;
        }
        return SwingDelay.isSwingAvailable(this.module.getSettings().getCpsProperty(), false);
    }

    @Subscribe
    public void onRenderWorld(final RenderWorldEvent event) {
        if (!this.module.getTargeting().isTargetSelected() || this.module.getTargeting().getTarget() == null || !this.module.getSettings().getVisuals().getProperty("Box").getValue()) {
            return;
        }

        final LivingEntity target = this.module.getTargeting().getTarget().getEntity();

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
        if (!shouldRun()) {
            this.module.getTargeting().reset();
            return;
        }

        this.module.getTargeting().update();

        final CurrentTarget target = this.module.getTargeting().getRotationTarget();
        if (target == null) {
            return;
        }

        RotationHelper.getHandler().rotate(
                target.getRotations().rotation(),
                this.module.getSettings().createRotationModel()
        );
    }

    @Subscribe
    public void onPreMovementPacket(final PreMovementPacketEvent event) {
        if (!this.module.getSettings().isTickLookahead() || this.module.getTargeting().getRotationTarget() == null || !shouldRun()) {
            return;
        }

        this.module.getTargeting().update();

        final CurrentTarget target = this.module.getTargeting().getRotationTarget();
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
        if (!this.module.getSettings().isTickLookahead()) {
            return;
        }
        final CurrentTarget target = this.module.getTargeting().getTarget();
        Predicate<Entity> entityPredicate = target == null ? Predicates.alwaysTrue() : e -> e == target.getEntity();
        this.hitResult = RaycastUtility.raycastEntity(mc.player.getEntityInteractionRange(), 1.0F, mc.player.getYaw(), mc.player.getPitch(), entityPredicate);
    }

    private boolean shouldRun() {
        if (mc.player == null) {
            return false;
        }

        if (this.module.getSettings().isRequireAttackKey() && !mc.options.attackKey.isPressed()) {
            return false;
        }

        final ItemStack heldItem = SlotHelper.getInstance().getMainHandStack(mc.player);
        if (this.module.getSettings().isRequireWeapon() &&
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

    @Override
    public void onDisable() {
        this.module.getTargeting().reset();
        this.hitResult = null;
        this.attacks = 0;
        super.onDisable();
    }
}
