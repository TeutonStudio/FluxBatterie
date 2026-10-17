package de.teutonstudio.fluxbattery

import net.minecraft.world.item.ItemStack
import net.neoforged.neoforge.energy.IEnergyStorage
import sonar.fluxnetworks.api.energy.IFNEnergyStorage

class BatteryEnergyHandler(private val stack: ItemStack) : IEnergyStorage, IFNEnergyStorage {
    override fun receiveEnergyL(maxReceive: Long, simulate: Boolean) = BatteryStateService.receive(stack, maxReceive, simulate)
    override fun extractEnergyL(maxExtract: Long, simulate: Boolean) = BatteryStateService.extract(stack, maxExtract, simulate)
    override fun getEnergyStoredL() = BatteryStateService.energy(stack).storedEnergy
    override fun getMaxEnergyStoredL() = BatteryStateService.energy(stack).capacity
    override fun receiveEnergy(maxReceive: Int, simulate: Boolean) = receiveEnergyL(maxReceive.toLong(), simulate).toInt()
    override fun extractEnergy(maxExtract: Int, simulate: Boolean) = extractEnergyL(maxExtract.toLong(), simulate).toInt()
    override fun getEnergyStored() = minOf(energyStoredL, Int.MAX_VALUE.toLong()).toInt()
    override fun getMaxEnergyStored() = minOf(maxEnergyStoredL, Int.MAX_VALUE.toLong()).toInt()
    override fun canExtract() = true
    override fun canReceive() = true
}
