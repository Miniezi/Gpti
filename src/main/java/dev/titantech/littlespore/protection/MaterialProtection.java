package dev.titantech.littlespore.protection;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import team.creative.littletiles.common.block.entity.BETiles;
import team.creative.littletiles.common.block.little.tile.LittleTile;
import team.creative.littletiles.common.block.little.tile.parent.IParentCollection;
import team.creative.littletiles.common.block.mc.BlockTile;

/** Reads real LittleTiles constituent materials without mutating the block. */
public final class MaterialProtection {
    private MaterialProtection() {}

    public static boolean shouldBlockSpore(Level level, BlockPos pos) {
        if (!(level.getBlockState(pos).getBlock() instanceof BlockTile)) return false;
        if (!(level.getBlockEntity(pos) instanceof BETiles be)) return false;
        try {
            for (IParentCollection group : be.groups()) {
                for (LittleTile tile : group) {
                    BlockState material = tile.getState();
                    if (material == null) continue;
                    String id = BuiltInRegistries.BLOCK.getKey(material.getBlock()).toString();
                    if (id.startsWith("minecraft:polished_deepslate") ||
                        id.startsWith("minecraft:deepslate_brick") ||
                        id.startsWith("minecraft:cracked_deepslate_brick") ||
                        id.startsWith("minecraft:deepslate_tile") ||
                        id.startsWith("minecraft:cracked_deepslate_tile") ||
                        id.equals("minecraft:chiseled_deepslate")) return true;
                    float hardness = material.getDestroySpeed(level, pos);
                    if (hardness < 0.0F || hardness > 5.0F) return true;
                }
            }
        } catch (RuntimeException e) {
            org.apache.logging.log4j.LogManager.getLogger("LittleTilesSporeFix")
                .warn("Could not inspect materials at {}: {}", pos, e.toString());
        }
        return false;
    }
}
