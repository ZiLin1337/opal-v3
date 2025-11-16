package wtf.opal.mixin;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.TitleScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static wtf.opal.client.Constants.mc;

@Mixin(TitleScreen.class)
public final class TitleScreenMixin {

    private TitleScreenMixin() {
    }

    @Inject(
            method = "render",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screen/TitleScreen;renderPanoramaBackground(Lnet/minecraft/client/gui/DrawContext;F)V", shift = At.Shift.AFTER)
    )
    private void applyBlur(DrawContext context, int mouseX, int mouseY, float deltaTicks, CallbackInfo ci) {
        mc.gameRenderer.renderBlur();
    }

}
