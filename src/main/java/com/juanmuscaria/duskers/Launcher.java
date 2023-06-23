package com.juanmuscaria.duskers;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

public class Launcher extends Application {
    @Override
    public void start(Stage stage) {
        try {
            Scene scene;

            if (checkInstall()) {
                scene = new Scene(FXMLLoader.load(
                        Objects.requireNonNull(getClass().getResource("/duskers_launcher.fxml"),
                                "Unable to load JavaFX resources")));
                stage.setTitle("Duskers Mod Manager");
            } else {
                scene = new Scene(FXMLLoader.load(
                        Objects.requireNonNull(getClass().getResource("/installer.fxml"),
                                "Unable to load JavaFX resources")));
                stage.setTitle("Duskers Mod Manager Installer");
                stage.setResizable(false);
            }
            stage.setScene(scene);
            stage.show();
            stage.setMinWidth(stage.getWidth());
            stage.setMinHeight(stage.getHeight());
        } catch (Throwable e) {
            DialogHelper.reportAndExit(e);
        }
    }

    public boolean checkInstall() {
        return Files.exists(Path.of("o_Duskers_linux_Data"));
    }

    public static void main(String[] args) {
        launch(args);
    }

    public static Path getSelfPath() {
        return Path.of(Launcher.class.getProtectionDomain()
                .getCodeSource().getLocation().getPath()).toAbsolutePath();
    }
}
