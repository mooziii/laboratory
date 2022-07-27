package me.obsilabor.laboratory.platform

import me.obsilabor.laboratory.platform.impl.PaperPlatform
import me.obsilabor.laboratory.platform.impl.QuiltPlatform

object PlatformResolver {
    fun resolvePlatform(input: String): IPlatform {
        return when(input) {
            "papermc" -> PaperPlatform
            "quiltmc" -> QuiltPlatform
            else -> throw RuntimeException("Unknown platform $input")
        }
    }
}