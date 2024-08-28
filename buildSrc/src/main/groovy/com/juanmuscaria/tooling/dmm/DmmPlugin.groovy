package com.juanmuscaria.tooling.dmm

import com.juanmuscaria.tooling.dmm.file.XZArchiver
import com.juanmuscaria.tooling.dmm.task.UpxTask
import de.undercouch.gradle.tasks.download.Download
import org.gradle.api.Named
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.file.RelativePath
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.resources.ReadableResource
import org.gradle.api.tasks.Sync
import org.gradle.internal.os.OperatingSystem

import java.nio.file.Path

class DmmPlugin implements Plugin<Project> {
    static class Extension implements Named {
        Property<String> version
        Property<Path> localUpxPath
        Provider<Path> upxExecutableProvider

        Extension(Property<String> version, Property<Path> localUpxPath, Provider<Path> upxExecutableProvider) {
            this.version = version
            this.localUpxPath = localUpxPath
            this.upxExecutableProvider = upxExecutableProvider
        }

        @Override
        String getName() {
            return "dmm"
        }
    }

    @Override
    void apply(Project target) {
        target.with {
            plugins.withId("de.undercouch.download") {
                var os = OperatingSystem.current()
                var version = objects.property(String).convention("4.0.2")
                var localUpxPath = objects.property(Path)

                var downloadUpx = tasks.register("downloadUpx", Download) {
                    group = "upx"
                    var platform = UpxSupportedOperatingSystems.current()
                    it.src("https://github.com/upx/upx/releases/download/v${version.get()}/upx-${version.get()}-${platform.fileSuffix}.${platform.extension}")
                    it.dest(layout.buildDirectory.file("upx/downloads/${os.isWindows() ? "upx.zip" : "upx.tar.xz"}"))
                    it.overwrite(false)
                }

                var unzipUpx = tasks.register("unzipUpx", Sync) {
                    group = "upx"
                    it.from(downloadUpx.map {
                        os.isWindows() ? zipTree(it.dest) : tarTree(XZArchiver.xz(resources, it.dest) as ReadableResource)
                    })
                    it.eachFile {
                        relativePath(new RelativePath(true, name))
                    }
                    it.into(layout.buildDirectory.dir("upx/exec"))
                }

                var upxExecutable = localUpxPath.orElse(unzipUpx.map { it.destinationDir.toPath().resolve("upx${os.isWindows() ? ".exe" : ""}") })

                var extension = extensions.create("upx", Extension, version, localUpxPath, upxExecutable)
                var compress = tasks.register("compress") {
                    group = "upx"
                }

                plugins.withId("org.graalvm.buildtools.native") {
                    tasks.getByName("nativeCompile") { nativeBuild ->
                        var compressTask = tasks.register("compress${nativeBuild.name.capitalize()}", UpxTask) {
                            group = "upx"
                            it.dependsOn(nativeBuild)
                            it.inputExecutable.set(nativeBuild.outputDirectory.flatMap { it.file(nativeBuild.executableName) })
                            it.upxExecutableFile.set(upxExecutable)
                        }

                        compress.configure {
                            it.dependsOn(compressTask)
                        }
                    }
                }
            }
        }
    }
}
