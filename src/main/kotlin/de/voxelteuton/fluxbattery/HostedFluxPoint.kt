package de.voxelteuton.fluxbattery

import mekanism.common.inventory.slot.EnergyInventorySlot
import mekanism.common.tile.base.TileEntityMekanism
import net.minecraft.core.GlobalPos
import net.minecraft.nbt.CompoundTag
import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import net.minecraft.world.inventory.AbstractContainerMenu
import sonar.fluxnetworks.api.FluxConstants
import sonar.fluxnetworks.api.device.*
import sonar.fluxnetworks.common.connection.FluxNetwork

class HostedFluxPoint(
    val tile: TileEntityMekanism,
    val slot: EnergyInventorySlot,
    val binding: BatteryHostLifecycle,
    val network: FluxNetwork
) : IFluxRuntimeDevice, IFluxPoint {
    var active = true
    var ticket = false
    private val handler = HostedPointTransferHandler(this)
    fun currentStack(): ItemStack? {
        if (!active || tile.isRemoved || tile.level?.getBlockEntity(tile.blockPos) !== tile) return null
        val stack = slot.stack
        return stack.takeIf { it.item is FluxBatteryItem && it.get(BatteryComponents.IDENTITY.get()) == binding.batteryId && BatteryStateService.config(it) == binding.configuration }
    }
    fun close(preserveTicket: Boolean = false) {
        if (!active) return
        active = false
        if (!preserveTicket) BatteryChunkTicketController.release(this)
        network.enqueueConnectionRemoval(this, false)
    }
    override fun disconnect() { FluxBatteryHostManager.remove(tile) }
    override fun getTransferHandler() = handler
    override fun markEnergyChanged() { tile.setChanged() }
    override fun getNetworkID() = binding.configuration.networkId
    override fun getOwnerUUID() = binding.configuration.ownerUuid
    override fun getDeviceType() = FluxDeviceType.POINT
    override fun getGlobalPos() = GlobalPos.of(binding.key.dimension, binding.key.position)
    override fun getCustomName() = binding.configuration.customName
    override fun getRawPriority() = handler.rawPriority
    override fun getRawLimit() = handler.rawLimit
    override fun getDisableLimit() = handler.disableLimit
    override fun getSurgeMode() = handler.surgeMode
    override fun getMaxTransferLimit() = Long.MAX_VALUE
    override fun isChunkLoaded() = active
    override fun isForcedLoading() = ticket
    override fun getTransferBuffer() = 0L
    override fun getTransferChange() = handler.change
    override fun getDisplayStack() = ItemStack(FluxBatteryMod.BATTERY.get())
    override fun onPlayerOpened(player: Player) {}
    override fun onPlayerClosed(player: Player) {}
    override fun createMenu(id: Int, inventory: Inventory, player: Player): AbstractContainerMenu? = null
    override fun readCustomTag(tag: CompoundTag, type: Byte) { /* All authoritative state belongs to the item. */ }
    override fun writeCustomTag(tag: CompoundTag, type: Byte) {
        if (type != FluxConstants.NBT_PHANTOM_UPDATE && type != FluxConstants.NBT_SAVE_ALL) return
        sonar.fluxnetworks.common.util.FluxUtils.writeGlobalPos(tag, globalPos)
        tag.putByte(FluxConstants.DEVICE_TYPE, deviceType.id)
        tag.putInt(FluxConstants.NETWORK_ID, networkID)
        tag.putString(FluxConstants.CUSTOM_NAME, customName)
        tag.putInt(FluxConstants.PRIORITY, rawPriority)
        tag.putLong(FluxConstants.LIMIT, rawLimit)
        tag.putBoolean(FluxConstants.SURGE_MODE, surgeMode)
        tag.putBoolean(FluxConstants.DISABLE_LIMIT, disableLimit)
        tag.putUUID(FluxConstants.PLAYER_UUID, ownerUUID)
        tag.putLong(FluxConstants.ENERGY, 0)
        tag.putLong(FluxConstants.CHANGE, transferChange)
        tag.putBoolean(FluxConstants.CHUNK_LOADED, active)
        tag.putBoolean(FluxConstants.FORCED_LOADING, ticket)
        tag.putString(FluxConstants.DEVICE_ITEM, "fluxbattery:flux_battery")
    }
}
