package de.teutonstudio.fluxbattery

import mekanism.common.inventory.slot.EnergyInventorySlot
import mekanism.common.tile.base.TileEntityMekanism
import sonar.fluxnetworks.api.device.FluxDeviceType
import sonar.fluxnetworks.api.device.IFluxPoint
import sonar.fluxnetworks.common.connection.FluxNetwork

class HostedFluxPoint(
    tile: TileEntityMekanism,
    slot: EnergyInventorySlot,
    binding: BatteryHostLifecycle,
    network: FluxNetwork
) : HostedFluxDevice(tile, slot, binding, network), IFluxPoint {
    override val transferHandler = HostedPointTransferHandler(this)
    override fun getDeviceType() = FluxDeviceType.POINT
}
