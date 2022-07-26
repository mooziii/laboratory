package me.obsilabor.laboratory.utils

import com.github.ajalt.mordant.rendering.TextColors
import io.ktor.client.call.*
import io.ktor.client.plugins.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import me.obsilabor.laboratory.mainScope
import me.obsilabor.laboratory.httpClient
import me.obsilabor.laboratory.terminal
import java.nio.file.Path
import kotlin.math.roundToInt

suspend fun downloadFile(url: String, destination: Path) {
    downloadFile(url, destination, 1, 1)
}

suspend fun downloadFile(url: String, destination: Path, current: Int, total: Int) {
    val downloadContent = httpClient.get(url) {
        onDownload { bytesSentTotal, contentLength ->
            val progress = bytesSentTotal.toDouble() / contentLength.toDouble()
            val hashtags = (progress * 30).roundToInt()
            val percentage = (progress * 100).roundToInt()
            mainScope.launch(Dispatchers.IO) {
                val string = buildString {
                    append("Downloading ${destination.toFile().name} [")
                    repeat(hashtags) {
                        append(TextColors.brightGreen("#"))
                    }
                    repeat(30 - hashtags) {
                        append(' ')
                    }
                    append("] ${percentage}%")
                    if (total > 1) {
                        append(" ($current/$total)")
                    }
                }
                terminal.print("\r  $string")
            }.join()
        }
    }.body<HttpResponse>().readBytes()
    terminal.println()
    destination.toFile().writeBytes(downloadContent)
}