package de.voxelteuton.fluxbattery

import net.minecraft.core.BlockPos
import net.minecraft.resources.ResourceKey
import net.minecraft.server.MinecraftServer
import net.minecraft.world.level.Level
import java.util.UUID

/** Runtime only. Item components are the sole persistent configuration and energy. */
class FluxPointAttachment {
    var point: HostedFluxPoint? = null
}

data class HostKey(val server: MinecraftServer, val dimension: ResourceKey<Level>, val position: BlockPos, val slotIndex: Int)

data class BatteryHostLifecycle(val key: HostKey, val batteryId: UUID, val configuration: BatteryConfig)
