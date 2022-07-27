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
import kotlin.concurrent.thread
import kotlin.io.path.absolutePathString

object QuiltPlatform : IPlatform {
    override val name = "quiltmc"
    override val jarNamePattern = "quiltmc-\$build.jar"
    override val coloredName = TextColors.blue(name)

    override suspend fun getMcVersions(): List<String> {
        return httpClient.get("https://meta.quiltmc.org/v3/versions/game").body<List<QuiltGameVersion>>().map { it.version }.reversed()
    }

    override suspend fun getBuilds(mcVersion: String): List<String> {
        return httpClient.get("https://meta.quiltmc.org/v3/versions/loader").body<List<QuiltLoaderVersion>>().map { it.version }.reversed()
    }

    override suspend fun downloadJarFile(path: Path, mcVersion: String, build: String): Boolean {
         val spinner = SpinnerAnimation("Resolving latest quilt installer")
        spinner.start()
        val downloadUrl = httpClient.get("https://meta.quiltmc.org/v3/versions/installer").body<List<QuiltInstallerVersion>>().first().url
        spinner.stop("Resolved latest quilt installer")
        downloadFile(downloadUrl, Path.of(path.toFile().parentFile.absolutePath, "quilt-installer.jar"))
        installServer(Path.of(path.toFile().parentFile.absolutePath), Path.of(path.toFile().parentFile.absolutePath, "quilt-installer.jar"), mcVersion, build)
        return true
    }

    override suspend fun installServer(workingDirectory: Path, installerJarFile: Path, mcVersion: String, build: String) {
        withContext(Dispatchers.IO) {
            ProcessBuilder(
                "java",
                "-jar",
                installerJarFile.toFile().name,
                "install",
                "server",
                mcVersion,
                build
            ).directory(workingDirectory.toFile()).start()
            val spinner = SpinnerAnimation("Waiting for quiltmc installer to download libraries")
            spinner.start()
            delay(3500)
            spinner.stop("QuiltMC hopefully installed")
            Files.copy(Path.of(workingDirectory.absolutePathString(), "server","quilt-server-launch.jar"), Path.of(Architecture.Platforms.absolutePath, "quiltmc/quiltmc-$build.jar"), StandardCopyOption.REPLACE_EXISTING)
            VanillaPlatform.downloadJarFile(Path.of(Architecture.Platforms.absolutePath, "vanilla/vanilla-$mcVersion.jar"), mcVersion, build)
        }
    }

    override suspend fun copyOtherFiles(destinationFolder: Path, mcVersion: String, build: String) {
        val quiltServerLauncherProperties = getFile(destinationFolder.toFile(), "quilt-server-launcher.properties")
        quiltServerLauncherProperties.writeText("serverJar=vanilla-$mcVersion.jar")
        Files.copy(Path.of(Architecture.Platforms.absolutePath, "vanilla/vanilla-$mcVersion.jar"), Path.of(destinationFolder.absolutePathString(), "vanilla-$mcVersion.jar"), StandardCopyOption.REPLACE_EXISTING)
        copyFolder(Path.of(Architecture.Platforms.absolutePath, "quiltmc/server/libraries"), Path.of(destinationFolder.absolutePathString(), "libraries"))
        Files.copy(Path.of(Architecture.Platforms.absolutePath, "quiltmc/server/quilt-server-launch.jar"), Path.of(destinationFolder.absolutePathString(), "server.jar"), StandardCopyOption.REPLACE_EXISTING)
    }

    @kotlinx.serialization.Serializable
    data class QuiltGameVersion(
        val version: String,
        val stable: Boolean
    )

    @kotlinx.serialization.Serializable
    data class QuiltLoaderVersion(
        val separator: String,
        val build: Int,
        val maven: String,
        val version: String
    )

    @kotlinx.serialization.Serializable
    data class QuiltInstallerVersion(
        val url: String,
        val maven: String,
        val version: String
    )
}