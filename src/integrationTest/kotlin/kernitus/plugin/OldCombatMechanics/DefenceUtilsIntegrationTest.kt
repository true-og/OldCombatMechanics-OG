/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package kernitus.plugin.OldCombatMechanics

import com.cryptomorin.xseries.XEnchantment
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.doubles.plusOrMinus
import io.kotest.matchers.shouldBe
import kernitus.plugin.OldCombatMechanics.utilities.damage.DefenceUtils
import org.bukkit.Material
import org.bukkit.event.entity.EntityDamageEvent
import org.bukkit.inventory.ItemStack

class DefenceUtilsIntegrationTest :
    FunSpec({
        val enchantmentReductionMethod =
            DefenceUtils::class.java.getDeclaredMethod(
                "calculateArmourEnchantmentReductionFactor",
                Array<ItemStack>::class.java,
                EntityDamageEvent.DamageCause::class.java,
                Boolean::class.javaPrimitiveType
            ).also { it.isAccessible = true }

        fun protectionArmour(): Array<ItemStack> {
            val protection = checkNotNull(XEnchantment.PROTECTION.get())
            return arrayOf(
                ItemStack(Material.DIAMOND_BOOTS),
                ItemStack(Material.DIAMOND_LEGGINGS),
                ItemStack(Material.DIAMOND_CHESTPLATE),
                ItemStack(Material.DIAMOND_HELMET)
            ).also { armour ->
                armour.forEach { it.addUnsafeEnchantment(protection, 4) }
            }
        }

        fun enchantmentReduction(cause: EntityDamageEvent.DamageCause): Double = enchantmentReductionMethod.invoke(
            null,
            protectionArmour(),
            cause,
            false
        ) as Double

        test("protection applies to burn ticks like vanilla 1.8") {
            enchantmentReduction(EntityDamageEvent.DamageCause.FIRE_TICK) shouldBe (0.8 plusOrMinus 0.0001)
        }
    })
