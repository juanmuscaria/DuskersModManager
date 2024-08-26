package com.juanmuscaria.tooling.dmm


import org.gradle.internal.os.OperatingSystem
import org.jetbrains.annotations.NotNull

enum UpxSupportedOperatingSystems {
    WINDOWS_x86("win32", "zip"),
    WINDOWS_x64("win64", "zip"),
    LINUX_x64("amd64_linux", "tar.xz"),
    LINUX_ARM64("arm64_linux", "tar.xz"),
    LINUX_ARM("amd_linux", "tar.xz"),
    LINUX_x86("i386_linux", "tar.xz");

    @NotNull
    private final String fileSuffix
    @NotNull
    private final String extension

    private UpxSupportedOperatingSystems(String fileSuffix, String extension) {
        this.fileSuffix = fileSuffix
        this.extension = extension
    }

    @NotNull
    final String getFileSuffix() {
        return this.fileSuffix
    }

    @NotNull
    final String getExtension() {
        return this.extension
    }

    @NotNull
    static UpxSupportedOperatingSystems current() {
        var os = OperatingSystem.current()
        var arch = System.getProperty("os.arch", "")
        boolean is64 = arch.contains("64")
        if (os.isWindows()) {
            return is64 ? WINDOWS_x64 : WINDOWS_x86
        } else if (os.isLinux()) {
            var isArm = arch.contains("arm") || arch.contains("aarch")
            return is64 && isArm ? LINUX_ARM64 : (!is64 && isArm ? LINUX_ARM : (is64 ? LINUX_x64 : LINUX_x86))
        } else {
            throw new UnsupportedOperationException("Current OS '$os' is not supported.")
        }
    }
}
