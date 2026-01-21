package wtf.opal.event.impl.render;

import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Hand;

public record RenderHandEvent(Hand hand,
                              MatrixStack matrixStack,
                              VertexConsumerProvider vertexConsumers,
                              int light) {

    public void setCanceled(boolean canceled) {
        this.canceled = canceled;
    }

    private boolean canceled;

    public boolean isCanceled() {
        return canceled;
    }
}
