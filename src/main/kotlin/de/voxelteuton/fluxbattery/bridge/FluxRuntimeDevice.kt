package de.voxelteuton.fluxbattery.bridge

import sonar.fluxnetworks.api.device.IFluxDevice
import sonar.fluxnetworks.common.connection.TransferHandler

/** A loaded Flux participant, whether a real Flux block or a hosted battery. */
interface FluxRuntimeDevice : IFluxDevice {
    val transferHandler: TransferHandler
    fun markEnergyChanged()
    fun disconnect()
    fun sendBlockUpdate()
}
