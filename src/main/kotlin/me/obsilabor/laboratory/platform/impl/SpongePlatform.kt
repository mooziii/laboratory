package me.obsilabor.laboratory.platform.impl

import com.github.ajalt.mordant.rendering.TextColors
import me.obsilabor.laboratory.platform.IPlatform
import java.nio.file.Path

object SpongePlatform : IPlatform {
    override val name = "sponge"
    override val jarNamePattern = "TODO"
    override val coloredName = TextColors.brightYellow(name)

    override suspend fun getMcVersions(): List<String> {

    }

    override suspend fun getBuilds(mcVersion: String): List<String> {
        TODO("Not yet implemented")
    }

    override suspend fun downloadJarFile(path: Path, mcVersion: String, build: String): Boolean {
        TODO("Not yet implemented")
    }

}