package com.juanmuscaria.tooling.dmm.task

import org.apache.commons.io.FilenameUtils
import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import org.gradle.internal.os.OperatingSystem

import java.nio.file.Path

class UpxTask extends DefaultTask {
    enum Command {
        COMPRESS(null),
        DECOMPRESS("-d"),
        TEST("-t"),
        LIST("-l");
        public final String command

        Command(String command) {
            this.command = command
        }
    }

    enum LogLevel {
        NORMAL(null),
        NO_WARNINGS("-q"),
        NO_ERRORS("-qq"),
        OFF("-qqq")
        public final String command

        LogLevel(String command) {
            this.command = command
        }
    }

    enum Overlay {
        COPY, STRIP, SKIP
    }

    abstract static class CompressionLevel implements Serializable {
        public static final CompressionLevel BEST = new CompressionLevel() {
            @Override
            String command() {
                return "--best"
            }
        }

        abstract String command();

        static class Number extends CompressionLevel {
            private final int amount

            Number(int amount) {
                this.amount = amount
            }

            @Override
            String command() {
                return "-{$amount}"
            }
        }
    }

    enum BruteLevel {
        BRUTE("--brute"), ULTRA_BRUTE("--ultra-brute")
        public final String command

        BruteLevel(String command) {
            this.command = command
        }
    }

    @InputFile
    final RegularFileProperty inputExecutable = project.objects.fileProperty()

    @OutputFile
    @Optional
    final RegularFileProperty outputExecutable = project.objects.fileProperty()
            .convention(inputExecutable.flatMap {
                var name = "${FilenameUtils.removeExtension(it.asFile.name)}-compressed${OperatingSystem.current().isWindows() ? ".exe" : ""}"
                var pathString = it.asFile.toPath().parent.resolve(name).toAbsolutePath().toString()
                return project.layout.buildDirectory.file(pathString)
            })

    @Input
    final Property<Command> command = project.objects.property(Command)
            .convention(Command.COMPRESS)

    @Input
    final Property<LogLevel> logLevel = project.objects.property(LogLevel)
            .convention(LogLevel.NORMAL)

    @Input
    final Property<Boolean> exact = project.objects.property(Boolean)
        .convention(false)

    @Input
    final Property<Overlay> overlay = project.objects.property(Overlay)
        .convention(Overlay.COPY)

    @Input
    final Property<CompressionLevel> compressionLevel = project.objects.property(CompressionLevel)
        .convention(CompressionLevel.BEST)

    @Input
    @Optional
    final Property<BruteLevel> bruteLevel = project.objects.property(BruteLevel)

    @Input
    final ListProperty<String> additionalOptions = project.objects.listProperty(String)
        .convention(Collections.emptyList())

    @InputFile
    final Property<Path> upxExecutableFile = project.objects.property(Path)

    @TaskAction
    void execute() {
        outputExecutable.get().asFile.delete()
        project.exec {
            executable = upxExecutableFile.get().toAbsolutePath().toString()
            var upxArgs = []
            command.get().command?.with { upxArgs << it }
            upxArgs << "-o" << outputExecutable.get().asFile.absolutePath
            logLevel.get().command?.with { upxArgs << it }
            if (exact.get()) upxArgs << "--exact"
            upxArgs << "--overlay=${overlay.get().name().toLowerCase()}".toString()
            upxArgs << compressionLevel.get().command()
            bruteLevel.orNull?.with { upxArgs << it.command }
            upxArgs.addAll(additionalOptions.get() )
            upxArgs << inputExecutable.get().asFile.absolutePath
            args(upxArgs)
        }
    }
}
