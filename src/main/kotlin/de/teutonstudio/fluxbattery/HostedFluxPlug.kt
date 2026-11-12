package de.teutonstudio.fluxbattery

import mekanism.common.inventory.slot.EnergyInventorySlot
import mekanism.common.tile.base.TileEntityMekanism
import sonar.fluxnetworks.api.device.FluxDeviceType
import sonar.fluxnetworks.api.device.IFluxPlug
import sonar.fluxnetworks.common.connection.FluxNetwork

class HostedFluxPlug(
    tile: TileEntityMekanism,
    slot: EnergyInventorySlot,
    binding: BatteryHostLifecycle,
    network: FluxNetwork
) : HostedFluxDevice(tile, slot, binding, network), IFluxPlug {
    override val transferHandler = HostedPlugTransferHandler(this)
    override fun getDeviceType() = FluxDeviceType.PLUG
}
