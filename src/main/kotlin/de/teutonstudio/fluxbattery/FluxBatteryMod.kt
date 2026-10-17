package de.teutonstudio.fluxbattery

import net.minecraft.core.registries.Registries
import net.neoforged.neoforge.attachment.AttachmentType
import net.neoforged.neoforge.registries.NeoForgeRegistries
import net.neoforged.neoforge.common.world.chunk.RegisterTicketControllersEvent
import net.neoforged.neoforge.common.NeoForge
import net.neoforged.neoforge.event.server.ServerStoppingEvent
import java.util.function.Supplier
import net.minecraft.world.item.Item
import net.minecraft.world.item.CreativeModeTabs
import net.neoforged.bus.api.IEventBus
import net.neoforged.fml.ModContainer
import net.neoforged.fml.common.Mod
import net.neoforged.fml.config.ModConfig
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent
import net.neoforged.neoforge.capabilities.Capabilities
import net.neoforged.neoforge.common.ModConfigSpec
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent
import net.neoforged.neoforge.registries.DeferredRegister
import sonar.fluxnetworks.api.FluxCapabilities

@Mod(FluxBatteryMod.ID)
class FluxBatteryMod(bus: IEventBus, container: ModContainer) {
    init {
        de.teutonstudio.fluxbattery.bridge.RuntimePatchGuard.verify()
        if (!net.neoforged.fml.loading.FMLLoader.isProduction() && System.getProperty("fluxbattery.dedicatedRegression") == "true") {
            NeoForge.EVENT_BUS.addListener<net.neoforged.neoforge.event.server.ServerStartedEvent> {
                de.teutonstudio.fluxbattery.gametest.DedicatedRegressionReporter.install()
            }
        }
        ITEMS.register(bus)
        bus.addListener<net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent>(BatteryNetworking::register)
        ATTACHMENTS.register(bus)
        if (!net.neoforged.fml.loading.FMLLoader.isProduction() && System.getProperty("fluxbattery.restartStage") != null) {
            bus.addListener<net.neoforged.neoforge.event.RegisterGameTestsEvent> { it.register(de.teutonstudio.fluxbattery.gametest.RestartGameTests::class.java) }
        }
        bus.addListener<RegisterTicketControllersEvent> { it.register(BatteryChunkTicketController.CONTROLLER) }
        NeoForge.EVENT_BUS.addListener<ServerStoppingEvent> { FluxBatteryHostManager.shutdown(); BatteryChunkTicketController.clear() }
        BatteryComponents.REGISTER.register(bus)
        bus.addListener<RegisterCapabilitiesEvent> { event ->
            event.registerItem(Capabilities.EnergyStorage.ITEM, { stack, _ -> BatteryEnergyHandler(stack) }, BATTERY.get())
            event.registerItem(FluxCapabilities.ITEM, { stack, _ -> BatteryEnergyHandler(stack) }, BATTERY.get())
        }
        bus.addListener<BuildCreativeModeTabContentsEvent> { event ->
            if (event.tabKey == CreativeModeTabs.TOOLS_AND_UTILITIES) event.accept(BATTERY.get())
        }
        container.registerConfig(ModConfig.Type.SERVER, BatteryServerConfig.SPEC)
    }
    companion object {
        const val ID = "fluxbattery"
        val ITEMS = DeferredRegister.createItems(ID)
        val BATTERY = ITEMS.register("flux_battery", Supplier { FluxBatteryItem(Item.Properties().stacksTo(1)) })
        val ATTACHMENTS = DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, ID)
        val HOST_ATTACHMENT = ATTACHMENTS.register("host", Supplier { AttachmentType.builder(Supplier { FluxPointAttachment() }).build() })
    }
}

object BatteryServerConfig {
    private val builder = ModConfigSpec.Builder()
    val chunkLoading = builder.comment("Allow battery chunk tickets. Also respects Flux Networks' chunkloading switch.").define("allowChunkLoading", false)
    val ticketQuota = builder.defineInRange("maxTicketsPerOwner", 4, 0, 1024)
    val capacity = builder.defineInRange("batteryCapacityFE", 1_000_000_000L, 1L, Long.MAX_VALUE)
    val SPEC: ModConfigSpec = builder.build()
}
