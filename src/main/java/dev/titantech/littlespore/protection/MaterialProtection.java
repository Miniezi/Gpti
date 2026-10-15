package dev.titantech.littlespore.protection;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import team.creative.littletiles.common.block.entity.BETiles;
import team.creative.littletiles.common.block.little.tile.LittleTile;
import team.creative.littletiles.common.block.little.tile.parent.IParentCollection;
import team.creative.littletiles.common.block.mc.BlockTile;

/**
 * Supplies Spore's existing decision code with the strongest real BlockState
 * in a LittleTiles container. This never mutates or replaces actual world data.
 */
public final class MaterialProtection {
    private MaterialProtection() {}

    public static BlockState effectiveState(Level level, BlockPos pos, BlockState original) {
        if (!(original.getBlock() instanceof BlockTile)) return original;
        if (!(level.getBlockEntity(pos) instanceof BETiles be)) return original;
        BlockState strongest = null;
        float maximum = -Float.MAX_VALUE;
        try {
            for (IParentCollection group : be.groups()) {
                for (LittleTile tile : group) {
                    BlockState material = tile.getState();
                    if (material == null) continue;
                    float hardness = material.getDestroySpeed(level, pos);
                    // An unbreakable constituent makes the entire container unbreakable.
                    if (hardness < 0.0F) return material;
                    if (!Float.isFinite(hardness)) continue;
                    if (strongest == null || hardness > maximum) {
                        strongest = material;
                        maximum = hardness;
                    }
                }
            }
        } catch (RuntimeException ex) {
            org.apache.logging.log4j.LogManager.getLogger("LittleTilesSporeFix")
                    .warn("Unable to read LittleTiles materials at {}: {}", pos, ex.toString());
            return original;
        }
        return strongest != null ? strongest : original;
    }

    public static boolean isUnbreakable(Level level, BlockPos pos) {
        BlockState original = level.getBlockState(pos);
        if (!(original.getBlock() instanceof BlockTile)) return false;
        BlockState effective = effectiveState(level, pos, original);
        return effective != original && effective.getDestroySpeed(level, pos) < 0.0F;
    }
}
