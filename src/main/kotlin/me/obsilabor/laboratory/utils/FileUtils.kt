package me.obsilabor.laboratory.utils

import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption

fun getDirectory(s: String): File {
    val file = File(s)
    if (!file.exists()) {
        file.mkdir()
    }
    return file
}

fun getDirectory(f: File, s: String): File {
    val file = File(f, s)
    if (!file.exists()) {
        file.mkdir()
    }
    return file
}

fun getFile(s: String): File {
    val file = File(s)
    if (!file.exists()) {
        file.createNewFile()
    }
    return file
}

fun getFile(f: File, s: String): File {
    val file = File(f, s)
    if (!file.exists()) {
        file.createNewFile()
    }
    return file
}

fun copyFolder(source: Path, destination: Path) {
    Files.walk(source).forEach {
        runCatching {
            val dest = destination.resolve(source.relativize(it))
            if (Files.isDirectory(dest)) {
                if (!Files.exists(dest)) {
                    Files.createDirectory(dest)
                }
                return@runCatching
            }
            Files.copy(it, dest, StandardCopyOption.REPLACE_EXISTING)
        }
    }
}