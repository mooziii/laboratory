package me.obsilabor.laboratory.platform.impl

import com.github.ajalt.mordant.rendering.TextColors
import me.obsilabor.laboratory.platform.IPlatform
import me.obsilabor.laboratory.terminal
import java.nio.file.Files
import java.nio.file.Path
import kotlin.system.exitProcess

object CustomJarPlatform : IPlatform {
    override val name = "custom-jar"
    override val jarNamePattern = "custom-\$build.jar"
    override val coloredName = TextColors.cyan(name)

    override suspend fun getMcVersions(): List<String> {
        return VanillaPlatform.getMcVersions() // support everything
    }

    override suspend fun getBuilds(mcVersion: String): List<String> {
        return listOf("9", "8", "7", "6", "5", "4", "3", "2", "1")
    }

    override suspend fun downloadJarFile(path: Path, mcVersion: String, build: String): Boolean {
        val customJarPath = Path.of(terminal.prompt("Please enter the path to your custom jarfile"))
        if (Files.exists(customJarPath)) {
            Files.copy(customJarPath, path)
        } else {
            terminal.println(TextColors.brightRed("Invalid path."))
            exitProcess(0)
        }
        return true
    }
}