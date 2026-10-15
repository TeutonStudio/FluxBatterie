package de.voxelteuton.fluxbattery.bridge

import net.minecraft.core.GlobalPos
import sonar.fluxnetworks.api.device.IFluxDevice
import sonar.fluxnetworks.common.connection.FluxNetwork
import java.util.LinkedList

/** Implemented on the original FluxNetwork before class definition. Tile overloads remain. */
interface FluxRuntimeNetwork {
    fun enqueueConnectionAddition(device: FluxRuntimeDevice): Boolean
    fun enqueueConnectionRemoval(device: FluxRuntimeDevice, unload: Boolean)
}

/** Narrow access to Flux's own lifecycle queues; no parallel participant registry. */
interface ServerRuntimeAccess {
    fun fluxBatteryPending(): LinkedList<FluxRuntimeDevice>
    fun fluxBatteryRemovals(): LinkedList<FluxRuntimeDevice>
    fun fluxBatteryConnections(): MutableMap<GlobalPos, IFluxDevice>
    fun fluxBatteryDeleted(): Boolean
    fun fluxBatterySetDeleted(value: Boolean)
}

@Suppress("UNCHECKED_CAST")
fun FluxNetwork.runtimeDevices(logic: Int): List<FluxRuntimeDevice> = getLogicalDevices(logic) as List<FluxRuntimeDevice>
fun FluxNetwork.registerRuntime(device: FluxRuntimeDevice): Boolean = (this as FluxRuntimeNetwork).enqueueConnectionAddition(device)
fun FluxNetwork.unregisterRuntime(device: FluxRuntimeDevice, unload: Boolean) = (this as FluxRuntimeNetwork).enqueueConnectionRemoval(device, unload)
