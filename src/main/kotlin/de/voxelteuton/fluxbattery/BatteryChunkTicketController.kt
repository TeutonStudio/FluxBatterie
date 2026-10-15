package de.voxelteuton.fluxbattery

import mekanism.common.tile.base.TileEntityMekanism
import net.minecraft.core.GlobalPos
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.level.ChunkPos
import net.neoforged.neoforge.common.world.chunk.TicketController
import sonar.fluxnetworks.FluxConfig
import sonar.fluxnetworks.common.connection.FluxNetworkData
import java.util.UUID

object BatteryChunkTicketController {
    private val owners = mutableMapOf<GlobalPos, UUID>()
    val CONTROLLER = TicketController(ResourceLocation.fromNamespaceAndPath(FluxBatteryMod.ID, "battery")) { level, helper ->
        for (pos in helper.blockTickets.keys.toList()) {
            val tile = level.getBlockEntity(pos) as? TileEntityMekanism
            val slot = tile?.let { MekanismHostAdapter.dischargeSlots(it).firstOrNull { (_, s) -> s.stack.item is FluxBatteryItem }?.second }
            val config = slot?.let { BatteryStateService.config(it.stack) }
            val key = GlobalPos.of(level.dimension(), pos)
            val pair = helper.blockTickets[pos]!!
            val chunk = ChunkPos(pos).toLong()
            val validShape = pair.nonTicking().isEmpty() && pair.ticking().size == 1 && pair.ticking().contains(chunk)
            if (config == null || !allowed(config) || !validShape || !FluxBatteryHostManager.permitted(FluxNetworkData.getNetwork(config.networkId), config) || !quota(key, config.ownerUuid)) {
                helper.removeAllTickets(pos)
                owners.remove(key)
            } else owners[key] = config.ownerUuid
        }
    }
    private fun allowed(config: BatteryConfig) = BatteryServerConfig.chunkLoading.get() && FluxConfig.enableChunkLoading && config.chunkLoadingEnabled
    private fun quota(key: GlobalPos, owner: UUID) = owners[key] == owner || owners.values.count { it == owner } < BatteryServerConfig.ticketQuota.get()
    fun update(point: HostedFluxPoint) {
        val config = point.binding.configuration
        val valid = point.currentStack() != null && allowed(config) && FluxBatteryHostManager.permitted(point.network, config) && quota(point.globalPos, config.ownerUuid)
        if (!valid) { release(point); return }
        if (owners[point.globalPos] == config.ownerUuid) point.ticket = true
        if (!point.ticket) {
            val level = point.tile.level as ServerLevel
            val pos = point.binding.key.position
            if (CONTROLLER.forceChunk(level, pos, pos.x shr 4, pos.z shr 4, true, true)) {
                point.ticket = true
                owners[point.globalPos] = config.ownerUuid
            }
        }
    }
    fun release(point: HostedFluxPoint) {
        if (!point.ticket && !owners.containsKey(point.globalPos)) return
        val level = point.tile.level as? ServerLevel ?: return
        val pos = point.binding.key.position
        CONTROLLER.forceChunk(level, pos, pos.x shr 4, pos.z shr 4, false, true)
        owners.remove(point.globalPos)
        point.ticket = false
    }
    fun releaseAt(level: ServerLevel, pos: net.minecraft.core.BlockPos) {
        val key = GlobalPos.of(level.dimension(), pos)
        if (owners.remove(key) != null) CONTROLLER.forceChunk(level, pos, pos.x shr 4, pos.z shr 4, false, true)
    }
    fun clear() { owners.clear() }
}
