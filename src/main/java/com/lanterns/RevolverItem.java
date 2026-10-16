package com.lanterns;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;

import java.util.List;

/** The Sheriff's last-stand revolver. Right-click fires; the server decides whether and what it hits. */
public class RevolverItem extends Item {
    public RevolverItem(Settings settings) { super(settings); }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        ItemStack stack = user.getStackInHand(hand);
        if (!world.isClient && user instanceof ServerPlayerEntity sp && LanternsMod.game() != null) {
            LanternsMod.game().onGunUse(sp);
        }
        return TypedActionResult.success(stack);
    }

    @Override
    public void appendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip, TooltipType type) {
        tooltip.add(Text.translatable("item.lanterns.revolver.tooltip1").formatted(Formatting.GOLD));
        tooltip.add(Text.translatable("item.lanterns.revolver.tooltip2").formatted(Formatting.GREEN));
        tooltip.add(Text.translatable("item.lanterns.revolver.tooltip3").formatted(Formatting.RED));
    }
}
