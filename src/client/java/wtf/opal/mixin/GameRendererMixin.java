package wtf.opal.mixin;

import com.google.common.base.Predicates;
import net.minecraft.client.gui.CubeMapRenderer;
import net.minecraft.client.gui.RotatingCubeMapRenderer;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import wtf.opal.client.OpalClient;
import wtf.opal.client.feature.module.impl.combat.PiercingModule;
import wtf.opal.client.renderer.shader.ShaderFramebuffer;
import wtf.opal.utility.player.RaycastUtility;

import static wtf.opal.client.Constants.mc;

@Mixin(GameRenderer.class)
public abstract class GameRendererMixin {

    @Unique
    private boolean passThroughBlocks;

    @Mutable
    @Shadow
    @Final
    protected CubeMapRenderer panoramaRenderer;

    @Mutable
    @Shadow
    @Final
    protected RotatingCubeMapRenderer rotatingPanoramaRenderer;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void replaceCubeMapRenderer(CallbackInfo ci) {
        this.panoramaRenderer = new CubeMapRenderer(Identifier.of("opal:panorama/panorama"));
        this.rotatingPanoramaRenderer = new RotatingCubeMapRenderer(this.panoramaRenderer);
    }

    @Inject(method = "onResized", at = @At("HEAD"))
    private void hookOnResized(int width, int height, CallbackInfo ci) {
        ShaderFramebuffer.onResized(width, height);
    }

    @Redirect(
            method = "findCrosshairTarget",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/util/hit/HitResult;getType()Lnet/minecraft/util/hit/HitResult$Type;")
    )
    private HitResult.Type redirectBlockHitResultType(HitResult instance) {
        if (passThroughBlocks) {
            passThroughBlocks = false;
            return HitResult.Type.MISS;
        }

        return instance.getType();
    }

    @Redirect(
            method = "findCrosshairTarget",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/util/math/Vec3d;squaredDistanceTo(Lnet/minecraft/util/math/Vec3d;)D", ordinal = 0)
    )
    private double redirectPassedThroughBlockDistance(Vec3d instance, Vec3d vec, @com.llamalad7.mixinextras.sugar.Local(ordinal = 1, argsOnly = true) double entityInteractionRange, @com.llamalad7.mixinextras.sugar.Local(argsOnly = true) float tickDelta) {
        if (OpalClient.getInstance().getModuleRepository().getModule(PiercingModule.class).isEnabled()) {
            final HitResult hitResult = RaycastUtility.raycastEntity(entityInteractionRange, tickDelta, mc.player.getYaw(), mc.player.getPitch(), Predicates.alwaysTrue());
            if (hitResult != null && hitResult.getType() == HitResult.Type.ENTITY) {
                passThroughBlocks = true;
                return Double.MAX_VALUE;
            }
        }

        return instance.squaredDistanceTo(vec);
    }
}