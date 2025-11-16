package wtf.opal.utility.player;

import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Vec2f;

public record RaytracedRotation(Vec2f rotation, HitResult hitResult) {
}
