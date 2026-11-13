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

/** Draft edits stay local until Save; changing tabs or resizing preserves every field. */
class BatteryScreen(private val snapshot: BatterySnapshot) : Screen(Component.translatable("item.fluxbattery.flux_battery")) {
    private enum class Tab { SETTINGS, NETWORKS }
    private var tab = Tab.SETTINGS
    private var config = snapshot.config
    private var draftName = config.customName
    private var draftPriority = config.priority.toString()
    private var draftLimit = config.transferLimit.toString()
    private var search = ""
    private var page = 0
    private var error: Component? = null
    private var left = 0
    private var top = 0
    private var panelWidth = 324
    private val panelHeight = 226
    private val text = 0xFFF0F0F4.toInt()
    private val muted = 0xFFA2A2AE.toInt()
    private val line = 0xFF36363F.toInt()
    private val surface = 0xFF1B1B22.toInt()
    private val networkWidgets = mutableListOf<net.minecraft.client.gui.components.AbstractWidget>()
    private val stack by lazy { net.minecraft.world.item.ItemStack(FluxBatteryMod.BATTERY.get()) }
    private val accent: Int get() = (snapshot.networks.firstOrNull { it.id == config.networkId }?.color ?: 0x55B8EF) or 0xFF000000.toInt()

    private fun tr(key: String, vararg args: Any) = Component.translatable("screen.fluxbattery.$key", *args)
    private fun networkName(choice: NetworkChoice? = snapshot.networks.firstOrNull { it.id == config.networkId }): Component =
        when (choice?.id) {
            0 -> tr("disconnected")
            null -> tr("invalid_network")
            else -> Component.literal(choice.name)
        }

    override fun init() {
        panelWidth = minOf(324, width - 12)
        left = (width - panelWidth) / 2
        top = (height - panelHeight) / 2
        networkWidgets.clear()
        val tabWidth = (panelWidth - 28) / 2
        for (entry in Tab.entries) {
            button(left + 12 + entry.ordinal * (tabWidth + 4), top + 28, tabWidth, 18,
                tr(if (entry == Tab.SETTINGS) "settings" else "networks"), selected = { tab == entry }, tabButton = true) {
                if (tab != entry) { tab = entry; rebuildWidgets() }
            }
        }
        // This bar always names the draft selection, never a live connection while held.
        button(left + 12, top + 52, panelWidth - 24, 20, networkName(), tr("network_hint", networkName()), network = true) {
            tab = Tab.NETWORKS
            rebuildWidgets()
        }
        if (tab == Tab.SETTINGS) initSettings() else initNetworks()
        button(left + 12, top + 202, 90, 16, Component.translatable("gui.cancel")) { onClose() }
        button(left + 108, top + 202, panelWidth - 120, 16, tr("save"), primary = true) { save() }
    }

    private fun initSettings() {
        edit(left + 86, top + 80, panelWidth - 98, draftName, 24, "name") { draftName = it }
        edit(left + 72, top + 103, 48, draftPriority, 5, "priority", { it.matches(Regex("-?[0-9]*")) }) { draftPriority = it }
        val limit = edit(left + 165, top + 103, panelWidth - 177, draftLimit, 19, "limit", { it.all { c -> c in '0'..'9' } }) { draftLimit = it }
        limit.setEditable(!config.disableTransferLimit)
        limit.tooltip = net.minecraft.client.gui.components.Tooltip.create(tr("limit_hint"))
        val switchWidth = panelWidth - 138
        button(left + 12, top + 128, switchWidth, 16, tr("surge"), tr("surge_hint"), selected = { config.surgeMode }, switch = true) {
            config = config.copy(surgeMode = !config.surgeMode)
        }
        button(left + 12, top + 145, switchWidth, 16, tr("unlimited"), tr("unlimited_hint"), selected = { config.disableTransferLimit }, switch = true) {
            config = config.copy(disableTransferLimit = !config.disableTransferLimit)
            limit.setEditable(!config.disableTransferLimit)
        }
        button(left + 12, top + 162, switchWidth, 16, tr("chunkloading"), tr("chunkloading_hint"), selected = { config.chunkLoadingEnabled }, switch = true) {
            config = config.copy(chunkLoadingEnabled = !config.chunkLoadingEnabled)
        }
    }

