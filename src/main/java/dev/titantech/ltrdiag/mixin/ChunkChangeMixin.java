package dev.titantech.ltrdiag.mixin;
import dev.titantech.ltrdiag.TraceLog;
import dev.titantech.ltrdiag.RenderEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.client.multiplayer.ClientLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import team.creative.littletiles.common.block.mc.BlockTile;
@Mixin(LevelChunk.class)
public abstract class ChunkChangeMixin {
 @Inject(method="m_6978_(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;Z)Lnet/minecraft/world/level/block/state/BlockState;",
  at=@At("RETURN"),remap=false,require=1)
 private void trace(BlockPos pos,BlockState next,boolean moving,CallbackInfoReturnable<BlockState> cir) {
  LevelChunk chunk=(LevelChunk)(Object)this;
  BlockState previous=cir.getReturnValue();
  if(previous==null||previous.equals(next))return;
  if(!(previous.getBlock() instanceof BlockTile)&&!(next.getBlock() instanceof BlockTile))return;
  if(chunk.getLevel() instanceof ClientLevel client) RenderEvents.changed(client,pos,previous.toString(),next.toString());
  else TraceLog.record("SERVER_CHANGE "+pos+" "+previous+" -> "+next);
 }
}
