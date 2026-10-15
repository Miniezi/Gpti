package dev.titantech.littlespore.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import team.creative.littletiles.common.block.mc.BlockTile;

/**
 * Global chunk mutation guard for direct and indirect Spore block replacement.
 * Applies to all Spore entities, not one attack method.
 */
@Mixin(value = LevelChunk.class, priority = 950)
public abstract class SporeChunkMutationGuardMixin {
    private static final Logger LITTLE_SPORE_LOG = LogManager.getLogger("LittleTilesSporeFix");
    private static final StackWalker LITTLE_SPORE_FRAMES = StackWalker.getInstance();

    @Inject(method = "m_6978_(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;Z)Lnet/minecraft/world/level/block/state/BlockState;",
            at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private void littlespore$guardChunkReplace(BlockPos pos, BlockState replacement,
                    boolean moving, CallbackInfoReturnable<BlockState> cir) {
        LevelChunk chunk = (LevelChunk)(Object)this;
        Level level = chunk.getLevel();
        if (level.isClientSide()) return;
        if (!LITTLE_SPORE_FRAMES.walk(frames -> frames.anyMatch(f ->
                    f.getClassName().startsWith("com.Harbinger.Spore.")))) return;

        BlockState original = chunk.getBlockState(pos);
        if (!(original.getBlock() instanceof BlockTile)) return;
        if (original.equals(replacement)) return;
        float hardness = original.getDestroySpeed(level, pos);
        if (hardness < 0.0F || hardness > 5.0F) {
            LITTLE_SPORE_LOG.info("Protected LittleTiles at {} from Spore (hardness={})", pos, hardness);
            cir.setReturnValue(null);
        }
    }
}