    private fun edit(x: Int, y: Int, w: Int, value: String, max: Int, key: String,
                     filter: (String) -> Boolean = { true }, changed: (String) -> Unit): EditBox {
        val frameX = x
        val frameY = y
        val box = object : EditBox(font, x + 5, y + 4, w - 10, 12, tr(key)) {
            override fun renderWidget(g: GuiGraphics, mx: Int, my: Int, dt: Float) {
                g.fill(frameX, frameY, frameX + w, frameY + 16, 0xFF09090E.toInt())
                g.fill(frameX, frameY + 15, frameX + w, frameY + 16, if (isFocused) accent else line)
                super.renderWidget(g, mx, my, dt)
            }
        }
        box.setBordered(false)
        box.setTextColor(text)
        box.setTextColorUneditable(0xFF656572.toInt())
        box.setMaxLength(max)
        box.value = value
        box.setFilter(filter)
        box.setResponder { changed(it); error = null }
        addRenderableWidget(box)
        return box
    }

    private fun button(x: Int, y: Int, w: Int, h: Int, label: Component, tip: Component? = null,
                       selected: (() -> Boolean)? = null, primary: Boolean = false, tabButton: Boolean = false,
                       network: Boolean = false, switch: Boolean = false, color: Int? = null,
                       display: String? = null, action: () -> Unit): Button {
        val b = object : Button(x, y, w, h, label, { pressed ->
            action()
            if (switch && selected != null) pressed.message = tr("toggle", label, tr(if (selected()) "on" else "off"))
        }, { it.get() }) {
            override fun renderWidget(g: GuiGraphics, mx: Int, my: Int, dt: Float) {
                val checked = selected?.invoke() == true
                val tint = color?.or(0xFF000000.toInt()) ?: accent
                val hover = active && isHoveredOrFocused
                val ink = if (active) text else muted
                g.fill(this.x, this.y, this.x + this.width, this.y + this.height,
                    if (hover || checked || primary || network) tint else line)
                g.fill(this.x + 1, this.y + 1, this.x + this.width - 1, this.y + this.height - 1,
                    if (hover || checked || primary) 0xFF292934.toInt() else surface)
                if (switch) {
                    val track = this.x + this.width - 28
                    g.fill(track, this.y + 4, track + 22, this.y + 12, if (checked) tint else 0xFF4B4B57.toInt())
                    val thumb = track + if (checked) 14 else 0
                    g.fill(thumb, this.y + 3, thumb + 8, this.y + 13, if (hover) text else 0xFFC5C5D0.toInt())
                    drawFit(g, label.string, this.x + 6, this.y + 4, this.width - 39, ink)
                } else if (network) {
                    g.fill(this.x + 1, this.y + 1, this.x + 4, this.y + this.height - 1, tint)
                    drawFit(g, label.string, this.x + 9, this.y + (this.height - 8) / 2, this.width - 28, ink)
                    g.drawString(font, ">", this.x + this.width - 12, this.y + (this.height - 8) / 2, tint, false)
                } else {
                    if (tabButton && checked) g.fill(this.x + 1, this.y + this.height - 2, this.x + this.width - 1, this.y + this.height, tint)
                    val visible = fit(display ?: label.string, this.width - 12)
                    g.drawString(font, visible, this.x + (this.width - font.width(visible)) / 2,
                        this.y + (this.height - 8) / 2, ink, false)
                }
            }
        }
        // Update the accessible name at activation, independent of visual rendering.
        if (switch && selected != null) b.message = tr("toggle", label, tr(if (selected()) "on" else "off"))
        if (tip != null) b.tooltip = net.minecraft.client.gui.components.Tooltip.create(tip)
        addRenderableWidget(b)
        return b
    }

