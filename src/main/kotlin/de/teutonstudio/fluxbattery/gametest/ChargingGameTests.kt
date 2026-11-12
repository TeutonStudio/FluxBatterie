package de.teutonstudio.fluxbattery.gametest

import com.mojang.authlib.GameProfile
import de.teutonstudio.fluxbattery.*
import de.teutonstudio.fluxbattery.bridge.runtimeDevices
import mekanism.api.Action
import mekanism.api.AutomationType
import mekanism.common.inventory.slot.EnergyInventorySlot
import mekanism.common.tile.TileEntityEnergyCube
import mekanism.common.tile.base.TileEntityMekanism
import mekanism.common.util.UnitDisplayUtils.EnergyUnit
import net.minecraft.core.BlockPos
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.gametest.framework.GameTest
import net.minecraft.gametest.framework.GameTestHelper
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.Tag
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.item.ItemStack
import net.neoforged.neoforge.capabilities.Capabilities
import net.neoforged.neoforge.common.util.FakePlayerFactory
import net.neoforged.neoforge.gametest.GameTestHolder
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate
import sonar.fluxnetworks.api.FluxCapabilities
import sonar.fluxnetworks.api.FluxConstants
import sonar.fluxnetworks.api.device.FluxDeviceType
import sonar.fluxnetworks.api.network.AccessLevel
import sonar.fluxnetworks.api.network.NetworkMember
import sonar.fluxnetworks.api.network.SecurityLevel
import sonar.fluxnetworks.common.connection.FluxNetwork
import sonar.fluxnetworks.common.connection.FluxNetworkData
import sonar.fluxnetworks.common.device.TileFluxStorage
import java.util.UUID

/** Exercise the actual Mekanism item-charging path and Flux's native distribution cycle. */
@GameTestHolder(FluxBatteryMod.ID)
@PrefixGameTestTemplate(false)
class ChargingGameTests {
    private fun cube(helper: GameTestHelper, x: Int = 2): TileEntityEnergyCube {
        val pos = BlockPos(x, 1, 2)
        helper.setBlock(pos, BuiltInRegistries.BLOCK.get(ResourceLocation.parse("mekanism:basic_energy_cube")))
        return helper.getBlockEntity(pos)
    }

    private fun network(helper: GameTestHelper): FluxNetwork = checkNotNull(
        FluxNetworkData.getInstance().createNetwork(
            FakePlayerFactory.getMinecraft(helper.level), "Charging test", 0x22FFFF, SecurityLevel.PRIVATE, ""
        )
    )

    private fun config(helper: GameTestHelper, network: FluxNetwork, limit: Long = 1000) = BatteryConfig(
        network.networkID, FakePlayerFactory.getMinecraft(helper.level).uuid, "charging test", transferLimit = limit
    )

    private fun battery(config: BatteryConfig, stored: Long = 1000, capacity: Long = 1000): ItemStack {
        val stack = ItemStack(FluxBatteryMod.BATTERY.get())
        stack.set(BatteryComponents.ENERGY.get(), BatteryEnergy(stored, capacity))
        BatteryStateService.setConfig(stack, config)
        return stack
    }

    private fun insert(tile: TileEntityMekanism, stack: ItemStack, charging: Boolean = true): EnergyInventorySlot {
        val slot = (if (charging) MekanismHostAdapter.chargeSlots(tile) else MekanismHostAdapter.dischargeSlots(tile)).single().second
        check(slot.insertItem(stack, Action.EXECUTE, AutomationType.MANUAL).isEmpty) {
            "Mekanism rejected a Flux battery in its ${if (charging) "charge" else "discharge"} slot"
        }
        FluxBatteryHostManager.tick(tile)
        return slot
    }

    private fun storage(helper: GameTestHelper, network: FluxNetwork): TileFluxStorage {
        val pos = BlockPos(12, 1, 2)
        helper.setBlock(pos, BuiltInRegistries.BLOCK.get(ResourceLocation.parse("fluxnetworks:basic_flux_storage")))
        val storage: TileFluxStorage = helper.getBlockEntity(pos)
        storage.transferHandler.setDisableLimit(true)
        check(storage.connect(network))
        return storage
    }

