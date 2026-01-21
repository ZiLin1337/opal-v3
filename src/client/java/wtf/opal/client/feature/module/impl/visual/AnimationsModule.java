package wtf.opal.client.feature.module.impl.visual;

import net.minecraft.client.MinecraftClient; // 修复 1: 替换 Minecraft 为 MinecraftClient
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.item.SwordItem;
import net.minecraft.network.packet.c2s.play.HandSwingC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractItemC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import wtf.opal.client.feature.module.Module;
import wtf.opal.client.feature.module.ModuleCategory;
import wtf.opal.client.feature.module.impl.combat.killaura.KillAuraModule;
import wtf.opal.client.feature.module.property.impl.GroupProperty;
import wtf.opal.client.feature.module.property.impl.bool.BooleanProperty;
import wtf.opal.client.feature.module.property.impl.mode.ModeProperty;
import wtf.opal.client.feature.module.property.impl.number.NumberProperty;
import wtf.opal.event.impl.game.PreGameTickEvent;
import wtf.opal.event.subscriber.Subscribe;
import wtf.opal.utility.player.BlockUtility;

// 修复 2: 修改 PacketEvent 导入路径
// 注意：ReceivePacketEvent 通常用于接收(S2C)包。
// 如果你要拦截玩家发送(C2S)的格挡包，通常应该使用 SendPacketEvent。
// 这里为了解决编译错误，我导入了这两个，请根据你的 Opal 事件系统确认使用哪一个。
import wtf.opal.event.impl.game.packet.ReceivePacketEvent;
import wtf.opal.event.impl.game.packet.SendPacketEvent;

import static wtf.opal.client.Constants.mc;

public final class AnimationsModule extends Module {

    // --- 属性设置 ---
    private final BooleanProperty swordBlocking = new BooleanProperty("Enabled", true);
    private final ModeProperty<BlockMode> blockAnimationMode = new ModeProperty<>("Block animation", BlockMode.class, BlockMode.V1_7).hideIf(() -> !swordBlocking.getValue());

    // Fake Block / Aura Block 属性
    public final BooleanProperty auraAutoBlock = new BooleanProperty("Aura Auto Block", true);
    public final BooleanProperty onlyAura = new BooleanProperty("Only Aura", false);

    // Shields
    private final BooleanProperty alwaysHideShield = new BooleanProperty("Always hidden", true);
    private final BooleanProperty hideShieldSlotInHotbar = new BooleanProperty("Hide offhand slot", true);

    // Player
    private final BooleanProperty oldBackwardsWalking = new BooleanProperty("Old backwards walking", true);
    private final BooleanProperty oldArmorDamageTint = new BooleanProperty("Old armor damage tint", true);
    private final BooleanProperty oldSneaking = new BooleanProperty("Old sneaking", false);
    private final BooleanProperty fixPoseRepeat = new BooleanProperty("Fix pose repeat", true);

    // Item
    private final NumberProperty mainHandScale = new NumberProperty("Scale", 0f, -2f, 2f, 0.1f);
    private final NumberProperty mainHandX = new NumberProperty("Offset X", 0.f, -2.f, 2.f, 0.1f);
    private final NumberProperty mainHandY = new NumberProperty("Offset Y", 0.f, -2.f, 2.f, 0.1f);
    private final NumberProperty swingSlowdown = new NumberProperty("Swing slowdown", 0.F, 0.F, 5.F, 0.25F);
    private final BooleanProperty oldCooldownAnimation = new BooleanProperty("Old cooldown animation", true);
    private final BooleanProperty swingWhileUsing = new BooleanProperty("Visual swing on use", true);
    private final BooleanProperty hideDropSwing = new BooleanProperty("Hide drop swing", false);
    private final BooleanProperty equipOffset = new BooleanProperty("Equip offset", false);

    // 用于跟踪状态
    public static boolean isBlocking = false;
    private KillAuraModule killAuraModule;

    public AnimationsModule() {
        super("Animations", "Modifies animations within the game.", ModuleCategory.VISUAL);
        setEnabled(true);
        addProperties(
                new GroupProperty("Sword blocking", swordBlocking, blockAnimationMode, auraAutoBlock, onlyAura),
                new GroupProperty("Shields", alwaysHideShield, hideShieldSlotInHotbar),
                new GroupProperty("Player", oldBackwardsWalking, oldArmorDamageTint, oldSneaking, fixPoseRepeat),
                new GroupProperty(
                        "Item",
                        mainHandScale, mainHandX, mainHandY, swingSlowdown,
                        oldCooldownAnimation, swingWhileUsing, hideDropSwing, equipOffset
                )
        );
    }

    // --- Getters ---
    public boolean isHideDropSwing() { return this.hideDropSwing.getValue(); }
    public boolean isOldSneaking() { return this.oldSneaking.getValue(); }
    public boolean isFixPoseRepeat() { return this.fixPoseRepeat.getValue(); }
    public float getSwingSlowdown() { return swingSlowdown.getValue().floatValue() + 1.F; }
    public boolean isSwordBlocking() { return swordBlocking.getValue(); }
    public boolean isEquipOffset() { return equipOffset.getValue(); }
    public boolean isOldCooldownAnimation() { return oldCooldownAnimation.getValue(); }
    public boolean isOldBackwardsWalking() { return oldBackwardsWalking.getValue(); }
    public boolean isOldArmorDamageTint() { return oldArmorDamageTint.getValue(); }
    public boolean isHideShield() { return alwaysHideShield.getValue(); }
    public boolean isHideShieldSlotInHotbar() { return hideShieldSlotInHotbar.getValue(); }
    public float getMainHandScale() { return mainHandScale.getValue().floatValue(); }
    public float getMainHandX() { return mainHandX.getValue().floatValue(); }
    public float getMainHandY() { return mainHandY.getValue().floatValue(); }
    public boolean isSwingWhileUsing() { return this.swingWhileUsing.getValue(); }

