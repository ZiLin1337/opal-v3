package wtf.opal.client.feature.module.impl.world.scaffold;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec2f;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.client.network.OtherClientPlayerEntity;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.BufferAllocator;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.network.packet.s2c.play.ItemPickupAnimationS2CPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import wtf.opal.client.feature.module.property.impl.mode.ModuleMode;
import wtf.opal.utility.player.RaytracedRotation;
import static wtf.opal.client.Constants.mc;
import static wtf.opal.utility.player.InventoryUtility.isGoodBlock;

public abstract class ScaffoldBase extends ModuleMode<ScaffoldModule> {

    // 共享字段
    protected BlockPos blockPos;
    protected Direction enumFacing;
    protected int yLevel;
    protected int placeTick;
    protected Vec2f intelligentRotation;
    protected RaytracedRotation rotation;

    // 构造函数
    public ScaffoldBase(ScaffoldModule module) {
        super(module);
    }

    // 核心共享方法
    public Vec3d getVec3() {
        // 修复点1：1.21.10中移除getPositionVec()，手动构建Vec3d
        if (mc.player == null) {
            return null;
        }
        double x = mc.player.getX();
        double y = mc.player.getY();
        double z = mc.player.getZ();
        return new Vec3d(x, y, z);
    }

    public boolean isValidStack() {
        if (mc.player == null) return false;
        for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.player.getInventory().getMainStacks().get(i);
            if (stack.getItem() instanceof BlockItem blockItem) {
                return true;
            }
        }
        return false;
    }

    public boolean isSolidAndNonInteractive(BlockPos pos) {
        BlockState state = mc.world.getBlockState(pos);
        // 修复点2：1.21.10中getMaterial()被移除，使用getFluidState().isEmpty()检查液体
        return !state.isReplaceable() && state.getFluidState().isEmpty();
    }

    public void place() {
        // 基础放置逻辑，子类可以覆盖
    }

    public void onAir() {
        // 空中逻辑，子类可以覆盖
    }

    public boolean checkBlock() {
        // 基础方块检查逻辑，子类可以覆盖
        return false;
    }

    // 辅助方法
    protected int getPlaceableBlock() {
        for (int i = 0; i < 9; i++) {
            final ItemStack itemStack = mc.player.getInventory().getMainStacks().get(i);
            if (itemStack.getItem() instanceof BlockItem blockItem
                    && isGoodBlock(blockItem.getBlock())) {
                return i;
            }
        }
        return -1;
    }

    // 抽象方法，子类必须实现
    public abstract void onPreTick();
    public abstract void onMoveInput();
    public abstract String getSuffix();
}