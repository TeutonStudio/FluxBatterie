package de.voxelteuton.fluxbattery.gametest

import de.voxelteuton.fluxbattery.bridge.*

import de.voxelteuton.fluxbattery.*
import mekanism.api.Action
import mekanism.api.AutomationType
import mekanism.common.inventory.slot.EnergyInventorySlot
import mekanism.common.tile.base.TileEntityMekanism
import net.minecraft.core.BlockPos
import net.minecraft.core.GlobalPos
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.gametest.framework.GameTest
import net.minecraft.gametest.framework.GameTestHelper
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.item.ItemStack
import net.neoforged.neoforge.capabilities.Capabilities
import net.neoforged.neoforge.common.util.FakePlayerFactory
import net.neoforged.neoforge.gametest.GameTestHolder
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate
import sonar.fluxnetworks.api.FluxCapabilities
import de.voxelteuton.fluxbattery.bridge.FluxRuntimeDevice
import sonar.fluxnetworks.api.network.SecurityLevel
import sonar.fluxnetworks.common.connection.FluxNetwork
import sonar.fluxnetworks.common.connection.FluxNetworkData
import sonar.fluxnetworks.common.data.FluxDeviceConfigComponent
import sonar.fluxnetworks.common.device.TileFluxPlug
import java.util.UUID

