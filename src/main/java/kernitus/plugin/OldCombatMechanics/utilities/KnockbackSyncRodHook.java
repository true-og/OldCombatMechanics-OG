/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package kernitus.plugin.OldCombatMechanics.utilities;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.lang.reflect.Method;

/**
 * Soft bridge to KnockbackSync-OG so it can own and latency-sync fishing-rod knockback
 * (see {@link kernitus.plugin.OldCombatMechanics.module.ModuleFishingKnockback}).
 *
 * <p>KnockbackSync is the velocity authority on this stack. Without this bridge a rod hit reaches
 * it as an ordinary melee damage event, so the rod pop is given melee-tuned latency correction and
 * ends up flattened/inconsistent. Handing KnockbackSync the bobber-direction vector lets it apply
 * the same latency sync it gives melee, so the rod feels like 1.8.</p>
 *
 * <p>Resolved reflectively (no compile-time dependency) so OCM keeps working when KnockbackSync is
 * absent. Mirrors the reflection approach used for other optional integrations on this stack.</p>
 */
public final class KnockbackSyncRodHook {

    private static final String PLUGIN_NAME = "KnockbackSync-OG";
    private static final String API_CLASS = "me.caseload.knockbacksync.api.KnockbackSyncRodApi";

    private static boolean resolved;
    private static Method applyRodKnockbackMethod; // null => unavailable

    private KnockbackSyncRodHook() {
    }

    /**
     * Asks KnockbackSync to own the given rod knockback for the victim. When it accepts, the
     * knockback is applied and latency-synced by the velocity event produced by the caller's
     * subsequent damage tick, so the caller must NOT also call {@code setVelocity}.
     *
     * @return true if KnockbackSync will apply the knockback; false if it is absent, disabled, or
     *         not tracking the player (the caller should then apply the knockback itself).
     */
    public static boolean applyRodKnockback(Player victim, Vector rodKnockback) {
        final Method method = resolve();
        if (method == null) return false;
        try {
            return (boolean) method.invoke(null, victim, rodKnockback);
        } catch (Throwable t) {
            // Any incompatibility: stop trying and let OCM apply the knockback itself.
            applyRodKnockbackMethod = null;
            return false;
        }
    }

    private static Method resolve() {
        if (resolved) return applyRodKnockbackMethod;
        resolved = true;
        if (Bukkit.getPluginManager().getPlugin(PLUGIN_NAME) == null)
            return null;
        try {
            final Class<?> apiClass = Class.forName(API_CLASS);
            applyRodKnockbackMethod = apiClass.getMethod("applyRodKnockback", Player.class, Vector.class);
        } catch (ReflectiveOperationException e) {
            applyRodKnockbackMethod = null;
        }
        return applyRodKnockbackMethod;
    }
}
