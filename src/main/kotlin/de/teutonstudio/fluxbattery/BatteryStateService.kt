package de.teutonstudio.fluxbattery

import net.minecraft.world.item.ItemStack
import java.util.UUID

/** All component writes happen here. Capabilities never cache energy values. */
object BatteryStateService {
    fun energy(stack: ItemStack): BatteryEnergy = stack.get(BatteryComponents.ENERGY.get()) ?: BatteryEnergy(0, BatteryServerConfig.capacity.get())
    fun config(stack: ItemStack): BatteryConfig = stack.get(BatteryComponents.CONFIG.get()) ?: BatteryConfig()
    fun identity(stack: ItemStack): UUID {
        return stack.get(BatteryComponents.IDENTITY.get()) ?: UUID.randomUUID().also {
            stack.set(BatteryComponents.IDENTITY.get(), it)
            if (!stack.has(BatteryComponents.ENERGY.get())) stack.set(BatteryComponents.ENERGY.get(), energy(stack))
        }
    }
    fun setConfig(stack: ItemStack, value: BatteryConfig) {
        require(value.networkId >= 0 && value.priority in -9999..9999 && value.transferLimit >= 0 && value.customName.length <= 24)
        identity(stack)
        stack.set(BatteryComponents.CONFIG.get(), value)
    }
    fun receive(stack: ItemStack, requested: Long, simulate: Boolean): Long {
        if (stack.isEmpty || requested <= 0) return 0
        val state = energy(stack)
        val accepted = EnergyMath.receive(state.storedEnergy, state.capacity, requested)
        if (!simulate && accepted > 0) stack.set(BatteryComponents.ENERGY.get(), state.copy(storedEnergy = state.storedEnergy + accepted))
        return accepted
    }
    fun extract(stack: ItemStack, requested: Long, simulate: Boolean): Long {
        if (stack.isEmpty || requested <= 0) return 0
        val state = energy(stack)
        val extracted = EnergyMath.extract(state.storedEnergy, requested)
        if (!simulate && extracted > 0) stack.set(BatteryComponents.ENERGY.get(), state.copy(storedEnergy = state.storedEnergy - extracted))
        return extracted
    }
}

/** Subtraction before addition prevents overflow even at Long.MAX_VALUE. */
object EnergyMath {
    fun receive(stored: Long, capacity: Long, requested: Long): Long {
        require(capacity >= 0 && stored in 0..capacity)
        return minOf(requested.coerceAtLeast(0), capacity - stored)
    }
    fun extract(stored: Long, requested: Long): Long {
        require(stored >= 0)
        return minOf(stored, requested.coerceAtLeast(0))
    }
}
