package dev.titantech.littlespore.mixin;

import com.Harbinger.Spore.Sentities.BaseEntities.Infected;
import com.Harbinger.Spore.Sentities.BaseEntities.Hyper;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import team.creative.littletiles.common.block.mc.BlockTile;

/** Intercept the three-argument destroyBlock used by Spore's Infected.interactBlock. */
@Mixin(value = Infected.class, remap = false)
public abstract class InfectedInteractBlockMixin {
    @Inject(method = "interactBlock", at = @At("HEAD"), cancellable = true, remap = false)
    private void littlespore$preventHardTileDestruction(BlockPos pos, Level level,
            CallbackInfoReturnable<Boolean> cir) {
        if (level.isClientSide()) return;
        BlockState state = level.getBlockState(pos);
        if (!(state.getBlock() instanceof BlockTile)) return;
        float hardness = state.getDestroySpeed(level, pos);
        if (hardness < 0 || (this instanceof Hyper hyper && hardness > hyper.getBreaking())) {
            cir.setReturnValue(false);
        }
    }
}