    private fun point(helper: GameTestHelper, network: FluxNetwork, capacity: Long = 10000): Pair<TileEntityMekanism, EnergyInventorySlot> {
        val pos = BlockPos(8, 1, 2)
        helper.setBlock(pos, BuiltInRegistries.BLOCK.get(ResourceLocation.parse("mekanism:enrichment_chamber")))
        val tile: TileEntityMekanism = helper.getBlockEntity(pos)
        return tile to insert(tile, battery(config(helper, network, capacity), 0, capacity), charging = false)
    }

    private fun cleanup(network: FluxNetwork, vararg tiles: TileEntityMekanism) {
        tiles.forEach { FluxBatteryHostManager.remove(it) }
        if (network.isValid) {
            network.onEndServerTick()
            FluxNetworkData.getInstance().deleteNetwork(network)
        }
    }

    @GameTest(template = "empty", timeoutTicks = 40)
    fun fullAndPartlyFullBatteriesEnterEmptyChargers(helper: GameTestHelper) {
        val network = network(helper)
        val full = cube(helper, 2)
        val partial = cube(helper, 5)
        try {
            check(full.energyContainer.energy == 0L && partial.energyContainer.energy == 0L)
            val fullSlot = insert(full, battery(config(helper, network)))
            val partialSlot = insert(partial, battery(BatteryConfig(), 500))
            check(BatteryStateService.energy(fullSlot.stack).storedEnergy == 1000L)
            check(BatteryStateService.energy(partialSlot.stack).storedEnergy == 500L)
            network.onEndServerTick()
            val plug = checkNotNull(full.getData(FluxBatteryMod.HOST_ATTACHMENT.get()).point)
            check(plug.deviceType == FluxDeviceType.PLUG)
            check(network.runtimeDevices(FluxNetwork.PLUG).single() === plug)
            check(partial.getData(FluxBatteryMod.HOST_ATTACHMENT.get()).point == null)
            check(MekanismHostAdapter.dischargeSlots(full).single().second !== fullSlot)
            val offered = EnergyUnit.FORGE_ENERGY.convertFrom(2000L)
            full.energyContainer.energy = offered
            check(checkNotNull(fullSlot.stack.getCapability(FluxCapabilities.ITEM)).receiveEnergyL(1000, false) == 0L)
            fullSlot.drainContainer()
            check(full.energyContainer.energy == offered) { "A network without demand consumed charger energy" }
            partial.energyContainer.energy = offered
            partialSlot.drainContainer()
            check(BatteryStateService.networkInput(fullSlot.stack) == 0L)
            check(BatteryStateService.energy(partialSlot.stack).storedEnergy == 1000L)
            check(BatteryStateService.networkInput(partialSlot.stack) == 0L)
            check(partial.energyContainer.energy == offered - EnergyUnit.FORGE_ENERGY.convertFrom(500L)) {
                "Unconfigured batteries must charge their local buffer normally"
            }
            helper.succeed()
        } finally {
            cleanup(network, full, partial)
        }
    }

    @GameTest(template = "empty", timeoutTicks = 40)
    fun chargingFillsLocalBufferThenStorage(helper: GameTestHelper) {
        val network = network(helper)
        val cube = cube(helper)
        try {
            val storage = storage(helper, network)
            val slot = insert(cube, battery(config(helper, network), 750))
            network.onEndServerTick()
            check(network.bufferLimiter >= 1000)
            val offered = EnergyUnit.FORGE_ENERGY.convertFrom(2000L)
            cube.energyContainer.energy = offered
            slot.drainContainer()
            val accepted = EnergyUnit.FORGE_ENERGY.convertTo(offered - cube.energyContainer.energy)
            check(accepted == 1250L) { "Charger must fill 250 FE locally and accept 1000 FE for the network" }
            check(BatteryStateService.energy(slot.stack).storedEnergy == 1000L)
            check(BatteryStateService.networkInput(slot.stack) == 1000L)
            check(storage.transferHandler.buffer == 0L) { "Input must enter native Flux distribution first" }
            check(accepted == BatteryStateService.energy(slot.stack).storedEnergy - 750 + BatteryStateService.networkInput(slot.stack))
            network.onEndServerTick()
            check(BatteryStateService.networkInput(slot.stack) == 0L)
            check(storage.transferHandler.buffer == 1000L)
            check(accepted == BatteryStateService.energy(slot.stack).storedEnergy - 750 + storage.transferHandler.buffer)
            helper.succeed()
        } finally {
            cleanup(network, cube)
        }
    }

