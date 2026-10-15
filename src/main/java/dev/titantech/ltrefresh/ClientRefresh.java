package dev.titantech.ltrefresh;

import java.util.LinkedHashSet;
import java.util.Set;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import team.creative.littletiles.common.block.entity.BETiles;

/** Invalidates both the LittleTiles tile mesh and vanilla section mesh after real client updates. */
@Mod.EventBusSubscriber(modid="littletilesrenderrefresh", value=Dist.CLIENT, bus=Mod.EventBusSubscriber.Bus.FORGE)
public final class ClientRefresh {
    private static ClientLevel current;
    private static final Set<BlockPos> PENDING = new LinkedHashSet<>();
    private static final int MAX_PER_TICK = 64;
    private ClientRefresh() {}

    public static void queueBlockChange(ClientLevel level, BlockPos pos) {
        if (current != level) { current = level; PENDING.clear(); }
        if (PENDING.size() < 4096) PENDING.add(pos.immutable());
    }

    @SubscribeEvent
    public static void tick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.level != current) { PENDING.clear(); current = mc.level; return; }
        int processed = 0;
        var it = PENDING.iterator();
        while (it.hasNext() && processed++ < MAX_PER_TICK) {
            BlockPos pos = it.next();
            it.remove();
            // Neighbours may have cached hidden faces that need to be exposed.
            refresh(mc, pos);
            for (Direction dir : Direction.values()) refresh(mc, pos.relative(dir));
            mc.levelRenderer.setBlocksDirty(pos.getX()-2,pos.getY()-2,pos.getZ()-2,
                    pos.getX()+2,pos.getY()+2,pos.getZ()+2);
        }
    }

    private static void refresh(Minecraft mc, BlockPos pos) {
        if (!(mc.level.getBlockEntity(pos) instanceof BETiles be)) return;
        if (be.isRemoved() || be.render == null) return;
        be.render.tilesChanged();
    }
}
