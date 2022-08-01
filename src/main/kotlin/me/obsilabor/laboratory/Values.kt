package me.obsilabor.laboratory

import com.github.ajalt.mordant.terminal.Terminal
import io.ktor.client.HttpClient
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.serialization.kotlinx.xml.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.serialization.json.Json
import nl.adaptivity.xmlutil.XmlDeclMode
import nl.adaptivity.xmlutil.serialization.XML

val terminal = Terminal()

val json = Json {
    prettyPrint = true
    ignoreUnknownKeys = true
}

lateinit var mainScope: CoroutineScope

val httpClient = HttpClient {
    install(ContentNegotiation) {
        json(json)
        xml(format = XML {
            xmlDeclMode = XmlDeclMode.Charset
        })
    }
}

const val VERSION = "0.0.1"