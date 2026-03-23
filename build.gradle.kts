/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import groovy.json.JsonSlurper
import io.papermc.hangarpublishplugin.model.Platforms
import org.gradle.api.Action
import org.gradle.api.attributes.java.TargetJvmVersion
import org.gradle.api.file.FileCopyDetails
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import xyz.jpenilla.runpaper.task.RunServer
import java.io.ByteArrayOutputStream
import java.io.Closeable
import java.io.Serializable
import java.net.URI
import java.nio.file.Files
import java.security.MessageDigest

val paperVersion: List<String> =
    (property("gameVersions") as String)
        .split(",")
        .map { it.trim() }

plugins {
    `java-library`
    kotlin("jvm") version "2.3.0"
    id("com.gradleup.shadow") version "9.3.0"
    id("xyz.jpenilla.run-paper") version "3.0.2"
    idea
    eclipse
    id("io.papermc.hangar-publish-plugin") version "0.1.4"
}

// Make sure javadocs are available to IDE
idea {
    module {
        isDownloadJavadoc = true
        isDownloadSources = true
    }
}

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
    // Spigot API
    maven("https://hub.spigotmc.org/nexus/content/repositories/snapshots/")
    maven("https://oss.sonatype.org/content/repositories/snapshots")
    maven("https://oss.sonatype.org/content/repositories/central")
    // PacketEvents
    maven("https://repo.codemc.io/repository/maven-releases/")
    maven("https://repo.codemc.io/repository/maven-snapshots/")
    // Placeholder API
    maven("https://repo.extendedclip.com/content/repositories/placeholderapi/")
    // Auth library from Minecraft
    maven("https://libraries.minecraft.net/")
}

group = "kernitus.plugin.OldCombatMechanics"
version = "2.4.0" // x-release-please-version
description = "OldCombatMechanics"

java {
    toolchain {
        // We can build with Java 17 but still support MC >=1.9
        // This is because MC >=1.9 server can be run with higher Java versions
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

configurations.named("compileClasspath") {
    attributes.attribute(TargetJvmVersion.TARGET_JVM_VERSION_ATTRIBUTE, 17)
}

dependencies {
    // Shaded in by Bukkit
    compileOnly("io.netty:netty-all:4.1.130.Final")
    // Placeholder API
    compileOnly("me.clip:placeholderapi:2.11.6")
    // For BSON file serialisation
    implementation("org.mongodb:bson:5.6.2")
    // Spigot
    compileOnly("org.spigotmc:spigot-api:1.21.11-R0.1-SNAPSHOT")
    // JSR-305 annotations (javax.annotation.Nullable)
    compileOnly("com.google.code.findbugs:jsr305:3.0.2")
    // PacketEvents
    implementation("com.github.retrooper:packetevents-spigot:2.11.2")
    // XSeries
    implementation("com.github.cryptomorin:XSeries:13.6.0")

    // For ingametesting
    // Mojang mappings for NMS
    /*
    compileOnly("com.mojang:authlib:6.0.54")
    paperweight.paperDevBundle("1.19.2-R0.1-SNAPSHOT")
    // For reflection remapping
    implementation("xyz.jpenilla:reflection-remapper:0.1.3")
     */

    // Integration test dependencies
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8:2.3.0")
}

// Substitute ${pluginVersion} in plugin.yml with version defined above
class ExpandPluginVersionAction(
    private val version: String,
) : Action<FileCopyDetails>,
    Serializable {
    override fun execute(details: FileCopyDetails) {
        details.expand(mapOf("pluginVersion" to version))
    }
}

val pluginVersion = project.version.toString()
val expandPluginVersionAction = ExpandPluginVersionAction(pluginVersion)
tasks.named<Copy>("processResources") {
    inputs.property("pluginVersion", pluginVersion)
    filesMatching("plugin.yml", expandPluginVersionAction)
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
    options.release.set(8)
}

val shadowJarTask =
    tasks.named<ShadowJar>("shadowJar") {
        dependsOn("jar")
        archiveFileName.set("${project.name}.jar")
        dependencies {
            exclude(dependency("org.jetbrains.kotlin:.*"))
            relocate("com.cryptomorin.xseries", "kernitus.plugin.OldCombatMechanics.lib.xseries")
            relocate("com.github.retrooper.packetevents", "kernitus.plugin.OldCombatMechanics.lib.packetevents.api")
            relocate("io.github.retrooper.packetevents", "kernitus.plugin.OldCombatMechanics.lib.packetevents.impl")
        }
    }

// For ingametesting
/*
tasks.reobfJar {
    outputJar.set(File(buildDir, "libs/${project.name}.jar"))
}
 */

tasks.assemble {
    // For ingametesting
    // dependsOn("reobfJar")
    dependsOn("shadowJar")
}

kotlin {
    jvmToolchain(17)
}
