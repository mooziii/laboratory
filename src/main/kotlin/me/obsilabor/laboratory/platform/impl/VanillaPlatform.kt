package me.obsilabor.laboratory.platform.impl

import com.github.ajalt.mordant.rendering.TextColors
import io.ktor.client.call.*
import io.ktor.client.request.*
import kotlinx.serialization.SerialName
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import me.obsilabor.laboratory.httpClient
import me.obsilabor.laboratory.platform.IPlatform
import me.obsilabor.laboratory.utils.downloadFile
import java.nio.file.Path

object VanillaPlatform : IPlatform {
    override val name: String = "vanilla"
    override val jarNamePattern = "vanilla-\$mcVersion.jar"
    override val coloredName = TextColors.brightGreen(name)

    override suspend fun getMcVersions(): List<String> {
        return httpClient.get("https://launchermeta.mojang.com/mc/game/version_manifest_v2.json").body<MojangLauncherMeta>().versions.reversed().map { it.id }
    }

    override suspend fun getBuilds(mcVersion: String): List<String> {
        return listOf("1")
    }

    override suspend fun downloadJarFile(path: Path, mcVersion: String, build: String): Boolean {
        val pistonMeta = httpClient.get("https://launchermeta.mojang.com/mc/game/version_manifest_v2.json").body<MojangLauncherMeta>().versions.first { it.id == mcVersion }
        val url = httpClient.get(pistonMeta.url).body<JsonObject>()["downloads"]?.jsonObject?.get("server")?.
            jsonObject?.get("url")?.jsonPrimitive?.toString()?.removePrefix("\"")?.removeSuffix("\"") ?: return false
        downloadFile(url, path)
        return true
    }

    @kotlinx.serialization.Serializable
    data class MojangLauncherMeta(
        @SerialName("latest") val latest: Latest,
        @SerialName("versions") val versions: List<Version>
    )

    @kotlinx.serialization.Serializable
    data class Latest(
        @SerialName("release") val release: String,
        @SerialName("snapshot") val snapshot: String
    )

    @kotlinx.serialization.Serializable
    data class Version(
        @SerialName("id") val id: String,
        @SerialName("releaseTime") val releaseTime: String,
        @SerialName("time") val time: String,
        @SerialName("type") val type: String,
        @SerialName("url") val url: String
    )
}