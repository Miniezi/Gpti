package dev.titantech.ltrefresh;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import team.creative.littletiles.common.block.entity.BETiles;

/**
 * Periodically invalidates rendering caches for nearby visible LittleTiles block
 * entities to recover from missed or delayed geometry rebuilds. Only the client
 * render manager is affected; world state, blockstates and tile data are untouched.
 */
@Mod.EventBusSubscriber(modid = "littletilesrenderrefresh", value = Dist.CLIENT,
        bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class ClientRefresh {
    private static final int RESCAN_INTERVAL = 100; // five seconds
    private static final int MAX_INVALIDATIONS_PER_TICK = 3;
    private static final int CHUNK_RADIUS = 1;
    private static final double MAX_DISTANCE_SQ = 48.0 * 48.0;

    private static final List<BETiles> pending = new ArrayList<>();
    private static ClientLevel trackedLevel;
    private static int tickCounter;
    private static int index;

    private ClientRefresh() {}

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null || mc.isPaused()) {
            pending.clear();
            trackedLevel = null;
            index = 0;
            tickCounter = 0;
            return;
        }
        ClientLevel level = mc.level;
        if (trackedLevel != level) {
            pending.clear();
            trackedLevel = level;
            index = 0;
            tickCounter = RESCAN_INTERVAL;
        }

        if (++tickCounter >= RESCAN_INTERVAL) {
            tickCounter = 0;
            pending.clear();
            index = 0;
            BlockPos center = mc.player.blockPosition();
            int cx = center.getX() >> 4;
            int cz = center.getZ() >> 4;
            for (int x = cx - CHUNK_RADIUS; x <= cx + CHUNK_RADIUS; x++) {
                for (int z = cz - CHUNK_RADIUS; z <= cz + CHUNK_RADIUS; z++) {
                    if (!level.getChunkSource().hasChunk(x, z)) continue;
                    LevelChunk chunk = level.getChunk(x, z);
                    for (BlockEntity entity : chunk.getBlockEntities().values()) {
                        if (!(entity instanceof BETiles be) || be.isRemoved() || be.render == null) continue;
                        BlockPos p = be.getBlockPos();
                        if (p.distSqr(center) <= MAX_DISTANCE_SQ) pending.add(be);
                    }
                }
            }
        }
        for (int done = 0; index < pending.size() && done < MAX_INVALIDATIONS_PER_TICK; index++, done++) {
            BETiles be = pending.get(index);
            if (be.isRemoved() || be.getLevel() != level || be.render == null || be.render.isInQueue()) continue;
            be.render.tilesChanged();
        }
    }
}
