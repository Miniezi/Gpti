package dev.titantech.littlespore.mixin;

import dev.titantech.littlespore.protection.MaterialProtection;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Spore Infected/Hyper mobs retain their own blockBreakingParameter and config. */
@Mixin(targets = "com.Harbinger.Spore.Sentities.BaseEntities.Infected", remap = false)
public abstract class InfectedMaterialViewMixin {
    @Redirect(method = "breakNearbyBlocks", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/world/level/Level;m_8055_(Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/level/block/state/BlockState;", remap = false),
        remap = false, require = 1)
    private BlockState littlespore$effectiveNearbyMaterial(Level level, BlockPos pos) {
        BlockState actual = level.getBlockState(pos);
        return MaterialProtection.effectiveState(level, pos, actual);
    }
}
