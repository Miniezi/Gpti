package dev.titantech.littlespore.trace;

import net.minecraft.core.BlockPos;
import dev.titantech.littlespore.logging.TraceLog;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import team.creative.littletiles.common.block.mc.BlockTile;

@Mixin(LevelChunk.class)
public abstract class ChunkTraceMixin {
    @Inject(method = "m_6978_(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;Z)Lnet/minecraft/world/level/block/state/BlockState;",
        at = @At("HEAD"), remap = false, require = 1)
    private void littletrace$beforeChange(BlockPos pos, BlockState replacement, boolean moving,
        CallbackInfoReturnable<BlockState> cir) {
        LevelChunk chunk = (LevelChunk)(Object)this;
        BlockState old = chunk.getBlockState(pos);
        if (old.getBlock() instanceof BlockTile || replacement.getBlock() instanceof BlockTile) {
            if (!old.equals(replacement)) {
                TraceLog.change("LevelChunk.setBlockState", pos, old, replacement);
            }
        }
    }
}
