package de.teutonstudio.fluxbattery

import sonar.fluxnetworks.common.connection.TransferHandler

/** Overflow belongs to the item until Flux's normal distribution actually takes it. */
class HostedPlugTransferHandler(private val plug: HostedFluxPlug) : TransferHandler(plug.binding.configuration.transferLimit) {
    private var received = 0L
    private var removed = 0L

    init {
        val config = plug.binding.configuration
        setPriority(config.priority)
        setSurgeMode(config.surgeMode)
        setDisableLimit(config.disableTransferLimit)
        mBuffer = plug.currentStack()?.let(BatteryStateService::networkInput) ?: 0L
    }

    private fun currentStack() = plug.currentStack()?.takeIf {
        FluxBatteryHostManager.permitted(plug.network, plug.binding.configuration)
    }

    fun receive(requested: Long, simulate: Boolean): Long {
        val stack = currentStack() ?: return 0
        if (requested <= 0) return 0
        val pending = BatteryStateService.networkInput(stack)
        val accepted = minOf(requested, (limit - received).coerceAtLeast(0),
            (plug.network.bufferLimiter - pending).coerceAtLeast(0), Long.MAX_VALUE - pending)
        if (!simulate && accepted > 0) {
            check(BatteryStateService.receiveNetworkInput(stack, accepted, false) == accepted)
            received += accepted
            mBuffer = pending + accepted
            plug.slot.onContentsChanged()
        }
        return accepted
    }

    override fun onCycleStart() {
        mBuffer = currentStack()?.let(BatteryStateService::networkInput) ?: 0L
    }

    override fun removeFromBuffer(energy: Long): Long {
        val stack = currentStack() ?: return 0
        val actual = minOf(energy.coerceAtLeast(0), BatteryStateService.networkInput(stack), (limit - removed).coerceAtLeast(0))
        if (actual > 0) {
            check(BatteryStateService.extractNetworkInput(stack, actual) == actual)
            removed += actual
            mBuffer = BatteryStateService.networkInput(stack)
            plug.slot.onContentsChanged()
        }
        return actual
    }

    override fun onCycleEnd() {
        mChange = received
        received = 0
        removed = 0
    }
}
