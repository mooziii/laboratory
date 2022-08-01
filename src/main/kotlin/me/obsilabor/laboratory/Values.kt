package me.obsilabor.laboratory

import com.github.ajalt.mordant.terminal.Terminal
import io.ktor.client.HttpClient
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.serialization.json.Json

val terminal = Terminal()

val json = Json {
    prettyPrint = true
    ignoreUnknownKeys = true
}

lateinit var mainScope: CoroutineScope

val httpClient = HttpClient {
    install(ContentNegotiation) {
        json(json)
    }
}

const val VERSION = "0.0.1"