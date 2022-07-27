package me.obsilabor.laboratory.arch

import com.github.ajalt.mordant.rendering.TextColors
import com.github.ajalt.mordant.rendering.TextStyles
import kotlinx.coroutines.*
import kotlinx.serialization.Serializable
import me.obsilabor.laboratory.db.JsonDatabase
import me.obsilabor.laboratory.platform.PlatformResolver
import me.obsilabor.laboratory.terminal
import me.obsilabor.laboratory.terminal.SpinnerAnimation
import me.obsilabor.laboratory.utils.OperatingSystem
import me.obsilabor.laboratory.utils.getDirectory
import me.obsilabor.laboratory.utils.getFile
import java.awt.Dimension
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import javax.swing.JFrame
import javax.swing.JLabel
import javax.swing.WindowConstants

@Serializable
data class Server(
    val id: Int,
    var name: String,
    var static: Boolean,
    var copyTemplates: Boolean,
    val templates: List<String>,
    var platform: String,
    var platformBuild: String,
    var mcVersion: String,
    var automaticUpdates: Boolean,
    var maxHeapMemory: Long,
    var jvmArguments: List<String>,
    var processArguments: List<String>
) {
    val terminalString: String
        get() = "${TextStyles.bold(PlatformResolver.resolvePlatform(platform).coloredName)}${TextColors.white("/")}${TextStyles.bold("${TextColors.brightWhite("$name-$id ")}${TextColors.green("$mcVersion-$platformBuild")}")}"

    val directory by lazy { getDirectory(Architecture.Servers, "$name-$id") }

    suspend fun start() {
        withContext(Dispatchers.IO) {
            if (!static) {
                directory.deleteRecursively()
                directory.mkdir()
            }
            val resolvedPlatform = PlatformResolver.resolvePlatform(platform)
            if (automaticUpdates) {
                val spinner = SpinnerAnimation("Resolving latest $platform build")
                spinner.start()
                mcVersion = resolvedPlatform.getMcVersions().last()
                platformBuild = resolvedPlatform.getBuilds(mcVersion).last()
                spinner.update("Updating..")
                JsonDatabase.editServer(this@Server)
                spinner.stop("Updated your server to $platform-$mcVersion-$platformBuild")
            }
            val jar = Architecture.findOrCreateJar(resolvedPlatform, mcVersion, platformBuild)
            Files.copy(jar, Path.of(directory.absolutePath, "server.jar"), StandardCopyOption.REPLACE_EXISTING)
            resolvedPlatform.copyOtherFiles(Path.of(directory.absolutePath), mcVersion, platformBuild)
            val spinner = SpinnerAnimation("Accepting mojang EULA")
            spinner.start()
            val eula = getFile(directory, "eula.txt")
            eula.writeText("""
                # By using laboratory you automatically agree to the Mojang and Laboratory Terms of Service
                eula=true
            """.trimIndent())
            spinner.stop()
        }
        if (OperatingSystem.notWindows) {
            val args = arrayListOf(
                "screen",
                "-dmS",
                "$name-$id",
                "java",
                "-Xmx${maxHeapMemory}M",
            )
            args.addAll(jvmArguments)
            args.add("-jar")
            args.add("server.jar")
            args.addAll(processArguments)
            val process = ProcessBuilder(args).directory(directory).start()
            terminal.println("Server is now running with PID ${process.pid()}. Attach using ${TextStyles.dim(TextColors.brightWhite("screen -dr $name-$id"))}")
        } else {
            val args = arrayListOf(
                "java",
                "-Xmx${maxHeapMemory}M",
            )
            args.addAll(jvmArguments)
            args.add("-jar")
            args.add("server.jar")
            args.addAll(processArguments)
            println(args)
            val process = ProcessBuilder(args,)
                .directory(directory)
                .redirectErrorStream(true)
                .redirectOutput(ProcessBuilder.Redirect.INHERIT)
                .redirectInput(ProcessBuilder.Redirect.INHERIT)
                .start()
            val frame = JFrame("Windows is for development purposes only!")
            frame.add(JLabel("Windows doesn't support screen and shouldn't be used in production. Closing this window will result in the process being terminated."))
            frame.addWindowListener(object : WindowAdapter() {
                override fun windowClosing(e: WindowEvent) {
                    process.destroyForcibly()
                }
            })
            frame.defaultCloseOperation = WindowConstants.EXIT_ON_CLOSE
            frame.isVisible = true
            frame.size = Dimension(780, 80)
        }
    }
}
