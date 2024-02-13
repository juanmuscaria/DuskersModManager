package com.juanmuscaria.dmm;

import io.micronaut.configuration.picocli.PicocliRunner;
import io.micronaut.context.ApplicationContext;
import io.micronaut.core.annotation.ReflectiveAccess;
import jakarta.inject.Inject;
import picocli.CommandLine.Command;
import picocli.CommandLine.Help.Ansi;
import picocli.CommandLine.Option;

import java.util.concurrent.CountDownLatch;

@Command(name = "dmm", description = "...",
    mixinStandardHelpOptions = true)
public class ModManagerCommand implements Runnable {
    public static final CountDownLatch stopLatch = new CountDownLatch(1);
    @Inject
    @ReflectiveAccess
    protected ApplicationContext context;
    @Option(names = {"--no-gui", "-G"}, description = "Enables CLI mode", defaultValue = "false")
    boolean noGui;

    public static void main(String[] args) {
        PicocliRunner.run(ModManagerCommand.class, args);
    }

    public void run() {
        if (noGui) {
            System.out.println(Ansi.AUTO.string("@|red CLI mode is not implemented yet!|@"));
        } else {
            ModManagerApplication.context = context;
            ModManagerApplication.launch(ModManagerApplication.class);
        }
    }
}