    private fun initNetworks() {
        val searchBox = edit(left + 12, top + 80, panelWidth - 24, search, 128, "search") {
            search = it
            page = 0
            refreshNetworks()
        }
        searchBox.setHint(tr("search_hint").withStyle(net.minecraft.ChatFormatting.GRAY))
        searchBox.tooltip = net.minecraft.client.gui.components.Tooltip.create(tr("search"))
        refreshNetworks()
    }

    private fun filteredNetworks() = snapshot.networks.filter { it.id != 0 &&
        (it.name.contains(search, ignoreCase = true) || it.id.toString().contains(search)) }

    private fun refreshNetworks() {
        networkWidgets.forEach { removeWidget(it) }
        networkWidgets.clear()
        val choices = filteredNetworks()
        val pages = maxOf(1, (choices.size + 3) / 4)
        page = page.coerceIn(0, pages - 1)
        for ((row, choice) in choices.drop(page * 4).take(4).withIndex()) {
            val name = networkName(choice)
            val b = button(left + 12, top + 101 + row * 18, panelWidth - 24, 16, name,
                tr("network_entry", choice.name, choice.id), { config.networkId == choice.id }, network = true, color = choice.color) {
                config = config.copy(networkId = choice.id)
                error = null
                rebuildWidgets()
            }
            networkWidgets.add(b)
        }
        val disconnect = button(left + 12, top + 177, 112, 16, tr("disconnect"), selected = { config.networkId == 0 }) {
            config = config.copy(networkId = 0)
            error = null
            rebuildWidgets()
        }
        networkWidgets.add(disconnect)
        val previous = button(left + panelWidth - 66, top + 177, 24, 16, tr("previous_page"), tr("previous_page"), display = "<") {
            page--; refreshNetworks()
        }
        previous.active = page > 0
        val next = button(left + panelWidth - 36, top + 177, 24, 16, tr("next_page"), tr("next_page"), display = ">") {
            page++; refreshNetworks()
        }
        next.active = page + 1 < pages
        networkWidgets.add(previous)
        networkWidgets.add(next)
    }

    private fun save() {
        val priority = draftPriority.toIntOrNull()
        val limit = draftLimit.toLongOrNull()
        error = when {
            priority == null || priority !in -9999..9999 -> tr("priority_error")
            limit == null || limit < 0 -> tr("limit_error")
            else -> null
        }
        if (error != null) {
            tab = Tab.SETTINGS
            rebuildWidgets()
            return
        }
        PacketDistributor.sendToServer(BatteryUpdate(snapshot.identity, snapshot.hand,
            config.copy(customName = draftName, priority = priority!!, transferLimit = limit!!)))
        onClose()
    }

    override fun renderBackground(g: GuiGraphics, mouseX: Int, mouseY: Int, partialTick: Float) {
        super.renderBackground(g, mouseX, mouseY, partialTick)
        g.fill(left + 3, top + 3, left + panelWidth + 3, top + panelHeight + 3, 0x88000000.toInt())
        // Independently drawn frame with the same network-color hierarchy as Flux devices.
        g.fill(left + 2, top, left + panelWidth - 2, top + panelHeight, accent)
        g.fill(left, top + 2, left + panelWidth, top + panelHeight - 2, accent)
        g.fill(left + 1, top + 2, left + panelWidth - 1, top + panelHeight - 2, 0xFF111116.toInt())
        g.fill(left + 2, top + 1, left + panelWidth - 2, top + 24, surface)
        g.renderItem(stack, left + 11, top + 5)
        g.drawString(font, title, left + 34, top + 9, text, false)
        val brand = "FLUX NETWORKS"
        g.drawString(font, brand, left + panelWidth - 12 - font.width(brand), top + 9, muted, false)
        if (tab == Tab.SETTINGS) renderSettings(g) else renderNetworks(g)
        g.fill(left + 12, top + 197, left + panelWidth - 12, top + 198, line)
    }

