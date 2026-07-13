/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package kernitus.plugin.OldCombatMechanics.utilities;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.Vector;

import java.lang.reflect.Method;

/** Reflective bridge handing KnockbackSync-OG the rod knockback for latency sync (see ModuleFishingKnockback); caches per plugin instance so a KnockbackSync reload re-resolves on the next rod hit. */
public final class KnockbackSyncRodHook {

    private static final String PLUGIN_NAME = "KnockbackSync-OG";
    private static final String API_CLASS = "me.caseload.knockbacksync.api.KnockbackSyncRodApi";

    // KnockbackSync instance the cached method was resolved against; a different instance means a reload happened.
    private static Plugin resolvedPlugin;
    private static Method applyRodKnockbackMethod; // null => unavailable for resolvedPlugin
    private static boolean warnedInvokeFailure;

    private KnockbackSyncRodHook() {
    }

    /** @return true if KnockbackSync will own and latency-sync the rod knockback (caller must NOT also setVelocity); false if absent/disabled/not tracking the player (caller applies it itself). */
    public static boolean applyRodKnockback(Player victim, Vector rodKnockback) {
        final Method method = resolve();
        if (method == null) return false;
        try {
            return (boolean) method.invoke(null, victim, rodKnockback);
        } catch (Throwable t) {
            // Drop the cache so the next hit re-resolves against the live plugin instance; warn once.
            resolvedPlugin = null;
            applyRodKnockbackMethod = null;
            if (!warnedInvokeFailure) {
                warnedInvokeFailure = true;
                Bukkit.getLogger().warning("[OldCombatMechanics] KnockbackSync rod hook call failed; " +
                        "will re-resolve on the next rod hit: " + t);
            }
            return false;
        }
    }

    private static Method resolve() {
        final Plugin plugin = Bukkit.getPluginManager().getPlugin(PLUGIN_NAME);
        if (plugin == null || !plugin.isEnabled()) {
            resolvedPlugin = null;
            applyRodKnockbackMethod = null;
            return null;
        }
        if (plugin == resolvedPlugin) return applyRodKnockbackMethod;

        // Resolve through the plugin's own classloader so a reloaded KnockbackSync yields the live class.
        try {
            final Class<?> apiClass = plugin.getClass().getClassLoader().loadClass(API_CLASS);
            applyRodKnockbackMethod = apiClass.getMethod("applyRodKnockback", Player.class, Vector.class);
        } catch (ReflectiveOperationException e) {
            applyRodKnockbackMethod = null;
        }
        resolvedPlugin = plugin; // cache the outcome (even a miss) per plugin instance
        return applyRodKnockbackMethod;
    }
}
