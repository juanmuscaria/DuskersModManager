package com.juanmuscaria.dmm.ui;

import atlantafx.base.theme.CupertinoLight;
import com.juanmuscaria.dmm.event.FXEvent;
import com.juanmuscaria.dmm.util.DialogHelper;
import io.micronaut.context.annotation.Value;
import io.micronaut.runtime.event.annotation.EventListener;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

@Singleton
public class UILoader {
    private static final Logger logger = LoggerFactory.getLogger(UILoader.class);
    @Inject
    FXMLLoader loader;
    @Value("${dmm.installer:true}")
    boolean isInstaller;

    @EventListener
    void onAppStart(FXEvent.FXStart event) {
        Application.setUserAgentStylesheet(new CupertinoLight().getUserAgentStylesheet());
        var stage = event.getPrimaryStage();
        try {
            for (int scale = 16; scale <= 256; scale = scale * 2) {
                var icon = getClass().getResourceAsStream("/icons/x%s.png".formatted(scale));
                if (icon != null) {
                    stage.getIcons().add(new Image(icon));
                } else {
                    logger.error("Missing icon x{}.png", scale);
                }
            }

            if (isInstaller) {
                loader.setLocation(
                    Objects.requireNonNull(getClass().getResource("/ui/installer.fxml"),
                        "Unable to load Installer scene"));
                stage.setTitle("Duskers Mod Manager Installer");
            } else {
                loader.setLocation(Objects.requireNonNull(getClass().getResource("/ui/duskers_launcher.fxml"),
                    "Unable to load Launcher scene"));
                stage.setTitle("Duskers Mod Manager");
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