    private fun renderSettings(g: GuiGraphics) {
        g.drawString(font, tr("name"), left + 12, top + 84, muted, false)
        g.drawString(font, tr("priority"), left + 12, top + 107, muted, false)
        g.drawString(font, tr("limit_short"), left + 132, top + 107, muted, false)
        val x = left + panelWidth - 116
        g.fill(x, top + 128, left + panelWidth - 12, top + 178, surface)
        g.drawString(font, tr("energy"), x + 6, top + 133, muted, false)
        val ratio = energyRatio()
        val percent = "${(ratio * 100).toInt()}%"
        g.drawString(font, percent, left + panelWidth - 18 - font.width(percent), top + 133, accent, false)
        g.fill(x + 6, top + 146, left + panelWidth - 18, top + 151, 0xFF08080D.toInt())
        g.fill(x + 6, top + 146, x + 6 + (92 * ratio).toInt(), top + 151, accent)
        drawFit(g, compactEnergy(snapshot.energy.storedEnergy) + " FE", x + 6, top + 157, 92, text)
        drawFit(g, tr("capacity", compactEnergy(snapshot.energy.capacity)).string, x + 6, top + 168, 92, muted)
        val status = error ?: when {
            snapshot.networks.none { it.id != 0 } -> tr("no_networks")
            config.networkId == 0 -> tr("select_network")
            snapshot.networks.none { it.id == config.networkId } -> tr("invalid_network")
            else -> tr("ready")
        }
        val statusColor = if (error != null) 0xFFFF9585.toInt() else muted
        g.fill(left + 12, top + 184, left + 15, top + 187, if (error != null) statusColor else accent)
        g.drawWordWrap(font, status, left + 21, top + 181, panelWidth - 33, statusColor)
    }

    private fun renderNetworks(g: GuiGraphics) {
        val choices = filteredNetworks()
        if (choices.isEmpty()) g.drawWordWrap(font, tr(if (snapshot.networks.none { it.id != 0 }) "no_networks" else "no_results"),
            left + 18, top + 116, panelWidth - 36, muted)
        val pages = maxOf(1, (choices.size + 3) / 4)
        val paging = tr("page", page + 1, pages).string
        drawFit(g, paging, left + 134, top + 181, panelWidth - 206, muted)
    }

    override fun render(g: GuiGraphics, mouseX: Int, mouseY: Int, partialTick: Float) {
        super.render(g, mouseX, mouseY, partialTick)
        if (tab == Tab.SETTINGS && mouseX in left + panelWidth - 116 until left + panelWidth - 12 && mouseY in top + 128 until top + 178) {
            g.renderComponentTooltip(font, listOf(Component.literal(energyLabel()), tr("energy_hint")), mouseX, mouseY)
        }
    }

    private fun fit(value: String, available: Int): String = if (font.width(value) <= available) value else
        font.plainSubstrByWidth(value, maxOf(0, available - font.width("..."))) + "..."
    private fun drawFit(g: GuiGraphics, value: String, x: Int, y: Int, available: Int, color: Int) =
        g.drawString(font, fit(value, available), x, y, color, false)
    private fun energyRatio() = if (snapshot.energy.capacity > 0)
        (snapshot.energy.storedEnergy.toDouble() / snapshot.energy.capacity).coerceIn(0.0, 1.0) else 0.0
    private fun locale() = java.util.Locale.forLanguageTag(Minecraft.getInstance().languageManager.selected.replace('_', '-'))
    private fun compactEnergy(value: Long) = java.text.NumberFormat.getCompactNumberInstance(locale(), java.text.NumberFormat.Style.SHORT).apply {
        maximumFractionDigits = 1
    }.format(value)
    private fun energyLabel(): String {
        val numbers = java.text.NumberFormat.getIntegerInstance(locale())
        return "${numbers.format(snapshot.energy.storedEnergy)} / ${numbers.format(snapshot.energy.capacity)} FE"
    }
    override fun isPauseScreen() = false
}
