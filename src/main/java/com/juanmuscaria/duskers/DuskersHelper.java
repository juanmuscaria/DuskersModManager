package com.juanmuscaria.duskers;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.SystemUtils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class DuskersHelper {
    private static final Logger log = Logger.getLogger(Constants.LOGGER_NAME);
    private static final String LINUX_STEAM_PATH = ".local/share/Steam/steamapps/common/Duskers";
    private static final String WINDOWS_STEAM_PATH = "C:\\Program Files (x86)\\Steam\\steamapps\\common\\Duskers";

    public static List<Path> getPossibleDuskersFolders() {
        var paths = new ArrayList<Path>(5);

        if (SystemUtils.IS_OS_WINDOWS) {
            paths.add(Path.of(WINDOWS_STEAM_PATH));

        } else if (SystemUtils.IS_OS_LINUX) {
            var userPath = SystemUtils.getUserHome().toPath();

            var steamInstall = userPath.resolve(LINUX_STEAM_PATH);
            if (Files.isDirectory(steamInstall)) {
                paths.add(steamInstall);
            }
        }
        return paths;
    }

    public static Path getDuskersBinary(Path basePath) {
        if (SystemUtils.IS_OS_WINDOWS) {
            return basePath.resolve("Duskers.exe");
        } else if (SystemUtils.IS_OS_LINUX) {
            return basePath.resolve("Duskers_linux.x86_64");
        }
        //TODO: So macos .app is a special folder thing, it will require some special handling when making the modloader
        // will code verification prevents us from modding it?

        throw new UnsupportedOperationException("Unsuported System");
    }

    public static Path getDataFolderName(Path basePath) {
        if (SystemUtils.IS_OS_WINDOWS) {
            return basePath.resolve("Duskers_Data");
        } else if (SystemUtils.IS_OS_LINUX) {
            return basePath.resolve("Duskers_linux_Data");
        }
        //TODO: MacOS support

        throw new UnsupportedOperationException("Unsupported System");
    }

    public static Path getNewDataFolder(Path basePath) {
        return basePath.resolve("o_" + getDataFolderName(basePath).getFileName());
    }

    public static Path getNewDuskersBinary(Path basePath) {
        return basePath.resolve("o_" + getDuskersBinary(basePath).getFileName());
    }

    public static void uninstallModManager(Path basePath) throws IOException {
        var duskersBinary = DuskersHelper.getDuskersBinary(basePath);
        var newDuskersBinary = DuskersHelper.getNewDuskersBinary(basePath);
        var newDataFolder = DuskersHelper.getNewDataFolder(basePath);

        if (Files.isRegularFile(newDuskersBinary)) {
            Files.delete(duskersBinary);
            Files.move(newDuskersBinary, duskersBinary);
        }
        if (Files.isSymbolicLink(newDataFolder)) {
            Files.delete(newDataFolder);
        }

        Files.deleteIfExists(basePath.resolve("winhttp.dll"));
        FileUtils.deleteDirectory(basePath.resolve("doorstop_libs").toFile());
        Path bepInEx = basePath.resolve("BepInEx");
        Files.deleteIfExists(bepInEx.resolve("config").resolve("BepInEx.cfg"));
        FileUtils.deleteDirectory(bepInEx.resolve("core").toFile());
    }

    public static void installModManager(Path basePath) throws IOException, ReportedException {
        var duskersBinary = DuskersHelper.getDuskersBinary(basePath);
        var newDuskersBinary = DuskersHelper.getNewDuskersBinary(basePath);
        var dataFolder = DuskersHelper.getDataFolderName(basePath);
        var newDataFolder = DuskersHelper.getNewDataFolder(basePath);

        Files.move(duskersBinary, newDuskersBinary);
        Files.copy(Launcher.getSelfPath(), duskersBinary);
        Files.createSymbolicLink(newDataFolder, dataFolder);

        unpackLoader(basePath);
    }

    private static void unpackLoader(Path basePath) throws IOException, ReportedException {
        try (var loader = DuskersHelper.class.getResourceAsStream((SystemUtils.IS_OS_WINDOWS ? "/win.zip" : "/unix.zip"))) {
            if (loader == null) {
                throw new ReportedException("Essential Files Missing!", "If you see this message it means something went " +
                        "wrong when building the installer and it's missing important files required to install the modloader.");
            }
            unzip(loader, basePath);
        }
//        try (var modloader = DuskersHelper.class.getResourceAsStream("/modloader.dll")) {
//            if (modloader == null) {
//                throw new ReportedException("Essential Files Missing!", "If you see this message it means something went " +
//                        "wrong when building the installer and it's missing important files required to install the modloader.");
//            }
//            Files.copy(modloader, basePath.resolve("modloader.dll"));
//        }
    }

    private static void unzip(InputStream source, Path target) throws IOException {
        try (ZipInputStream zip = new ZipInputStream(source)) {
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                Path filePath = target.resolve(entry.getName()).normalize();

                if (entry.isDirectory()) {
                    Files.createDirectories(filePath);
                } else {
                    if (filePath.getParent() != null && Files.notExists(filePath.getParent())) {
                        Files.createDirectories(filePath.getParent());
                    }
                    Files.copy(zip, filePath);
                }
            }
        }
    }


    public static ProcessBuilder buildDuskersLaunchProcess(boolean modded) throws ReportedException {
        var pb = new ProcessBuilder();
        var cmd = new ArrayList<String>();
        var env = pb.environment();
        var local = Path.of(".").toAbsolutePath();
        log.info("Duskers path:" + local);

        if (SystemUtils.IS_OS_WINDOWS) {
            cmd.addAll(Arrays.asList("cmd", "/c",
                    getNewDuskersBinary(local).toAbsolutePath().toString()));
            writeWinConfig(modded);
        } else if (SystemUtils.IS_OS_LINUX) {
            cmd.add(getNewDuskersBinary(local).toAbsolutePath().toString());
            env.put("LD_LIBRARY_PATH", local + "/doorstop_libs:" + env.get("LD_LIBRARY_PATH"));
            env.put("LD_PRELOAD","libdoorstop_x64.so:" + env.get("LD_PRELOAD"));
            env.put("DOORSTOP_ENABLE", modded ? "TRUE" : "FALSE");
            env.put("DOORSTOP_INVOKE_DLL_PATH", local + "/BepInEx/core/BepInEx.Preloader.dll");
            env.put("DOORSTOP_CORLIB_OVERRIDE_PATH", "");
        }

        pb.redirectErrorStream(true);
        pb.command(cmd);
        return pb;
    }

    private static void writeWinConfig(boolean modded) throws ReportedException {
        try {
            Files.writeString(Path.of(".", "doorstop_config.ini"), String.format("""
                    [UnityDoorstop]
                    enabled=%b
                    targetAssembly=BepInEx\\core\\BepInEx.Preloader.dll
                    redirectOutputLog=false
                    ignoreDisableSwitch=false
                    dllSearchPathOverride=
                    """, modded), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new ReportedException("Unable to write to doorstop_config.ini", "An IO error occurred writing to" +
                    " doorstop_config.ini, ensure your user has permission to write to the game directory.", e);
        }
    }
}
