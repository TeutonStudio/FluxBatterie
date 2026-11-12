package de.teutonstudio.fluxbattery

import mekanism.common.inventory.slot.EnergyInventorySlot
import mekanism.common.tile.base.TileEntityMekanism

/** Energy flow is named from the battery's perspective. */
interface BatteryDischargeSlot {
    var fluxBatteryDischargeSlot: Boolean
    var fluxBatteryChargeSlot: Boolean
}

object MekanismHostAdapter {
    fun energySlots(tile: TileEntityMekanism): List<Pair<Int, EnergyInventorySlot>> =
        (if (tile is mekanism.common.lib.multiblock.IMultiblock<*>) emptyList() else tile.getInventorySlots(null)).mapIndexedNotNull { index, slot ->
            val direction = slot as? BatteryDischargeSlot
            if (slot is EnergyInventorySlot && direction != null && (direction.fluxBatteryDischargeSlot || direction.fluxBatteryChargeSlot)) index to slot else null
        }

    fun dischargeSlots(tile: TileEntityMekanism): List<Pair<Int, EnergyInventorySlot>> =
        energySlots(tile).filter { (_, slot) -> (slot as BatteryDischargeSlot).fluxBatteryDischargeSlot }

    fun chargeSlots(tile: TileEntityMekanism): List<Pair<Int, EnergyInventorySlot>> =
        energySlots(tile).filter { (_, slot) -> (slot as BatteryDischargeSlot).fluxBatteryChargeSlot }
}
