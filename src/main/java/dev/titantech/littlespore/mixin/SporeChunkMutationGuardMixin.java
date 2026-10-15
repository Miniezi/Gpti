package dev.titantech.littlespore.mixin;

import dev.titantech.littlespore.protection.MaterialProtection;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import org.apache.logging.log4j.LogManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Last-resort guard: refuse Spore's chunk mutation before the block changes. */
@Mixin(value = LevelChunk.class, priority = 950)
public abstract class SporeChunkMutationGuardMixin {
    @Inject(method = "m_6978_(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;Z)Lnet/minecraft/world/level/block/state/BlockState;",
            at = @At("HEAD"), cancellable = true, remap = false, require = 1)
    private void littlespore$guardChunkReplace(BlockPos pos, BlockState replacement,
            boolean moving, CallbackInfoReturnable<BlockState> cir) {
        Level level = ((LevelChunk)(Object)this).getLevel();
        if (level.isClientSide() || !replacement.isAir()) return;
        if (!StackWalker.getInstance().walk(frames -> frames.anyMatch(f ->
                f.getClassName().startsWith("com.Harbinger.Spore.")))) return;
        if (!MaterialProtection.isUnbreakable(level, pos)) return;

        LogManager.getLogger("LittleTilesSporeFix")
            .info("BLOCKED Spore chunk removal BEFORE change at {}", pos);
        // Null is the LevelChunk#setBlockState 'unchanged' sentinel.
        cir.setReturnValue(null);
    }
}
