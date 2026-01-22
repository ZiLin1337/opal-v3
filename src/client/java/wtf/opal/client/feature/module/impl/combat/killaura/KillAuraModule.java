package wtf.opal.client.feature.module.impl.combat.killaura;

import net.hypixel.data.type.GameType;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.BufferAllocator;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.math.Vec3d;
import wtf.opal.client.OpalClient;
import wtf.opal.client.feature.helper.impl.LocalDataWatch;
import wtf.opal.client.feature.helper.impl.player.rotation.RotationHelper;
import wtf.opal.client.feature.helper.impl.player.slot.SlotHelper;
import wtf.opal.client.feature.helper.impl.server.impl.HypixelServer;
import wtf.opal.client.feature.module.Module;
import wtf.opal.client.feature.module.ModuleCategory;
import wtf.opal.client.feature.module.impl.combat.killaura.target.CurrentTarget;
import wtf.opal.client.feature.module.impl.combat.killaura.target.KillAuraTargeting;
import wtf.opal.client.feature.module.impl.world.scaffold.ScaffoldModule;
import wtf.opal.client.renderer.world.WorldRenderer;
import wtf.opal.event.impl.game.PreGameTickEvent;
import wtf.opal.event.impl.render.RenderWorldEvent;
import wtf.opal.event.subscriber.Subscribe;
import wtf.opal.utility.misc.math.MathUtility;
import wtf.opal.utility.player.RaycastUtility;
import wtf.opal.utility.render.ColorUtility;
import wtf.opal.utility.render.CustomRenderLayers;

import java.util.function.Predicate;

import static wtf.opal.client.Constants.mc;

public final class KillAuraModule extends Module {

    private final KillAuraSettings settings = new KillAuraSettings(this);
    private final KillAuraTargeting targeting = new KillAuraTargeting(this.settings);

    private long lastAttackTime = 0;

    public KillAuraModule() {
        super(
                "KillAura",
                "Automatically attacks entities (HeyPixel Logic).",
                ModuleCategory.COMBAT
        );
    }

    public KillAuraSettings getSettings() {
        return settings;
    }

    @Override
    public String getSuffix() {
        return this.settings.getMode().toString();
    }

    public KillAuraTargeting getTargeting() {
        return targeting;
    }

    /**
     * 修复报错：供 TargetStrafe 等外部模块调用
     * 返回当前攻击的目标实体
     */
    public Entity getTargetEntity() {
        final CurrentTarget target = this.targeting.getTarget();
        return target != null ? target.getEntity() : null;
    }

    /**
     * 修复报错：供 TargetStrafe 等外部模块调用
     * 判断是否有目标被选中
     */
    public boolean isTargetSelected() {
        return this.targeting.isTargetSelected();
    }

    @Subscribe(priority = 2)
    public void onPreGameTick(final PreGameTickEvent event) {
        if (!shouldRun()) {
            this.targeting.reset();
            return;
        }

        this.targeting.update();
        final CurrentTarget currentTarget = this.targeting.getTarget();

        if (currentTarget != null) {
            Entity targetEntity = currentTarget.getEntity();

            RotationHelper.getHandler().rotate(
                    currentTarget.getRotations().rotation(),
                    settings.createRotationModel()
            );

            if (this.canRaycastTarget(targetEntity)) {
                this.performAttack(targetEntity);
            }
        }
    }

    private boolean canRaycastTarget(Entity target) {
        // 修复报错：Vec2f 使用 .x 和 .y 获取偏航和俯仰 (Minecraft映射中通常为 x=yaw, y=pitch)
        float yaw = RotationHelper.getHandler().getTargetRotation().x;
        float pitch = RotationHelper.getHandler().getTargetRotation().y;

        double range = this.settings.getSwingRange();

        Predicate<Entity> filter = e -> e.equals(target);
        EntityHitResult result = RaycastUtility.raycastEntity(range, 1.0F, yaw, pitch, filter);

        return result != null && result.getEntity() != null && result.getEntity().equals(target);
    }

    private void performAttack(Entity target) {
        long time = System.currentTimeMillis();

        // 修复报错：直接从 NumberProperty 获取 double 值
        double cps = this.settings.getCps();
        double baseDelay = 1000.0 / cps;

        // HeyPixel 随机延迟算法
        long delay = (long) (baseDelay + (Math.random() - 0.5) * baseDelay * 0.4);

        if (time - lastAttackTime >= delay) {
            mc.interactionManager.attackEntity(mc.player, target);
            mc.player.swingHand(Hand.MAIN_HAND);
            lastAttackTime = time;
        }
    }

    @Subscribe
    public void onRenderWorld(final RenderWorldEvent event) {
        if (!targeting.isTargetSelected() || targeting.getTarget() == null || !settings.getVisuals().getProperty("Box").getValue()) {
            return;
        }

        final LivingEntity target = targeting.getTarget().getEntity();

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

    private boolean shouldRun() {
        if (mc.player == null || mc.world == null) {
            return false;
        }

        if (OpalClient.getInstance().getModuleRepository().getModule(ScaffoldModule.class).isEnabled()) {
            return false;
        }

        if (settings.isRequireAttackKey() && !mc.options.attackKey.isPressed()) {
            return false;
        }

        final ItemStack heldItem = SlotHelper.getInstance().getMainHandStack(mc.player);
        if (settings.isRequireWeapon() &&
                !(heldItem.isIn(ItemTags.SWORDS) || heldItem.isIn(ItemTags.AXES) || heldItem.isIn(ItemTags.PICKAXES))) {
            return false;
        }

        if (LocalDataWatch.get().getKnownServerManager().getCurrentServer() instanceof HypixelServer) {
            final HypixelServer.ModAPI.Location currentLocation = HypixelServer.ModAPI.get().getCurrentLocation();
            return currentLocation == null || (!currentLocation.isLobby() && currentLocation.serverType() != GameType.REPLAY);
        }

        return true;
    }

    @Override
    protected void onDisable() {
        this.targeting.reset();
        this.lastAttackTime = 0;
        super.onDisable();
    }
}