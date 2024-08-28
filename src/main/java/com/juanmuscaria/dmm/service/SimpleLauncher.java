package com.juanmuscaria.dmm.service;

import com.juanmuscaria.dmm.util.DuskersHelper;
import com.juanmuscaria.dmm.util.LogPump;
import com.juanmuscaria.dmm.util.ReportedException;
import io.micronaut.context.annotation.Requires;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

@Singleton
@Requires(property = "dmm.installer", value = "false", defaultValue = "true")
public class SimpleLauncher {
    private static final Logger logger = LoggerFactory.getLogger(SimpleLauncher.class);
    @Inject
    public ModManager modManager;

    public Process launch(boolean modded) throws ReportedException {
        if (modded) {
            logger.info("Preparing mods");
            try {
                modManager.updateInstalledMods();
            } catch (Exception e) {
                throw new ReportedException("Mod preparation failed", "Seems like something went wrong when preparing your mods, possibly a bug on the mod manager!", e);
            }
        }

        logger.info("Launching Duskers [modded:{}]", modded);
        var pb = DuskersHelper.buildDuskersLaunchProcess(modded);
        Process process;
        try {
            process = pb.start();
        } catch (IOException e) {
            throw new ReportedException("Process launch failed", "Something went wrong when launching Duskers' process", e);
        }
        var logPump = new LogPump(process, "duskers:" + process.pid());
        logPump.start();
        return process;
    }
}
