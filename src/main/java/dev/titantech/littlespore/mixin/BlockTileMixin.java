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

/**
 * A LittleTiles block is atomic for breaking: material hardness is the hardest
 * constituent tile, not the weak container hardness.
 *
 * Unbreakable constituent (e.g. bedrock) makes the WHOLE container unbreakable.
 * No internal tile deletion, no geometry math.
 */
@Mixin(value = BlockTile.class, remap = false)
public abstract class BlockTileMixin {
    @Inject(method = "m_7749_", at = @At("HEAD"), cancellable = true, remap = false)
    private void littlespore$hardness(BlockState block, BlockGetter world,
                                   BlockPos pos, CallbackInfoReturnable<Float> cir) {
        if (!(world.getBlockEntity(pos) instanceof BETiles be)) return;
        float maximum = 0.0F;
        boolean found = false;
        try {
            for (IParentCollection group : be.groups()) {
                for (LittleTile tile : group) {
                    BlockState material = tile.getState();
                    if (material == null) continue;
                    float hardness = material.getDestroySpeed(world, pos);
                    if (hardness < 0.0F) {
                        cir.setReturnValue(-1.0F);
                        return;
                    }
                    if (!Float.isFinite(hardness)) continue;
                    found = true;
                    maximum = Math.max(maximum, hardness);
                }
            }
        } catch (RuntimeException ex) {
            return;
        }
        if (found) cir.setReturnValue(maximum);
    }
}
