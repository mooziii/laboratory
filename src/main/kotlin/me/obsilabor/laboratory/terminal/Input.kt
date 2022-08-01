package me.obsilabor.laboratory.terminal

import com.github.ajalt.mordant.rendering.TextColors
import com.github.ajalt.mordant.terminal.Terminal
import me.obsilabor.laboratory.arch.Server
import me.obsilabor.laboratory.db.JsonDatabase

fun Terminal.promptYesOrNo(question: String, default: Boolean? = null, yesFlag: Boolean = false): Boolean {
    val keyString = if (default == null) "(y/n)" else (if (default) "(Y/n)" else "(y/N)")
    while (true) {
        print("$question $keyString ")
        if (yesFlag) {
            println("yes")
            return true
        }
        return when (readlnOrNull()?.trim()?.lowercase()) {
            "n", "no" -> false
            "y", "yes" -> true
            "" -> {
                if (default == null) continue else {
                    cursor.move { up(1) }
                    println("$question $keyString ${if (default) "yes" else "no"}")
                    default
                }
            }
            null -> {
                println()
                false
            }
            else -> continue
        }
    }
}

/**
 * @return memory in megabyte
 */
fun Terminal.awaitMemoryInput(
    message: String,
    default: String = "1024M"
): Long {
    val input = prompt("$message (Default: $default)") ?: default
    var amount = input.replace("M", "").replace("G", "").toLongOrNull() ?: 1024
    if (input.endsWith("G")) {
        amount *= 1024L
    }
    return amount
}

fun <T> Terminal.choose(
    message: String,
    entries: List<Pair<T, String>>,
): T? {
    entries.forEachIndexed { index, (_, string) ->
        if (index + 1 <= 9) {
            println(TextColors.magenta("  ${index+1} $string"))
        } else {
            println(TextColors.magenta(" ${index+1} $string"))
        }
    }
    while (true) {
        print("$message (enter the number): ")
        val input = readlnOrNull() ?: return null

        if (input.isEmpty()) {
            warning("Please enter a number")
            continue
        }

        val index = input.toIntOrNull()?.minus(1)
        if (index == null) {
            warning("'$input' isn't a number")
            continue
        }

        if (index !in entries.indices) {
            warning("No such choice: '$input'")
            continue
        }

        return entries.getOrNull(index)?.first
    }
}

fun Terminal.chooseServer(query: String): Server? {
    val resolvedServer: Server
    var servers = JsonDatabase.findServer(query).toMutableSet()
    if (servers.isEmpty()) {
        val id = query.toIntOrNull()
        if (id == null) {
            if (JsonDatabase.servers.isEmpty()) {
                println(TextColors.brightRed("No server found."))
                return null
            }
            servers.addAll(JsonDatabase.servers)
        }
        val server = JsonDatabase.findServer(id ?: 0)
        if (server == null) {
            if (JsonDatabase.servers.isEmpty()) {
                println(TextColors.brightRed("No server found."))
                return null
            }
            if (query.isEmpty() || query.isBlank()) {
                servers.addAll(JsonDatabase.servers)
            }
        } else {
            servers = mutableSetOf(server)
        }
    }
    resolvedServer = choose("Which one did you mean?", servers.map {
            it to it.terminalString
        }) ?: return null
    return resolvedServer
}