package de.teutonstudio.fluxbattery.gametest

import de.teutonstudio.fluxbattery.*
import de.teutonstudio.fluxbattery.bridge.runtimeDevices
import mekanism.common.tile.TileEntityEnergyCube
import mekanism.common.tile.base.TileEntityMekanism
import net.minecraft.core.BlockPos
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.gametest.framework.GameTest
import net.minecraft.gametest.framework.GameTestHelper
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.ChunkPos
import net.minecraft.world.level.ForcedChunksSavedData
import net.neoforged.neoforge.common.util.FakePlayerFactory
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate
import sonar.fluxnetworks.api.device.FluxDeviceType
import sonar.fluxnetworks.api.network.SecurityLevel
import sonar.fluxnetworks.common.connection.FluxNetwork
import sonar.fluxnetworks.common.connection.FluxNetworkData
import sonar.fluxnetworks.common.device.TileFluxStorage

/** Registered explicitly only for the two-process restart test. */
@PrefixGameTestTemplate(false)
class RestartGameTests {
    @GameTest(template = "empty", templateNamespace = "fluxbattery", timeoutTicks = 60)
    fun ticketAcrossProcessRestart(helper: GameTestHelper) {
        val level = helper.level
        val pos = BlockPos(4096, 64, 4096)
        val orphan = pos.offset(32, 0, 0)
        val tickets = level.dataStorage.computeIfAbsent(ForcedChunksSavedData.factory(), "chunks").blockForcedChunks.tickingChunks
        when (System.getProperty("fluxbattery.restartStage")) {
            "prepare" -> {
                check(BatteryServerConfig.chunkLoading.get()) { "Restart fixture requires allowChunkLoading=true in defaultconfigs" }
                level.setBlockAndUpdate(pos, BuiltInRegistries.BLOCK.get(ResourceLocation.parse("mekanism:enrichment_chamber")).defaultBlockState())
                val tile = level.getBlockEntity(pos) as TileEntityMekanism
                val energy = tile.getEnergyContainers(null).first()
                energy.energy = energy.maxEnergy // Hold item energy stationary while the server saves.
                val owner = FakePlayerFactory.getMinecraft(level)
                val network = checkNotNull(FluxNetworkData.getInstance().createNetwork(owner, "Restart fixture", 0x22FFFF, SecurityLevel.PRIVATE, ""))
                val stack = ItemStack(FluxBatteryMod.BATTERY.get())
                BatteryStateService.setConfig(stack, BatteryConfig(network.networkID, owner.uuid, "restart fixture", chunkLoadingEnabled = true))
                BatteryStateService.receive(stack, 987654, false)
                val slot = MekanismHostAdapter.dischargeSlots(tile).first().second
                slot.setStack(stack)
                FluxBatteryHostManager.tick(tile)
                check(tile.getData(FluxBatteryMod.HOST_ATTACHMENT.get()).point?.ticket == true)
                // Deliberately stale ticket; the next JVM must reject it.
                BatteryChunkTicketController.CONTROLLER.forceChunk(level, orphan, orphan.x shr 4, orphan.z shr 4, true, true)
                check(tickets.values.any { it.contains(ChunkPos(pos).toLong()) })
            }
            "verify" -> {
                val tile = level.getBlockEntity(pos) as? TileEntityMekanism ?: error("Persisted fixture missing after restart")
                val slot = MekanismHostAdapter.dischargeSlots(tile).first().second
                check(BatteryStateService.energy(slot.stack).storedEnergy == 987654L)
                check(BatteryStateService.config(slot.stack).customName == "restart fixture")
                FluxBatteryHostManager.tick(tile)
                val point = checkNotNull(tile.getData(FluxBatteryMod.HOST_ATTACHMENT.get()).point)
                check(point.ticket) { "Valid ticket was not restored" }
                check(tickets.values.none { it.contains(ChunkPos(orphan).toLong()) }) { "Stale ticket survived restart" }
                slot.setEmpty()
                check(tickets.values.none { it.contains(ChunkPos(pos).toLong()) }) { "Restored ticket survived battery removal" }
                FluxNetworkData.getInstance().deleteNetwork(point.network)
                level.removeBlock(pos, false)
            }
            else -> error("Unknown restart stage")
        }
        helper.succeed()
    }

