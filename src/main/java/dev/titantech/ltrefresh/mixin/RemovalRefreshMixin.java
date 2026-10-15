package dev.titantech.ltrefresh.mixin;
import dev.titantech.ltrefresh.ClientRefresh;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.client.multiplayer.ClientLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;


/** Observe the *completed* client-side change, including updates from other mods. */
@Mixin(LevelChunk.class)
public abstract class RemovalRefreshMixin {
    @Inject(method="m_6978_(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;Z)Lnet/minecraft/world/level/block/state/BlockState;",
            at=@At("RETURN"), remap=false, require=1)
    private void ltrefresh$after(BlockPos pos, BlockState next, boolean moving,
            CallbackInfoReturnable<BlockState> cir) {
        LevelChunk chunk=(LevelChunk)(Object)this;
        if (!(chunk.getLevel() instanceof ClientLevel client)) return;
        BlockState previous=cir.getReturnValue();
        if (previous == null || previous.equals(next)) return;
        ClientRefresh.queueBlockChange(client, pos);
    }
}
