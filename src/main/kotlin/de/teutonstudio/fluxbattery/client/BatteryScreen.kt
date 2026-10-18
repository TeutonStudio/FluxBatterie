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

/** A held battery is configured here; it only connects once installed in a machine. */
class BatteryScreen(private val snapshot: BatterySnapshot) : Screen(Component.translatable("item.fluxbattery.flux_battery")) {
    private var config = snapshot.config
    private lateinit var nameBox: EditBox
    private lateinit var priorityBox: EditBox
    private lateinit var limitBox: EditBox
    private var error: Component? = null
    private var left = 0
    private var top = 0
    private var panelWidth = 368
    private val panelHeight = 234
    private val accent = 0xFF54DDD0.toInt()
    private val muted = 0xFF93A9AF.toInt()
    private val text = 0xFFE2EFF0.toInt()
    private val stack by lazy { net.minecraft.world.item.ItemStack(FluxBatteryMod.BATTERY.get()) }

    private fun tr(key: String, vararg args: Any) = Component.translatable("screen.fluxbattery.$key", *args)
    private fun networkName(): Component = if (config.networkId == 0) tr("disconnected") else
        snapshot.networks.firstOrNull { it.id == config.networkId }?.let { Component.literal(it.name) } ?: tr("invalid_network")

    override fun init() {
        panelWidth = minOf(368, width - 12)
        left = (width - panelWidth) / 2
        top = maxOf(0, (height - panelHeight) / 2)
        val inner = panelWidth - 24
        fun edit(x: Int, y: Int, w: Int, value: String, max: Int, key: String): EditBox {
            val box = object : EditBox(font, x, y, w, 18, tr(key)) {
                override fun renderWidget(g: GuiGraphics, mx: Int, my: Int, dt: Float) {
                    g.fill(this.x - 4, this.y - 5, this.x + this.width + 4, this.y + 13, 0xFF0B141A.toInt())
                    g.fill(this.x - 4, this.y + 12, this.x + this.width + 4, this.y + 13,
                        if (isFocused) accent else 0xFF344C55.toInt())
                    super.renderWidget(g, mx, my, dt)
                }
            }
            box.setBordered(false)
            box.setTextColor(text)
            box.setTextColorUneditable(muted)
            box.setMaxLength(max)
            box.value = value
            box.setResponder { error = null }
            addRenderableWidget(box)
            return box
        }
        fun button(x: Int, y: Int, w: Int, label: Component, tip: Component? = null, selected: (() -> Boolean)? = null, primary: Boolean = false, action: () -> Unit): Button {
            val b = object : Button(x, y, w, 20, label, { action() }, { it.get() }) {
                override fun renderWidget(g: GuiGraphics, mx: Int, my: Int, dt: Float) {
                    val enabled = primary || selected?.invoke() == true
                    val edge = if (isHoveredOrFocused) accent else if (enabled) 0xFF398F89.toInt() else 0xFF344C55.toInt()
                    g.fill(this.x, this.y, this.x + this.width, this.y + this.height, edge)
                    g.fill(this.x + 1, this.y + 1, this.x + this.width - 1, this.y + this.height - 1,
                        if (isHoveredOrFocused || enabled) 0xFF203C42.toInt() else 0xFF182830.toInt())
                    val inset = if (selected != null) 17 else 6
                    if (selected != null) {
                        g.fill(this.x + 6, this.y + 7, this.x + 12, this.y + 13, if (enabled) accent else muted)
                        if (enabled) g.fill(this.x + 8, this.y + 8, this.x + 10, this.y + 11, 0xFF10252A.toInt())
                        message = tr("toggle", label, tr(if (enabled) "on" else "off"))
                    }
                    val visible = font.plainSubstrByWidth(label.string, this.width - inset - 5)
                    g.drawString(font, visible, this.x + inset, this.y + 6, if (active) text else muted, false)
                }
            }
            if (tip != null) b.tooltip = net.minecraft.client.gui.components.Tooltip.create(tip)
            addRenderableWidget(b)
            return b
        }
        fun cycle(direction: Int) {
            if (snapshot.networks.isNotEmpty()) {
                val index = snapshot.networks.indexOfFirst { it.id == config.networkId }
                config = config.copy(networkId = snapshot.networks[Math.floorMod(index + direction, snapshot.networks.size)].id)
                error = null
                rebuildWidgets()
            }
        }
        button(left + 12, top + 63, 20, Component.literal("<"), tr("previous_network")) { cycle(-1) }
        button(left + 36, top + 63, inner - 48, networkName(), tr("network_hint", networkName())) { cycle(1) }
        button(left + panelWidth - 32, top + 63, 20, Component.literal(">"), tr("next_network")) { cycle(1) }
        nameBox = edit(left + 16, top + 100, inner - 8,
            if (this::nameBox.isInitialized) nameBox.value else config.customName, 24, "name")
        val priorityWidth = 86
        priorityBox = edit(left + 16, top + 135, priorityWidth - 8,
            if (this::priorityBox.isInitialized) priorityBox.value else config.priority.toString(), 5, "priority")
        priorityBox.setFilter { it.matches(Regex("-?[0-9]*")) }
        limitBox = edit(left + 28 + priorityWidth, top + 135, inner - priorityWidth - 20,
            if (this::limitBox.isInitialized) limitBox.value else config.transferLimit.toString(), 19, "limit")
        limitBox.setFilter { it.all { c -> c in '0'..'9' } }
        limitBox.setEditable(!config.disableTransferLimit)
        limitBox.tooltip = net.minecraft.client.gui.components.Tooltip.create(tr("limit_hint"))
        val switchWidth = (inner - 8) / 3
        button(left + 12, top + 157, switchWidth, tr("unlimited"), tr("unlimited_hint"), { config.disableTransferLimit }) {
            config = config.copy(disableTransferLimit = !config.disableTransferLimit)
            limitBox.setEditable(!config.disableTransferLimit)
        }
        button(left + 16 + switchWidth, top + 157, switchWidth, tr("surge"), tr("surge_hint"), { config.surgeMode }) {
            config = config.copy(surgeMode = !config.surgeMode)
        }
        button(left + 20 + switchWidth * 2, top + 157, switchWidth, tr("chunkloading"), tr("chunkloading_hint"), { config.chunkLoadingEnabled }) {
            config = config.copy(chunkLoadingEnabled = !config.chunkLoadingEnabled)
        }
        button(left + 12, top + 204, 80, Component.translatable("gui.cancel")) { onClose() }
        button(left + 100, top + 204, inner - 88, tr("save"), primary = true) { save() }
    }

