package com.juanmuscaria.dmm.ui;

import atlantafx.base.theme.CupertinoLight;
import com.juanmuscaria.dmm.event.FXEvent;
import com.juanmuscaria.dmm.util.DialogHelper;
import com.juanmuscaria.dmm.util.DuskersHelper;
import io.micronaut.runtime.event.annotation.EventListener;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.Objects;

@Singleton
public class UILoader {
    private static final Logger logger = LoggerFactory.getLogger(UILoader.class);
    @Inject
    FXMLLoader loader;
    
    @EventListener
    void onAppStart(FXEvent.FXStart event) {
        Application.setUserAgentStylesheet(new CupertinoLight().getUserAgentStylesheet());
        var stage = event.getPrimaryStage();
        try {
            if (DuskersHelper.isInstalled(Path.of(".")) || Boolean.getBoolean("dmm.forceLauncher")) {
                loader.setLocation(Objects.requireNonNull(getClass().getResource("/ui/duskers_launcher.fxml"),
                    "Unable to load JavaFX resources"));
                stage.setTitle("Duskers Mod Manager");
            } else {
                loader.setLocation(
                    Objects.requireNonNull(getClass().getResource("/ui/installer.fxml"),
                        "Unable to load JavaFX resources"));
                stage.setTitle("Duskers Mod Manager Installer");
            }
            stage.setScene(new Scene(loader.load()));
            stage.show();
            stage.setMinWidth(stage.getWidth());
            stage.setMinHeight(stage.getHeight());
        } catch (Throwable e) {
            logger.error("Unable to start GUI", e);
            DialogHelper.reportAndExit(e);
        }
    }
}
