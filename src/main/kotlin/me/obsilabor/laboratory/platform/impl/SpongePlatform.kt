package me.obsilabor.laboratory.platform.impl

import com.github.ajalt.mordant.rendering.TextColors
import io.ktor.client.call.*
import io.ktor.client.request.*
import me.obsilabor.laboratory.httpClient
import me.obsilabor.laboratory.platform.IPlatform
import me.obsilabor.laboratory.utils.downloadFile
import java.nio.file.Path
import java.util.regex.Pattern

object SpongePlatform : IPlatform {
    override val name = "sponge"
    override val jarNamePattern = "spongevanilla-\$mcVersion-\$build.jar"
    override val coloredName = TextColors.brightYellow(name)
    private val hrefPattern = Pattern.compile("href=\"(.*?)\"", Pattern.DOTALL)

    override suspend fun getMcVersions(): List<String> {
        val html = httpClient.get("https://repo.spongepowered.org/service/rest/repository/browse/maven-releases/org/spongepowered/spongevanilla/") {
            header("Accept", "*/*")
            header("Accept-Language", "*")
            header("Accept-Encoding", "*")
        }
        val matcher = hrefPattern.matcher(html.body<String>())
        val set = mutableSetOf<String>()
        while (matcher.find()) {
            var link = matcher.group(1)
            if (!link.contains("https://") && !link.equals("../")) {
                link = link.split("-")[0]
                set.add(link)
            }
        }
        return set.toList()
    }

    override suspend fun getBuilds(mcVersion: String): List<String> {
        val html = httpClient.get("https://repo.spongepowered.org/service/rest/repository/browse/maven-releases/org/spongepowered/spongevanilla/") {
            header("Accept", "*/*")
            header("Accept-Language", "*")
            header("Accept-Encoding", "*")
        }
        val matcher = hrefPattern.matcher(html.body<String>())
        val set = mutableSetOf<String>()
        while (matcher.find()) {
            var link = matcher.group(1)
            if (!link.contains("https://") && !link.equals("../")) {
                if (link.split("-")[0] == mcVersion) {
                    val split1 = link.split("-")[1]
                    var split2 = ""
                    if (link.split("-").size == 3) {
                        split2 += "-"
                        split2 += link.split("-")[2]
                    }
                    link = split1 + split2.removeSuffix("/")
                    set.add(link)
                }
            }
        }
        return set.toList()
    }

    override suspend fun downloadJarFile(path: Path, mcVersion: String, build: String): Boolean {
        var url = "https://repo.spongepowered.org/repository/maven-releases/org/spongepowered/spongevanilla/${mcVersion}-${build}/spongevanilla-${mcVersion}-${build}"
        url += if (mcVersion != "1.12.2") {
            "-universal.jar"
        } else {
            ".jar"
        }
        println(url)
        runCatching {
            downloadFile(url, path)
        }.onFailure {
            it.printStackTrace()
            return false
        }
        return true
    }
}