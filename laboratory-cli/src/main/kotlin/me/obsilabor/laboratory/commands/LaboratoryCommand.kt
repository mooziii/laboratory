package me.obsilabor.laboratory.commands

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.subcommands

class LaboratoryCommand : CliktCommand(
    name = "laboratory",
    help = "The root command of laboratory"
) {
    init {
        subcommands(
            CreateCommand(),
            ClearCommand(),
            StartCommand(),
            ListCommand(),
            TemplateCommand(),
            ServerCommand(),
            InfoCommand(),
            BackupCommand(),
            StopCommand(),
            RestartCommand(),
            ModrinthCommand(),
            ExecuteCommand(),
            AttachCommand()
        )
    }

    override fun aliases(): Map<String, List<String>> {
        return mapOf(
            "rm" to listOf("server", "delete"),
            "del" to listOf("server", "delete"),
            "modify" to listOf("server", "modify")
        )
    }

    override fun run() = Unit
}