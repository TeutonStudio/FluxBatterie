package de.voxelteuton.fluxbattery

import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.TooltipFlag
import net.minecraft.network.chat.Component

class FluxBatteryItem(properties: Properties) : Item(properties) {
    override fun use(level: net.minecraft.world.level.Level, player: net.minecraft.world.entity.player.Player, hand: net.minecraft.world.InteractionHand): net.minecraft.world.InteractionResultHolder<ItemStack> {
        val stack = player.getItemInHand(hand)
        if (!player.isShiftKeyDown) return net.minecraft.world.InteractionResultHolder.pass(stack)
        if (player is net.minecraft.server.level.ServerPlayer) BatteryNetworking.open(player, hand)
        return net.minecraft.world.InteractionResultHolder.sidedSuccess(stack, level.isClientSide)
    }

    override fun appendHoverText(stack: ItemStack, context: TooltipContext, lines: MutableList<Component>, flag: TooltipFlag) {
        val energy = BatteryStateService.energy(stack)
        lines.add(Component.literal("${energy.storedEnergy} / ${energy.capacity} FE"))
        lines.add(Component.literal("Flux network: ${BatteryStateService.config(stack).networkId}"))
    }
}
