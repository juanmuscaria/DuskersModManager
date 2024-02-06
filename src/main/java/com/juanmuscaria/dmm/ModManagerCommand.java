package com.juanmuscaria.dmm;

import io.micronaut.configuration.picocli.PicocliRunner;
import org.graalvm.nativeimage.IsolateThread;
import picocli.CommandLine.Command;
import picocli.CommandLine.Help.Ansi;
import picocli.CommandLine.Option;

import org.graalvm.nativeimage.c.function.CEntryPoint;

@Command(name = "dmm", description = "...",
    mixinStandardHelpOptions = true)
public class ModManagerCommand implements Runnable {
    @Option(names = {"--no-gui", "-G"}, description = "Enables CLI mode", defaultValue = "false")
    boolean noGui;

    public static void main(String[] args) {
        PicocliRunner.run(ModManagerCommand.class, args);
    }

    @CEntryPoint(name = "dmm_main")
    public static void dmm_main(IsolateThread thread) {
        ModManagerApplication.launch(ModManagerApplication.class);
    }

    public void run() {
        if (noGui) {
            System.out.println(Ansi.AUTO.string("@|red CLI mode is not implemented yet!|@"));
        } else {
            ModManagerApplication.launch(ModManagerApplication.class);
        }
    }
}
