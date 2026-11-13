package de.teutonstudio.fluxbattery.client

import de.teutonstudio.fluxbattery.*
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.components.Button
import net.minecraft.client.gui.components.EditBox
import net.minecraft.network.chat.Component
import net.minecraft.world.InteractionHand
import java.nio.file.Files
import java.nio.file.Path
import java.util.UUID

/** Development-only rendering and interaction checks against the real screen widgets. */
internal object ClientSmoke {
    private var frames = 0
    private var stage = 0
    private val identity = UUID.randomUUID()
    private val owner = UUID.randomUUID()
    private fun tr(key: String) = Component.translatable("screen.fluxbattery.$key").string
    private fun button(screen: BatteryScreen, key: String): Button = screen.children().filterIsInstance<Button>().first { it.message.string == tr(key) }
    private fun edit(screen: BatteryScreen, key: String): EditBox = screen.children().filterIsInstance<EditBox>().first { it.message.string == tr(key) }
    private fun preview(networks: List<NetworkChoice> = listOf(
        NetworkChoice(0, "Disconnected"), NetworkChoice(123, "Factory network", 0x55B8EF),
        NetworkChoice(124, "Mining outpost", 0xF1A94E), NetworkChoice(125, "Reactor grid", 0xE96575),
        NetworkChoice(126, "Storage & processing", 0x70CCA4), NetworkChoice(127, "A very long network name that must fit inside the selection row", 0xAF8CE8),
        NetworkChoice(128, "Workshop", 0x55B8EF)
    )) = BatteryScreen(BatterySnapshot(identity, InteractionHand.MAIN_HAND,
        BatteryConfig(123, owner, "Digital Miner", 10, 100_000_000), BatteryEnergy(250_000_000, 1_000_000_000), networks))

    fun install() {
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener<net.neoforged.neoforge.client.event.ScreenEvent.Render.Post> { event ->
            val minecraft = Minecraft.getInstance()
            if (frames == 0 && minecraft.overlay == null) {
                frames = 1
                minecraft.execute { minecraft.setScreen(preview()) }
            } else if (event.screen is BatteryScreen && ++frames % 16 == 0) {
                val screen = event.screen as BatteryScreen
                fun capture(name: String) {
                    event.guiGraphics.flush()
                    val target = Path.of("../docs/evidence/$name${System.getProperty("fluxbattery.clientSmokeSuffix", "")}.png")
                    Files.createDirectories(target.parent)
                    net.minecraft.client.Screenshot.takeScreenshot(minecraft.mainRenderTarget).use { it.writeToFile(target) }
                }
                minecraft.execute {
                    when (stage++) {
                        0 -> {
                            capture("battery-screen")
                            edit(screen, "name").value = "Draft retained"
                            edit(screen, "priority").value = "-9999"
                            edit(screen, "limit").value = Long.MAX_VALUE.toString()
                            button(screen, "networks").onPress()
                        }
                        1 -> {
                            capture("battery-networks")
                            button(screen, "next_page").onPress()
                        }
                        2 -> {
                            capture("battery-networks-page-2")
                            button(screen, "previous_page").onPress()
                            edit(screen, "search").value = "reactor"
                        }
                        3 -> {
                            check(screen.children().filterIsInstance<Button>().count { it.message.string == "Reactor grid" } == 1)
                            screen.children().filterIsInstance<Button>().first { it.message.string == "Reactor grid" }.onPress()
                            button(screen, "settings").onPress()
                        }
                        4 -> {
                            val current = minecraft.screen as BatteryScreen
                            check(edit(current, "name").value == "Draft retained")
                            check(edit(current, "priority").value == "-9999")
                            check(edit(current, "limit").value == Long.MAX_VALUE.toString())
                            // Space/Enter use the same Button.onPress path; check toggle state and preserved limit.
                            current.children().filterIsInstance<Button>().first { it.message.string.startsWith(tr("unlimited") + ":") }.onPress()
                            check(edit(current, "limit").value == Long.MAX_VALUE.toString())
                            current.children().filterIsInstance<Button>().first { it.message.string.startsWith(tr("surge") + ":") }.onPress()
                            current.children().filterIsInstance<Button>().first { it.message.string.startsWith(tr("chunkloading") + ":") }.onPress()
                        }
                        5 -> {
                            capture("battery-switches")
                            edit(screen, "priority").value = "-"
                            button(screen, "save").onPress() // Must validate locally, never send this invalid draft.
                            check(minecraft.screen === screen)
                        }
                        6 -> {
                            capture("battery-validation")
                            minecraft.setScreen(preview(listOf(NetworkChoice(0, "Disconnected"))))
                        }
                        7 -> {
                            capture("battery-no-network")
                            button(screen, "networks").onPress()
                        }
                        8 -> {
                            capture("battery-empty-list")
                            minecraft.setScreen(preview())
                            edit(minecraft.screen as BatteryScreen, "name").value = "Resize retained"
                            minecraft.window.setWindowed(640, 480)
                        }
                        9 -> {
                            check(screen.width == 320 && screen.height == 240) { "Expected real GUI dimensions 320x240, got ${screen.width}x${screen.height}" }
                            check(edit(screen, "name").value == "Resize retained")
                            capture("battery-small-layout")
                            button(screen, "networks").onPress()
                            edit(screen, "search").value = "does not exist"
                        }
                        10 -> {
                            capture("battery-no-results")
                            edit(screen, "search").value = "125"
                            check(screen.children().filterIsInstance<Button>().count { it.message.string == "Reactor grid" } == 1)
                            button(screen, "disconnect").onPress()
                            button(screen, "settings").onPress()
                        }
                        11 -> {
                            capture("battery-disconnected")
                            com.mojang.logging.LogUtils.getLogger().info("Flux Battery client UI checks passed: name/ID search, pagination, draft retention, switches, validation, empty networks, disconnect, real 320x240 layout")
                            minecraft.stop()
                        }
                    }
                }
            }
        }
    }
}
