package de.teutonstudio.fluxbattery.client

import de.teutonstudio.fluxbattery.*
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.components.Button
import net.minecraft.client.gui.components.EditBox
import net.minecraft.client.gui.screens.Screen
import net.minecraft.network.chat.Component
import net.neoforged.api.distmarker.Dist
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.fml.common.EventBusSubscriber
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent
import net.neoforged.neoforge.network.PacketDistributor

@EventBusSubscriber(modid = FluxBatteryMod.ID, bus = EventBusSubscriber.Bus.MOD, value = [Dist.CLIENT])
object BatteryClient {
    @SubscribeEvent
    fun setup(event: FMLClientSetupEvent) {
        BatteryNetworking.clientReceiver = { Minecraft.getInstance().setScreen(BatteryScreen(it)) }
        if (!net.neoforged.fml.loading.FMLLoader.isProduction() && java.lang.Boolean.getBoolean("fluxbattery.clientSmoke")) ClientSmoke.install()
    }
}

class BatteryScreen(private val snapshot: BatterySnapshot) : Screen(Component.literal("Flux Battery")) {
    private var config = snapshot.config
    private lateinit var nameBox: EditBox
    private lateinit var priorityBox: EditBox
    private lateinit var limitBox: EditBox
    private var error = ""
    override fun init() {
        val left = width / 2 - 150
        val top = maxOf(4, (height - 224) / 2)
        fun edit(y: Int, value: String, maxLength: Int): EditBox {
            val box = EditBox(font, left + 115, y, 185, 20, Component.empty())
            box.setMaxLength(maxLength); box.value = value
            addRenderableWidget(box)
            return box
        }
        nameBox = edit(top + 50, if (this::nameBox.isInitialized) nameBox.value else config.customName, 24)
        priorityBox = edit(top + 74, if (this::priorityBox.isInitialized) priorityBox.value else config.priority.toString(), 6)
        limitBox = edit(top + 98, if (this::limitBox.isInitialized) limitBox.value else config.transferLimit.toString(), 19)
        fun button(y: Int, label: () -> String, action: () -> Unit) {
            addRenderableWidget(Button.builder(Component.literal(font.plainSubstrByWidth(label(), 280))) { b -> action(); b.message = Component.literal(font.plainSubstrByWidth(label(), 280)) }.bounds(left, y, 300, 20).build())
        }
        button(top + 26, { "Network: ${snapshot.networks.firstOrNull { it.id == config.networkId }?.name ?: "Invalid / not authorized"}" }) {
            val index = snapshot.networks.indexOfFirst { it.id == config.networkId }
            if (snapshot.networks.isNotEmpty()) {
                config = config.copy(networkId = snapshot.networks[(index + 1) % snapshot.networks.size].id)
            }
        }
        button(top + 122, { "Unlimited transfer: ${config.disableTransferLimit}" }) { config = config.copy(disableTransferLimit = !config.disableTransferLimit) }
        button(top + 145, { "Surge: ${config.surgeMode}" }) { config = config.copy(surgeMode = !config.surgeMode) }
        button(top + 168, { "Chunkloading: ${config.chunkLoadingEnabled}" }) { config = config.copy(chunkLoadingEnabled = !config.chunkLoadingEnabled) }
        button(top + 204, { "Save configuration" }) {
            val priority = priorityBox.value.toIntOrNull()
            val limit = limitBox.value.toLongOrNull()
            if (priority == null || priority !in -9999..9999 || limit == null || limit < 0) error = "Priority: -9999..9999; limit: nonnegative Long"
            else {
                PacketDistributor.sendToServer(BatteryUpdate(snapshot.identity, snapshot.hand, config.copy(customName = nameBox.value, priority = priority, transferLimit = limit)))
                onClose()
            }
        }
    }
    override fun render(graphics: GuiGraphics, mouseX: Int, mouseY: Int, partialTick: Float) {
        super.render(graphics, mouseX, mouseY, partialTick)
        val left = width / 2 - 150; val top = maxOf(4, (height - 224) / 2)
        graphics.drawString(font, title, left, top, 0xFFFFFF)
        graphics.drawString(font, "${snapshot.energy.storedEnergy} / ${snapshot.energy.capacity} FE", left, top + 15, 0xAAFFAA)
        graphics.drawString(font, "Name", left, top + 56, 0xFFFFFF)
        graphics.drawString(font, "Priority", left, top + 80, 0xFFFFFF)
        graphics.drawString(font, "Limit FE/cycle", left, top + 104, 0xFFFFFF)
        val status = when {
            error.isNotEmpty() -> error
            snapshot.networks.none { it.id != 0 } -> "Create an accessible network in Flux Networks first"
            config.networkId == 0 -> "Select a network above, then save"
            snapshot.networks.none { it.id == config.networkId } -> "Status: invalid network / not authorized"
            else -> "Status: ready (held item is disconnected)"
        }
        graphics.drawString(font, status, left, top + 191, if (error.isEmpty()) 0xAAAAAA else 0xFF5555)
    }
    override fun isPauseScreen() = false
}


/** Development-only screenshot harness, enabled by -PclientSmoke. */
private object ClientSmoke {
    private var frames = 0
    fun install() {
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener<net.neoforged.neoforge.client.event.ScreenEvent.Render.Post> { event ->
            val minecraft = Minecraft.getInstance()
            if (event.screen !is BatteryScreen && minecraft.overlay == null && frames == 0) {
                frames = 1
                minecraft.execute {
                    minecraft.setScreen(BatteryScreen(BatterySnapshot(java.util.UUID.randomUUID(), net.minecraft.world.InteractionHand.MAIN_HAND,
                        BatteryConfig(123, java.util.UUID.randomUUID(), "Digital Miner", 10, 1_000_000), BatteryEnergy(250_000_000, 1_000_000_000),
                        listOf(NetworkChoice(0, "Disconnected"), NetworkChoice(123, "Factory network")))))
                }
            } else if (event.screen is BatteryScreen && frames in 1..40) {
                frames++
                if (frames == 40) {
                    event.guiGraphics.flush()
                    val target = java.nio.file.Path.of("../docs/evidence/battery-screen.png")
                    java.nio.file.Files.createDirectories(target.parent)
                    net.minecraft.client.Screenshot.takeScreenshot(minecraft.mainRenderTarget).use { it.writeToFile(target) }
                    com.mojang.logging.LogUtils.getLogger().info("Flux Battery client smoke screenshot captured")
                    minecraft.execute { minecraft.stop() }
                }
            }
        }
    }
}
