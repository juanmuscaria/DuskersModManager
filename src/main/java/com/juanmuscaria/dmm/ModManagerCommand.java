package com.juanmuscaria.dmm;

import com.juanmuscaria.dmm.service.SimpleLauncher;
import com.juanmuscaria.dmm.util.DuskersHelper;
import io.micronaut.configuration.picocli.PicocliRunner;
import io.micronaut.context.ApplicationContext;
import io.micronaut.context.annotation.Value;
import io.micronaut.core.annotation.ReflectiveAccess;
import jakarta.inject.Inject;
import lombok.SneakyThrows;
import picocli.CommandLine.Command;
import picocli.CommandLine.Help.Ansi;
import picocli.CommandLine.Option;

import java.nio.file.Path;

@Command(name = "dmm", description = "...",
    mixinStandardHelpOptions = true)
public class ModManagerCommand implements Runnable {
    @Inject
    @ReflectiveAccess
    protected ApplicationContext context;
    @Value("${dmm.installer:true}")
    boolean isInstaller;
    @Option(names = {"--no-gui", "-G"}, description = "Enables CLI mode", defaultValue = "false")
    boolean noGui;
    @Option(names = {"--modded", "-M"}, description = "Whenever to launch with mods")
    boolean modded;

    public static void main(String[] args) {
        // HACK - Kinda ugly way to detect if the installer or launcher should be executed
        System.setProperty("dmm.installer",
            System.getProperty("dmm.installer", String.valueOf(!DuskersHelper.isInstalled(Path.of(".")))));
        PicocliRunner.run(ModManagerCommand.class, args);
    }

    @SneakyThrows // Force fail on any exception
    public void run() {
        if (noGui) {
            if (isInstaller) {
                System.out.println(Ansi.AUTO.string("@|red CLI install is not supported !|@"));
            } else {
                context.getBean(SimpleLauncher.class).launch(modded).waitFor();
            }
        } else {
            System.out.println("Launching GUI toolkit (\u2060◠\u2060‿\u2060・\u2060)\u2060—\u2060☆");
            ModManagerApplication.context = context;
            ModManagerApplication.launch(ModManagerApplication.class);
        }
    }
}
