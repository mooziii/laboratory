package me.obsilabor.laboratory.commands

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.subcommands
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.optional
import com.github.ajalt.mordant.rendering.TextColors
import kotlinx.coroutines.launch
import me.obsilabor.laboratory.db.JsonDatabase
import me.obsilabor.laboratory.internal.ServerEditAction
import me.obsilabor.laboratory.mainScope
import me.obsilabor.laboratory.terminal
import me.obsilabor.laboratory.terminal.SpinnerAnimation
import me.obsilabor.laboratory.terminal.choose
import me.obsilabor.laboratory.terminal.chooseServer
import me.obsilabor.laboratory.terminal.promptYesOrNo

class ServerCommand : CliktCommand(
    name = "server",
    help = "Manage your servers"
) {
    init {
        subcommands(Modify(), Delete())
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

        override fun run() {
            val server = terminal.chooseServer(query ?: "") ?: return
            if (server != null) {
                if (terminal.promptYesOrNo(TextColors.brightRed("The server ${server.terminalString} and all its content will be deleted. Are you sure about that?"), default = false)) {
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
}