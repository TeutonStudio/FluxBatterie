package de.voxelteuton.fluxbattery.bridge

/** ABI available without a Kotlin object, reflection or KFF initialization. */
interface LoadTimePatched {
    fun fluxBatteryPatchVersion(): Int
}
