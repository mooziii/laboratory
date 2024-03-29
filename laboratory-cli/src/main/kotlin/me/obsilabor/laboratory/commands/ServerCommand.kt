package me.obsilabor.laboratory.commands

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.subcommands
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.optional
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.mordant.rendering.TextColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.obsilabor.laboratory.db.JsonDatabase
import me.obsilabor.laboratory.internal.ServerEditAction
import me.obsilabor.laboratory.mainScope
import me.obsilabor.laboratory.platform.PlatformResolver
import me.obsilabor.laboratory.terminal
import me.obsilabor.laboratory.terminal.SpinnerAnimation
import me.obsilabor.laboratory.terminal.choose
import me.obsilabor.laboratory.terminal.chooseServer
import me.obsilabor.laboratory.terminal.promptYesOrNo
import me.obsilabor.laboratory.utils.OperatingSystem

class ServerCommand : CliktCommand(
    name = "server",
    help = "Manage your servers"
) {
    init {
        subcommands(Modify(), Delete(), Config(), Update())
    }

    override fun run() = Unit

    class Modify : CliktCommand(
        name = "modify",
        help = "Modify a server"
    ) {
        private val query by argument("query", help = "Name or id of the server to modify").optional()

        override fun run() {
            val server = terminal.chooseServer(query ?: "") ?: return
            val action = terminal.choose("What do you want to do?", ServerEditAction.values().map {
                it to TextColors.brightWhite(it.actionString)
            }) ?: return
            action.perform.invoke(server)
        }
    }

    class Delete : CliktCommand(
        name = "delete",
        help = "Deletes a server"
    ) {
        private val query by argument("query", help = "Name or id of the server to delete").optional()
        private val yesFlag by option(
            "-y", "--yes",
            help = "When this flag is not, the user will no longer be prompted for any actions"
        ).flag()

        override fun run() {
            val server = terminal.chooseServer(query ?: "") ?: return
            if (server != null) {
                if (terminal.promptYesOrNo(TextColors.brightRed("The server ${server.terminalString} and all its content will be deleted. Are you sure about that?"), default = false, yesFlag = this.yesFlag)) {
                    mainScope.launch {
                        val spinner = SpinnerAnimation("Deleting server ${server.terminalString}")
                        JsonDatabase.deleteServer(server)
                        server.directory.deleteRecursively()
                        spinner.stop("Deletion complete")
                    }
                } else {
                    terminal.println("Aborting.")
                }
            }
        }
    }

    class Config : CliktCommand(
        name = "config",
        help = "Edit a servers configuration file"
    ) {
        private val query by argument("query", help = "Name or id of the server to delete").optional()

        override fun run() {
            val server = terminal.chooseServer(query ?: "") ?: return
            val platform = PlatformResolver.resolvePlatform(server.platform)
            val files = platform.configurationFiles
            val file = if (files.size == 1){
                files.keys.first()
            } else {
                terminal.choose("Which configuration do you want to edit?", files.map { it.key to it.key + " (${it.value})" }) ?: return
            }
            var path = server.directory.absolutePath + "/$file"
            if (!OperatingSystem.notWindows) {
                path = path.replace("/", "\\")
            }
            mainScope.launch {
                withContext(Dispatchers.Default) {
                    val processBuilder = ProcessBuilder(me.obsilabor.laboratory.config.Config.userConfig.textEditor, path)
                    processBuilder.redirectOutput(ProcessBuilder.Redirect.INHERIT)
                    processBuilder.redirectError(ProcessBuilder.Redirect.INHERIT)
                    processBuilder.redirectInput(ProcessBuilder.Redirect.INHERIT)
                    val process = processBuilder.start()
                    process.waitFor()
                }
            }
        }
    }

    class Update : CliktCommand(
        name = "update",
        help = "Update a server to the newest version"
    ) {
        private val query by argument("query", help = "Name or id of the server to delete").optional()
        private val yesFlag by option(
            "-y", "--yes",
            help = "When this flag is not, the user will no longer be prompted for any actions"
        ).flag()

        override fun run() {
            val server = terminal.chooseServer(query ?: "") ?: return
            mainScope.launch {
                server.update(PlatformResolver.resolvePlatform(server.platform), noConfirm = yesFlag)
            }
        }
    }
}