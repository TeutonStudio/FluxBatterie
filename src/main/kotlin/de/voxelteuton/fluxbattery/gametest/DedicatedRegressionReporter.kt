package de.voxelteuton.fluxbattery.gametest

import net.minecraft.gametest.framework.GameTestInfo
import net.minecraft.gametest.framework.GameTestRegistry
import net.minecraft.gametest.framework.GlobalTestReporter
import net.minecraft.gametest.framework.LogTestReporter
import net.minecraft.gametest.framework.TestReporter
import org.slf4j.LoggerFactory

/** TestCommand broadcasts its summary to players, so a headless console needs a reporter. */
object DedicatedRegressionReporter {
    fun install() {
        val expected = GameTestRegistry.getAllTestFunctions().size
        check(expected == 14) { "Dedicated regression expected 14 registered tests, found $expected" }
        val log = LoggerFactory.getLogger("FluxBatteryDedicatedRegression")
        GlobalTestReporter.replaceWith(object : TestReporter {
            private val delegate = LogTestReporter()
            private var completed = 0
            private var failed = 0
            override fun onTestSuccess(info: GameTestInfo) {
                log.info("Dedicated regression PASS: {}", info.testName)
                completed()
            }
            override fun onTestFailed(info: GameTestInfo) {
                delegate.onTestFailed(info)
                failed++
                completed()
            }
            private fun completed() {
                completed++
                if (completed == expected) {
                    if (failed == 0) log.info("DEDICATED REGRESSION COMPLETE: {} required tests passed", completed)
                    else log.error("DEDICATED REGRESSION FAILED: {} of {} tests failed", failed, completed)
                }
            }
        })
    }
}
