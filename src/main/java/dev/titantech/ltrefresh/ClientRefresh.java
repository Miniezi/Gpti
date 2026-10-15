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

/**
 * Recalculates actual LittleTiles occluded faces after a neighbour block is
 * removed/placed, then invalidates the custom render cache and vanilla chunks.
 * Does not mutate world blocks, tile geometry or server data.
 */
@Mod.EventBusSubscriber(modid="littletilesrenderrefresh",value=Dist.CLIENT,bus=Mod.EventBusSubscriber.Bus.FORGE)
public final class ClientRefresh {
    private static ClientLevel current;
    private static final Set<BlockPos> PENDING = new LinkedHashSet<>();
    private static final int MAX_PER_TICK = 48;

    private ClientRefresh() {}

    public static void queueBlockChange(ClientLevel level, BlockPos pos) {
        if (current != level) { current=level; PENDING.clear(); }
        // Changes outside of loaded nearby LittleTiles are cheap to discard.
        if (PENDING.size() < 2048) PENDING.add(pos.immutable());
    }

    @SubscribeEvent
    public static void tick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.level != current) {
            PENDING.clear(); current=mc.level; return;
        }
        int count = 0;
        var it = PENDING.iterator();
        while (it.hasNext() && count++ < MAX_PER_TICK) {
            BlockPos pos=it.next();
            it.remove();
            boolean any = refresh(mc,pos);
            for (Direction dir : Direction.values()) any |= refresh(mc,pos.relative(dir));
            if (any) mc.levelRenderer.setBlocksDirty(pos.getX()-2,pos.getY()-2,pos.getZ()-2,
                pos.getX()+2,pos.getY()+2,pos.getZ()+2);
        }
    }

    private static boolean refresh(Minecraft mc, BlockPos pos) {
        if (!(mc.level.getBlockEntity(pos) instanceof BETiles be) || be.isRemoved()) return false;
        try {
            be.rebuildFaces();
            if (be.render != null) be.render.tilesChanged();
            return true;
        } catch (RuntimeException ex) {
            org.apache.logging.log4j.LogManager.getLogger("LittleTilesRenderRefresh")
                .warn("Unable to rebuild faces at {}: {}",pos,ex.toString());
            return false;
        }
    }
}
