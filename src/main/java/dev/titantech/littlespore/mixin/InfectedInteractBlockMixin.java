package dev.titantech.littlespore.mixin;

import dev.titantech.littlespore.protection.MaterialProtection;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import org.apache.logging.log4j.LogManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Denies infected mob destruction before Spore calls destroyBlock. */
@Mixin(targets = "com.Harbinger.Spore.Sentities.BaseEntities.Infected", remap = false)
public abstract class InfectedInteractBlockMixin {
    @Inject(method = "interactBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/Level;)Z",
            at = @At("HEAD"), cancellable = true, remap = false, require = 1)
    private void littlespore$preventHardTileDestruction(BlockPos pos, Level level,
            CallbackInfoReturnable<Boolean> cir) {
        if (level.isClientSide()) return;
        if (!MaterialProtection.isUnbreakable(level, pos)) return;
        LogManager.getLogger("LittleTilesSporeFix")
            .info("BLOCKED Spore interaction BEFORE breaking LittleTiles at {}", pos);
        cir.setReturnValue(false);
    }
}
