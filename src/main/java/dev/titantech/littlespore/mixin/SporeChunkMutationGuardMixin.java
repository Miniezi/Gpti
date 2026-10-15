package dev.titantech.littlespore.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.core.registries.BuiltInRegistries;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import team.creative.littletiles.common.block.entity.BETiles;
import team.creative.littletiles.common.block.little.tile.LittleTile;
import team.creative.littletiles.common.block.little.tile.parent.IParentCollection;
import team.creative.littletiles.common.block.mc.BlockTile;

/**
 * Material-aware destruction guard at the exact LevelChunk mutation point
 * observed by LittleTiles Break Tracer 1.0.2.
 *
 * For Spore-triggered replacement with air, the strongest material governs
 * the whole LittleTiles container. An unbreakable constituent blocks removal.
 */
@Mixin(value = LevelChunk.class, priority = 950)
public abstract class SporeChunkMutationGuardMixin {
    private static final Logger LOG = LogManager.getLogger("LittleTilesSporeFix");

    @Inject(method = "m_6978_(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;Z)Lnet/minecraft/world/level/block/state/BlockState;",
            at = @At("HEAD"), cancellable = true, remap = false, require = 1)
    private void littlespore$guardChunkReplace(BlockPos pos, BlockState replacement,
            boolean moving, CallbackInfoReturnable<BlockState> cir) {
        LevelChunk chunk = (LevelChunk)(Object)this;
        Level level = chunk.getLevel();
        if (level.isClientSide() || !replacement.isAir()) return;
        if (!StackWalker.getInstance().walk(frames -> frames.anyMatch(f ->
                f.getClassName().startsWith("com.Harbinger.Spore.")))) return;

        BlockState original = chunk.getBlockState(pos);
        if (!(original.getBlock() instanceof BlockTile)) return;
        if (!(chunk.getBlockEntity(pos) instanceof BETiles be)) {
            LOG.warn("Spore attempted removal of LittleTiles at {} but BETiles missing", pos);
            return;
        }

        float hardest = 0.0F;
        BlockState strongest = null;
        boolean protectedDeepslate = false;
        boolean found = false;
        try {
            for (IParentCollection group : be.groups()) {
                for (LittleTile tile : group) {
                    BlockState material = tile.getState();
                    if (material == null) continue;
                    float value = material.getDestroySpeed(level, pos);
                    String id = BuiltInRegistries.BLOCK.getKey(material.getBlock()).toString();
                    // Spore Inquisitor does not break these full-size vanilla variants.
                    // Keep their LittleTiles representations equally protected.
                    if (id.startsWith("minecraft:polished_deepslate") ||
                        id.startsWith("minecraft:deepslate_brick") ||
                        id.startsWith("minecraft:cracked_deepslate_brick") ||
                        id.startsWith("minecraft:deepslate_tile") ||
                        id.startsWith("minecraft:cracked_deepslate_tile") ||
                        id.equals("minecraft:chiseled_deepslate")) {
                        protectedDeepslate = true;
                        strongest = material;
                    }
                    if (value < 0.0F) {
                        LOG.info("BLOCKED Spore removal of LittleTiles at {}: unbreakable material {}", pos, material);
                        cir.setReturnValue(null);
                        return;
                    }
                    if (!Float.isFinite(value)) continue;
                    found = true;
                    hardest = Math.max(hardest, value);
                }
            }
        } catch (RuntimeException ex) {
            LOG.warn("Failed reading LittleTiles materials at {}: {}", pos, ex.toString());
            return;
        }
        if (protectedDeepslate) {
            LOG.info("BLOCKED Spore removal at {}: protected deepslate material {}", pos, strongest);
            cir.setReturnValue(null);
        } else if (found && hardest > 5.0F) {
            LOG.info("BLOCKED Spore removal of LittleTiles at {}: hardest hardness={}", pos, hardest);
            cir.setReturnValue(null);
        } else {
            LOG.info("ALLOWED Spore removal of LittleTiles at {}: hardest hardness={}, materialPresent={}",
                    pos, hardest, found);
        }
    }
}
