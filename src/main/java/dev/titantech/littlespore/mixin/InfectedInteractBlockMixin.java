package dev.titantech.littlespore.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import team.creative.littletiles.common.block.mc.BlockTile;

/**
 * Targets the Spore entity by name, avoiding a compile-time dependency on the
 * proprietary, very large Spore JAR. Experimental hardness threshold.
 */
@Mixin(targets = "com.Harbinger.Spore.Sentities.BaseEntities.Infected", remap = false)
public abstract class InfectedInteractBlockMixin {
    @Inject(method = "interactBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/Level;)Z",
            at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private void littlespore$preventHardTileDestruction(BlockPos pos, Level level,
            CallbackInfoReturnable<Boolean> cir) {
        if (level.isClientSide()) return;
        BlockState state = level.getBlockState(pos);
        if (!(state.getBlock() instanceof BlockTile)) return;
        float hardness = state.getDestroySpeed(level, pos);
        if (hardness < 0.0F || hardness > 5.0F) cir.setReturnValue(false);
    }
}
