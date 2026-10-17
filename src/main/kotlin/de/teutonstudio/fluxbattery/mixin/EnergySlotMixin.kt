package de.teutonstudio.fluxbattery.mixin

import de.teutonstudio.fluxbattery.BatteryDischargeSlot
import mekanism.common.inventory.slot.EnergyInventorySlot
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.Unique
import org.spongepowered.asm.mixin.injection.At
import org.spongepowered.asm.mixin.injection.Inject
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable

@Mixin(value = [EnergyInventorySlot::class], remap = false)
abstract class EnergySlotMixin : BatteryDischargeSlot {
    @Unique
    override var fluxBatteryDischargeSlot: Boolean = false
    private companion object {
        @JvmStatic
        @Inject(method = ["fill", "fillOrConvert"], at = [At("RETURN")], remap = false)
        private fun markDischargeSlot(ci: CallbackInfoReturnable<EnergyInventorySlot>) {
            (ci.returnValue as BatteryDischargeSlot).fluxBatteryDischargeSlot = true
        }
    }
}
