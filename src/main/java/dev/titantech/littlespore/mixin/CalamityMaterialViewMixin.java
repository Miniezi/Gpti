package dev.titantech.littlespore.mixin;

import dev.titantech.littlespore.protection.MaterialProtection;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Calamity mobs, including Sieger, use their own getDestroySpeed threshold. */
@Mixin(targets = "com.Harbinger.Spore.Sentities.BaseEntities.Calamity", remap = false)
public abstract class CalamityMaterialViewMixin {
    @Redirect(method = "grief(Lnet/minecraft/world/phys/AABB;)V", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/world/level/Level;m_8055_(Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/level/block/state/BlockState;", remap = false),
        remap = false, require = 1)
    private BlockState littlespore$effectiveCalamityMaterial(Level level, BlockPos pos) {
        BlockState actual = level.getBlockState(pos);
        return MaterialProtection.effectiveState(level, pos, actual);
    }
}
