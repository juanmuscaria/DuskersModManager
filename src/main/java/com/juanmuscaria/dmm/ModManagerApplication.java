package com.juanmuscaria.dmm;

import com.juanmuscaria.dmm.util.DialogHelper;
import com.juanmuscaria.dmm.util.DuskersHelper;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import atlantafx.base.theme.CupertinoLight;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.Objects;

public class ModManagerApplication extends Application {
    private static final Logger logger = LoggerFactory.getLogger(ModManagerApplication.class);
    @Override
    public void start(Stage stage) {
        setUserAgentStylesheet(new CupertinoLight().getUserAgentStylesheet());
        try {
            Scene scene;
            if (DuskersHelper.isInstalled(Path.of(".")) || Boolean.getBoolean("dmm.forceLauncher")) {
                scene = new Scene(FXMLLoader.load(
                        Objects.requireNonNull(getClass().getResource("/ui/duskers_launcher.fxml"),
                                "Unable to load JavaFX resources")));
                stage.setTitle("Duskers Mod Manager");
            } else {
                scene = new Scene(FXMLLoader.load(
                        Objects.requireNonNull(getClass().getResource("/ui/installer.fxml"),
                                "Unable to load JavaFX resources")));
                stage.setTitle("Duskers Mod Manager Installer");
            }
            stage.setScene(scene);
            stage.show();
            stage.setMinWidth(stage.getWidth());
            stage.setMinHeight(stage.getHeight());
        } catch (Throwable e) {
            logger.error("Unable to start GUI", e);
            DialogHelper.reportAndExit(e);
        }
    }

    public static void main(String[] args) {
        launch(args);
    }

    public static Path getSelfPath() {
        return Path.of(ModManagerApplication.class.getProtectionDomain()
                .getCodeSource().getLocation().getPath()).toAbsolutePath();
    }
}
