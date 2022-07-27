package me.obsilabor.laboratory.platform

import me.obsilabor.laboratory.platform.impl.PaperPlatform
import me.obsilabor.laboratory.platform.impl.QuiltPlatform
import me.obsilabor.laboratory.platform.impl.VanillaPlatform

object PlatformResolver {
    fun resolvePlatform(input: String): IPlatform {
        return when(input) {
            "papermc" -> PaperPlatform
            "quiltmc" -> QuiltPlatform
            "vanilla" -> VanillaPlatform
            else -> throw RuntimeException("Unknown platform $input")
        }
    }
}