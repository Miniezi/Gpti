package dev.titantech.littlespore.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import team.creative.littletiles.common.block.entity.BETiles;
import team.creative.littletiles.common.block.little.tile.LittleTile;
import team.creative.littletiles.common.block.little.tile.parent.IParentCollection;
import team.creative.littletiles.common.block.mc.BlockTile;

/** Experimental: use weakest constituent hardness for composite break rules. */
@Mixin(value = BlockTile.class, remap = false)
public abstract class BlockTileMixin {
    @Inject(method = "m_7749_", at = @At("HEAD"), cancellable = true, remap = false)
    private void littlespore$hardness(BlockState block, BlockGetter world,
                                   BlockPos pos, CallbackInfoReturnable<Float> cir) {
        if (!(world.getBlockEntity(pos) instanceof BETiles be)) return;
        float minimum = Float.POSITIVE_INFINITY;
        boolean found = false;
        try {
            for (IParentCollection group : be.groups()) {
                for (LittleTile tile : group) {
                    BlockState material = tile.getState();
                    if (material == null) continue;
                    found = true;
                    float hardness = material.getDestroySpeed(world, pos);
                    if (hardness >= 0 && Float.isFinite(hardness))
                        minimum = Math.min(minimum, hardness);
                }
            }
        } catch (RuntimeException ex) {
            return; // Fall back to LittleTiles for temporarily invalid tile entities.
        }
        if (!found) return;
        cir.setReturnValue(minimum == Float.POSITIVE_INFINITY ? -1.0F : minimum);
    }
}
