package de.voxelteuton.fluxbattery.compat

import sonar.fluxnetworks.common.connection.FluxNetwork
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.Unique

/** Small coexistence probe: Mixin and the class transformer both extend the original. */
@Mixin(value = [FluxNetwork::class], remap = false)
abstract class FluxLoadOrderMixin {
    @Unique
    fun fluxBatteryMixinProbe(): Int = 42
}