    private fun save() {
        val priority = priorityBox.value.toIntOrNull()
        val limit = limitBox.value.toLongOrNull()
        error = when {
            priority == null || priority !in -9999..9999 -> tr("priority_error")
            limit == null || limit < 0 -> tr("limit_error")
            else -> null
        }
        if (error != null) return
        PacketDistributor.sendToServer(BatteryUpdate(snapshot.identity, snapshot.hand,
            config.copy(customName = nameBox.value, priority = priority!!, transferLimit = limit!!)))
        onClose()
    }

    override fun renderBackground(graphics: GuiGraphics, mouseX: Int, mouseY: Int, partialTick: Float) {
        super.renderBackground(graphics, mouseX, mouseY, partialTick)
        graphics.fill(left + 3, top + 3, left + panelWidth + 3, top + panelHeight + 3, 0x88000000.toInt())
        graphics.fill(left, top, left + panelWidth, top + panelHeight, 0xFF3C5962.toInt())
        graphics.fill(left + 1, top + 1, left + panelWidth - 1, top + panelHeight - 1, 0xFF101C23.toInt())
        graphics.fill(left + 1, top + 1, left + panelWidth - 1, top + 30, 0xFF1B2D35.toInt())
        graphics.fill(left + 12, top, left + 70, top + 2, accent)
        graphics.renderItem(stack, left + 12, top + 8)
        graphics.drawString(font, title, left + 34, top + 7, text, false)
        graphics.drawString(font, tr("subtitle"), left + 34, top + 18, muted, false)
        val ratio = if (snapshot.energy.capacity > 0) (snapshot.energy.storedEnergy.toDouble() / snapshot.energy.capacity).coerceIn(0.0, 1.0) else 0.0
        val energy = energyLabel()
        graphics.drawString(font, tr("energy"), left + 12, top + 37, muted, false)
        val available = panelWidth - 24 - font.width(tr("energy")) - 8
        val energyText = font.plainSubstrByWidth(energy, available)
        graphics.drawString(font, energyText, left + panelWidth - 12 - font.width(energyText), top + 37, text, false)
        graphics.fill(left + 12, top + 50, left + panelWidth - 12, top + 55, 0xFF081116.toInt())
        graphics.fill(left + 12, top + 50, left + 12 + ((panelWidth - 24) * ratio).toInt(), top + 55, accent)
        for (i in 1..19) {
            val x = left + 12 + (panelWidth - 24) * i / 20
            graphics.fill(x, top + 50, x + 1, top + 55, 0xFF101C23.toInt())
        }
        graphics.drawString(font, tr("name"), left + 12, top + 87, muted, false)
        graphics.drawString(font, tr("priority"), left + 12, top + 122, muted, false)
        graphics.drawString(font, tr("limit"), left + 110, top + 122, muted, false)
        val status = error ?: when {
            snapshot.networks.none { it.id != 0 } -> tr("no_networks")
            config.networkId == 0 -> tr("select_network")
            snapshot.networks.none { it.id == config.networkId } -> tr("invalid_network")
            else -> tr("ready")
        }
        val statusColor = if (error != null) 0xFFFF977F.toInt() else muted
        graphics.fill(left + 12, top + 185, left + 16, top + 189, if (error != null) statusColor else accent)
        graphics.drawWordWrap(font, status, left + 22, top + 182, panelWidth - 34, statusColor)
    }

    override fun render(graphics: GuiGraphics, mouseX: Int, mouseY: Int, partialTick: Float) {
        super.render(graphics, mouseX, mouseY, partialTick)
        if (mouseX in left + 12 until left + panelWidth - 12 && mouseY in top + 34 until top + 56) {
            graphics.renderTooltip(font, Component.literal(energyLabel()), mouseX, mouseY)
        }
    }
    private fun energyLabel(): String {
        val locale = java.util.Locale.forLanguageTag(Minecraft.getInstance().languageManager.selected.replace('_', '-'))
        val numbers = java.text.NumberFormat.getIntegerInstance(locale)
        return "${numbers.format(snapshot.energy.storedEnergy)} / ${numbers.format(snapshot.energy.capacity)} FE"
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
