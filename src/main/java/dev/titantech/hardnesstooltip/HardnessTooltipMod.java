package dev.titantech.hardnesstooltip;

import java.util.Locale;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.fml.common.Mod;

/** Client-side tooltip for vanilla and modded BlockItems. */
@Mod("blockhardnesstooltip")
public class HardnessTooltipMod {
    public HardnessTooltipMod() {
        MinecraftForge.EVENT_BUS.addListener(this::onTooltip);
    }

    private void onTooltip(ItemTooltipEvent event) {
        ItemStack stack = event.getItemStack();
        if (!(stack.getItem() instanceof BlockItem item)) return;
        if (!Screen.hasShiftDown()) {
            event.getToolTip().add(Component.literal("Shift: показать твёрдость").withStyle(ChatFormatting.DARK_GRAY));
            return;
        }
        BlockState state = item.getBlock().defaultBlockState();
        try {
            // Item tooltip has no placed block position or block entity.
            float hardness = state.getDestroySpeed(null, BlockPos.ZERO);
            String value = hardness < 0 ? "неразрушимый (-1)" :
                    Float.isFinite(hardness) ? String.format(Locale.ROOT, "%.2f", hardness) : "недоступно";
            event.getToolTip().add(Component.literal("Твёрдость блока: " + value).withStyle(ChatFormatting.AQUA));
        } catch (RuntimeException | LinkageError ex) {
            event.getToolTip().add(Component.literal("Твёрдость: зависит от мира или состояния блока").withStyle(ChatFormatting.YELLOW));
        }
    }
}
