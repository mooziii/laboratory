package me.obsilabor.laboratory.platform

import me.obsilabor.laboratory.platform.impl.*

object PlatformResolver {
    val platforms = hashMapOf<String, IPlatform>(
        "papermc" to PaperPlatform,
        "quiltmc" to QuiltPlatform,
        "fabricmc" to FabricPlatform,
        "legacyfabric" to LegacyFabricPlatform,
        "vanilla" to VanillaPlatform,
        "velocity" to VelocityPlatform,
        "waterfall" to WaterfallPlatform,
        "customjar" to CustomJarPlatform,
        "sponge" to SpongePlatform,
        "paper-mojmap" to MojMapPaperPlatform,
        "purpurmc" to PurpurPlatform,
        "forge" to ForgePlatform
    )

    fun resolvePlatform(input: String): IPlatform {
        return platforms[input.lowercase()] ?: throw RuntimeException("Unknown platform $input")
    }
}