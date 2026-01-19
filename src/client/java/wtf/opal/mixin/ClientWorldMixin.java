package wtf.opal.mixin;

import net.minecraft.client.world.ClientWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import wtf.opal.event.EventDispatcher;
import wtf.opal.event.impl.game.world.PlaySoundEvent;

@Mixin(ClientWorld.class)
public final class ClientWorldMixin {

    private ClientWorldMixin() {
    }

    @Inject(method = "playSound(DDDLnet/minecraft/sound/SoundEvent;Lnet/minecraft/sound/SoundCategory;FFZJ)V", at = @At("HEAD"), cancellable = true)
    private void playSound(double x, double y, double z, SoundEvent event, SoundCategory category, float volume, float pitch, boolean useDistance, long seed, CallbackInfo ci) {
        final PlaySoundEvent playSoundEvent = new PlaySoundEvent(event, x, y, z);
        EventDispatcher.dispatch(playSoundEvent);
        if (playSoundEvent.isCancelled()) {
            ci.cancel();
        }
    }

}
