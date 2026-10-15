package de.voxelteuton.fluxbattery.bridge

import sonar.fluxnetworks.common.connection.FluxNetwork

/** Queue lifecycle guards only. Energy distribution stays entirely in ServerFluxNetwork. */
object RuntimeHooks {
    @JvmStatic
    fun canAdd(network: FluxNetwork, device: FluxRuntimeDevice): Boolean {
        val access = network as ServerRuntimeAccess
        if (access.fluxBatteryDeleted()) return false
        val existing = access.fluxBatteryConnections()[device.globalPos]
        return existing !is FluxRuntimeDevice || existing === device
    }

    @JvmStatic
    fun cancelPending(network: FluxNetwork, device: FluxRuntimeDevice): Boolean {
        val access = network as ServerRuntimeAccess
        if (!access.fluxBatteryPending().remove(device) || network.runtimeDevices(FluxNetwork.ANY).contains(device)) return false
        access.fluxBatteryConnections().remove(device.globalPos, device)
        return true
    }

    @JvmStatic
    fun delete(network: FluxNetwork) {
        val access = network as ServerRuntimeAccess
        access.fluxBatterySetDeleted(true)
        val participants = LinkedHashSet(network.runtimeDevices(FluxNetwork.ANY))
        participants.addAll(access.fluxBatteryPending())
        participants.forEach { it.disconnect() }
        for (logic in FluxNetwork.ANY..FluxNetwork.CONTROLLER) network.runtimeDevices(logic).let { (it as MutableList).clear() }
        access.fluxBatteryPending().clear()
        access.fluxBatteryRemovals().clear()
        access.fluxBatteryConnections().clear()
    }

    @JvmStatic
    fun addNonNegative(total: Long, amount: Long): Long =
        if (amount <= 0) total else if (amount > Long.MAX_VALUE - total) Long.MAX_VALUE else total + amount
}
