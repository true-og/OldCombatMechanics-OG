/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package kernitus.plugin.OldCombatMechanics

import io.kotest.common.ExperimentalKotest
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.shouldBe
import kernitus.plugin.OldCombatMechanics.module.ModuleOldBrewingStand
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.block.BrewingStand
import org.bukkit.entity.Player
import org.bukkit.event.inventory.ClickType
import org.bukkit.event.inventory.InventoryAction
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.InventoryType
import org.bukkit.inventory.ItemStack
import org.bukkit.plugin.java.JavaPlugin
import java.util.concurrent.Callable

@OptIn(ExperimentalKotest::class)
class OldBrewingStandIntegrationTest : FunSpec({
    val testPlugin = JavaPlugin.getPlugin(OCMTestMain::class.java)
    val module = ModuleLoader.getModules()
        .filterIsInstance<ModuleOldBrewingStand>()
        .firstOrNull() ?: error("ModuleOldBrewingStand not registered")

    lateinit var player: Player
    lateinit var fakePlayer: FakePlayer
    lateinit var brewingStand: BrewingStand

    fun <T> runSync(action: () -> T): T {
        if (Bukkit.isPrimaryThread()) {
            return action()
        }
        return Bukkit.getScheduler().callSyncMethod(testPlugin, Callable { action() }).get()
    }

    fun fuelLevel(stand: BrewingStand): Int? {
        val getter = stand.javaClass.methods.firstOrNull {
            it.name == "getFuelLevel" && it.parameterCount == 0
        } ?: return null
        return (getter.invoke(stand) as? Number)?.toInt()
    }

    fun setFuelLevel(
        stand: BrewingStand,
        level: Int,
    ) {
        val setter = stand.javaClass.methods.firstOrNull {
            it.name == "setFuelLevel" && it.parameterCount == 1
        } ?: return
        setter.invoke(stand, level)
        stand.update()
    }

    fun openBrewingView() = runSync {
        player.openInventory(brewingStand.inventory)
    }

    extensions(MainThreadDispatcherExtension(testPlugin))

    beforeSpec {
        runSync {
            val world = checkNotNull(Bukkit.getWorld("world"))
            fakePlayer = FakePlayer(testPlugin)
            fakePlayer.spawn(Location(world, 0.0, 100.0, 0.0))
            player = checkNotNull(Bukkit.getPlayer(fakePlayer.uuid))

            val block = world.getBlockAt(2, 100, 0)
            block.type = Material.BREWING_STAND
            brewingStand = block.state as BrewingStand
        }
    }

    afterSpec {
        runSync {
            brewingStand.block.type = Material.AIR
            fakePlayer.removePlayer()
        }
    }

    beforeTest {
        runSync {
            player.closeInventory()
            player.inventory.clear()
            brewingStand.inventory.clear()
            setFuelLevel(brewingStand, 0)
            brewingStand.update()
        }
    }

    test("opening a brewing stand re-stocks the synthetic blaze powder") {
        val view = openBrewingView()

        runSync {
            if (view.topInventory.size <= 4) {
                return@runSync
            }

            view.topInventory.getItem(4)?.type shouldBe Material.BLAZE_POWDER
            fuelLevel(brewingStand.block.state as BrewingStand)?.let { it shouldBe 20 }
        }
    }

    test("fuel-slot clicks are cancelled and the blaze powder stays put") {
        val view = openBrewingView()

        runSync {
            if (view.topInventory.size <= 4) {
                return@runSync
            }

            val topInventory = view.topInventory
            topInventory.setItem(4, ItemStack(Material.BLAZE_POWDER, 1))
            setFuelLevel(brewingStand.block.state as BrewingStand, 0)

            val event = InventoryClickEvent(
                view,
                InventoryType.SlotType.CONTAINER,
                4,
                ClickType.LEFT,
                InventoryAction.PICKUP_ALL,
            )
            Bukkit.getPluginManager().callEvent(event)

            event.isCancelled.shouldBeTrue()
            topInventory.getItem(4)?.type shouldBe Material.BLAZE_POWDER
            fuelLevel(brewingStand.block.state as BrewingStand)?.let { it shouldBe 20 }
        }
    }
})