    @GameTest(template = "empty", timeoutTicks = 40)
    fun capabilitySimulationAndCycleLimit(helper: GameTestHelper) {
        val network = network(helper)
        val cube = cube(helper)
        val (sink, sinkSlot) = point(helper, network)
        try {
            val slot = insert(cube, battery(config(helper, network)))
            network.onEndServerTick()
            val first = checkNotNull(slot.stack.getCapability(Capabilities.EnergyStorage.ITEM))
            val second = checkNotNull(slot.stack.getCapability(Capabilities.EnergyStorage.ITEM))
            val flux = checkNotNull(slot.stack.getCapability(FluxCapabilities.ITEM))
            val plug = checkNotNull(cube.getData(FluxBatteryMod.HOST_ATTACHMENT.get()).point)
            check(network.runtimeDevices(FluxNetwork.PLUG).single() === plug)
            check(network.runtimeDevices(FluxNetwork.POINT).none { it === plug })
            val snapshot = slot.stack.save(helper.level.registryAccess())
            val demand = network.bufferLimiter
            val copied = slot.stack.copy()
            check(checkNotNull(copied.getCapability(FluxCapabilities.ITEM)).receiveEnergyL(1000, false) == 0L) {
                "A copied battery borrowed the active charging host"
            }
            check(BatteryStateService.networkInput(copied) == 0L)
            check(first.receiveEnergy(Int.MAX_VALUE, true) == 1000)
            check(flux.receiveEnergyL(Long.MAX_VALUE, true) == 1000L)
            check(slot.stack.save(helper.level.registryAccess()) == snapshot) { "Simulation mutated the item" }
            check(network.bufferLimiter == demand && BatteryStateService.networkInput(slot.stack) == 0L)
            check(BatteryStateService.energy(sinkSlot.stack).storedEnergy == 0L)
            check(first.receiveEnergy(400, false) == 400)
            check(second.receiveEnergy(400, false) == 400)
            check(flux.receiveEnergyL(Long.MAX_VALUE, false) == 200L)
            check(first.receiveEnergy(1, true) == 0 && flux.receiveEnergyL(1, false) == 0L) {
                "New capability wrappers bypassed the shared per-cycle limit"
            }
            check(BatteryStateService.networkInput(slot.stack) == 1000L)
            network.onEndServerTick()
            check(plug.transferChange == 1000L) { "Hosted input was omitted from Flux transfer statistics" }
            check(BatteryStateService.networkInput(slot.stack) == 0L)
            check(BatteryStateService.energy(sinkSlot.stack).storedEnergy == 1000L)
            check(flux.receiveEnergyL(1000, false) == 1000L) { "Input allowance did not reset after a network cycle" }
            check(2000L == BatteryStateService.networkInput(slot.stack) + BatteryStateService.energy(sinkSlot.stack).storedEnergy)
            network.onEndServerTick()
            check(BatteryStateService.energy(sinkSlot.stack).storedEnergy == 2000L)
            check(BatteryStateService.energy(slot.stack).storedEnergy == 1000L)
            helper.succeed()
        } finally {
            cleanup(network, cube, sink)
        }
    }

    @GameTest(template = "empty", timeoutTicks = 40)
    fun pendingInputSurvivesRemovalAndSave(helper: GameTestHelper) {
        val network = network(helper)
        val cube = cube(helper)
        try {
            val storage = storage(helper, network)
            val slot = insert(cube, battery(config(helper, network)))
            network.onEndServerTick()
            val capability = checkNotNull(slot.stack.getCapability(FluxCapabilities.ITEM))
            check(capability.receiveEnergyL(600, false) == 600L)
            val removed = slot.extractItem(1, Action.EXECUTE, AutomationType.MANUAL)
            check(BatteryStateService.networkInput(removed) == 600L)
            check(capability.receiveEnergyL(1, false) == 0L) { "Removed battery retained its charging host" }
            network.onEndServerTick()
            check(network.runtimeDevices(FluxNetwork.PLUG).none { it.deviceType == FluxDeviceType.PLUG })
            check(storage.transferHandler.buffer == 0L) { "Detached input was spent by the old host" }
            val saved = removed.save(helper.level.registryAccess())
            val restored = ItemStack.parse(helper.level.registryAccess(), saved).orElseThrow()
            check(BatteryStateService.networkInput(restored) == 600L)
            val replacement = insert(cube, restored)
            network.onEndServerTick()
            check(BatteryStateService.networkInput(replacement.stack) == 0L)
            check(BatteryStateService.energy(replacement.stack).storedEnergy == 1000L)
            check(storage.transferHandler.buffer == 600L) { "Persisted input did not resume native Flux distribution" }
            helper.succeed()
        } finally {
            cleanup(network, cube)
        }
    }

