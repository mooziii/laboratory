package me.obsilabor.laboratory.platform.impl

import com.github.ajalt.mordant.rendering.TextColors
import io.ktor.client.call.*
import io.ktor.client.request.*
import kotlinx.serialization.SerialName
import me.obsilabor.laboratory.httpClient
import me.obsilabor.laboratory.platform.IPlatform
import me.obsilabor.laboratory.utils.downloadFile
import java.nio.file.Path

object VelocityPlatform : IPlatform {
    override val name = "velocity"
    override val jarNamePattern = "velocity-\$mcVersion-\$build.jar"
    override val coloredName = TextColors.brightCyan(name)
    override val isProxy = true
    override val configurationFiles = mapOf("velocity.toml" to "Velocity config")
    override val modsFolder = "plugins"

    override suspend fun getBuilds(mcVersion: String): List<String> {
        return httpClient.get("https://api.papermc.io/v2/projects/velocity/versions/$mcVersion").body<ProjectVersionsResponse>().builds.map { it.toString() }
    }

    override suspend fun getMcVersions(): List<String> {
        return httpClient.get("https://api.papermc.io/v2/projects/velocity/").body<ProjectResponse>().versions
    }

    override suspend fun downloadJarFile(path: Path, mcVersion: String, build: String): Boolean {
        val url = "https://api.papermc.io/v2/projects/velocity/versions/$mcVersion/builds/$build/downloads/velocity-$mcVersion-$build.jar"
        runCatching {
            downloadFile(url, path)
        }.onFailure {
            it.printStackTrace()
            return false
        }
        return true
    }

    @kotlinx.serialization.Serializable
    data class ProjectResponse(
        @SerialName("project_id") val projectId: String,
        @SerialName("project_name") val projectName: String,
        @SerialName("version_groups") val versionGroups: List<String>,
        val versions: List<String>,
    )

    @kotlinx.serialization.Serializable
    data class ProjectVersionsResponse(
        @SerialName("project_id") val projectId: String,
        @SerialName("project_name") val projectName: String,
        val version: String,
        val builds: List<Int>
    )
}