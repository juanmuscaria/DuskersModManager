package com.juanmuscaria.tooling.dmm

import com.juanmuscaria.tooling.dmm.file.XZArchiver
import com.juanmuscaria.tooling.dmm.task.UpxTask
import com.kichik.pecoff4j.ImageDataDirectory
import com.kichik.pecoff4j.ResourceDirectory
import com.kichik.pecoff4j.ResourceDirectoryTable
import com.kichik.pecoff4j.ResourceEntry
import com.kichik.pecoff4j.constant.ImageDataDirectoryType
import com.kichik.pecoff4j.constant.ResourceType
import com.kichik.pecoff4j.io.DataReader
import com.kichik.pecoff4j.io.DataWriter
import com.kichik.pecoff4j.io.PEParser
import com.kichik.pecoff4j.resources.GroupIconDirectory
import com.kichik.pecoff4j.resources.GroupIconDirectoryEntry
import com.kichik.pecoff4j.resources.IconImage
import com.kichik.pecoff4j.resources.VersionInfo
import com.kichik.pecoff4j.util.IconFile
import com.kichik.pecoff4j.util.PaddingType
import com.kichik.pecoff4j.util.ResourceHelper
import de.undercouch.gradle.tasks.download.Download
import org.gradle.api.Named
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.file.RelativePath
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.resources.ReadableResource
import org.gradle.api.tasks.Sync
import org.gradle.internal.os.OperatingSystem

import java.nio.file.Files
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
                    src("https://github.com/upx/upx/releases/download/v${version.get()}/upx-${version.get()}-${platform.fileSuffix}.${platform.extension}")
                    dest(layout.buildDirectory.file("upx/downloads/${os.isWindows() ? "upx.zip" : "upx.tar.xz"}"))
                    overwrite(false)
                }

                var unzipUpx = tasks.register("unzipUpx", Sync) {
                    group = "upx"
                    from(downloadUpx.map {
                        os.isWindows() ? zipTree(it.dest) : tarTree(XZArchiver.xz(resources, it.dest) as ReadableResource)
                    })
                    eachFile {
                        relativePath(new RelativePath(true, name))
                    }
                    into(layout.buildDirectory.dir("upx/exec"))
                }

                var upxExecutable = localUpxPath.orElse(unzipUpx.map { it.destinationDir.toPath().resolve("upx${os.isWindows() ? ".exe" : ""}") })

                var extension = extensions.create("upx", Extension, version, localUpxPath, upxExecutable)
                var compress = tasks.register("compress") {
                    group = "upx"
                }

                plugins.withId("org.graalvm.buildtools.native") {
                    tasks.named("nativeCompile") { nativeBuild ->
                        var compressTask = tasks.register("compress${nativeBuild.name.capitalize()}", UpxTask) {
                            it.dependsOn(nativeBuild)
                            it.inputExecutable.set(nativeBuild.outputDirectory.flatMap { it.file(nativeBuild.executableName) })
                            it.upxExecutableFile.set(upxExecutable)
                        }

                        compress.configure {
                            it.dependsOn(compressTask)
                        }

                        nativeBuild.doLast {
                            if (os.isWindows()) {
                                //var pe = PEParser.parse(nativeBuild.outputDirectory.flatMap { it.file(nativeBuild.executableName) })
//                        var pe = PEParser.parse(getLayout().getBuildDirectory().file("dmm.exe").get().asFile)
//                        var rd = Objects.requireNonNullElseGet(pe.getImageData().getResourceTable(), () -> {
//                            var directory = new ResourceDirectory()
//                            var table = new ResourceDirectoryTable()
//                            table.setMajorVersion(4)
//                            directory.setTable(table)
//                            pe.getImageData().setResourceTable()
//                            pe.getImageData().setResourceTable(directory)
//                            return directory
//                        })
//                        var icon = IconFile.read(new DataReader(Files.newInputStream(projectDir.toPath().resolve("src/main/resources/icons/DMM.ico"))))
//
//                        List<ResourceEntry> iconEntries = new ArrayList<>()
//                        for (var iconImage : icon.getImages()) {
//                            iconEntries.add(entry(iconEntries.size() + 1,
//                                    directory(entry(2057, iconImage.toByteArray()))))
//                        }
//                        rd.getEntries().add(entry(ResourceType.ICON,
//                                directory(iconEntries.toArray(ResourceEntry[]::new))))
//
//                        // add icon directory
//                        byte[] iconDirData = createIconDirectory(icon.getImages()).toByteArray()
//                        rd.getEntries().add(entry(ResourceType.GROUP_ICON,
//                                directory(entry(1, directory(entry(2057, iconDirData))))))
//
//                        pe.rebuild(PaddingType.PATTERN)
//                        pe.write(new DataWriter(getLayout().getBuildDirectory().file("dmm1.exe").get().asFile))
//                    }
                            }
                        }
                    }
                }
            }
        }
    }

    private static GroupIconDirectory createIconDirectory(IconImage[] icons) {
        GroupIconDirectory directory = new GroupIconDirectory()
        directory.setReserved(0)
        directory.setType(1)

        int id = 1
        for (IconImage icon : icons) {
            GroupIconDirectoryEntry entry = new GroupIconDirectoryEntry()
            entry.setWidth(icon.getHeader() != null ? icon.getHeader().getWidth() : 0)
            entry.setHeight(icon.getHeader() != null ? icon.getHeader().getHeight() : 0)
            entry.setColorCount(0)
            entry.setReserved(0)
            entry.setPlanes(icon.getHeader() != null ? icon.getHeader().getPlanes() : 1)
            entry.setBitCount(icon.getHeader() != null ? icon.getHeader().getBitCount() : 32)
            entry.setBytesInRes(icon.sizeOf());
            entry.setId(id++)
            directory.getEntries().add(entry)
        }

        return directory
    }

    private static ResourceEntry entry(int id, ResourceDirectory directory) {
        var entry = new ResourceEntry()
        entry.setId(id)
        entry.setDirectory(directory)
        return entry
    }

    private static ResourceEntry entry(int id, byte[] data) {
        var entry = new ResourceEntry()
        entry.setId(id)
        entry.setCodePage(1252)
        entry.setData(data)
        return entry
    }

    private static ResourceDirectory directory(ResourceEntry... entries) {
        var dir = new ResourceDirectory()
        var table = new ResourceDirectoryTable()
        table.setMajorVersion(4)
        dir.setTable(table)
        for (var entry : entries) {
            dir.getEntries().add(entry)
        }
        return dir
    }
}
