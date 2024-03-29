package me.obsilabor.laboratory.internal

import com.github.ajalt.mordant.rendering.TextColors
import kotlinx.coroutines.launch
import me.obsilabor.laboratory.arch.Architecture
import me.obsilabor.laboratory.arch.Server
import me.obsilabor.laboratory.db.JsonDatabase
import me.obsilabor.laboratory.platform.PlatformResolver
import me.obsilabor.laboratory.terminal
import me.obsilabor.laboratory.terminal.choose
import me.obsilabor.laboratory.utils.copyFolder
import me.obsilabor.laboratory.utils.getDirectory
import me.obsilabor.laboratory.mainScope
import me.obsilabor.laboratory.platform.impl.*
import me.obsilabor.laboratory.terminal.SpinnerAnimation
import me.obsilabor.laboratory.terminal.awaitMemoryInput
import me.obsilabor.laboratory.utils.OperatingSystem
import me.obsilabor.laboratory.utils.searchForJavaInstallations
import java.io.File
import kotlin.io.path.absolutePathString

enum class ServerEditAction(val actionString: String, val perform: (Server) -> Unit) {
    RENAME("Rename the server", perform = {
        terminal.println(TextColors.yellow("Please make sure that the server isn't running."))
        it.stop(false)
        val newName = terminal.prompt("Enter a new name for the Server")
        val oldName = it.name
        if (newName != null) {
            copyFolder(it.directory.toPath(),
               getDirectory(Architecture.Servers, "$newName-${it.id}").toPath()
            )
            it.directory.deleteRecursively()
            it.name = newName
            JsonDatabase.editServer(it)
            terminal.println(TextColors.brightGreen("The server $oldName has been renamed to $newName. In case the server was running, you have to start it again."))
        }
    }),
    COPY_TEMPLATES("Enable/disable templates", perform = {
        it.copyTemplates = !it.copyTemplates
        JsonDatabase.editServer(it)
        terminal.println(TextColors.brightGreen("The server ${it.terminalString} is no${if (it.copyTemplates) "w copying templates" else "longer using templates"}"))
    }),
    STATIC("Make the server static/dynamic", perform = {
        it.static = !it.static
        JsonDatabase.editServer(it)
        terminal.println(TextColors.brightGreen("The server ${it.terminalString} is now ${if (it.static) "static" else "dynamic"}"))
    }),
    ADD_TEMPLATE("Add a template", perform = {
        val template = terminal.choose("Please provide the template to add", Architecture.Templates.listFiles().map { t -> t.name to t.name })
        template?.let { chosenTemplate ->
            it.templates.add(chosenTemplate)
            JsonDatabase.editServer(it)
            terminal.println(TextColors.brightGreen("The template $chosenTemplate has been added to the server ${it.terminalString}"))
        }
    }),
    REMOVE_TEMPLATE("Remove a template", perform = {
        val template = terminal.choose("Please provide the template to remove", it.templates.map { t -> t to t })
        template?.let { chosenTemplate ->
            it.templates.remove(chosenTemplate)
            JsonDatabase.editServer(it)
            terminal.println(TextColors.brightRed("The template $chosenTemplate has been removed from the server ${it.terminalString}"))
        }
    }),
    PLATFORM("Migrate the servers platform", perform = {
        mainScope.launch {
            val newPlatform = terminal.choose("Please provide the new platform", PlatformResolver.platforms.filter { p -> p.key != it.platform && p.value.getMcVersions().contains(it.mcVersion) }.map { p -> p to p.value.coloredName }) ?: return@launch
            val oldPlatform = PlatformResolver.resolvePlatform(it.platform)
            if (oldPlatform.isProxy && !newPlatform.value.isProxy) {
                terminal.println(TextColors.brightRed("Cannot migrate from a proxy platform to a non-proxy platform."))
                return@launch
            }
            if (!oldPlatform.isProxy && newPlatform.value.isProxy) {
                terminal.println(TextColors.brightRed("Cannot migrate from a non-proxy platform to a proxy platform."))
                return@launch
            }
            val spinner = SpinnerAnimation("Migrating your server..")
            spinner.start()
            it.platform = newPlatform.key
            it.platformBuild = newPlatform.value.getBuilds(it.mcVersion).last()
            JsonDatabase.editServer(it)
            if (oldPlatform == PaperPlatform && (newPlatform.value == VanillaPlatform || newPlatform.value == me.obsilabor.laboratory.platform.impl.QuiltPlatform)) {
                val worldNether = getDirectory(it.directory, "world_nether")
                val worldTheEnd = getDirectory(it.directory, "world_the_end")
                val dim1 = getDirectory(File(it.directory, "DIM1"), "world/DIM1")
                val dimMinus1 = getDirectory(File(it.directory, "DIM-1"), "world/DIM-1")
                copyFolder(worldTheEnd.toPath(), dim1.toPath())
                copyFolder(worldNether.toPath(), dimMinus1.toPath())
                worldNether.deleteRecursively()
                worldTheEnd.deleteRecursively()
            }
            spinner.stop(TextColors.brightGreen("Migration completed"))
        }
    }),
    PLATFORM_BUILD("Change the platform build", perform = {
        mainScope.launch {
            val newBuild = terminal.choose("Please provide the new platform build", PlatformResolver.resolvePlatform(it.platform).getBuilds(it.mcVersion).map { it to TextColors.brightWhite(it) }) ?: return@launch
            it.platformBuild = newBuild
            JsonDatabase.editServer(it)
            terminal.println(TextColors.brightGreen("Platform build was changed successful"))
        }
    }),
    MC_VERSION("Migrate the servers mc version", perform = {
        mainScope.launch {
            val newVersion = terminal.choose("Please provide the new minecraft version", PlatformResolver.resolvePlatform(it.platform).getMcVersions().map { it to TextColors.brightWhite(it) }) ?: return@launch
            it.mcVersion = newVersion
            PLATFORM_BUILD.perform.invoke(it)
            terminal.println(TextColors.brightGreen("Minecraft version was changed successful"))
        }
    }),
    AUTOMATIC_UPDATES("Toggle automatic updates", perform = {
        it.automaticUpdates = !it.automaticUpdates
        JsonDatabase.editServer(it)
        terminal.println(TextColors.brightGreen("The server ${it.terminalString} is no${if (it.automaticUpdates) "w updating itself automatically" else " longer updating itself."}"))
    }),
    RAM("Modify the servers heap memory", perform = {
        val newRam = terminal.awaitMemoryInput("Enter the new amount of heap memory for the server", default = "${it.maxHeapMemory*2}M")
        it.maxHeapMemory = newRam
        JsonDatabase.editServer(it)
        terminal.println(TextColors.brightGreen("The max heap memory for the server ${it.terminalString} has been changed to ${newRam}M"))
    }),
    ADD_JVM_ARG("Add a jvm argument", perform = {
        mainScope.launch {
            val argument = terminal.prompt("Please enter the argument to add") ?: return@launch
            it.jvmArguments.add(argument)
            JsonDatabase.editServer(it)
            terminal.println(TextColors.brightGreen("JVM argument '$argument' added to server ${it.terminalString}"))
        }
    }),
    REMOVE_JVM_ARG("Remove a jvm argument", perform = {
        mainScope.launch {
            val argument = terminal.choose("Please enter the JVM argument to remove", it.jvmArguments.map { a -> a to a }) ?: return@launch
            it.jvmArguments.remove(argument)
            JsonDatabase.editServer(it)
            terminal.println(TextColors.brightGreen("JVM argument '$argument' removed"))
        }
    }),
    ADD_PROCESS_ARG("Add a process argument", perform = {
        mainScope.launch {
            val argument = terminal.prompt("Please enter the argument to add") ?: return@launch
            it.processArguments.add(argument)
            JsonDatabase.editServer(it)
            terminal.println(TextColors.brightGreen("Process argument '$argument' added to server ${it.terminalString}"))
        }
    }),
    REMOVE_PROCESS_ARG("Remove a process argument", perform = {
        mainScope.launch {
            val argument = terminal.choose("Please enter the process argument to remove", it.processArguments.map { a -> a to a }) ?: return@launch
            it.processArguments.remove(argument)
            JsonDatabase.editServer(it)
            terminal.println(TextColors.brightGreen("Process argument '$argument' removed"))
        }
    }),
    CHANGE_PORT("Change the servers port", perform = {
        mainScope.launch {
            val port = terminal.prompt("Please enter the new port to listen on", "25565")?.toIntOrNull() ?: return@launch
            it.port = port
            JsonDatabase.editServer(it)
            terminal.println(TextColors.brightGreen("Port of server ${it.terminalString} changed to $port"))
        }
    }),
    TOGGLE_AUTOMATIC_BACKUPS("Toggle automatic backups", perform = {
        it.backupOnUpdate = !(it.backupOnUpdate ?: true)
        JsonDatabase.editServer(it)
        terminal.println(TextColors.brightGreen("The server ${it.terminalString} is no${if (it.backupOnUpdate != false) "w creating backups when updating" else " longer creating automatic backups"}"))
    }),
    CHANGE_JAVA_VERSION("Change java version", perform = {
        it.javaCommand = "${terminal.choose("Please select a new java version", searchForJavaInstallations().map { jvm -> jvm to jvm.name })?.toPath()?.resolve("bin")?.resolve("java${if (!OperatingSystem.notWindows) ".exe" else ""}")?.absolutePathString()}"
        JsonDatabase.editServer(it)
        terminal.println(TextColors.brightGreen("Java version changed."))
    }),
    TOGGLE_AUTOMATIC_RESTARTS("Toggle automatic restarts", perform = {
        it.automaticRestarts = !(it.automaticRestarts ?: true)
        JsonDatabase.editServer(it)
        terminal.println(TextColors.brightGreen("The server ${it.terminalString} is no${if (it.backupOnUpdate != false) "w automaticially restarting" else " longer automaticially restarting"}"))
    })
}