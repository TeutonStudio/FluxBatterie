package de.teutonstudio.fluxbattery.gametest

import de.teutonstudio.fluxbattery.bridge.*

import de.teutonstudio.fluxbattery.*
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
import de.teutonstudio.fluxbattery.bridge.FluxRuntimeDevice
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
    fun batteryReequipAnimation(helper: GameTestHelper) {
        val item = FluxBatteryMod.BATTERY.get()
        val original = ItemStack(item)
        BatteryStateService.identity(original)
        val updated = original.copy()
        BatteryStateService.receive(updated, 1000, false)
        BatteryStateService.setConfig(updated, BatteryConfig(customName = "Updated", priority = 10))
        check(!item.shouldCauseReequipAnimation(original, updated, false)) {
            "Battery state updates must not replay the equip animation"
        }
        check(item.shouldCauseReequipAnimation(original, updated, true)) { "Slot changes must animate" }
        val otherBattery = ItemStack(item)
        BatteryStateService.identity(otherBattery)
        check(item.shouldCauseReequipAnimation(original, otherBattery, false)) { "Replacing a battery must animate" }
        check(item.shouldCauseReequipAnimation(original, ItemStack(net.minecraft.world.item.Items.STICK), false))
        check(item.shouldCauseReequipAnimation(original, ItemStack.EMPTY, false))
        helper.succeed()
    }

    @GameTest(template = "empty", timeoutTicks = 40)
    fun creativeTabIntegration(helper: GameTestHelper) {
        net.minecraft.world.item.CreativeModeTabs.tryRebuildTabContents(
            helper.level.enabledFeatures(), true, helper.level.registryAccess())
        val fluxTab = sonar.fluxnetworks.register.RegistryCreativeModeTabs.CREATIVE_MODE_TAB.get()
        val toolsTab = BuiltInRegistries.CREATIVE_MODE_TAB.get(net.minecraft.world.item.CreativeModeTabs.TOOLS_AND_UTILITIES)
        for (tab in listOf(fluxTab, checkNotNull(toolsTab))) {
            check(tab.displayItems.count { it.`is`(FluxBatteryMod.BATTERY.get()) } == 1) {
                "Battery must appear exactly once in ${tab.displayName.string}"
            }
            check(tab.searchTabDisplayItems.any { it.`is`(FluxBatteryMod.BATTERY.get()) }) {
                "Battery missing from creative search"
            }
        }
        helper.succeed()
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
        val pendingPoint = checkNotNull(tile.getData(FluxBatteryMod.HOST_ATTACHMENT.get()).point)
        FluxNetworkData.getInstance().deleteNetwork(network)
        check(!network.isValid && !pendingPoint.active)
        check(!network.registerRuntime(pendingPoint)) { "Deleted network accepted a new participant" }
        check((network as ServerRuntimeAccess).fluxBatteryPending().isEmpty())
        check(network.runtimeDevices(FluxNetwork.ANY).isEmpty())
        network.onEndServerTick() // Deletion leaves usable empty lists, never null arrays.
        FluxBatteryHostManager.tick(tile)
        check(tile.getData(FluxBatteryMod.HOST_ATTACHMENT.get()).point == null)
        helper.succeed()
    }

    @GameTest(template = "empty", timeoutTicks = 300)
    fun operatingDigitalMiner(helper: GameTestHelper) {
        val network = network(helper)
        val tile = machine(helper, "digital_miner") as mekanism.common.tile.machine.TileEntityDigitalMiner
        val slot = insert(tile, config(helper, network, BatteryConfig().transferLimit))
        val plug = supply(helper, network, 1_000_000_000)
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
            helper.assertTrue(plug.transferHandler.buffer < 1_000_000_000, "Miner must consume energy from the Flux network")
            tile.stop()
            // Stress the real item adapter at maximum upgrade, range, height and silk costs.
            for (upgrade in tile.supportedUpgrade) tile.getComponent().addUpgrades(upgrade, upgrade.max)
            tile.setRadiusFromPacket(mekanism.common.config.MekanismConfig.general.minerMaxRadius.get())
            tile.setMinYFromPacket(helper.level.minBuildHeight)
            tile.setMaxYFromPacket(helper.level.maxBuildHeight - 1)
            tile.toggleSilkTouch()
            val container = tile.energyContainer
            val demand = container.energyPerTick
            check(demand > 0 && demand <= container.maxEnergy)
            BatteryStateService.extract(slot.stack, Long.MAX_VALUE, false)
            container.setEnergy(0)
            val before = plug.transferHandler.buffer
            network.onEndServerTick()
            check(before - plug.transferHandler.buffer == 100_000_000L) { "Default battery limit must allow 100 million FE per cycle" }
            val totalBefore = plug.transferHandler.buffer + BatteryStateService.energy(slot.stack).storedEnergy
            repeat(64) {
                network.onEndServerTick()
                slot.fillContainerOrConvert()
                check(container.extract(demand, Action.EXECUTE, AutomationType.INTERNAL) == demand) {
                    "Fully upgraded miner ran short of energy on cycle $it"
                }
            }
            val remainingFE = plug.transferHandler.buffer + BatteryStateService.energy(slot.stack).storedEnergy
            val consumedJoules = mekanism.common.util.UnitDisplayUtils.EnergyUnit.FORGE_ENERGY.convertFrom(totalBefore - remainingFE)
            check(consumedJoules == 64 * demand + container.energy) { "High-throughput transfer lost or created energy" }
            cleanup(network, tile)
        }
    }

    @GameTest(template = "empty", timeoutTicks = 40)
    fun normalFluxDevices(helper: GameTestHelper) {
        val network = network(helper)
        val plug = supply(helper, network, 1000)
        if (System.getProperty("fluxbattery.compatFixture") == "true") {
            check(network.javaClass.getMethod("fluxBatteryMixinProbe").invoke(network) == 42) { "External Mixin fixture did not apply" }
        }
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
            player.setItemInHand(hand, stack)
            player.isShiftKeyDown = false
            check(stack.item.use(helper.level, player, hand).result.consumesAction()) { "Normal right-click must open configuration" }
            check(stack.has(BatteryComponents.IDENTITY.get())) { "Opening configuration must initialize the battery identity" }
            val initialRequest = BatteryUpdate(BatteryStateService.identity(stack), hand, config)
            check(BatteryNetworking.applyUpdate(player, initialRequest)) { "Fresh battery must accept its first network configuration" }
            val tile = machine(helper, "enrichment_chamber")
            try {
                val slot = MekanismHostAdapter.dischargeSlots(tile).first().second
                check(slot.insertItem(stack.copy(), Action.EXECUTE, AutomationType.MANUAL).isEmpty) { "Newly configured empty battery must fit the energy slot" }
                FluxBatteryHostManager.tick(tile)
                supply(helper, network, 1000)
                network.onEndServerTick()
                check(BatteryStateService.energy(slot.stack).storedEnergy == 1000L)
                slot.fillContainerOrConvert()
                check(tile.getEnergyContainers(null).sumOf { it.energy } > 0) { "Newly configured battery must power the machine" }
            } finally {
                FluxBatteryHostManager.remove(tile)
            }
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

    @GameTest(template = "empty", timeoutTicks = 40)
    fun hostedDisplayColorAndSaturatedStatistics(helper: GameTestHelper) {
        val capacity = BatteryServerConfig.capacity.get()
        val network = network(helper)
        val first = machine(helper, "enrichment_chamber", 2)
        val second = machine(helper, "energized_smelter", 6)
        val player = FakePlayerFactory.getMinecraft(helper.level)
        val previousMenu = player.containerMenu
        try {
            BatteryServerConfig.capacity.set(Long.MAX_VALUE)
            val a = insert(first, config(helper, network, Long.MAX_VALUE).copy(disableTransferLimit = true))
            val b = insert(second, config(helper, network, Long.MAX_VALUE).copy(disableTransferLimit = true))
            val plug = supply(helper, network, 0)
            val otherPlugPos = BlockPos(13, 1, 2)
            helper.setBlock(otherPlugPos, BuiltInRegistries.BLOCK.get(ResourceLocation.parse("fluxnetworks:flux_plug")))
            val otherPlug: TileFluxPlug = helper.getBlockEntity(otherPlugPos)
            otherPlug.transferHandler.applyConfiguration(FluxDeviceConfigComponent.EMPTY, 0)
            otherPlug.transferHandler.setDisableLimit(true)
            check(otherPlug.connect(network))
            repeat(20) { network.onEndServerTick() }
            check(plug.transferHandler.receive(Long.MAX_VALUE, net.minecraft.core.Direction.NORTH, false, network.bufferLimiter) == Long.MAX_VALUE)
            check(otherPlug.transferHandler.receive(Long.MAX_VALUE, net.minecraft.core.Direction.NORTH, false, network.bufferLimiter) == Long.MAX_VALUE)
            network.onEndServerTick()
            check(BatteryStateService.energy(a.stack).storedEnergy == Long.MAX_VALUE)
            check(BatteryStateService.energy(b.stack).storedEnergy == Long.MAX_VALUE)
            check(network.statistics.energyInput == Long.MAX_VALUE / 4)
            check(network.statistics.energyOutput == Long.MAX_VALUE / 4)
            check(network.statistics.fluxPointCount == 2 && network.statistics.fluxPlugCount == 2)
            check(plug.transferHandler.buffer == 0L && otherPlug.transferHandler.buffer == 0L)
            val tag = net.minecraft.nbt.CompoundTag()
            network.writeCustomTag(tag, sonar.fluxnetworks.api.FluxConstants.NBT_NET_ALL_CONNECTIONS)
            val entries = tag.getList("connections", net.minecraft.nbt.Tag.TAG_COMPOUND.toInt())
            // Use Flux's existing deserializer, including its display-item field and machine GlobalPos.
            val hosted = (0 until entries.size).map { entries.getCompound(it).let { c -> sonar.fluxnetworks.common.connection.PhantomFluxDevice.makeUpdated(sonar.fluxnetworks.common.util.FluxUtils.readGlobalPos(c), c) } }
                .filter { it.deviceType.isPoint }
            check(hosted.size == 2)
            check(hosted.all { it.displayStack.item is FluxBatteryItem && it.customName == "test" && it.transferChange == -Long.MAX_VALUE })
            check(hosted.map { it.globalPos.pos() }.toSet() == setOf(first.blockPos, second.blockPos))
            val point = checkNotNull(first.getData(FluxBatteryMod.HOST_ATTACHMENT.get()).point)
            player.containerMenu = sonar.fluxnetworks.common.connection.FluxMenu(17, player.inventory, point)
            val handler = sonar.fluxnetworks.register.Messages::class.java.declaredMethods.single { it.name.startsWith("lambda\$onEditNetwork\$") }
            handler.isAccessible = true
            handler.invoke(null, java.util.function.Supplier { player }, network.networkID, 17, network.networkName, 0x123456, SecurityLevel.PUBLIC, "")
            check(network.networkColor == 0x123456) { "Flux color-update callback failed" }
            check(point.active && helper.level.getBlockEntity(first.blockPos) === first)
            otherPlug.disconnect()
            helper.succeed()
        } catch (error: Throwable) {
            error.printStackTrace()
            throw error
        } finally {
            player.containerMenu = previousMenu
            cleanup(network, first, second)
            BatteryServerConfig.capacity.set(capacity)
        }
    }

    @GameTest(template = "empty", timeoutTicks = 80)
    fun breakPickupAndReplaceMachine(helper: GameTestHelper) {
        val enabled = BatteryServerConfig.chunkLoading.get()
        val fluxEnabled = sonar.fluxnetworks.FluxConfig.enableChunkLoading
        val player = FakePlayerFactory.getMinecraft(helper.level)
        val hand = net.minecraft.world.InteractionHand.MAIN_HAND
        val previous = player.getItemInHand(hand)
        val network = network(helper)
        val tile = machine(helper, "enrichment_chamber")
        val oldPos = tile.blockPos.immutable()
        val newPos = BlockPos(8192, 64, 8192)
        // GameTestServer has no GameProfileCache. Real servers populate UsernameCache on login.
        net.neoforged.neoforge.common.UsernameCache::class.java.getDeclaredMethod("setUsername", UUID::class.java, String::class.java).apply {
            isAccessible = true
        }.invoke(null, player.uuid, player.gameProfile.name)
        try {
            BatteryServerConfig.chunkLoading.set(true)
            sonar.fluxnetworks.FluxConfig.enableChunkLoading = true
            val machineEnergy = tile.getEnergyContainers(null).first()
            machineEnergy.energy = machineEnergy.maxEnergy
            val machineStored = machineEnergy.energy
            val configuration = config(helper, network).copy(customName = "machine item roundtrip", priority = 73, surgeMode = true, chunkLoadingEnabled = true)
            val slot = insert(tile, configuration)
            BatteryStateService.receive(slot.stack, 765432, false)
            val identity = BatteryStateService.identity(slot.stack)
            network.onEndServerTick()
            val originalPoint = checkNotNull(tile.getData(FluxBatteryMod.HOST_ATTACHMENT.get()).point)
            check(originalPoint.ticket)
            player.setItemInHand(hand, ItemStack(net.minecraft.world.item.Items.DIAMOND_PICKAXE))
            player.gameMode.changeGameModeForPlayer(net.minecraft.world.level.GameType.SURVIVAL)
            check(player.gameMode.destroyBlock(oldPos)) { "Survival machine break failed" }
            network.onEndServerTick()
            check(!originalPoint.active && !originalPoint.ticket)
            check(network.getConnectionByPos(GlobalPos.of(helper.level.dimension(), oldPos)) == null)
            check(network.runtimeDevices(FluxNetwork.POINT).isEmpty())
            val tickets = helper.level.dataStorage.computeIfAbsent(net.minecraft.world.level.ForcedChunksSavedData.factory(), "chunks").blockForcedChunks.tickingChunks
            check(tickets.values.none { it.contains(net.minecraft.world.level.ChunkPos(oldPos).toLong()) }) { "Ticket remained after machine break" }
            val drops = helper.level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity::class.java, net.minecraft.world.phys.AABB(oldPos).inflate(2.0))
            val dropped = drops.single { it.item.`is`(tile.blockState.block.asItem()) }
            dropped.setNoPickUpDelay()
            dropped.playerTouch(player)
            check(dropped.isRemoved) { "Machine item was not picked up" }
            val inventoryIndex = (0 until player.inventory.containerSize).first { player.inventory.getItem(it).`is`(tile.blockState.block.asItem()) }
            val machineItem = player.inventory.removeItemNoUpdate(inventoryIndex)
            val saved = machineItem.save(helper.level.registryAccess())
            val restored = ItemStack.parse(helper.level.registryAccess(), saved).orElseThrow()
            val storedBattery = restored.get(mekanism.common.registries.MekanismDataComponents.ATTACHED_ITEMS.get())!!.containers().single { it.item is FluxBatteryItem }
            check(BatteryStateService.identity(storedBattery) == identity)
            check(BatteryStateService.config(storedBattery) == configuration)
            check(BatteryStateService.energy(storedBattery).storedEnergy == 765432L)
            // DedicatedServer terrain can occupy the target; GameTestServer's flat world is empty here.
            helper.level.removeBlock(newPos, false)
            helper.level.setBlockAndUpdate(newPos.below(), net.minecraft.world.level.block.Blocks.STONE.defaultBlockState())
            player.setItemInHand(hand, restored)
            val hit = net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(newPos.below()), net.minecraft.core.Direction.UP, newPos.below(), false)
            val context = net.minecraft.world.item.context.BlockPlaceContext(player, hand, restored, hit)
            check((restored.item as net.minecraft.world.item.BlockItem).place(context).consumesAction()) { "Machine item placement failed" }
            val replacement = helper.level.getBlockEntity(newPos) as TileEntityMekanism
            val restoredSlot = MekanismHostAdapter.dischargeSlots(replacement).first().second
            check(BatteryStateService.energy(restoredSlot.stack).storedEnergy == 765432L)
            check(BatteryStateService.config(restoredSlot.stack) == configuration)
            check(BatteryStateService.identity(restoredSlot.stack) == identity)
            check(replacement.getEnergyContainers(null).first().energy == machineStored)
            FluxBatteryHostManager.tick(replacement)
            network.onEndServerTick()
            check(network.runtimeDevices(FluxNetwork.POINT).single().globalPos.pos() == newPos)
            check(network.getConnectionByPos(GlobalPos.of(helper.level.dimension(), oldPos)) == null)
            check(replacement.getData(FluxBatteryMod.HOST_ATTACHMENT.get()).point?.ticket == true)
            helper.level.removeBlock(newPos, false)
            network.onEndServerTick()
            check(network.runtimeDevices(FluxNetwork.POINT).isEmpty())
            check(tickets.values.none { it.contains(net.minecraft.world.level.ChunkPos(newPos).toLong()) })
            helper.succeed()
        } catch (error: Throwable) {
            error.printStackTrace()
            throw error
        } finally {
            player.setItemInHand(hand, previous)
            helper.level.removeBlock(newPos, false)
            helper.level.removeBlock(newPos.below(), false)
            cleanup(network, tile)
            BatteryServerConfig.chunkLoading.set(enabled)
            sonar.fluxnetworks.FluxConfig.enableChunkLoading = fluxEnabled
        }
    }

}