    @GameTest(template = "empty", templateNamespace = "fluxbattery", timeoutTicks = 60)
    fun chargingAcrossProcessRestart(helper: GameTestHelper) {
        val level = helper.level
        val pos = BlockPos(4224, 64, 4096)
        val storagePos = pos.offset(4, 0, 0)
        val localEnergy = 987654L
        val pendingEnergy = 654321L
        val tickets = level.dataStorage.computeIfAbsent(ForcedChunksSavedData.factory(), "chunks").blockForcedChunks.tickingChunks
        when (System.getProperty("fluxbattery.restartStage")) {
            "prepare" -> {
                check(BatteryServerConfig.chunkLoading.get()) { "Restart fixture requires allowChunkLoading=true in defaultconfigs" }
                level.setBlockAndUpdate(pos, BuiltInRegistries.BLOCK.get(ResourceLocation.parse("mekanism:basic_energy_cube")).defaultBlockState())
                val cube = level.getBlockEntity(pos) as TileEntityEnergyCube
                cube.energyContainer.energy = 0L
                val owner = FakePlayerFactory.getMinecraft(level)
                val network = checkNotNull(FluxNetworkData.getInstance().createNetwork(owner, "Charging restart fixture", 0x22FFFF, SecurityLevel.PRIVATE, ""))
                val stack = ItemStack(FluxBatteryMod.BATTERY.get())
                stack.set(BatteryComponents.ENERGY.get(), BatteryEnergy(localEnergy, localEnergy))
                BatteryStateService.setConfig(stack, BatteryConfig(network.networkID, owner.uuid, "charging restart", chunkLoadingEnabled = true))
                check(BatteryStateService.receiveNetworkInput(stack, pendingEnergy, false) == pendingEnergy)
                val slot = MekanismHostAdapter.chargeSlots(cube).single().second
                slot.setStack(stack)
                FluxBatteryHostManager.tick(cube)
                val plug = checkNotNull(cube.getData(FluxBatteryMod.HOST_ATTACHMENT.get()).point)
                check(plug.deviceType == FluxDeviceType.PLUG && plug.ticket)
                network.onEndServerTick()
                check(network.runtimeDevices(FluxNetwork.PLUG).single() === plug)
                check(network.runtimeDevices(FluxNetwork.POINT).isEmpty())
                check(BatteryStateService.networkInput(slot.stack) == pendingEnergy)
                check(BatteryStateService.energy(slot.stack) == BatteryEnergy(localEnergy, localEnergy))
                check(tickets.values.any { it.contains(ChunkPos(pos).toLong()) })
            }
            "verify" -> {
                val cube = level.getBlockEntity(pos) as? TileEntityEnergyCube ?: error("Persisted charging fixture missing after restart")
                val slot = MekanismHostAdapter.chargeSlots(cube).single().second
                check(BatteryStateService.energy(slot.stack) == BatteryEnergy(localEnergy, localEnergy))
                check(BatteryStateService.networkInput(slot.stack) == pendingEnergy)
                check(BatteryStateService.config(slot.stack).customName == "charging restart")
                check(cube.energyContainer.energy == 0L)
                check(tickets.values.any { it.contains(ChunkPos(pos).toLong()) }) { "Charging ticket was not restored" }
                FluxBatteryHostManager.tick(cube)
                val plug = checkNotNull(cube.getData(FluxBatteryMod.HOST_ATTACHMENT.get()).point)
                check(plug.deviceType == FluxDeviceType.PLUG && plug.ticket)
                val network = plug.network
                check(network.securityLevel == SecurityLevel.PRIVATE)
                check(BatteryStateService.config(slot.stack).ownerUuid == network.ownerUUID)
                level.setBlockAndUpdate(storagePos, BuiltInRegistries.BLOCK.get(ResourceLocation.parse("fluxnetworks:basic_flux_storage")).defaultBlockState())
                val storage = level.getBlockEntity(storagePos) as TileFluxStorage
                storage.transferHandler.setDisableLimit(true)
                check(storage.connect(network))
                network.onEndServerTick()
                check(network.runtimeDevices(FluxNetwork.PLUG).contains(plug))
                check(network.runtimeDevices(FluxNetwork.POINT).none { it === plug })
                check(BatteryStateService.networkInput(slot.stack) == 0L) { "Saved charging overflow did not resume delivery" }
                check(storage.transferBuffer == pendingEnergy)
                check(BatteryStateService.energy(slot.stack) == BatteryEnergy(localEnergy, localEnergy)) { "Overflow delivery consumed the local battery buffer" }
                slot.setEmpty()
                check(!plug.ticket)
                check(tickets.values.none { it.contains(ChunkPos(pos).toLong()) }) { "Restored charging ticket survived battery removal" }
                storage.disconnect()
                network.onEndServerTick()
                check(network.runtimeDevices(FluxNetwork.ANY).isEmpty())
                FluxNetworkData.getInstance().deleteNetwork(network)
                level.removeBlock(storagePos, false)
                level.removeBlock(pos, false)
            }
            else -> error("Unknown restart stage")
        }
        helper.succeed()
    }
}
