package dev.titantech.ltrdiag.mixin;
import dev.titantech.ltrdiag.TraceLog;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import team.creative.littletiles.client.render.block.BERenderManager;
@Mixin(BERenderManager.class)
public abstract class TileRenderMixin {
 @Inject(method="tilesChanged",at=@At("HEAD"),require=0,remap=false)
 private void traceTilesChanged(CallbackInfo ci) {
    TraceLog.record("RENDER tilesChanged renderer="+System.identityHashCode(this));
 }
}