    /**
     * 判断是否触发了 KillAura 的 Fake Block
     */
    public boolean isAuraFakeBlocking() {
        if (!this.isEnabled() || !this.auraAutoBlock.getValue()) return false;
        KillAuraModule ka = getKillAura();
        return ka != null && ka.isEnabled() && ka.getTargetEntity() != null;
    }

    /**
     * 获取 KillAura 模块实例 (使用了安全的反射/单例获取，请确保 wtf.opal.client.Client 存在)
     */
    private KillAuraModule getKillAura() {
        if (this.killAuraModule == null) {
            try {
                this.killAuraModule = (KillAuraModule) wtf.opal.client.Client.INSTANCE.getModuleManager().get(KillAuraModule.class);
            } catch (Exception e) {
                return null;
            }
        }
        return this.killAuraModule;
    }

    /**
     * 判断玩家是否持有剑 (兼容多种 Mappings)
     */
    private boolean isHoldingSword(ClientPlayerEntity player) {
        if (player == null) return false;
        ItemStack stack = player.getMainHandStack();
        if (stack == null || stack.isEmpty()) return false;
        // 使用类名字符串判断，避免 SwordItem / ItemSword 的导入错误
        String className = stack.getItem().getClass().getSimpleName();
        return className.contains("Sword");
    }

    // --- 事件处理 ---

    /**
     * 拦截真实格挡包
     * 修复 3: 使用 SendPacketEvent (因为这是发送出去的 C2S 包)
     * 如果必须使用 ReceivePacketEvent 才能编译，请将参数改回 ReceivePacketEvent，但逻辑可能无效。
     */
    @Subscribe
    public void onPacket(SendPacketEvent event) {
        // PlayerInteractItemC2SPacket 是使用物品/格挡的数据包
        if (event.getPacket() instanceof PlayerInteractItemC2SPacket) {
            // 如果处于 Fake Block 状态且手持剑，拦截包
            if (isAuraFakeBlocking() && isHoldingSword(mc.player)) {
                event.setCancelled(true);
            }
        }
    }

    /**
     * 发送挥手包模拟动画
     */
    @Subscribe
    public void onPreTick(PreGameTickEvent event) {
        if (mc.player == null) return;

        if (isAuraFakeBlocking()) {
            isBlocking = true;
            // 模拟挥手防止发呆判定
            if (mc.player.age % 10 < 5) {
                // 修复 4: 确保这里使用的是正确的发包方法
                // 如果 MinecraftClient.getInstance().getNetworkHandler() 为空，添加检查
                if (mc.getNetworkHandler() != null) {
                    mc.getNetworkHandler().sendPacket(new HandSwingC2SPacket(Hand.MAIN_HAND));
                }
            }
        } else {
            // 真实格挡判定
            isBlocking = mc.player.isUsingItem() && isHoldingSword(mc.player);
        }
    }

    /**
     * 核心渲染逻辑
     */
    public void applyTransformations(final MatrixStack matrices, final float swingProgress) {
        boolean shouldRenderBlock = isSwordBlocking() && (
                (mc.player.isUsingItem() && isHoldingSword(mc.player)) // 真实格挡
                        || isAuraFakeBlocking() // KA Fake Block
        );

        if (!shouldRenderBlock) {
            return;
        }

        final float convertedProgress = MathHelper.sin(MathHelper.sqrt(swingProgress) * (float) Math.PI);
        final float f = MathHelper.sin(swingProgress * swingProgress * (float) Math.PI);

        switch (blockAnimationMode.getValue()) {
            case V1_7 -> {
                BlockUtility.applySwingTransformation(matrices, swingProgress, convertedProgress);
                BlockUtility.applyBlockTransformation(matrices);
            }
            case V1_8 -> {
                BlockUtility.applyBlockTransformation(matrices);
            }
            case RUB -> {
                BlockUtility.applyBlockTransformation(matrices);
                matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(f * -30.0F));
                matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(convertedProgress * -30.0F));
            }
            case STELLA -> {
                BlockUtility.applySwingTransformation(matrices, swingProgress, convertedProgress);
                matrices.translate(-0.15F, 0.16F, 0.15F);
                matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-24.0F));
                matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(75.0F));
                matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(90.0F));
            }
            case BOUNCE -> {
                BlockUtility.applyBlockTransformation(matrices);
                matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(0.0F));
                matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(convertedProgress * 42.0F));
                matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(-convertedProgress * 22.0F));
            }
            case DIAGONAL -> {
                BlockUtility.applyBlockTransformation(matrices);
                matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(5.0F - (convertedProgress * 32.0F)));
                matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(0.0F));
                matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(0.0F));
            }
            case SWANK -> {
                matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(45.0F + f * -5.0F));
                matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(convertedProgress * -20.0F));
                matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(convertedProgress * -40.0F));
                matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-45.0F));
                BlockUtility.applyBlockTransformation(matrices);
            }
        }
    }

    public enum BlockMode {
        V1_7("1.7"),
        V1_8("1.8"),
        RUB("Rub"),
        STELLA("Stella"),
        BOUNCE("Bounce"),
        DIAGONAL("Diagonal"),
        SWANK("Swank");

        private final String name;

        BlockMode(String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            return name;
        }
    }
}