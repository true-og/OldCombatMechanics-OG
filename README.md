<!--
     This Source Code Form is subject to the terms of the Mozilla Public
     License, v. 2.0. If a copy of the MPL was not distributed with this
     file, You can obtain one at https://mozilla.org/MPL/2.0/.
-->

# OldCombatMechanics-OG
#### a soft fork of [BukkitOldCombatMechanics](https://github.com/kernitus/BukkitOldCombatMechanics) by kernitus and Rayzr522
Maintained by [NotAlexNoyle](https://github.com/NotAlexNoyle/) for [TrueOG](https://true-og.net).

## Changes
- Removed metrics.
- Removed log4j.
- Updated gradle and dependencies.
- Includes TrueOG Network config.yml.

## Installation
Build the plugin using:

./gradlew build

Then move it to your server's plugins folder.

## 🧰 Modesets
Modesets can be configured for your players to choose from. Each modeset can have any combination of the below features enabled, with examples provided to replicate 1.9 vs 1.8 combat. Players can switch between modesets via command, choosing from the modesets allowed for the world they are in.

## Configurable Features
Features are grouped in `module`s as listed below, and can be individually configured and disabled. Modules that are fully disabled will have no impact on server performance.

#### ⚔ Combat
- Attack cooldown
- Attack frequency
- Tool damage
- Critical hits
- Player regen

#### 🤺 Armour
- Armour strength
- Armour durability

#### 🛡 Swords & Shields
- Sword blocking
- Shield damage reduction
- Sword sweep

#### 🌬 Knockback
- Player knockback
- Fishing knockback
- Fishing rod velocity
- Projectile knockback

#### 🧙 Gapples & Potions
- Golden apple crafting & effects
- Potion effects & duration
- Chorus fruit

#### ❌ New feature disabling
- Item crafting
- Offhand
- New attack sounds
- Enderpearl cooldown
- Brewing stand refuel
- Enchantment table auto-lapis
- Burn delay

## 🔌 Plugin Compatibility
Most plugins will work fine with OCM. Some are explicitly supported, including:
- Placeholder API (see [wiki](https://github.com/kernitus/BukkitOldCombatMechanics/wiki/PlaceholderAPI) for details)

## 🤝 Contributions

If you are interested in contributing, please [check this page first](.github/CONTRIBUTING.md).
<hr/>

<a href="https://hangar.papermc.io/kernitus/OldCombatMechanics">
    <img src="res/paper.png" alt="Paper" height="100">
</a>
<a href="https://www.spigotmc.org/resources/19510/">
    <img src="res/spigot.png" alt="Spigot" height="100">
</a>
<a href="https://dev.bukkit.org/projects/oldcombatmechanics">
    <img src="res/bukkit.png" alt="Bukkit" height="100">
</a>
<a href="https://ci.rayzr.dev/job/OldCombatMechanics/">
    <img src="res/jenkins.png" alt="Jenkins" height="100">
</a>

<hr/>


<a href="https://bstats.org/plugin/bukkit/OldCombatMechanics">
    <img src="https://bstats.org/signatures/bukkit/OldCombatMechanics.svg" alt="bStats">
</a>

If you want to contribute to TrueOG Network, join our [Discord](https://discord.gg/ma9pMYpBU6).
