package me.obsilabor.laboratory.platform.impl

import com.github.ajalt.mordant.rendering.TextColors
import io.ktor.client.call.*
import io.ktor.client.request.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import me.obsilabor.laboratory.arch.Architecture
import me.obsilabor.laboratory.httpClient
import me.obsilabor.laboratory.platform.IPlatform
import me.obsilabor.laboratory.terminal.SpinnerAnimation
import me.obsilabor.laboratory.utils.copyFolder
import me.obsilabor.laboratory.utils.downloadFile
import me.obsilabor.laboratory.utils.getFile
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import kotlin.io.path.absolutePathString

object FabricPlatform : IPlatform {
    override val name = "fabricmc"
    override val jarNamePattern = "fabricmc-\$build.jar"
    override val coloredName = TextColors.brightGreen(name)

    override suspend fun getMcVersions(): List<String> {
        return httpClient.get("https://meta.fabricmc.net/v2/versions/game").body<List<FabricGameVersion>>().map { it.version }.reversed()
    }

    override suspend fun getBuilds(mcVersion: String): List<String> {
        return httpClient.get("https://meta.fabricmc.net/v2/versions/loader").body<List<FabricLoaderVersion>>().map { it.version }.reversed()
    }

    override suspend fun downloadJarFile(path: Path, mcVersion: String, build: String): Boolean {
        val spinner = SpinnerAnimation("Resolving latest fabricmc installer")
        spinner.start()
        val downloadUrl = httpClient.get("https://meta.fabricmc.net/v2/versions/installer").body<List<FabricInstallerVersion>>().first().url
        spinner.stop("Resolved latest fabricmc installer")
        downloadFile(downloadUrl, Path.of(path.toFile().parentFile.absolutePath, "fabric-installer.jar"))
        installServer(
            Path.of(path.toFile().parentFile.absolutePath),
            Path.of(path.toFile().parentFile.absolutePath, "fabric-installer.jar"),
            mcVersion,
            build
        )
        return true
    }

    override suspend fun installServer(workingDirectory: Path, installerJarFile: Path, mcVersion: String, build: String) {
        withContext(Dispatchers.IO) {
            ProcessBuilder(
                "java",
                "-jar",
                installerJarFile.toFile().name,
                "server",
                "-mcversion",
                mcVersion,
                "-loader",
                build
            ).directory(workingDirectory.toFile()).start()
            val spinner = SpinnerAnimation("Waiting for fabricmc installer to download libraries")
            spinner.start()
            delay(3500)
            spinner.stop("FabricMC hopefully installed")
            Files.copy(Path.of(workingDirectory.absolutePathString(), "fabric-server-launch.jar"), Path.of(
                Architecture.Platforms.absolutePath, "fabricmc/fabricmc-$build.jar"), StandardCopyOption.REPLACE_EXISTING)
            VanillaPlatform.downloadJarFile(Path.of(Architecture.Platforms.absolutePath, "vanilla/vanilla-$mcVersion.jar"), mcVersion, build)
        }
    }

    override suspend fun copyOtherFiles(destinationFolder: Path, mcVersion: String, build: String) {
        val fabricServerLauncherProperties = getFile(destinationFolder.toFile(), "fabric-server-launcher.properties")
        fabricServerLauncherProperties.writeText("serverJar=vanilla-$mcVersion.jar")
        Files.copy(Path.of(Architecture.Platforms.absolutePath, "vanilla/vanilla-$mcVersion.jar"), Path.of(destinationFolder.absolutePathString(), "vanilla-$mcVersion.jar"), StandardCopyOption.REPLACE_EXISTING)
        copyFolder(Path.of(Architecture.Platforms.absolutePath, "fabricmc/libraries"), Path.of(destinationFolder.absolutePathString(), "libraries"))
        Files.copy(Path.of(Architecture.Platforms.absolutePath, "fabricmc/fabric-server-launch.jar"), Path.of(destinationFolder.absolutePathString(), "server.jar"), StandardCopyOption.REPLACE_EXISTING)
    }

    @kotlinx.serialization.Serializable
    data class FabricGameVersion(
        val version: String,
        val stable: Boolean
    )

    @kotlinx.serialization.Serializable
    data class FabricLoaderVersion(
        val separator: String,
        val build: Int,
        val maven: String,
        val version: String,
        val stable: Boolean
    )

    @kotlinx.serialization.Serializable
    data class FabricInstallerVersion(
        val url: String,
        val maven: String,
        val version: String,
        val stable: Boolean
    )
}