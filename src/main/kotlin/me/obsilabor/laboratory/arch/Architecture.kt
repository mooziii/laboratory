package me.obsilabor.laboratory.arch

import me.obsilabor.laboratory.platform.IPlatform
import me.obsilabor.laboratory.utils.getDirectory
import java.io.File
import java.nio.file.Path

object Architecture {

    fun setupArchitecture() {
        Root
        Live
        Servers
        Containers
        Platforms
        Vanilla
        Templates
    }

    val Root by lazy { getDirectory(System.getProperty("user.home") + "/laboratory") }
    val Database by lazy { File(Root, "db.json") }
    val Live by lazy { getDirectory(Root, "live") }
    val Servers by lazy { getDirectory(Live, "servers") }
    val Containers by lazy { getDirectory(Root, "containers") }
    val Platforms by lazy { getDirectory(Containers, "platforms") }
    val Vanilla by lazy { getDirectory(Platforms, "vanilla") }
    val Templates by lazy { getDirectory(Containers, "templates") }

    suspend fun findOrCreateJar(platform: IPlatform, version: String, build: String): Path {
        val platformFolder = getDirectory(Platforms, platform.name)
        val file = File(platformFolder, platform.jarNamePattern.replace("\$mcVersion", version).replace("\$build", build))
        if (!file.exists()) {
            platform.downloadJarFile(file.toPath(), version, build)
        }
        return file.toPath()
    }
}