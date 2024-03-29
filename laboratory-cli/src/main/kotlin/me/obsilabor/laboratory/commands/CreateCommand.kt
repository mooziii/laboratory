package me.obsilabor.laboratory.commands

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.default
import com.github.ajalt.clikt.parameters.types.choice
import com.github.ajalt.mordant.rendering.TextColors
import kotlinx.coroutines.launch
import me.obsilabor.laboratory.arch.Server
import me.obsilabor.laboratory.arch.ServerState
import me.obsilabor.laboratory.db.JsonDatabase
import me.obsilabor.laboratory.mainScope
import me.obsilabor.laboratory.platform.PlatformResolver
import me.obsilabor.laboratory.terminal
import me.obsilabor.laboratory.terminal.SpinnerAnimation
import me.obsilabor.laboratory.terminal.awaitMemoryInput
import me.obsilabor.laboratory.terminal.promptYesOrNo
import java.awt.Desktop

class CreateCommand : CliktCommand(
    name = "create",
    help = "Creates a new server"
) {
    private val name by argument(
        "name",
        help = "The name of this server"
    )

    private val software by argument(
        "software",
        help = "The server-software that should be used for the new server"
    ).choice(ignoreCase = true, choices = PlatformResolver.platforms.keys.toTypedArray())

    private val version by argument(
        "version",
        help = "The version of minecraft to use for this server"
    ).default("latest")

    private val build by argument(
        "build",
        help = "The build of your chosen software to use for this server"
    ).default("latest")

    override fun run() {
        mainScope.launch {
            val platform = PlatformResolver.resolvePlatform(software)
            var spinner = SpinnerAnimation("Resolving minecraft versions for ${platform.name}")
            spinner.start()
            var updates = true
            var chosenVersion = version
            if (chosenVersion == "latest") {
                chosenVersion = platform.getMcVersions().last()
            } else {
                updates = false
            }
            var chosenBuild = build
            if (chosenBuild == "latest") {
                spinner.update("Resolving ${platform.name} builds")
                chosenBuild = platform.getBuilds(chosenVersion).last()
            } else {
                updates = false
            }
            spinner.stop("Resolved versions")
            val db = JsonDatabase.db
            val server = Server(
                db.internalCounter,
                name,
                true,
                true,
                mutableSetOf(),
                software,
                chosenBuild,
                chosenVersion,
                updates,
                1024,
                mutableSetOf("-Dlog4j2.formatMsgNoLookups=true"),
                if (!Desktop.isDesktopSupported()) mutableSetOf("nogui") else mutableSetOf(),
                25565,
                true,
                true,
                "java",
            )
            db.internalCounter++
            JsonDatabase.writeFile(db)
            if (terminal.promptYesOrNo("Do you want to configure your new server right now?", true)) {
                server.static = terminal.promptYesOrNo("Do you want your new server to be static?", true)
                server.automaticUpdates = terminal.promptYesOrNo("Do you want laboratory to keep your server up-to-date?", updates)
                server.maxHeapMemory = terminal.awaitMemoryInput("How much heap space do you want to give your server?", "1024M")
                server.port = terminal.prompt("Enter the port for your server", "25565")?.toIntOrNull() ?: 25565
            }
            spinner = SpinnerAnimation("Saving server configuration to database")
            spinner.start()
            JsonDatabase.registerServer(server)
            spinner.stop("Saved server configuration to database")
            if (server.state != ServerState.RUNNING) {
                if (!terminal.promptYesOrNo("Should the server be automatically started now?", true)) {
                    terminal.println(TextColors.brightGreen("Server setup complete"))
                } else {
                    server.start()
                }
            }
        }
    }
}