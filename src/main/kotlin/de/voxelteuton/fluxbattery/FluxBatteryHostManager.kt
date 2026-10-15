package de.voxelteuton.fluxbattery

import de.voxelteuton.fluxbattery.bridge.*

import mekanism.common.inventory.slot.BasicInventorySlot
import mekanism.common.inventory.slot.EnergyInventorySlot
import mekanism.common.tile.base.TileEntityMekanism
import net.minecraft.world.item.ItemStack
import net.minecraft.server.level.ServerLevel
import net.neoforged.neoforge.server.ServerLifecycleHooks
import sonar.fluxnetworks.common.connection.FluxNetwork
import sonar.fluxnetworks.common.connection.FluxNetworkData
import sonar.fluxnetworks.api.network.SecurityLevel
import java.util.IdentityHashMap

object FluxBatteryHostManager {
    private val hosts = IdentityHashMap<TileEntityMekanism, HostedFluxPoint>()
    private val slots = IdentityHashMap<BasicInventorySlot, HostedFluxPoint>()

    fun permitted(network: FluxNetwork, config: BatteryConfig): Boolean = network.isValid && config.ownerUuid != UNOWNED &&
        (network.securityLevel == SecurityLevel.PUBLIC || network.ownerUUID == config.ownerUuid ||
            network.getMemberByUUID(config.ownerUuid)?.accessLevel?.canUse() == true)

    fun tick(tile: TileEntityMekanism) {
        val level = tile.level as? ServerLevel ?: return
        if (tile.isRemoved || level.getBlockEntity(tile.blockPos) !== tile) { remove(tile); return }
        val selected = MekanismHostAdapter.dischargeSlots(tile).firstOrNull { it.second.stack.item is FluxBatteryItem }
        val old = hosts[tile]
        if (selected == null) { remove(tile); return }
        val (index, slot) = selected
        val stack = slot.stack
        val config = BatteryStateService.config(stack)
        val network = FluxNetworkData.getNetwork(config.networkId)
        val id = BatteryStateService.identity(stack)
        if (!permitted(network, config)) { remove(tile); return }
        if (old != null && old.binding.batteryId == id && old.binding.configuration == config && old.slot === slot && old.active) {
            BatteryChunkTicketController.update(old)
            return
        }
        remove(tile)
        val binding = BatteryHostLifecycle(HostKey(level.server, level.dimension(), tile.blockPos.immutable(), index), id, config)
        val point = HostedFluxPoint(tile, slot, binding, network)
        if (network.registerRuntime(point)) {
            hosts[tile] = point
            slots[slot] = point
            tile.getData(FluxBatteryMod.HOST_ATTACHMENT.get()).point = point
            BatteryChunkTicketController.update(point)
        } else point.active = false
    }

    fun slotChanged(slot: BasicInventorySlot) {
        if (ServerLifecycleHooks.getCurrentServer()?.isSameThread != true) return
        val point = slots[slot] ?: return
        if (point.currentStack() == null) remove(point.tile)
    }
    fun remove(tile: TileEntityMekanism, preserveTicket: Boolean = false) {
        val point = hosts.remove(tile)
        if (point == null) {
            if (!preserveTicket) (tile.level as? ServerLevel)?.let { BatteryChunkTicketController.releaseAt(it, tile.blockPos) }
            return
        }
        slots.remove(point.slot)
        point.close(preserveTicket)
        if (tile.hasData(FluxBatteryMod.HOST_ATTACHMENT.get())) tile.getData(FluxBatteryMod.HOST_ATTACHMENT.get()).point = null
    }
    fun shutdown() { hosts.keys.toList().forEach { remove(it, preserveTicket = true) }; slots.clear() }
}