    @GameTest(template = "empty", timeoutTicks = 40)
    fun revokedPermissionAndDeletionPreservePending(helper: GameTestHelper) {
        val owner = FakePlayerFactory.get(helper.level, GameProfile(UUID.randomUUID(), "ChargingOwner"))
        val network = checkNotNull(FluxNetworkData.getInstance().createNetwork(owner, "Charging rights", 0x22FFFF, SecurityLevel.PRIVATE, ""))
        val member = FakePlayerFactory.getMinecraft(helper.level)
        val members = CompoundTag()
        network.writeCustomTag(members, FluxConstants.NBT_NET_MEMBERS)
        val memberTag = CompoundTag()
        NetworkMember.create(member, AccessLevel.USER).writeNBT(memberTag)
        members.getList("members", Tag.TAG_COMPOUND.toInt()).add(memberTag)
        network.readCustomTag(members, FluxConstants.NBT_NET_MEMBERS)
        val cube = cube(helper)
        try {
            val storage = storage(helper, network)
            val slot = insert(cube, battery(config(helper, network)))
            network.onEndServerTick()
            val capability = checkNotNull(slot.stack.getCapability(FluxCapabilities.ITEM))
            check(capability.receiveEnergyL(400, false) == 400L)
            val membership = checkNotNull(network.getMemberByUUID(member.uuid))
            membership.setAccessLevel(AccessLevel.BLOCKED)
            check(capability.receiveEnergyL(1000, true) == 0L && capability.receiveEnergyL(1000, false) == 0L)
            network.onEndServerTick()
            check(storage.transferHandler.buffer == 0L)
            check(BatteryStateService.networkInput(slot.stack) == 400L) { "Revoking access lost accepted input" }
            FluxBatteryHostManager.tick(cube)
            check(cube.getData(FluxBatteryMod.HOST_ATTACHMENT.get()).point == null)
            membership.setAccessLevel(AccessLevel.USER)
            FluxBatteryHostManager.tick(cube)
            network.onEndServerTick()
            check(storage.transferHandler.buffer == 400L)
            check(BatteryStateService.networkInput(slot.stack) == 0L)
            check(capability.receiveEnergyL(300, false) == 300L)
            FluxNetworkData.getInstance().deleteNetwork(network)
            check(capability.receiveEnergyL(1, false) == 0L)
            check(cube.getData(FluxBatteryMod.HOST_ATTACHMENT.get()).point == null)
            check(BatteryStateService.networkInput(slot.stack) == 300L) { "Deleting the network lost accepted input" }
            check(BatteryStateService.energy(slot.stack).storedEnergy == 1000L)
            helper.succeed()
        } finally {
            cleanup(network, cube)
        }
    }

    @GameTest(template = "empty", timeoutTicks = 40)
    fun longInputSaturatesWithoutOverflow(helper: GameTestHelper) {
        val network = network(helper)
        val cube = cube(helper)
        val (sink, sinkSlot) = point(helper, network, Long.MAX_VALUE)
        try {
            val slot = insert(cube, battery(config(helper, network, Long.MAX_VALUE)))
            network.onEndServerTick()
            check(network.bufferLimiter == Long.MAX_VALUE)
            val capability = checkNotNull(slot.stack.getCapability(FluxCapabilities.ITEM))
            check(capability.receiveEnergyL(Long.MIN_VALUE, false) == 0L)
            check(capability.receiveEnergyL(Long.MAX_VALUE, true) == Long.MAX_VALUE)
            check(BatteryStateService.networkInput(slot.stack) == 0L)
            check(capability.receiveEnergyL(Long.MAX_VALUE, false) == Long.MAX_VALUE)
            check(BatteryStateService.networkInput(slot.stack) == Long.MAX_VALUE)
            check(capability.receiveEnergyL(1, false) == 0L)
            network.onEndServerTick()
            check(BatteryStateService.networkInput(slot.stack) == 0L)
            check(BatteryStateService.energy(sinkSlot.stack).storedEnergy == Long.MAX_VALUE)
            check(BatteryStateService.energy(slot.stack).storedEnergy == 1000L)
            helper.succeed()
        } finally {
            cleanup(network, cube, sink)
        }
    }
}
