package de.voxelteuton.fluxbattery

import sonar.fluxnetworks.common.connection.TransferHandler

/** Network grants are credited immediately; there is no unload-sensitive intermediary buffer. */
class HostedPointTransferHandler(private val point: HostedFluxPoint) : TransferHandler(point.binding.configuration.transferLimit) {
    private var credited = 0L
    init {
        val c = point.binding.configuration
        setPriority(c.priority)
        setSurgeMode(c.surgeMode)
        setDisableLimit(c.disableTransferLimit)
    }
    override fun onCycleStart() { credited = 0; mChange = 0; mBuffer = 0 }
    override fun getRequest(): Long {
        val stack = point.currentStack() ?: return 0
        if (!FluxBatteryHostManager.permitted(point.network, point.binding.configuration)) return 0
        val energy = BatteryStateService.energy(stack)
        return minOf(energy.capacity - energy.storedEnergy, limit - credited)
    }
    override fun addToBuffer(energy: Long) {
        require(energy >= 0 && energy <= request)
        val stack = checkNotNull(point.currentStack())
        val actual = BatteryStateService.receive(stack, energy, false)
        check(actual == energy) { "Flux grant exceeded the current battery demand" }
        credited += actual
        // Update before listener callbacks: removing the item can never discard a grant.
        mChange = -credited
        point.slot.onContentsChanged()
    }
    override fun onCycleEnd() { mChange = -credited }
}
