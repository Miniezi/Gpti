package dev.titantech.littlespore.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import dev.titantech.littlespore.protection.MaterialProtection;

/**
 * Secondary protection for Spore entity AI that calls Level.destroyBlock/setBlock
 * without consulting LittleTiles material hardness.
 *
 * Deliberately protects only LittleTiles and only when the current call stack
 * belongs to Fungal Infection Spore, keeping player edits unaffected.
 */
@Mixin(value = Level.class, priority = 900)
public abstract class SporeLevelBreakGuardMixin {
    private static final StackWalker SPORE_STACK = StackWalker.getInstance();

    private static boolean sporeCall() {
        return SPORE_STACK.walk(frames -> frames.anyMatch(f ->
                f.getClassName().startsWith("com.Harbinger.Spore.")));
    }

    private boolean protect(BlockPos pos) {
        Level level = (Level)(Object)this;
        if (level.isClientSide() || !sporeCall()) return false;
        return MaterialProtection.isUnbreakable(level, pos);
    }

    @Inject(method = "m_7471_(Lnet/minecraft/core/BlockPos;Z)Z",
            at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private void littlespore$guardDestroy(BlockPos pos, boolean drop,
            CallbackInfoReturnable<Boolean> cir) {
        if (protect(pos)) cir.setReturnValue(false);
    }

    @Inject(method = "m_7731_(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;I)Z",
            at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private void littlespore$guardSet(BlockPos pos, BlockState replacement, int flags,
            CallbackInfoReturnable<Boolean> cir) {
        if (protect(pos)) cir.setReturnValue(false);
    }
}
