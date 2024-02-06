package com.juanmuscaria.dmm.ui;

import com.juanmuscaria.dmm.DialogHelper;
import com.juanmuscaria.dmm.DuskersHelper;
import com.juanmuscaria.dmm.ReportedException;
import io.micronaut.core.annotation.ReflectiveAccess;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.BorderPane;
import javafx.stage.DirectoryChooser;

import java.nio.file.Files;
import java.nio.file.Path;

@ReflectiveAccess
public class InstallerController {
    @FXML
    @ReflectiveAccess
    private ComboBox<String> folderList;
    @FXML
    @ReflectiveAccess
    private BorderPane root;
    @FXML
    @ReflectiveAccess
    private Button install;
    @FXML
    @ReflectiveAccess
    private Label installInfo;

    @FXML
    @ReflectiveAccess
    void initialize() {
        for (Path path : DuskersHelper.getPossibleDuskersFolders()) {
            folderList.getItems().add(path.toString());
        }
        folderList.valueProperty().addListener(this::onFolderChanged);
        disableInstallButton();
    }

    @FXML
    @ReflectiveAccess
    void install(ActionEvent event) {
        event.consume();
        try {
            var gamePath = Path.of(folderList.getValue()).toAbsolutePath();
            if (Files.isWritable(gamePath.getParent())) {
                DuskersHelper.uninstallModManager(gamePath);
                DuskersHelper.installModManager(gamePath);
                installInfo.setText("Mod loader installed");
                DialogHelper.infoAndWait("Mod loader installed",
                        "You may close the installer and launch the game normally");
            } else {
                throw new ReportedException("Insufficient Permission", "The installer could not write to the game folder " +
                        "due to missing permission, try running the installer as administrator instead.");
            }
        } catch (ReportedException e) {
            DialogHelper.reportAndWait(e);
        } catch (Exception e) {
            DialogHelper.reportAndWait(e, "Error during installation",
                    "An unknown error occurred during the installation");
        }
    }

    @FXML
    @ReflectiveAccess
    void selectFolder(ActionEvent event) {
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle("Select Duskers Folder");
        var path = chooser.showDialog(root.getScene().getWindow());
        if (path != null) {
            folderList.setValue(path.getAbsolutePath());
        }
        event.consume();
    }

    private void onFolderChanged(ObservableValue<? extends String> observable, String oldValue, String newValue) {
        var path = Path.of(newValue);
        if (Files.isDirectory(path)) {
            if (DuskersHelper.isInstalled(path)) {
                install.setText("Update/Reinstall");
                install.setDisable(false);
                installInfo.setText("Updates a currently installed version of the mod loader");
                return;
            } else if (Files.exists(DuskersHelper.getDuskersBinary(path))) {
                install.setText("Install");
                install.setDisable(false);
                installInfo.setText("Install the mod loader");
                return;
            }
        }
        disableInstallButton();
    }

    private void disableInstallButton() {
        install.setText("Invalid Path");
        install.setDisable(true);
        installInfo.setText("Duskers was not detected.");
    }
}
