package de.voxelteuton.fluxbattery.mixin

import de.voxelteuton.fluxbattery.*
import mekanism.api.AutomationType
import mekanism.common.inventory.slot.BasicInventorySlot
import net.minecraft.world.item.ItemStack
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.injection.At
import org.spongepowered.asm.mixin.injection.Inject
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable

@Mixin(value = [BasicInventorySlot::class], remap = false)
abstract class BasicSlotMixin {
    @Inject(method = ["isItemValidForInsertion"], at = [At("HEAD")], cancellable = true, remap = false)
    private fun allowConfiguredBattery(stack: ItemStack, automation: AutomationType, ci: CallbackInfoReturnable<Boolean>) {
        if ((this as? BatteryDischargeSlot)?.fluxBatteryDischargeSlot == true && stack.item is FluxBatteryItem) {
            val config = BatteryStateService.config(stack)
            if (config.networkId > 0 && config.ownerUuid != UNOWNED) ci.returnValue = true
        }
    }
    @Inject(method = ["onContentsChanged"], at = [At("TAIL")], remap = false)
    private fun batteryContentsChanged(ci: CallbackInfo) {
        FluxBatteryHostManager.slotChanged(this as Any as BasicInventorySlot)
    }
}
