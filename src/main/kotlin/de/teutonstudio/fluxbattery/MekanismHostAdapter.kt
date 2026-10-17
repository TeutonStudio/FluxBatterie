package de.teutonstudio.fluxbattery

import mekanism.common.inventory.slot.EnergyInventorySlot
import mekanism.common.tile.base.TileEntityMekanism

/** Marked only by the fill/fillOrConvert factories, never by drain. */
interface BatteryDischargeSlot {
    var fluxBatteryDischargeSlot: Boolean
}

object MekanismHostAdapter {
    fun dischargeSlots(tile: TileEntityMekanism): List<Pair<Int, EnergyInventorySlot>> =
        (if (tile is mekanism.common.lib.multiblock.IMultiblock<*>) emptyList() else tile.getInventorySlots(null)).mapIndexedNotNull { index, slot ->
            if (slot is EnergyInventorySlot && (slot as? BatteryDischargeSlot)?.fluxBatteryDischargeSlot == true) index to slot else null
        }
}