@GameTestHolder(FluxBatteryMod.ID)
@PrefixGameTestTemplate(false)
class BatteryGameTests {
    private fun machine(helper: GameTestHelper, name: String, x: Int = 2): TileEntityMekanism {
        val pos = BlockPos(x, 1, 2)
        helper.setBlock(pos, BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath("mekanism", name)))
        return helper.getBlockEntity(pos)
    }
    private fun network(helper: GameTestHelper, security: SecurityLevel = SecurityLevel.PUBLIC): FluxNetwork {
        val owner = FakePlayerFactory.getMinecraft(helper.level)
        return checkNotNull(FluxNetworkData.getInstance().createNetwork(owner, "Battery test", 0x22FFFF, security, ""))
    }
    private fun config(helper: GameTestHelper, network: FluxNetwork, limit: Long = 1000, priority: Int = 0, surge: Boolean = false) =
        BatteryConfig(network.networkID, FakePlayerFactory.getMinecraft(helper.level).uuid, "test", priority, limit, false, surge)
    private fun insert(tile: TileEntityMekanism, config: BatteryConfig): EnergyInventorySlot {
        val slot = MekanismHostAdapter.dischargeSlots(tile).first().second
        val stack = ItemStack(FluxBatteryMod.BATTERY.get())
        BatteryStateService.setConfig(stack, config)
        check(slot.insertItem(stack, Action.EXECUTE, AutomationType.MANUAL).isEmpty) { "Configured empty battery rejected" }
        FluxBatteryHostManager.tick(tile)
        return slot
    }
    private fun supply(helper: GameTestHelper, network: FluxNetwork, energy: Long): TileFluxPlug {
        val pos = BlockPos(12, 1, 2)
        helper.setBlock(pos, BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath("fluxnetworks", "flux_plug")))
        val plug: TileFluxPlug = helper.getBlockEntity(pos)
        plug.transferHandler.applyConfiguration(FluxDeviceConfigComponent.EMPTY, energy)
        plug.transferHandler.setDisableLimit(true)
        check(plug.connect(network))
        return plug
    }
    private fun cleanup(network: FluxNetwork, vararg tiles: TileEntityMekanism) {
        tiles.forEach { FluxBatteryHostManager.remove(it) }
        network.onEndServerTick()
        FluxNetworkData.getInstance().deleteNetwork(network)
    }

    @GameTest(template = "empty", timeoutTicks = 40)
    fun sharedCapabilitiesAndPersistence(helper: GameTestHelper) {
        val originalCapacity = BatteryServerConfig.capacity.get()
        try {
            BatteryServerConfig.capacity.set(Long.MAX_VALUE)
            val stack = ItemStack(FluxBatteryMod.BATTERY.get())
            val fe = checkNotNull(stack.getCapability(Capabilities.EnergyStorage.ITEM))
            val flux = checkNotNull(stack.getCapability(FluxCapabilities.ITEM))
            check(flux.receiveEnergyL(Long.MAX_VALUE, true) == Long.MAX_VALUE)
            check(stack.get(BatteryComponents.ENERGY.get()) == null) { "Simulation mutated components" }
            check(fe.receiveEnergy(Int.MAX_VALUE, false) == Int.MAX_VALUE)
            check(flux.receiveEnergyL(1000, false) == 1000L)
            check(flux.energyStoredL == Int.MAX_VALUE.toLong() + 1000)
            check(fe.energyStored == Int.MAX_VALUE)
            check(flux.extractEnergyL(Long.MAX_VALUE, true) == Int.MAX_VALUE.toLong() + 1000)
            check(flux.energyStoredL == Int.MAX_VALUE.toLong() + 1000)
            val config = BatteryConfig(123, UUID.randomUUID(), "persist", 42, Long.MAX_VALUE, true, true, true)
            BatteryStateService.setConfig(stack, config)
            val saved = stack.save(helper.level.registryAccess())
            val restored = ItemStack.parse(helper.level.registryAccess(), saved).orElseThrow()
            check(BatteryStateService.config(restored) == config)
            check(BatteryStateService.energy(restored) == BatteryStateService.energy(stack))
            check(restored.get(BatteryComponents.IDENTITY.get()) == stack.get(BatteryComponents.IDENTITY.get()))
            val secondHandler = checkNotNull(stack.getCapability(FluxCapabilities.ITEM))
            check(secondHandler.extractEnergyL(1000, false) == 1000L)
            check(flux.energyStoredL == Int.MAX_VALUE.toLong()) { "Capability handlers did not share state" }
            helper.succeed()
        } finally { BatteryServerConfig.capacity.set(originalCapacity) }
    }

    @GameTest(template = "empty", timeoutTicks = 40)
    fun sameTickRemoval(helper: GameTestHelper) {
        val tile = machine(helper, "digital_miner")
        val network = network(helper)
        val slot = insert(tile, config(helper, network))
        slot.setEmpty()
        network.onEndServerTick()
        check(network.getConnectionByPos(GlobalPos.of(helper.level.dimension(), tile.blockPos)) == null)
        check(network.runtimeDevices(FluxNetwork.POINT).isEmpty())
        cleanup(network, tile)
        helper.succeed()
    }

    @GameTest(template = "empty", timeoutTicks = 40)
    fun digitalMinerAndTwoMachines(helper: GameTestHelper) {
        val network = network(helper)
        val tiles = listOf("digital_miner", "enrichment_chamber", "energized_smelter").mapIndexed { i, name -> machine(helper, name, 2 + i * 3) }
        val slots = tiles.map { insert(it, config(helper, network)) }
        val plug = supply(helper, network, 3000)
        network.onEndServerTick()
        check(slots.all { BatteryStateService.energy(it.stack).storedEnergy == 1000L })
        check(plug.transferHandler.buffer == 0L)
        check(network.runtimeDevices(FluxNetwork.POINT).size == 3)
        slots.forEachIndexed { i, slot ->
            val before = tiles[i].getEnergyContainers(null).sumOf { it.energy }
            slot.fillContainerOrConvert()
            check(BatteryStateService.energy(slot.stack).storedEnergy == 0L)
            check(tiles[i].getEnergyContainers(null).sumOf { it.energy } > before)
            check(slot.extractItem(1, Action.EXECUTE, AutomationType.MANUAL).item is FluxBatteryItem)
        }
        network.onEndServerTick()
        check(network.runtimeDevices(FluxNetwork.POINT).isEmpty())
        cleanup(network, *tiles.toTypedArray())
        helper.succeed()
    }

    @GameTest(template = "empty", timeoutTicks = 40)
    fun prioritySurgeAndScarcity(helper: GameTestHelper) {
        val network = network(helper)
        val first = machine(helper, "enrichment_chamber", 2)
        val second = machine(helper, "energized_smelter", 6)
        val a = insert(first, config(helper, network, 100, priority = -10, surge = true))
        val b = insert(second, config(helper, network, 200, priority = 9999))
        val plug = supply(helper, network, 150)
        network.onEndServerTick()
        check(BatteryStateService.energy(a.stack).storedEnergy == 100L)
        check(BatteryStateService.energy(b.stack).storedEnergy == 50L)
        check(plug.transferHandler.buffer == 0L)
        val changes = network.runtimeDevices(FluxNetwork.POINT).sumOf { it.transferChange }
        check(changes == -150L)
        cleanup(network, first, second)
        helper.succeed()
    }

    @GameTest(template = "empty", timeoutTicks = 40)
    fun authorizationAndDeletion(helper: GameTestHelper) {
        val network = network(helper, SecurityLevel.PRIVATE)
        val tile = machine(helper, "enrichment_chamber")
        val alien = config(helper, network).copy(ownerUuid = UUID.randomUUID())
        val slot = insert(tile, alien)
        network.onEndServerTick()
        check(network.runtimeDevices(FluxNetwork.POINT).isEmpty())
        BatteryStateService.setConfig(slot.stack, config(helper, network))
        FluxBatteryHostManager.tick(tile)
        network.onEndServerTick()
        check(network.runtimeDevices(FluxNetwork.POINT).size == 1)
        FluxNetworkData.getInstance().deleteNetwork(network)
        check(tile.getData(FluxBatteryMod.HOST_ATTACHMENT.get()).point == null)
        check(BatteryStateService.energy(slot.stack).storedEnergy == 0L)
        helper.succeed()
    }
    @GameTest(template = "empty", timeoutTicks = 40)
    fun chunkTicketsAndRemoval(helper: GameTestHelper) {
        val enabled = BatteryServerConfig.chunkLoading.get()
        val fluxEnabled = sonar.fluxnetworks.FluxConfig.enableChunkLoading
        try {
            BatteryServerConfig.chunkLoading.set(true)
            sonar.fluxnetworks.FluxConfig.enableChunkLoading = true
            val network = network(helper)
            val tile = machine(helper, "enrichment_chamber")
            val slot = insert(tile, config(helper, network).copy(chunkLoadingEnabled = true))
            val point = checkNotNull(tile.getData(FluxBatteryMod.HOST_ATTACHMENT.get()).point)
            check(point.ticket)
            val tickets = helper.level.dataStorage.computeIfAbsent(net.minecraft.world.level.ForcedChunksSavedData.factory(), "chunks").blockForcedChunks
            check(tickets.getTickingChunks().values.any { it.contains(net.minecraft.world.level.ChunkPos(tile.blockPos).toLong()) })
            slot.setEmpty()
            check(!point.ticket)
            check(tickets.getTickingChunks().values.none { it.contains(net.minecraft.world.level.ChunkPos(tile.blockPos).toLong()) })
            insert(tile, config(helper, network).copy(chunkLoadingEnabled = true))
            BatteryServerConfig.chunkLoading.set(false)
            FluxBatteryHostManager.tick(tile)
            check(tile.getData(FluxBatteryMod.HOST_ATTACHMENT.get()).point?.ticket == false)
            cleanup(network, tile)
            helper.succeed()
        } finally {
            BatteryServerConfig.chunkLoading.set(enabled)
            sonar.fluxnetworks.FluxConfig.enableChunkLoading = fluxEnabled
        }
    }

    @GameTest(template = "empty", timeoutTicks = 40)
    fun moveAndNetworkChange(helper: GameTestHelper) {
        val firstNetwork = network(helper)
        val secondNetwork = network(helper)
        val first = machine(helper, "enrichment_chamber", 2)
        val second = machine(helper, "energized_smelter", 6)
        val slot = insert(first, config(helper, firstNetwork))
        BatteryStateService.receive(slot.stack, 345, false)
        val identity = slot.stack.get(BatteryComponents.IDENTITY.get())
        val moved = slot.extractItem(1, Action.EXECUTE, AutomationType.MANUAL)
        val target = MekanismHostAdapter.dischargeSlots(second).first().second
        check(target.insertItem(moved, Action.EXECUTE, AutomationType.MANUAL).isEmpty)
        FluxBatteryHostManager.tick(second)
        firstNetwork.onEndServerTick()
        check(firstNetwork.runtimeDevices(FluxNetwork.POINT).size == 1)
        check(firstNetwork.getConnectionByPos(GlobalPos.of(helper.level.dimension(), first.blockPos)) == null)
        check(target.stack.get(BatteryComponents.IDENTITY.get()) == identity)
        check(BatteryStateService.energy(target.stack).storedEnergy == 345L)
        BatteryStateService.setConfig(target.stack, config(helper, secondNetwork))
        FluxBatteryHostManager.tick(second)
        firstNetwork.onEndServerTick(); secondNetwork.onEndServerTick()
        check(firstNetwork.runtimeDevices(FluxNetwork.POINT).isEmpty())
        check(secondNetwork.runtimeDevices(FluxNetwork.POINT).size == 1)
        helper.setBlock(BlockPos(6, 1, 2), net.minecraft.world.level.block.Blocks.AIR)
        secondNetwork.onEndServerTick()
        check(secondNetwork.runtimeDevices(FluxNetwork.POINT).isEmpty())
        cleanup(firstNetwork, first)
        cleanup(secondNetwork, second)
        helper.succeed()
    }

    @GameTest(template = "empty", timeoutTicks = 40)
    fun deletePendingNetwork(helper: GameTestHelper) {
        val network = network(helper)
        val tile = machine(helper, "enrichment_chamber")
        insert(tile, config(helper, network))
        FluxNetworkData.getInstance().deleteNetwork(network)
        FluxBatteryHostManager.tick(tile)
        check(tile.getData(FluxBatteryMod.HOST_ATTACHMENT.get()).point == null)
        helper.succeed()
    }

    @GameTest(template = "empty", timeoutTicks = 300)
    fun operatingDigitalMiner(helper: GameTestHelper) {
        val network = network(helper)
        val tile = machine(helper, "digital_miner") as mekanism.common.tile.machine.TileEntityDigitalMiner
        val slot = insert(tile, config(helper, network, 100_000))
        val plug = supply(helper, network, 1_000_000)
        val ore = BlockPos(4, 1, 2)
        helper.setBlock(ore, net.minecraft.world.level.block.Blocks.IRON_ORE)
        val filter = mekanism.common.content.miner.MinerItemStackFilter()
        filter.itemStack = ItemStack(net.minecraft.world.item.Items.IRON_ORE)
        tile.filterManager.addFilter(filter)
        tile.setRadiusFromPacket(3)
        val y = helper.absolutePos(ore).y
        tile.setMinYFromPacket(y)
        tile.setMaxYFromPacket(y)
        helper.runAfterDelay(2) { tile.start() }
        helper.succeedWhen {
            helper.assertBlockNotPresent(net.minecraft.world.level.block.Blocks.IRON_ORE, ore)
            helper.assertTrue(tile.getInventorySlots(null).any { it.stack.`is`(net.minecraft.world.item.Items.RAW_IRON) }, "Miner must produce raw iron")
            helper.assertTrue(plug.transferHandler.buffer < 1_000_000, "Miner must consume energy from the Flux network")
            tile.stop()
            cleanup(network, tile)
        }
    }

    @GameTest(template = "empty", timeoutTicks = 40)
    fun normalFluxDevices(helper: GameTestHelper) {
        val network = network(helper)
        val plug = supply(helper, network, 1000)
        fun device(name: String, x: Int): sonar.fluxnetworks.common.device.TileFluxDevice {
            val pos = BlockPos(x, 1, 5)
            helper.setBlock(pos, BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath("fluxnetworks", name)))
            val tile: sonar.fluxnetworks.common.device.TileFluxDevice = helper.getBlockEntity(pos)
            check(tile.connect(network))
            return tile
        }
        val point = device("flux_point", 2)
        val storage = device("basic_flux_storage", 6)
        val controller = device("flux_controller", 10)
        repeat(21) { network.onEndServerTick() }
        check(plug.transferHandler.buffer + storage.transferBuffer == 1000L)
        check(network.statistics.fluxPlugCount == 1 && network.statistics.fluxPointCount == 1)
        check(network.statistics.fluxStorageCount == 1 && network.statistics.fluxControllerCount == 1)
        listOf(point, storage, controller, plug).forEach { it.disconnect() }
        network.onEndServerTick()
        check(network.runtimeDevices(FluxNetwork.ANY).isEmpty())
        FluxNetworkData.getInstance().deleteNetwork(network)
        helper.succeed()
    }

    @GameTest(template = "empty", timeoutTicks = 40)
    fun maliciousConfigurationRequests(helper: GameTestHelper) {
        val player = FakePlayerFactory.getMinecraft(helper.level)
        val hand = net.minecraft.world.InteractionHand.MAIN_HAND
        val previous = player.getItemInHand(hand)
        val network = network(helper)
        try {
            val stack = ItemStack(FluxBatteryMod.BATTERY.get())
            val config = config(helper, network)
            BatteryStateService.setConfig(stack, config)
            BatteryStateService.receive(stack, 12345, false)
            player.setItemInHand(hand, stack)
            val request = BatteryUpdate(BatteryStateService.identity(stack), hand, config)
            check(!BatteryNetworking.applyUpdate(player, request.copy(identity = UUID.randomUUID())))
            check(!BatteryNetworking.applyUpdate(player, request.copy(config = config.copy(priority = 10000))))
            check(!BatteryNetworking.applyUpdate(player, request.copy(config = config.copy(transferLimit = -1))))
            check(!BatteryNetworking.applyUpdate(player, request.copy(config = config.copy(networkId = Int.MAX_VALUE))))
            val other = FakePlayerFactory.get(helper.level, com.mojang.authlib.GameProfile(UUID.randomUUID(), "BatteryOther"))
            val privateNetwork = checkNotNull(FluxNetworkData.getInstance().createNetwork(other, "Private test", 0xFFFFFF, SecurityLevel.PRIVATE, "secret"))
            check(!BatteryNetworking.applyUpdate(player, request.copy(config = config.copy(networkId = privateNetwork.networkID))))
            other.setItemInHand(hand, stack)
            check(!BatteryNetworking.applyUpdate(other, request))
            other.setItemInHand(hand, ItemStack.EMPTY)
            check(BatteryNetworking.applyUpdate(player, request.copy(config = config.copy(customName = "valid", ownerUuid = UUID.randomUUID()))))
            check(BatteryStateService.config(stack).ownerUuid == player.uuid)
            check(BatteryStateService.energy(stack).storedEnergy == 12345L)
            FluxNetworkData.getInstance().deleteNetwork(privateNetwork)
            helper.succeed()
        } finally {
            player.setItemInHand(hand, previous)
            FluxNetworkData.getInstance().deleteNetwork(network)
        }
    }

    @GameTest(template = "empty", timeoutTicks = 40)
    fun longMaximumDemandAndFullBattery(helper: GameTestHelper) {
        val capacity = BatteryServerConfig.capacity.get()
        val network = network(helper)
        val first = machine(helper, "enrichment_chamber", 2)
        val second = machine(helper, "energized_smelter", 6)
        try {
            BatteryServerConfig.capacity.set(Long.MAX_VALUE)
            val a = insert(first, config(helper, network, Long.MAX_VALUE).copy(disableTransferLimit = true))
            val b = insert(second, config(helper, network, Long.MAX_VALUE).copy(disableTransferLimit = true))
            network.onEndServerTick()
            check(network.bufferLimiter == Long.MAX_VALUE) { "Summed demand overflowed" }
            val plug = supply(helper, network, Long.MAX_VALUE)
            network.onEndServerTick()
            check(BatteryStateService.energy(a.stack).storedEnergy == Long.MAX_VALUE)
            check(BatteryStateService.energy(b.stack).storedEnergy == 0L)
            check(plug.transferHandler.buffer == 0L)
            check(first.getData(FluxBatteryMod.HOST_ATTACHMENT.get()).point!!.transferHandler.request == 0L)
            check(BatteryStateService.receive(a.stack, Long.MAX_VALUE, false) == 0L)
            check(BatteryStateService.extract(a.stack, Long.MIN_VALUE, false) == 0L)
            val full = a.extractItem(1, Action.EXECUTE, AutomationType.MANUAL)
            check(BatteryStateService.energy(full).storedEnergy == Long.MAX_VALUE)
            helper.succeed()
        } finally {
            cleanup(network, first, second)
            BatteryServerConfig.capacity.set(capacity)
        }
    }

}
