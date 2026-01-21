package wtf.opal.event.impl.render;

import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Hand;

public class RenderHandEvent {
    // 原record的组件字段
    private final Hand hand;
    private final MatrixStack matrixStack;
    private final VertexConsumerProvider vertexConsumers;
    private final int light;

    // 取消状态字段（可修改）
    private boolean canceled;

    // 构造器（对应原record的组件）
    public RenderHandEvent(Hand hand,
                           MatrixStack matrixStack,
                           VertexConsumerProvider vertexConsumers,
                           int light) {
        this.hand = hand;
        this.matrixStack = matrixStack;
        this.vertexConsumers = vertexConsumers;
        this.light = light;
        this.canceled = false; // 默认未取消
    }

    // 原record的组件访问器（保持和record一致的方法名）
    public Hand hand() {
        return hand;
    }

    public MatrixStack matrixStack() {
        return matrixStack;
    }

    public VertexConsumerProvider vertexConsumers() {
        return vertexConsumers;
    }

    public int light() {
        return light;
    }

    // 取消状态的设置和获取方法
    public void setCanceled(boolean canceled) {
        this.canceled = canceled;
    }

    public boolean isCanceled() {
        return canceled;
    }
}